package com.finpay.transfer.infrastructure.saga;

import com.finpay.transfer.application.saga.port.FundsReservationPort;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory stand-in for the wallet-service reservation step. This is a lab
 * seam: in production this port is backed by wallet-service (with
 * timeout/retry/circuit-breaker, AGENTS.md Rule 8).
 *
 * <p>{@code reserve} is keyed by {@code transferId} — a crash-recovery redrive
 * returns the original reservation instead of double-reserving (idempotent by
 * transferId).
 */
@Component
public class InMemoryFundsReservationService implements FundsReservationPort {

    private final Map<UUID, Reservation> reservationsByTransfer = new ConcurrentHashMap<>();

    public record Reservation(
            UUID reservationId,
            UUID transferId,
            UUID walletId,
            BigDecimal amount,
            String currency) {}

    @Override
    public UUID reserve(UUID transferId, UUID walletId, BigDecimal amount, String currency) {
        return reservationsByTransfer
                .computeIfAbsent(transferId, id -> new Reservation(
                        UUID.randomUUID(), id, walletId, amount, currency))
                .reservationId();
    }

    public Reservation get(UUID transferId) {
        return reservationsByTransfer.get(transferId);
    }

    /** Test hook: resets all in-memory reservations. */
    public void clearState() {
        reservationsByTransfer.clear();
    }
}