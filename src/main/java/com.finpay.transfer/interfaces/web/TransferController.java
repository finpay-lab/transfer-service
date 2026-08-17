package com.finpay.transfer.interfaces.web;

import com.finpay.transfer.application.transfer.CreateTransferCommand;
import com.finpay.transfer.application.transfer.CreateTransferResult;
import com.finpay.transfer.application.transfer.CreateTransferUseCase;
import com.finpay.transfer.application.transfer.GetTransferUseCase;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transport layer only (AGENTS.md Rule 3): maps the HTTP request to the use
 * case and the result back to a response. No business logic.
 */
@RestController
@RequestMapping("/v1/transfers")
@Validated
public class TransferController {

    private static final String IDEMPOTENCY_KEY_PATTERN =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private final CreateTransferUseCase createTransferUseCase;
    private final GetTransferUseCase getTransferUseCase;

    public TransferController(
            CreateTransferUseCase createTransferUseCase,
            GetTransferUseCase getTransferUseCase) {
        this.createTransferUseCase = createTransferUseCase;
        this.getTransferUseCase = getTransferUseCase;
    }

    /**
     * Creates a transfer bound to the {@code Idempotency-Key} header
     * (AGENTS.md Rule 6). The unique constraint on the key guarantees
     * at-most-one transfer per key; the saga runs asynchronously afterwards.
     */
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = IDEMPOTENCY_KEY_PATTERN, message = "Idempotency-Key must be a UUID")
            String idempotencyKey,
            @Valid @RequestBody CreateTransferRequest request) {
        CreateTransferResult result = createTransferUseCase.execute(
                new CreateTransferCommand(
                        request.customerId(),
                        request.from(),
                        request.to(),
                        new BigDecimal(request.amount()),
                        request.currency(),
                        idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TransferResponse.from(result.transfer()));
    }

    /** Read path: current aggregate + saga step state. */
    @GetMapping("/{transferId}")
    public ResponseEntity<TransferResponse> getTransfer(
            @PathVariable UUID transferId) {
        return ResponseEntity.ok(TransferResponse.from(getTransferUseCase.execute(transferId)));
    }
}