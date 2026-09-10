package com.frigus.coreapi.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.frigus.coreapi.dto.transaction.TransactionQueuePayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionQueueProducer {
    public static final String TRANSACTION_QUEUE = "queue:transactions";

    private final RedisTemplate<String, Object> redisTemplate;
    
    // ! remover logs
    public void enqueue(TransactionQueuePayload payload) {
        log.info("adicionando a fila a transacao {}, idempotency key: {}", payload.getTransactionId(), payload.getIdempotencyKey());

        redisTemplate.opsForList().leftPush(TRANSACTION_QUEUE, payload);

        log.info("transacao adicionada com sucesso");
    }
    
}
