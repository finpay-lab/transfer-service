package com.finpay.transfer.service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferJpaRepository extends JpaRepository<TransferEntity, String> {}
