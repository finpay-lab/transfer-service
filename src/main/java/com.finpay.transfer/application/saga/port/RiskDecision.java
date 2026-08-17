package com.finpay.transfer.application.saga.port;

/** Risk evaluation decision (contracts/events/v1/RiskCheckCompleted.json). */
public record RiskDecision(Decision decision, int score, java.util.List<String> rulesHit) {

    public enum Decision {
        APPROVE,
        REVIEW,
        REJECT
    }

    public boolean isApproved() {
        return decision == Decision.APPROVE;
    }
}
