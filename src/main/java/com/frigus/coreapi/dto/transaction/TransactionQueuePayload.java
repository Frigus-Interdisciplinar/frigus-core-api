package com.frigus.coreapi.dto.transaction;

import lombok.Data;
import lombok.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import com.frigus.coreapi.enums.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionQueuePayload implements Serializable {
    private UUID transactionId;
    private String idempotencyKey;
    private UUID userId;
    private String planCode;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String fakeCardLast4;
    private String fakePixKey;
    private Integer attempt;
}
