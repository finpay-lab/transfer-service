package com.finpay.transfer.application.saga.port;

/** Outcome of a risk evaluation. REVIEW is treated as approved (monitored). */
public enum RiskDecision {
    APPROVED,
    REJECTED
}
