package com.finpay.transfer.application.transfer;

import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory {@link TransferRepository} for unit tests (no DB needed). */
public class FakeTransferRepository implements TransferRepository {

    private final Map<UUID, Transfer> byId = new LinkedHashMap<>();

    @Override
    public Optional<Transfer> findById(UUID transferId) {
        return Optional.ofNullable(byId.get(transferId));
    }

    @Override
    public Transfer save(Transfer transfer) {
        byId.put(transfer.transferId(), transfer);
        return transfer;
    }

    @Override
    public List<Transfer> findNonTerminal(int limit) {
        return byId.values().stream()
                .filter(transfer -> !transfer.isTerminal())
                .limit(limit)
                .toList();
    }

    public Map<UUID, Transfer> all() {
        return byId;
    }
}