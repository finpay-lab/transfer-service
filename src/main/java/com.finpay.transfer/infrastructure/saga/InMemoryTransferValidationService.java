package com.finpay.transfer.infrastructure.saga;

import com.finpay.transfer.application.saga.port.TransferValidationPort;
import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferValidationException;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory stand-in for the validation step (customer ACTIVE, account OPEN,
 * beneficiary valid, limit checks). This is a lab seam: in production this
 * port is backed by customer/account/limit services (with
 * timeout/retry/circuit-breaker, AGENTS.md Rule 8).
 *
 * <p>The stand-in "knows" the ids it was told about; unknown customer or
 * accounts fail validation deterministically. {@link #markLimitExceeded(UUID)}
 * lets a test simulate a limit rejection at the VALIDATION step.
 */
@Component
public class InMemoryTransferValidationService implements TransferValidationPort {

    private final Set<UUID> knownCustomers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> knownAccounts = ConcurrentHashMap.newKeySet();
    private final Set<UUID> limitExceededCustomers = ConcurrentHashMap.newKeySet();

    public InMemoryTransferValidationService() {
        // None pre-registered; tests register what the saga may use.
    }

    @Override
    public void validate(Transfer transfer) throws TransferValidationException {
        if (!knownCustomers.contains(transfer.customerId())) {
            throw new TransferValidationException("Customer " + transfer.customerId() + " is not active");
        }
        if (!knownAccounts.contains(transfer.sourceAccountId())) {
            throw new TransferValidationException("Source account " + transfer.sourceAccountId() + " is not open");
        }
        if (!knownAccounts.contains(transfer.destinationAccountId())) {
            throw new TransferValidationException("Beneficiary account " + transfer.destinationAccountId() + " is not open");
        }
        if (limitExceededCustomers.contains(transfer.customerId())) {
            throw new TransferValidationException("Transfer exceeds customer " + transfer.customerId() + " limit");
        }
    }

    /** Test hook: registers a customer id that passes validation. */
    public void registerCustomer(UUID customerId) {
        knownCustomers.add(customerId);
    }

    /** Test hook: registers an account id that passes validation. */
    public void registerAccount(UUID accountId) {
        knownAccounts.add(accountId);
    }

    /** Test hook: further validations for this customer fail the limit check. */
    public void markLimitExceeded(UUID customerId) {
        limitExceededCustomers.add(customerId);
    }

    /** Test hook: resets all in-memory validation state. */
    public void clearState() {
        knownCustomers.clear();
        knownAccounts.clear();
        limitExceededCustomers.clear();
    }
}