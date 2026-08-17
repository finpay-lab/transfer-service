package com.finpay.transfer.interfaces.web;

import com.finpay.transfer.application.transfer.CreateTransferCommand;
import com.finpay.transfer.application.transfer.CreateTransferResult;
import com.finpay.transfer.application.transfer.CreateTransferUseCase;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Create-transfer request body (money as decimal string, no floating point). */
public record CreateTransferRequest(
        @NotNull(message = "customerId is required") UUID customerId,
        @NotNull(message = "from is required") UUID from,
        @NotNull(message = "to is required") UUID to,
        @NotBlank(message = "amount is required")
        @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "amount must be a positive decimal with at most 2 fraction digits") String amount,
        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO-4217 code") String currency) {}