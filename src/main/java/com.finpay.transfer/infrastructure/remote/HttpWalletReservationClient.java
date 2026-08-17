package com.finpay.transfer.infrastructure.remote;

import com.finpay.transfer.application.saga.port.FundsReservationPort;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.web.client.RestClient;

/**
 * HTTP adapter for the wallet-service reservation step. {@code reserve} is
 * idempotent by {@code transferId} (the wallet reuses the reservation when the
 * transfer is re-driven after a crash); {@code release} is idempotent by
 * {@code reservationId} — releasing an already-released reservation returns
 * 404 and is treated as success.
 */
public class HttpWalletReservationClient extends RemoteClientSupport implements FundsReservationPort {

    private final RestClient walletClient;

    public HttpWalletReservationClient(RestClient walletClient, RemoteClientsProperties properties) {
        super(properties.getMaxAttempts(), properties.getBackoffMillis());
        this.walletClient = walletClient;
    }

    @Override
    public UUID reserve(UUID transferId, UUID walletId, BigDecimal amount, String currency) {
        ReservationResponse response = callWithRetry("wallet reserve " + transferId, () ->
                walletClient.post()
                        .uri("/v1/wallets/reservations")
                        .body(new ReserveRequest(
                                transferId.toString(),
                                walletId.toString(),
                                amount.toPlainString(),
                                currency))
                        .retrieve()
                        .body(ReservationResponse.class));
        if (response == null || response.reservationId() == null) {
            throw new IllegalStateException("wallet-service returned no reservationId for transfer " + transferId);
        }
        return UUID.fromString(response.reservationId());
    }

    @Override
    public void release(UUID reservationId) {
        callWithRetry("wallet release " + reservationId, () -> {
            try {
                walletClient.delete()
                        .uri("/v1/wallets/reservations/{reservationId}", reservationId)
                        .retrieve()
                        .toBodilessEntity();
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
                // Already released: idempotent success.
            }
            return null;
        });
    }

    public record ReserveRequest(String transferId, String walletId, String amount, String currency) {}

    public record ReservationResponse(String reservationId) {}
}