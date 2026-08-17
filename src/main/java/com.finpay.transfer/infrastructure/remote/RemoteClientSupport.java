package com.finpay.transfer.infrastructure.remote;

import java.util.function.Supplier;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Base for HTTP adapters of the saga step ports (AGENTS.md Rule 8: every
 * remote dependency defines timeout/retry). {@link #callWithRetry} retries
 * transient failures (connection/read timeouts, 5xx) with a small linear
 * backoff, but <em>never</em> retries 4xx business rejections — those are
 * definitive and are surfaced to the caller for the orchestrator to decide
 * (compensation vs. plain failure).
 *
 * <p>A full circuit breaker (fail-fast once the dependency is known to be
 * down) is a documented follow-up; the retry bounds already cap the blast
 * radius of a slow/hung dependency. Timeouts are configured per client
 * (connect/read) and centralised in {@link RemoteClientsProperties}.
 */
public abstract class RemoteClientSupport {

    private final int maxAttempts;
    private final long backoffMillis;

    protected RemoteClientSupport(int maxAttempts, long backoffMillis) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
        this.backoffMillis = backoffMillis;
    }

    protected <T> T callWithRetry(String operation, Supplier<T> call) {
        int attempt = 1;
        while (true) {
            try {
                return call.get();
            } catch (HttpClientErrorException e) {
                // Definitive business rejection (4xx): no retry.
                throw e;
            } catch (RestClientException e) {
                if (attempt >= maxAttempts) {
                    throw new RemoteStepCallException(operation, e);
                }
                attempt++;
                sleep(backoffMillis * attempt);
            }
        }
    }

    protected void callWithRetry(String operation, Runnable call) {
        callWithRetry(operation, () -> {
            call.run();
            return null;
        });
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteStepCallException("Interrupted during retry backoff", e);
        }
    }
}