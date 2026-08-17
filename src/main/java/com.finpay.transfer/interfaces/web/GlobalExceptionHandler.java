package com.finpay.transfer.interfaces.web;

import com.finpay.common.web.error.ErrorCode;
import com.finpay.common.web.error.ProblemDetail;
import com.finpay.transfer.domain.TransferDomainException;
import com.finpay.transfer.domain.transfer.DuplicateIdempotencyKeyException;
import com.finpay.transfer.domain.transfer.IdempotencyConflictException;
import com.finpay.transfer.domain.transfer.IllegalTransferStateTransitionException;

import java.util.Map;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingRequestHeaderException;

/** Maps exceptions to the shared FinPay error envelope (common-web ProblemDetail). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({IdempotencyConflictException.class, DuplicateIdempotencyKeyException.class})
    public ResponseEntity<ProblemDetail> handleIdempotencyConflict(TransferDomainException ex) {
        return error(HttpStatus.CONFLICT, ErrorCode.IDEMPOTENCY_CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalTransferStateTransitionException.class)
    public ResponseEntity<ProblemDetail> handleIllegalTransition(TransferDomainException ex) {
        return error(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION, ex.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        ConstraintViolationException.class,
        MethodArgumentTypeMismatchException.class,
        MissingRequestHeaderException.class,
        HttpMessageNotReadableException.class,
        NumberFormatException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex) {
        // ErrorCode has no validation entry yet; use a stable literal code.
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", messageOf(ex));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Unexpected server error");
    }

    private ResponseEntity<ProblemDetail> error(HttpStatus status, ErrorCode code, String message) {
        return error(status, code.name(), message);
    }

    private ResponseEntity<ProblemDetail> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ProblemDetail(status.value(), code, message, traceId(), Map.of()));
    }

    private String messageOf(Exception ex) {
        return ex.getMessage() == null ? "Invalid request" : ex.getMessage();
    }

    private String traceId() {
        return MDC.get("correlationId");
    }
}