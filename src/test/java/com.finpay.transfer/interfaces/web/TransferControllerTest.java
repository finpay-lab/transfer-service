package com.finpay.transfer.interfaces.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.finpay.transfer.application.transfer.CreateTransferCommand;
import com.finpay.transfer.application.transfer.CreateTransferResult;
import com.finpay.transfer.application.transfer.CreateTransferUseCase;
import com.finpay.transfer.application.transfer.GetTransferUseCase;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    private static final UUID CUSTOMER = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID FROM = UUID.fromString("11223344-5566-7788-99aa-bbccddeeff00");
    private static final UUID TO = UUID.fromString("22334455-6677-8899-aabb-ccddeeff0011");

    @Mock
    private CreateTransferUseCase createTransferUseCase;

    @Mock
    private GetTransferUseCase getTransferUseCase;

    @Test
    void create_transfer_maps_request_to_use_case_and_returns_created() {
        String idempotencyKey = UUID.randomUUID().toString();
        Transfer transfer = Transfer.create(
                UUID.randomUUID(), CUSTOMER, FROM, TO,
                new BigDecimal("150.00"), "EUR", idempotencyKey,
                Instant.parse("2026-08-12T06:30:00Z"));
        when(createTransferUseCase.execute(any(CreateTransferCommand.class)))
                .thenReturn(CreateTransferResult.of(transfer));

        TransferController controller = new TransferController(createTransferUseCase, getTransferUseCase);

        ResponseEntity<TransferResponse> response = controller.createTransfer(
                idempotencyKey,
                new CreateTransferRequest(
                        CUSTOMER, FROM, TO, "150.00", "EUR"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().transferId()).isEqualTo(transfer.transferId());
        assertThat(response.getBody().amount()).isEqualTo("150.00");
        assertThat(response.getBody().currency()).isEqualTo("EUR");
        assertThat(response.getBody().status()).isEqualTo(TransferStatus.CREATED);
        assertThat(response.getBody().sagaStep()).isEqualTo(SagaStep.VALIDATION);
    }

    @Test
    void get_transfer_returns_current_aggregate_and_saga_step() {
        Transfer transfer = Transfer.create(
                UUID.randomUUID(), CUSTOMER, FROM, TO,
                new BigDecimal("75.50"), "USD", UUID.randomUUID().toString(),
                Instant.parse("2026-08-12T06:30:00Z"));
        transfer.complete(Instant.now());
        when(getTransferUseCase.execute(transfer.transferId())).thenReturn(transfer);

        TransferController controller = new TransferController(createTransferUseCase, getTransferUseCase);

        ResponseEntity<TransferResponse> response = controller.getTransfer(transfer.transferId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(response.getBody().customerId()).isEqualTo(CUSTOMER);
    }
}