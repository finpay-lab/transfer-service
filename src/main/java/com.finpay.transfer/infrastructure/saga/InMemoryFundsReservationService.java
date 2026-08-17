package com.finpay.transfer.infrastructure.saga;

import com.finpay.transfer.application.saga.port.FundsReservationPort;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-memory stand-in for the wallet-service reservation step. This is a lab
 * seam: in production this port is backed by a call to wallet-service (with
 * timeout/retry/circuit-breaker, AGENTS.md Rule 8).
 *
 * <p>Both operations are idempotent and safe to retry after a crash:
 * <ul>
 *   <li>{@code reserve} is keyed by {@code transferId} — a redrive returns the
 *       original reservation instead of double-reserving.</li>
 *   <li>{@code release} is keyed by {@code reservationId} — releasing twice is
 *       a no-op.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(
        name = "finpay.saga.remote-mode",
        havingValue = "in-memory",
        matchIfMissing = true)
public class InMemoryFundsReservationService implements FundsReservationPort {

    private final Map<UUID, Reservation> reservationsByTransfer = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> released = new ConcurrentHashMap<>();

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

    @Override
    public void release(UUID reservationId) {
        released.putIfAbsent(reservationId, Boolean.TRUE);
    }

    public Reservation get(UUID transferId) {
        return reservationsByTransfer.get(transferId);
    }

    public boolean isReleased(UUID reservationId) {
        return released.containsKey(reservationId);
    }

    /** Test hook: resets all in-memory reservations/releases. */
    public void clearState() {
        reservationsByTransfer.clear();
        released.clear();
    }
}