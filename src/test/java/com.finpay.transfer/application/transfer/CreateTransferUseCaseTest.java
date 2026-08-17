package com.finpay.transfer.application.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finpay.transfer.domain.event.TransferCreatedEvent;
import com.finpay.transfer.domain.transfer.DuplicateIdempotencyKeyException;
import com.finpay.transfer.domain.transfer.IdempotencyConflictException;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;
import com.finpay.transfer.domain.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateTransferUseCaseTest {

    private static final UUID FROM = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID TO = UUID.fromString("11223344-5566-7788-99aa-bbccddeeff00");
    private static final String KEY = UUID.randomUUID().toString();

    private final TransferRepository repository = mock(TransferRepository.class);
    private final TransferCreatedEventPublisher publisher = mock(TransferCreatedEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-17T10:00:00Z"), ZoneOffset.UTC);
    private final CreateTransferUseCase useCase = new CreateTransferUseCase(repository, publisher, clock);

    private CreateTransferCommand command(String key, BigDecimal amount) {
        return new CreateTransferCommand(FROM, TO, amount, "EUR", key);
    }

    @BeforeEach
    void setUp() {
        when(repository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
    }

    @Test
    void newKey_createsTransferInInitialStateAndPublishesEvent() {
        CreateTransferResult result = useCase.execute(command(KEY, new BigDecimal("150.00")));

        assertThat(result.created()).isTrue();
        Transfer transfer = result.transfer();
        assertThat(transfer.status()).isEqualTo(TransferStatus.CREATED);
        assertThat(transfer.sagaStep()).isEqualTo(SagaStep.VALIDATION);
        assertThat(transfer.idempotencyKey()).isEqualTo(KEY);
        assertThat(transfer.amount()).isEqualByComparingTo(new BigDecimal("150.00"));

        verify(repository).save(transfer);
        verify(publisher).publish(any(TransferCreatedEvent.class));
    }

    @Test
    void sameKeySamePayload_replaysExistingTransferWithoutSavingOrPublishing() {
        Transfer existing = Transfer.create(
                UUID.randomUUID(), FROM, TO, new BigDecimal("150.00"), "EUR", KEY,
                Instant.parse("2026-08-17T09:00:00Z"));
        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

        CreateTransferResult result = useCase.execute(command(KEY, new BigDecimal("150")));

        assertThat(result.created()).isFalse();
        assertThat(result.transfer()).isSameAs(existing);
        verify(repository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void sameKeyDifferentPayload_throwsIdempotencyConflict() {
        Transfer existing = Transfer.create(
                UUID.randomUUID(), FROM, TO, new BigDecimal("150.00"), "EUR", KEY,
                Instant.parse("2026-08-17T09:00:00Z"));
        when(repository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute(command(KEY, new BigDecimal("999.00"))))
                .isInstanceOf(IdempotencyConflictException.class);

        verify(repository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void duplicateSaveRace_propagatesDuplicateIdempotencyKeyException() {
        when(repository.save(any())).thenThrow(new DuplicateIdempotencyKeyException(KEY));

        assertThatThrownBy(() -> useCase.execute(command(KEY, new BigDecimal("150.00"))))
                .isInstanceOf(DuplicateIdempotencyKeyException.class);

        verify(publisher, never()).publish(any());
    }
}
