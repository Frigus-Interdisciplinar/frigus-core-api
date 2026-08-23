package com.frigus.coreapi.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frigus.coreapi.dto.transaction.TransactionQueuePayload;
import com.frigus.coreapi.enums.BillingInterval;
import com.frigus.coreapi.enums.SubscriptionStatus;
import com.frigus.coreapi.enums.TransactionStatus;
import com.frigus.coreapi.model.Subscription;
import com.frigus.coreapi.model.Transaction;
import com.frigus.coreapi.model.TransactionEvent;
import com.frigus.coreapi.repository.SubscriptionRepository;
import com.frigus.coreapi.repository.TransactionEventRepository;
import com.frigus.coreapi.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessorService {

    private final TransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionEventRepository transactionEventRepository;

    @Transactional
    public void processTransaction(TransactionQueuePayload payload) {
        Transaction transaction = transactionRepository.findById(payload.getTransactionId())
                .orElse(null);

        if (transaction == null || transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Transação id={} ignorada (não encontrada ou status diferente de PENDING)", payload.getTransactionId());
            return;
        }

        String jobId = UUID.randomUUID().toString();
        transaction.setQueueJobId(jobId);
        transaction.setAttempts(transaction.getAttempts() + 1);
        transaction.setStatus(TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);

        recordEvent(transaction, TransactionStatus.PROCESSING, "Processamento de pagamento iniciado (Job ID: " + jobId + ")");

        boolean isCardRejected = "0000".equals(payload.getFakeCardLast4());
        boolean isPixKeyRejected = payload.getFakePixKey() != null 
                && payload.getFakePixKey().toLowerCase().contains("recusar");

        if (isCardRejected || isPixKeyRejected) {
            transaction.setStatus(TransactionStatus.REJECTED);
            transaction.setProcessedAt(Instant.now());
            transaction.setErrorMessage("Pagamento Recusado - Cartão inválido ou saldo insuficiente");
            transactionRepository.save(transaction);

            recordEvent(transaction, TransactionStatus.REJECTED, transaction.getErrorMessage());
            log.info("Transação id={} rejeitada: {}", transaction.getId(), transaction.getErrorMessage());
            return;
        }

        Subscription subscription = handleSubscription(transaction);
        transaction.setSubscription(subscription);
        transaction.setStatus(TransactionStatus.APPROVED);
        transaction.setProcessedAt(Instant.now());
        transaction.setErrorMessage(null);
        transactionRepository.save(transaction);

        recordEvent(transaction, TransactionStatus.APPROVED, "Pagamento aprovado e assinatura ativada com sucesso");
        log.info("Transação id={} aprovada com sucesso para o usuário id={}", transaction.getId(), transaction.getUser().getId());
    }

    @Transactional
    public boolean handleProcessingFailure(TransactionQueuePayload payload, String errorMessage) {
        Transaction transaction = transactionRepository.findById(payload.getTransactionId())
                .orElse(null);

        if (transaction == null) {
            return false;
        }

        int attempts = transaction.getAttempts() == null ? 1 : transaction.getAttempts();
        int maxAttempts = transaction.getMaxAttempts() == null ? 3 : transaction.getMaxAttempts();

        if (attempts >= maxAttempts) {
            transaction.setStatus(TransactionStatus.ERROR);
            transaction.setProcessedAt(Instant.now());
            transaction.setErrorMessage("Falha definitiva após " + attempts + " tentativas: " + errorMessage);
            transactionRepository.save(transaction);

            recordEvent(transaction, TransactionStatus.ERROR, transaction.getErrorMessage());
            log.error("Transação id={} atingiu limite de tentativas ({}) e foi marcada como ERROR", transaction.getId(), maxAttempts);
            return false;
        }

        log.warn("Transação id={} falhou (tentativa {}/{}). Será re-enfileirada.", transaction.getId(), attempts, maxAttempts);
        return true;
    }

    private Subscription handleSubscription(Transaction transaction) {
        Subscription subscription = subscriptionRepository.findByUserId(transaction.getUser().getId())
                .orElseGet(() -> Subscription.builder()
                        .user(transaction.getUser())
                        .startedAt(Instant.now())
                        .build()
                );

        Instant periodStart = Instant.now();
        Instant periodEnd;

        // Se assinatura já estava ativa e a data de término for futura, estende a partir da data de término
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE 
                && subscription.getCurrentPeriodEnd() != null 
                && subscription.getCurrentPeriodEnd().isAfter(Instant.now())) {
            periodStart = subscription.getCurrentPeriodStart() != null ? subscription.getCurrentPeriodStart() : Instant.now();
            periodEnd = calculatePeriodEnd(subscription.getCurrentPeriodEnd(), transaction.getPlan().getBillingInterval());
        } else {
            periodEnd = calculatePeriodEnd(periodStart, transaction.getPlan().getBillingInterval());
        }

        subscription.setPlan(transaction.getPlan());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(periodEnd);

        subscription = subscriptionRepository.save(subscription);
        log.info("Assinatura id={} atualizada para usuário id={} com término em {}", 
                subscription.getId(), transaction.getUser().getId(), periodEnd);

        return subscription;
    }

    private Instant calculatePeriodEnd(Instant baseDate, BillingInterval interval) {
        if (interval == BillingInterval.YEARLY) {
            return baseDate.plus(365, ChronoUnit.DAYS);
        }
        return baseDate.plus(30, ChronoUnit.DAYS);
    }

    private void recordEvent(Transaction transaction, TransactionStatus status, String message) {
        TransactionEvent event = TransactionEvent.builder()
                .transaction(transaction)
                .status(status)
                .message(message)
                .createdAt(Instant.now())
                .build();
        transactionEventRepository.save(event);
    }
}
