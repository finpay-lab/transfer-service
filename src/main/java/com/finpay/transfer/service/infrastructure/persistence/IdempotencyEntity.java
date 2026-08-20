package com.finpay.transfer.service.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transfer_idempotency")
public class IdempotencyEntity {
    @Id
    private String idempotencyKey;
    private String transferId;
    public IdempotencyEntity() {}
    public IdempotencyEntity(String k, String t) { this.idempotencyKey = k; this.transferId = t; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String v) { this.idempotencyKey = v; }
    public String getTransferId() { return transferId; }
    public void setTransferId(String v) { this.transferId = v; }
}
