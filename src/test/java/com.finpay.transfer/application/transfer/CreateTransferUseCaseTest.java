package com.finpay.transfer.application.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.finpay.transfer.domain.event.TransferCreatedEvent;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateTransferUseCaseTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-12T06:30:00Z"), ZoneOffset.UTC);

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransferCreatedEventPublisher eventPublisher;

    @Test
    void creates_transfer_in_initial_saga_state() {
        CreateTransferUseCase useCase = new CreateTransferUseCase(transferRepository, eventPublisher, CLOCK);

        CreateTransferResult result = useCase.execute(new CreateTransferCommand(
                UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
                UUID.fromString("11223344-5566-7788-99aa-bbccddeeff00"),
                UUID.fromString("22334455-6677-8899-aabb-ccddeeff0011"),
                new BigDecimal("150.00"),
                "EUR",
                UUID.randomUUID().toString()));

        assertThat(result.transfer()).isNotNull();
        assertThat(result.transfer().status()).isEqualTo(TransferStatus.CREATED);
        assertThat(result.transfer().sagaStep()).isEqualTo(SagaStep.VALIDATION);
        assertThat(result.transfer().amount()).isEqualByComparingTo("150.00");
        assertThat(result.transfer().currency()).isEqualTo("EUR");
    }

    @Test
    void persists_transfer_and_publishes_created_event() {
        CreateTransferUseCase useCase = new CreateTransferUseCase(transferRepository, eventPublisher, CLOCK);

        useCase.execute(new CreateTransferCommand(
                UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
                UUID.fromString("11223344-5566-7788-99aa-bbccddeeff00"),
                UUID.fromString("22334455-6677-8899-aabb-ccddeeff0011"),
                new BigDecimal("150.00"),
                "EUR",
                "c3d4e5f6-0a1b-2c3d-4e5f-6a7b8c9d0e1f"));

        verify(transferRepository).save(any(Transfer.class));
        verify(eventPublisher).publish(any(TransferCreatedEvent.class));
    }

    @Test
    void created_event_matches_the_v1_contract_shape() {
        CreateTransferUseCase useCase = new CreateTransferUseCase(transferRepository, eventPublisher, CLOCK);

        useCase.execute(new CreateTransferCommand(
                UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
                UUID.fromString("11223344-5566-7788-99aa-bbccddeeff00"),
                UUID.fromString("22334455-6677-8899-aabb-ccddeeff0011"),
                new BigDecimal("150.00"),
                "EUR",
                "c3d4e5f6-0a1b-2c3d-4e5f-6a7b8c9d0e1f"));

        ArgumentCaptor<TransferCreatedEvent> captor = ArgumentCaptor.forClass(TransferCreatedEvent.class);
        verify(eventPublisher).publish(captor.capture());

        TransferCreatedEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo("TransferCreated");
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.occurredAt()).isEqualTo(CLOCK.instant());
        assertThat(event.partitionKey()).isEqualTo(event.payload().transferId().toString());
        assertThat(event.payload().amount()).isEqualTo("150.00");
        assertThat(event.payload().currency()).isEqualTo("EUR");
        assertThat(event.payload().status()).isEqualTo(TransferStatus.CREATED.name());
        assertThat(event.payload().sagaStep()).isEqualTo(SagaStep.VALIDATION.name());
    }
}