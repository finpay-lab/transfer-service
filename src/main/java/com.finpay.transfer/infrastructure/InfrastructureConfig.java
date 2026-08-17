package com.finpay.transfer.infrastructure;

import java.time.Clock;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Infrastructure wiring: JPA repositories + entities and the system clock.
 * Repository/entity scanning is pinned here because the Spring Boot application
 * class lives in {@code com.finpay.transfer.service} and would otherwise not
 * see these packages (AutoConfigurationPackage roots at the app class package).
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
