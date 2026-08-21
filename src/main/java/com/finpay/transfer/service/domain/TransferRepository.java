package com.finpay.transfer.service.domain;

import java.util.Optional;

public interface TransferRepository {
    Optional<Transfer> find(String transferId);

    Transfer save(Transfer transfer);

    boolean idempotencyExists(String key);

    void markIdempotent(String key, String transferId);
}
