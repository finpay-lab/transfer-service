package com.finpay.transfer.service.domain;

/** Transactional outbox port (Rule 5: persist+commit, then publish). */
public interface Outbox {
    void stage(String eventType, String aggregateId, String payload);
}
