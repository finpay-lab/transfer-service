package com.finpay.transfer.interfaces.web;

import com.finpay.common.test.ArchitectureRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Reuses the platform architecture rules (com.finpay:common-test). Enforces
 * AGENTS.md Rule 4: domain logic must be free of Spring/JPA/Kafka imports.
 */
@AnalyzeClasses(packages = "com.finpay.transfer")
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule domainIsIndependentOfInfrastructure =
            ArchitectureRules.domainIsIndependentOfInfrastructure();
}