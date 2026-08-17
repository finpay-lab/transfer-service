package com.finpay.transfer.infrastructure;

import java.time.Clock;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Infrastructure wiring: JPA repositories + entities and the system clock.
 *
 * <p>Repository/entity scanning is pinned here because the Spring Boot
 * application class lives in {@code com.finpay.transfer.service} and JPA
 * auto-configuration would otherwise root at that package (AutoConfiguration
 * Package) and miss {@code com.finpay.transfer.infrastructure.persistence}.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.finpay.transfer.infrastructure.persistence")
@EntityScan(basePackages = "com.finpay.transfer.infrastructure.persistence")
public class InfrastructureConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}