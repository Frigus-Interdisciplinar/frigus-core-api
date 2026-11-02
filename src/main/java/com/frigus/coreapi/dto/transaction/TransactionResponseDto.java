package com.frigus.coreapi.dto.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.frigus.coreapi.enums.PaymentMethod;
import com.frigus.coreapi.enums.TransactionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponseDto {
    private UUID id;
    private TransactionStatus status;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String idempotencyKey;
    private String planCode;
    private String errorMessage;
    private Instant createdAt;
    private Instant queuedAt;
    private Instant processedAt;
}
