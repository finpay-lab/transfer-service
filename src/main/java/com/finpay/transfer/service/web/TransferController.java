package com.finpay.transfer.service.web;

import com.finpay.transfer.service.domain.SagaOrchestrator;
import com.finpay.transfer.service.domain.SagaOrchestrator.Request;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/** Transport <-> use-case mapping only (Rule 3: no business logic here). */
@RestController
@RequestMapping("/v1/transfers")
public class TransferController {

    private final SagaOrchestrator orchestrator;

    public TransferController(SagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public ResponseEntity<TransferCreated> create(@RequestBody CreateTransfer body) {
        String id = orchestrator.orchestrate(new Request(
                body.idempotencyKey(), body.fromAccount(), body.toAccount(),
                body.amount(), body.currency()));
        return ResponseEntity.accepted().body(new TransferCreated(id));
    }

    public record CreateTransfer(String idempotencyKey, String fromAccount, String toAccount,
                                 BigDecimal amount, String currency) {}
    public record TransferCreated(String transferId) {}
}
