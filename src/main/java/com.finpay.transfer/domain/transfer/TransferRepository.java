package com.finpay.transfer.domain.transfer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for the transfer aggregate. Owned by transfer-service
 * (no shared database, ADR-0005 / DATA_OWNERSHIP.md). Implementation lives in
 * {@code infrastructure/} — domain code depends only on this interface
 * (AGENTS.md Rule 4).
 */
public interface TransferRepository {

    Optional<Transfer> findById(UUID transferId);

    Transfer save(Transfer transfer);

    /** Non-terminal transfers, oldest-first — the saga recovery scan set. */
    List<Transfer> findNonTerminal(int limit);
}
