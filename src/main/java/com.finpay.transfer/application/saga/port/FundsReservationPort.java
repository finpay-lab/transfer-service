package com.finpay.transfer.application.saga.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Funds reservation step dependency (wallet-service: available→pending,
 * reservationId; transaction-flows.md). Must be idempotent by transferId — a
 * redrive returns the original reservation instead of double-reserving.
 */
public interface FundsReservationPort {

    UUID reserve(UUID transferId, UUID walletId, BigDecimal amount, String currency);
}
