package com.finpay.transfer.application.transfer;

import java.math.BigDecimal;
import java.util.UUID;

/** Request for idempotent transfer creation (AGENTS.md Rule 6). */
public record CreateTransferCommand(
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        String idempotencyKey) {}
