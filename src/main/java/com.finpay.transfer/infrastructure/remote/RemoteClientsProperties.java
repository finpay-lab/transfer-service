package com.finpay.transfer.infrastructure.remote;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the saga's remote dependencies (AGENTS.md Rule 8).
 * Bound from {@code finpay.remote.*}; used only when
 * {@code finpay.saga.remote-mode=http}.
 */
@ConfigurationProperties(prefix = "finpay.remote")
public class RemoteClientsProperties {

    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 5000;
    private int maxAttempts = 3;
    private long backoffMillis = 200;

    private final Client customer = new Client();
    private final Client account = new Client();
    private final Client limit = new Client();
    private final Client risk = new Client();
    private final Client wallet = new Client();
    private final Client ledger = new Client();

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getBackoffMillis() {
        return backoffMillis;
    }

    public void setBackoffMillis(long backoffMillis) {
        this.backoffMillis = backoffMillis;
    }

    public Client getCustomer() {
        return customer;
    }

    public Client getAccount() {
        return account;
    }

    public Client getLimit() {
        return limit;
    }

    public Client getRisk() {
        return risk;
    }

    public Client getWallet() {
        return wallet;
    }

    public Client getLedger() {
        return ledger;
    }

    /** Base URL of one remote dependency. */
    public static class Client {

        private String baseUrl = "";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}