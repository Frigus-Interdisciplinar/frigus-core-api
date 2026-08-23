package com.frigus.coreapi.mapper;

import org.springframework.stereotype.Component;

import com.frigus.coreapi.dto.transaction.CheckoutRequestDto;
import com.frigus.coreapi.dto.transaction.TransactionResponseDto;
import com.frigus.coreapi.model.Transaction;

@Component
public class TransactionMapper implements BaseMapper<Transaction, TransactionResponseDto, CheckoutRequestDto> {
    
    @Override
    public TransactionResponseDto toDto(Transaction model) {
        return TransactionResponseDto.builder()
                .id(model.getId())
                .idempotencyKey(model.getIdempotencyKey())
                .amount(model.getAmount())
                .paymentMethod(model.getPaymentMethod())
                .planCode(model.getPlan() != null ? model.getPlan().getPlanCode() : null)
                .errorMessage(model.getErrorMessage())
                .status(model.getStatus())
                .createdAt(model.getCreatedAt())
                .queuedAt(model.getQueuedAt())
                .processedAt(model.getProcessedAt())
                .build();
    }

    @Override
    public Transaction toEntity(CheckoutRequestDto dto) {
       return Transaction.builder()
       .paymentMethod(dto.getPaymentMethod())
       .fakeCardLast4(dto.getFakeCardLast4())
       .fakePixKey(dto.getFakePixKey())
       .build();    
    }
    
}
