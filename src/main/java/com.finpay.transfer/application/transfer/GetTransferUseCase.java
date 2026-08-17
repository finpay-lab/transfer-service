package com.finpay.transfer.application.transfer;

import com.finpay.transfer.domain.transfer.Transfer;
import com.finpay.transfer.domain.transfer.TransferRepository;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads a transfer aggregate for the read path (transport only, no logic). */
@Service
public class GetTransferUseCase {

    private final TransferRepository transferRepository;

    public GetTransferUseCase(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    @Transactional(readOnly = true)
    public Transfer execute(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));
    }
}
