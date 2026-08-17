package com.finpay.transfer.infrastructure.remote;

import com.finpay.transfer.application.saga.port.TransferValidationPort;
import com.finpay.transfer.domain.transfer.TransferValidationException;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * HTTP adapter for the VALIDATION step. Calls customer-service (customer
 * active), account-service (source + destination open/valid) and limit-service
 * (limit check). A definitive business rejection surfaces as
 * {@link TransferValidationException}; transient failures are retried
 * (Rule 8) and finally surface as {@link RemoteStepCallException}.
 */
public class HttpTransferValidationClient extends RemoteClientSupport implements TransferValidationPort {

    private static final String ACTIVE = "ACTIVE";
    private static final String OPEN = "OPEN";

    private final RestClient customerClient;
    private final RestClient accountClient;
    private final RestClient limitClient;

    public HttpTransferValidationClient(
            RestClient customerClient,
            RestClient accountClient,
            RestClient limitClient,
            RemoteClientsProperties properties) {
        super(properties.getMaxAttempts(), properties.getBackoffMillis());
        this.customerClient = customerClient;
        this.accountClient = accountClient;
        this.limitClient = limitClient;
    }

    @Override
    public void validate(ValidationRequest request) {
        requireCustomerActive(request.customerId());
        requireAccountOpen(request.sourceAccountId());
        requireAccountOpen(request.destinationAccountId());
        checkLimit(request);
    }

    private void requireCustomerActive(UUID customerId) {
        CustomerResponse customer = callWithRetry("customer lookup " + customerId, () ->
                customerClient.get()
                        .uri("/v1/customers/{customerId}", customerId)
                        .retrieve()
                        .body(CustomerResponse.class));
        if (customer == null || !ACTIVE.equals(customer.status())) {
            throw new TransferValidationException(
                    "Customer " + customerId + " is not active");
        }
    }

    private void requireAccountOpen(UUID accountId) {
        AccountResponse account = callWithRetry("account lookup " + accountId, () ->
                accountClient.get()
                        .uri("/api/v1/accounts/{accountId}", accountId)
                        .retrieve()
                        .body(AccountResponse.class));
        if (account == null || !OPEN.equals(account.status())) {
            throw new TransferValidationException(
                    "Account " + accountId + " is not open");
        }
    }

    private void checkLimit(ValidationRequest request) {
        try {
            callWithRetry("limit check for customer " + request.customerId(), () -> {
                limitClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1/limits/check")
                                .queryParam("customerId", request.customerId())
                                .queryParam("accountId", request.sourceAccountId())
                                .queryParam("amount", request.amount().toPlainString())
                                .queryParam("currency", request.currency())
                                .build())
                        .retrieve()
                        .toBodilessEntity();
                return null;
            });
        } catch (HttpClientErrorException e) {
            // Definitive 4xx from limit-service: a business rejection, not a
            // transient failure (which callWithRetry already retried).
            throw new TransferValidationException(
                    "Limit check failed for customer " + request.customerId() + ": " + e.getResponseBodyAsString());
        }
    }

    /** Customer status projection (contract: customer-service). */
    public record CustomerResponse(String status) {}

    /** Account status projection (matches account-service OpenAPI). */
    public record AccountResponse(String status) {}
}