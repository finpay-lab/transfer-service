package com.finpay.transfer.infrastructure.remote;

import com.finpay.transfer.application.saga.port.FundsReservationPort;
import com.finpay.transfer.application.saga.port.LedgerPostingPort;
import com.finpay.transfer.application.saga.port.RiskCheckPort;
import com.finpay.transfer.application.saga.port.TransferValidationPort;

import java.time.Duration;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

/**
 * Wires the saga step ports to the real external services via HTTP
 * (AGENTS.md Rule 8: timeouts + retries). Active only when
 * {@code finpay.saga.remote-mode=http}; the default lab mode ({@code in-memory})
 * activates the in-memory stand-ins instead.
 *
 * <p>The endpoints below follow the sibling services' conventions
 * ({@code /v1/...}, {@code /api/v1/...}); exact request/response contracts for
 * customer/limit/wallet/ledger land in the platform repo in a later phase.
 */
@Configuration
@ConditionalOnProperty(name = "finpay.saga.remote-mode", havingValue = "http")
@EnableConfigurationProperties(RemoteClientsProperties.class)
public class RemoteClientsConfig {

    @Bean
    public TransferValidationPort transferValidationPort(RemoteClientsProperties properties) {
        return new HttpTransferValidationClient(
                restClient(properties.getCustomer().getBaseUrl(), properties),
                restClient(properties.getAccount().getBaseUrl(), properties),
                restClient(properties.getLimit().getBaseUrl(), properties),
                properties);
    }

    @Bean
    public RiskCheckPort riskCheckPort(RemoteClientsProperties properties) {
        return new HttpRiskCheckClient(
                restClient(properties.getRisk().getBaseUrl(), properties),
                properties);
    }

    @Bean
    public FundsReservationPort fundsReservationPort(RemoteClientsProperties properties) {
        return new HttpWalletReservationClient(
                restClient(properties.getWallet().getBaseUrl(), properties),
                properties);
    }

    @Bean
    public LedgerPostingPort ledgerPostingPort(RemoteClientsProperties properties) {
        return new HttpLedgerPostingClient(
                restClient(properties.getLedger().getBaseUrl(), properties),
                properties);
    }

    private RestClient restClient(String baseUrl, RemoteClientsProperties properties) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactorySettings.defaults()
                        .withConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                        .withReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs())))
                .build();
    }
}