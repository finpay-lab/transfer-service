package com.finpay.transfer.infrastructure.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finpay.transfer.application.saga.TransferSagaCoordinator;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;
import com.finpay.transfer.domain.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end saga compensation + crash-recovery against a real PostgreSQL via
 * Testcontainers. Skipped automatically when Docker is unavailable
 * (@Testcontainers(disabledWithoutDocker = true)).
 *
 * <p>The scheduled recovery job is disabled; tests drive the saga explicitly
 * through {@link TransferSagaCoordinator} so assertions are deterministic. A
 * "crash" is simulated by persisting a transfer in a mid-saga state (exactly
 * what the DB would contain if the process died at that point) and re-driving
 * it — proving that the persisted saga state is sufficient to resume.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "transfer.saga.recovery.enabled=false")
class TransferSagaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("transfer")
            .withUsername("transfer")
            .withPassword("transfer");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final String FROM = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final String TO = "11223344-5566-7788-99aa-bbccddeeff00";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private TransferSagaCoordinator coordinator;

    @Autowired
    private InMemoryFundsReservationService reservationService;

    @Autowired
    private InMemoryLedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService.clearState();
        reservationService.clearState();
    }

    private UUID createViaApi(String key, String to) throws Exception {
        String response = mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"%s","to":"%s","amount":"150.00","currency":"EUR"}
                                """.formatted(FROM, to)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(response, "$.transferId"));
    }

    @Test
    void createViaApi_thenRecovery_drivesSagaToCompleted() throws Exception {
        UUID transferId = createViaApi(UUID.randomUUID().toString(), TO);

        coordinator.run(transferId);

        assertThat(queryStatus(transferId)).isEqualTo(TransferStatus.COMPLETED.name());
        assertThat(querySagaStep(transferId)).isEqualTo(SagaStep.FINALIZATION.name());
        assertThat(reservationService.get(transferId)).isNotNull();
        assertThat(ledgerService.hasPosting(transferId, "DEBIT")).isTrue();
        assertThat(ledgerService.hasPosting(transferId, "CREDIT")).isTrue();

        assertThat(outboxEventTypes(transferId)).containsExactlyInAnyOrder(
                "TransferCreated", "TransferCompleted");
    }

    @Test
    void crashAfterReservation_resumesForwardWithoutReReserving() {
        UUID transferId = UUID.randomUUID();
        UUID reservationId = reservationService.reserve(transferId, UUID.fromString(FROM),
                new BigDecimal("150.00"), "EUR");
        // Persisted state as if the process died after RISK_CHECK executed.
        transferRepository.save(Transfer.restore(
                transferId,
                UUID.fromString(FROM),
                UUID.fromString(TO),
                new BigDecimal("150.00"),
                "EUR",
                UUID.randomUUID().toString(),
                Instant.parse("2026-08-17T09:00:00Z"),
                TransferStatus.CREATED,
                SagaStep.DEBIT,
                Set.of(SagaStep.VALIDATION, SagaStep.RESERVATION, SagaStep.RISK_CHECK),
                Set.of(),
                false,
                reservationId,
                null,
                null,
                Instant.parse("2026-08-17T09:01:00Z")));

        coordinator.run(transferId);

        assertThat(queryStatus(transferId)).isEqualTo(TransferStatus.COMPLETED.name());
        // The reservation was not re-created: the stored reference is unchanged
        // and exactly one reservation exists for the transfer.
        assertThat(reservationService.get(transferId).reservationId()).isEqualTo(reservationId);
        assertThat(ledgerService.hasPosting(transferId, "DEBIT")).isTrue();
        assertThat(ledgerService.hasPosting(transferId, "CREDIT")).isTrue();
    }

    @Test
    void creditToClosedAccount_compensatesInReverseAndFails() throws Exception {
        UUID closedAccount = UUID.fromString(TO);
        ledgerService.markAccountClosed(closedAccount);
        UUID transferId = createViaApi(UUID.randomUUID().toString(), TO);

        coordinator.run(transferId);

        assertThat(queryStatus(transferId)).isEqualTo(TransferStatus.FAILED.name());
        assertThat(querySagaStep(transferId)).isEqualTo(SagaStep.COMPENSATION.name());
        assertThat(queryFailureReason(transferId)).contains("closed");

        // Compensation ran: debit reversed, reservation released, no credit posted.
        assertThat(ledgerService.hasPosting(transferId, "REVERSE_DEBIT")).isTrue();
        assertThat(ledgerService.hasPosting(transferId, "CREDIT")).isFalse();
        UUID reservationId = reservationService.get(transferId).reservationId();
        assertThat(reservationService.isReleased(reservationId)).isTrue();

        assertThat(outboxEventTypes(transferId)).containsExactlyInAnyOrder(
                "TransferCreated", "TransferFailed");
    }

    @Test
    void redrivingCompletedTransfer_isIdempotent() throws Exception {
        UUID transferId = createViaApi(UUID.randomUUID().toString(), TO);

        coordinator.run(transferId);
        coordinator.run(transferId);

        assertThat(queryStatus(transferId)).isEqualTo(TransferStatus.COMPLETED.name());
        assertThat(ledgerService.hasPosting(transferId, "DEBIT")).isTrue();
        assertThat(outboxEventTypes(transferId)).containsExactlyInAnyOrder(
                "TransferCreated", "TransferCompleted");
    }

    private String queryStatus(UUID transferId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM transfer WHERE transfer_id = ?", String.class, transferId);
    }

    private String querySagaStep(UUID transferId) {
        return jdbcTemplate.queryForObject(
                "SELECT saga_step FROM transfer WHERE transfer_id = ?", String.class, transferId);
    }

    private String queryFailureReason(UUID transferId) {
        return jdbcTemplate.queryForObject(
                "SELECT failure_reason FROM transfer WHERE transfer_id = ?", String.class, transferId);
    }

    private Set<String> outboxEventTypes(UUID transferId) {
        return Set.copyOf(jdbcTemplate.queryForList(
                "SELECT event_type FROM transfer_outbox WHERE aggregate_id = ?",
                String.class, transferId));
    }
}
