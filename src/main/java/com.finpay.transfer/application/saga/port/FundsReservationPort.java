package com.finpay.transfer.application.saga.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port for the wallet-service reservation step (SAGA command, ADR-0003).
 *
 * <p>Contract:
 * <ul>
 *   <li>{@link #reserve} is idempotent by {@code transferId}: re-invoking it
 *       for the same transfer (crash-recovery redrive) returns the same
 *       reservation instead of double-reserving.</li>
 *   <li>{@link #release} is idempotent by {@code reservationId}: releasing an
 *       already-released reservation is a no-op.</li>
 * </ul>
 */
public interface FundsReservationPort {

    /** Reserves funds for the transfer; returns the reservation reference. */
    UUID reserve(UUID transferId, UUID walletId, BigDecimal amount, String currency);

    /** Releases a previously created reservation (compensation step). */
    void release(UUID reservationId);
}
