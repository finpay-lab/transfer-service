package com.finpay.transfer.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Infrastructure wiring shared by the service. */
@Configuration
public class InfrastructureConfig {

    /** Injectable clock so tests can control time deterministically. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}