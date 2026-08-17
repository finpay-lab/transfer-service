package com.finpay.transfer.interfaces.web;

import com.finpay.transfer.application.transfer.CreateTransferCommand;
import com.finpay.transfer.application.transfer.CreateTransferResult;
import com.finpay.transfer.application.transfer.CreateTransferUseCase;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transport layer only (AGENTS.md Rule 3): maps the HTTP request to the use
 * case and the result back to a response. No business logic.
 *
 * <p>The transfer is created in status CREATED/VALIDATION; the SAGA money-flow
 * steps are then driven asynchronously by the recovery job (ADR-0003), so this
 * endpoint never performs a remote call and stays idempotent.
 */
@RestController
@RequestMapping("/v1/transfers")
@Validated
public class TransferController {

    private static final String IDEMPOTENCY_KEY_PATTERN =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private final CreateTransferUseCase createTransferUseCase;

    public TransferController(CreateTransferUseCase createTransferUseCase) {
        this.createTransferUseCase = createTransferUseCase;
    }

    /**
     * Idempotent creation keyed by the {@code Idempotency-Key} header
     * (AGENTS.md Rule 6). Returns 201 on first creation and 200 with the same
     * body on a replay.
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
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(TransferResponse.from(result.transfer()));
    }
}