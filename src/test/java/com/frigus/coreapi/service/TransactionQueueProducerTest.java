package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.transaction.TransactionQueuePayload;
import com.frigus.coreapi.enums.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionQueueProducerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    @InjectMocks
    private TransactionQueueProducer transactionQueueProducer;

    @Test
    @DisplayName("Deve enfileirar payload na lista do Redis com sucesso")
    void shouldEnqueuePayloadSuccessfully() {
        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(UUID.randomUUID())
                .idempotencyKey("idemp-key-123")
                .userId(UUID.randomUUID())
                .planCode("PRO")
                .amount(new BigDecimal("99.90"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("4242")
                .attempt(0)
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);

        transactionQueueProducer.enqueue(payload);

        verify(redisTemplate).opsForList();
        verify(listOperations).leftPush(TransactionQueueProducer.TRANSACTION_QUEUE, payload);
    }
}
