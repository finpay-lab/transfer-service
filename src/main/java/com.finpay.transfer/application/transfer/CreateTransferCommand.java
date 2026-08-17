package com.finpay.transfer.application.transfer;

import java.math.BigDecimal;
import java.util.UUID;

/** Input to {@link CreateTransferUseCase}. */
public record CreateTransferCommand(
        UUID customerId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        String idempotencyKey) {}
