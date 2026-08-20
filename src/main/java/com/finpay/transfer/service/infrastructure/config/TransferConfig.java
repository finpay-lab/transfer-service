package com.finpay.transfer.service.infrastructure.config;

import com.finpay.transfer.service.domain.Outbox;
import com.finpay.transfer.service.domain.SagaOrchestrator;
import com.finpay.transfer.service.domain.TransferRepository;
import com.finpay.transfer.service.infrastructure.saga.LocalSagaParticipant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransferConfig {

    @Bean
    public SagaOrchestrator sagaOrchestrator(TransferRepository repository, Outbox outbox,
                                             LocalSagaParticipant participant) {
        return new SagaOrchestrator(repository, outbox, participant);
    }
}
