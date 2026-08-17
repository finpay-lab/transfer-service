package com.finpay.transfer.infrastructure.remote;

import com.finpay.transfer.application.saga.port.RiskCheckPort;
import com.finpay.transfer.application.saga.port.RiskDecision;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.web.client.RestClient;

/**
 * HTTP adapter for the RISK_CHECK step. risk-service exposes a gRPC sync
 * endpoint (finpay/risk/v1) in its own repo; until the client stubs are wired
 * here, this adapter calls the service's HTTP contract, which is a documented
 * lab shortcut. {@code transferId} is sent as the evaluation idempotency key.
 *
 * <p>Decision mapping mirrors the RiskCheckCompleted contract: APPROVE and
 * REVIEW pass (REVIEW is monitored, not blocked); REJECT fails the saga.
 */
public class HttpRiskCheckClient extends RemoteClientSupport implements RiskCheckPort {

    private final RestClient riskClient;

    public HttpRiskCheckClient(RestClient riskClient, RemoteClientsProperties properties) {
        super(properties.getMaxAttempts(), properties.getBackoffMillis());
        this.riskClient = riskClient;
    }

    @Override
    public RiskDecision evaluate(UUID transferId, UUID customerId, BigDecimal amount, String currency) {
        RiskResponse response = callWithRetry("risk evaluation " + transferId, () ->
                riskClient.post()
                        .uri("/v1/risk/evaluate")
                        .body(new RiskRequest(
                                transferId.toString(),
                                customerId.toString(),
                                amount.toPlainString(),
                                currency))
                        .retrieve()
                        .body(RiskResponse.class));
        if (response == null || response.decision() == null) {
            throw new IllegalStateException("risk-service returned no decision for transfer " + transferId);
        }
        return switch (response.decision()) {
            case "APPROVE", "REVIEW" -> RiskDecision.APPROVED;
            default -> RiskDecision.REJECTED;
        };
    }

    public record RiskRequest(String transferId, String customerId, String amount, String currency) {}

    public record RiskResponse(String decision) {}
}