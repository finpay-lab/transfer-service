package com.finpay.transfer.interfaces.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/** Create-transfer request body (money as decimal string, no floating point). */
public record CreateTransferRequest(
        @NotNull(message = "from is required") UUID from,
        @NotNull(message = "to is required") UUID to,
        @NotBlank(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive") String amount,
        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO-4217 code") String currency) {}
