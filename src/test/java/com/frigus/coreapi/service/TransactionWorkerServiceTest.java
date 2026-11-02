package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.transaction.TransactionQueuePayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionWorkerServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Mock
    private TransactionProcessorService transactionProcessorService;

    @InjectMocks
    private TransactionWorkerService workerService;

    @Test
    @DisplayName("Deve delegar processamento para o TransactionProcessorService quando houver mensagem")
    void shouldDelegateProcessingToProcessorService() {
        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(eq(TransactionQueueProducer.TRANSACTION_QUEUE), eq(2L), eq(TimeUnit.SECONDS)))
                .thenReturn(payload);

        workerService.consumeQueue();

        verify(transactionProcessorService).processTransaction(payload);
    }

    @Test
    @DisplayName("Deve re-enfileirar mensagem quando falhar e shouldRetry for true")
    void shouldReenqueueMessageWhenFailureAndRetryAllowed() {
        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(eq(TransactionQueueProducer.TRANSACTION_QUEUE), eq(2L), eq(TimeUnit.SECONDS)))
                .thenReturn(payload);

        doThrow(new RuntimeException("DB Connection Timeout"))
                .when(transactionProcessorService).processTransaction(payload);

        when(transactionProcessorService.handleProcessingFailure(eq(payload), any())).thenReturn(true);

        workerService.consumeQueue();

        verify(listOperations).leftPush(TransactionQueueProducer.TRANSACTION_QUEUE, payload);
    }

    @Test
    @DisplayName("Não deve re-enfileirar mensagem quando shouldRetry for false")
    void shouldNotReenqueueWhenRetryFalse() {
        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(eq(TransactionQueueProducer.TRANSACTION_QUEUE), eq(2L), eq(TimeUnit.SECONDS)))
                .thenReturn(payload);

        doThrow(new RuntimeException("Fatal Error"))
                .when(transactionProcessorService).processTransaction(payload);

        when(transactionProcessorService.handleProcessingFailure(eq(payload), any())).thenReturn(false);

        workerService.consumeQueue();

        verify(listOperations, never()).leftPush(any(), any());
    }

    @Test
    @DisplayName("Não deve executar processamento se fila estiver vazia")
    void shouldDoNothingWhenQueueIsEmpty() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(eq(TransactionQueueProducer.TRANSACTION_QUEUE), eq(2L), eq(TimeUnit.SECONDS)))
                .thenReturn(null);

        workerService.consumeQueue();

        verify(transactionProcessorService, never()).processTransaction(any());
    }
}
