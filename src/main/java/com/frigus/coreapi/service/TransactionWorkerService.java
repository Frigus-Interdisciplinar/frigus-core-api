package com.frigus.coreapi.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frigus.coreapi.dto.transaction.TransactionQueuePayload;
import com.frigus.coreapi.enums.SubscriptionStatus;
import com.frigus.coreapi.enums.TransactionStatus;
import com.frigus.coreapi.model.Subscription;
import com.frigus.coreapi.model.Transaction;
import com.frigus.coreapi.repository.SubscriptionRepository;
import com.frigus.coreapi.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionWorkerService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final TransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Scheduled(fixedDelay = 1000)
    public void consumeQueue() {
        // fifo
        Object message = redisTemplate.opsForList().rightPop(TransactionQueueProducer.TRANSACTION_QUEUE, 2, TimeUnit.SECONDS);

        if(message instanceof TransactionQueuePayload payload) {
            processPayload(payload);
        }
    }

    @Transactional
    private void processPayload(TransactionQueuePayload payload) {
        Transaction transaction = transactionRepository.findById(payload.getTransactionId())
                .orElse(null);

        if(transaction == null || transaction.getStatus() != TransactionStatus.PENDING) {
            return;
        }
        
        boolean isCardRejected = "0000".equals(payload.getFakeCardLast4());
        boolean isPixKeyRejected = payload.getFakePixKey() != null && payload.getFakePixKey().contains("recusar");


        if(isCardRejected || isPixKeyRejected) {
            transaction.setStatus(TransactionStatus.REJECTED);
            transaction.setProcessedAt(Instant.now());
            transaction.setQueueJobId(TransactionQueueProducer.TRANSACTION_QUEUE);
            transaction.setErrorMessage("Pagamento Recusado - Cartão inválido ou saldo insuficiente");
            transactionRepository.save(transaction);
            return;
        }

        transaction.setStatus(TransactionStatus.APPROVED);
        transaction.setProcessedAt(Instant.now());
        transaction.setQueueJobId(TransactionQueueProducer.TRANSACTION_QUEUE);  

        transactionRepository.save(handleSubscription(transaction));
        log.info("transacao salva: {}", transaction);
    }

    @Transactional
    private Transaction handleSubscription(Transaction transaction) {
        // se existir, renova, se nao, cria
        Subscription subscription = subscriptionRepository.findByUserId(transaction.getUser().getId())
            .orElseGet(() -> Subscription.builder()
                .user(transaction.getUser())
                .startedAt(Instant.now())
                .build()
            );

        subscription.setPlan(transaction.getPlan());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(Instant.now());
        subscription.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
        
        subscription = subscriptionRepository.save(subscription);
        log.info("assinatura atualizada: {}", subscription);

        transaction.setSubscription(subscription);

        return transaction;
    }
}
