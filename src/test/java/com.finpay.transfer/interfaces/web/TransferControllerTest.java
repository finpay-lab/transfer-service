package com.finpay.transfer.interfaces.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.finpay.transfer.application.transfer.CreateTransferCommand;
import com.finpay.transfer.application.transfer.CreateTransferResult;
import com.finpay.transfer.application.transfer.CreateTransferUseCase;
import com.finpay.common.web.filter.CorrelationIdFilter;
import com.finpay.transfer.domain.transfer.IdempotencyConflictException;
import com.finpay.transfer.domain.transfer.Transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class TransferControllerTest {

    private static final String KEY = UUID.randomUUID().toString();

    private final CreateTransferUseCase useCase = mock(CreateTransferUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TransferController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .addFilters(new CorrelationIdFilter())
                .build();
    }

    @Test
    void createTransfer_returns201WithTransferBody() throws Exception {
        when(useCase.execute(any(CreateTransferCommand.class)))
                .thenReturn(CreateTransferResult.created(existingTransfer()));

        mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"%s","to":"%s","amount":"150.00","currency":"EUR"}
                                """.formatted("a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "11223344-5566-7788-99aa-bbccddeeff00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferId").value("c3d4e5f6-0a1b-2c3d-4e5f-6a7b8c9d0e1f"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.sagaStep").value("VALIDATION"))
                .andExpect(jsonPath("$.amount").value("150.00"))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void replay_sameKey_returns200WithSameBody() throws Exception {
        when(useCase.execute(any(CreateTransferCommand.class)))
                .thenReturn(CreateTransferResult.replayed(existingTransfer()));

        mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"%s","to":"%s","amount":"150.00","currency":"EUR"}
                                """.formatted("a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "11223344-5566-7788-99aa-bbccddeeff00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value("c3d4e5f6-0a1b-2c3d-4e5f-6a7b8c9d0e1f"));
    }

    @Test
    void sameKeyDifferentPayload_returns409IdempotencyConflict() throws Exception {
        when(useCase.execute(any(CreateTransferCommand.class)))
                .thenThrow(new IdempotencyConflictException(KEY));

        mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"%s","to":"%s","amount":"999.00","currency":"EUR"}
                                """.formatted("a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "11223344-5566-7788-99aa-bbccddeeff00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void invalidAmount_returns400() throws Exception {
        mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"%s","to":"%s","amount":"0","currency":"EUR"}
                                """.formatted("a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "11223344-5566-7788-99aa-bbccddeeff00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidCurrency_returns400() throws Exception {
        mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"%s","to":"%s","amount":"10.00","currency":"eur"}
                                """.formatted("a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "11223344-5566-7788-99aa-bbccddeeff00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"%s","to":"%s","amount":"10.00","currency":"EUR"}
                                """.formatted("a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "11223344-5566-7788-99aa-bbccddeeff00")))
                .andExpect(status().isBadRequest());
    }

    private Transfer existingTransfer() {
        return Transfer.create(
                UUID.fromString("c3d4e5f6-0a1b-2c3d-4e5f-6a7b8c9d0e1f"),
                UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
                UUID.fromString("11223344-5566-7788-99aa-bbccddeeff00"),
                new BigDecimal("150.00"),
                "EUR",
                KEY,
                Instant.parse("2026-08-17T10:00:00Z"));
    }
}
