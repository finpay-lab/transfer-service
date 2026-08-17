package com.finpay.transfer.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Transfer-service: SAGA orchestrator for money transfers (ADR-0003).
 *
 * <p>{@code scanBasePackages} reaches the application/domain/infrastructure/
 * interfaces packages that live under {@code com.finpay.transfer} but outside
 * this class's own package; {@code @EnableScheduling} drives the saga recovery
 * job.
 */
@SpringBootApplication(scanBasePackages = "com.finpay.transfer")
@EnableScheduling
public class TransferServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransferServiceApplication.class, args);
    }
}