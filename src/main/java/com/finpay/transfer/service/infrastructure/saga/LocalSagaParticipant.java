package com.finpay.transfer.service.infrastructure.saga;

import com.finpay.transfer.service.domain.SagaOrchestrator;
import com.finpay.transfer.service.domain.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-process SAGA participant (FP-11). In production these would call
 * customer/limit/risk/account/wallet services over the network with
 * timeout/retry/circuit-breaker (Rule 8). For the lab they validate locally
 * and record the step; compensation handlers undo the respective reservation.
 */
@Component
public class LocalSagaParticipant implements SagaOrchestrator.SagaParticipant {

    private static final Logger log = LoggerFactory.getLogger(LocalSagaParticipant.class);

    @Override public void validate(Transfer t) {
        if (t.fromAccount() == null || t.toAccount() == null || t.fromAccount().equals(t.toAccount())) {
            throw new IllegalArgumentException("invalid transfer accounts");
        }
    }
    @Override public void checkLimit(Transfer t) { log.info("limit ok for {}", t.transferId()); }
    @Override public void checkRisk(Transfer t) { log.info("risk ok for {}", t.transferId()); }
    @Override public void reserve(Transfer t) { log.info("reserved {} for {}", t.amount(), t.transferId()); }
    @Override public void debit(Transfer t) { log.info("debited {} from {}", t.amount(), t.fromAccount()); }
    @Override public void credit(Transfer t) { log.info("credited {} to {}", t.amount(), t.toAccount()); }
    @Override public void finalize(Transfer t) { log.info("finalized {}", t.transferId()); }

    @Override public void undoReserve(Transfer t) { log.info("undo reserve {}", t.transferId()); }
    @Override public void undoDebit(Transfer t) { log.info("undo debit {}", t.transferId()); }
    @Override public void undoCredit(Transfer t) { log.info("undo credit {}", t.transferId()); }
}
