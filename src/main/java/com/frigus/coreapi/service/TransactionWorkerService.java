package com.frigus.coreapi.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.frigus.coreapi.dto.transaction.TransactionQueuePayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionWorkerService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final TransactionProcessorService transactionProcessorService;

    @Scheduled(fixedDelay = 1000)
    public void consumeQueue() {
        try {
            Object message = redisTemplate.opsForList().rightPop(
                    TransactionQueueProducer.TRANSACTION_QUEUE, 
                    2, 
                    TimeUnit.SECONDS
            );

            if (message instanceof TransactionQueuePayload payload) {
                try {
                    transactionProcessorService.processTransaction(payload);
                } catch (Exception ex) {
                    log.error("Erro inesperado ao processar transação id={}: {}", payload.getTransactionId(), ex.getMessage(), ex);
                    boolean shouldRetry = transactionProcessorService.handleProcessingFailure(payload, ex.getMessage());
                    if (shouldRetry) {
                        // Re-enfileira no Redis para nova tentativa
                        redisTemplate.opsForList().leftPush(TransactionQueueProducer.TRANSACTION_QUEUE, payload);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Falha ao consumir fila de transações do Redis: {}", ex.getMessage());
        }
    }
}
