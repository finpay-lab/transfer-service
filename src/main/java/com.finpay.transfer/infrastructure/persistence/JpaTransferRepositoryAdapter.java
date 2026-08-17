package com.finpay.transfer.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.transfer.domain.transfer.SagaExecutionState;
import com.finpay.transfer.domain.transfer.SagaStep;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;
import com.finpay.transfer.domain.transfer.TransferStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed implementation of {@link TransferRepository}.
 *
 * <p>The saga step state (current step, executed step set, failure info) is
 * persisted on the transfer row — the whole saga state survives a crash and
 * can be resumed deterministically (ADR-0003).
 */
@Repository
public class JpaTransferRepositoryAdapter implements TransferRepository {

    private final TransferJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public JpaTransferRepositoryAdapter(
            TransferJpaRepository jpaRepository,
            ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transfer> findById(UUID transferId) {
        return jpaRepository.findById(transferId).map(this::toDomain);
    }

    @Override
    @Transactional
    public Transfer save(Transfer transfer) {
        jpaRepository.saveAndFlush(toEntity(transfer));
        return transfer;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transfer> findNonTerminal(int limit) {
        return jpaRepository.findByStatusOrderByUpdatedAtAsc(TransferStatus.CREATED).stream()
                .limit(limit)
                .map(this::toDomain)
                .toList();
    }

    private TransferJpaEntity toEntity(Transfer transfer) {
        SagaExecutionState execution = transfer.execution();
        return new TransferJpaEntity(
                transfer.transferId(),
                transfer.customerId(),
                transfer.sourceAccountId(),
                transfer.destinationAccountId(),
                transfer.amount(),
                transfer.currency(),
                transfer.status(),
                execution.sagaStep(),
                transfer.idempotencyKey(),
                transfer.createdAt(),
                execution.updatedAt(),
                execution.failureReason(),
                execution.failedAtStep(),
                writeSteps(execution.executedSteps()));
    }

    private Transfer toDomain(TransferJpaEntity entity) {
        return Transfer.restore(
                entity.transferId(),
                entity.customerId(),
                entity.sourceAccountId(),
                entity.destinationAccountId(),
                entity.amount(),
                entity.currency(),
                entity.idempotencyKey(),
                entity.createdAt(),
                entity.status(),
                entity.sagaStep(),
                readSteps(entity.executedSteps()),
                entity.failureReason(),
                entity.failedAtStep(),
                entity.updatedAt());
    }

    private String writeSteps(Set<SagaStep> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize saga steps", e);
        }
    }

    private Set<SagaStep> readSteps(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Set<SagaStep>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize saga steps", e);
        }
    }
}
