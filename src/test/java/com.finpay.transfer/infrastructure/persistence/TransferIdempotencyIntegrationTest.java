package com.finpay.transfer.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end idempotent creation against a real PostgreSQL via Testcontainers.
 * Skipped automatically when Docker is unavailable
 * (@Testcontainers(disabledWithoutDocker = true)).
 *
 * <p>Verifies the full contract (AGENTS.md Rule 6): 201 on creation, 200
 * replay for the same key/payload, 409 conflict for the same key with a
 * different payload, and exactly one outbox row for the single transfer.
 *
 * <p>The scheduled saga-recovery job is disabled so it cannot advance transfers
 * (and add outbox rows) between assertions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "transfer.saga.recovery.enabled=false")
class TransferIdempotencyIntegrationTest {

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String FROM = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final String TO = "11223344-5566-7788-99aa-bbccddeeff00";

    private String body(String amount) {
        return """
                {"from":"%s","to":"%s","amount":"%s","currency":"EUR"}
                """.formatted(FROM, TO, amount);
    }

    @Test
    void duplicateKeyIsReplayedNotDuplicated() throws Exception {
        String key = UUID.randomUUID().toString();

        MvcResult first = mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("150.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        String transferId = com.jayway.jsonpath.JsonPath.read(first.getResponse().getContentAsString(), "$.transferId");

        mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("150.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value(transferId));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transfer", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transfer_outbox", Integer.class)).isEqualTo(1);
    }

    @Test
    void sameKeyDifferentPayload_returnsConflict() throws Exception {
        String key = UUID.randomUUID().toString();

        mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("150.00")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("999.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transfer", Integer.class)).isEqualTo(1);
    }
}
