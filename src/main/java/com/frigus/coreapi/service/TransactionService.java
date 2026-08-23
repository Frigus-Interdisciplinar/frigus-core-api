package com.frigus.coreapi.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.DigestUtils;

import com.frigus.coreapi.dto.transaction.CheckoutRequestDto;
import com.frigus.coreapi.dto.transaction.TransactionQueuePayload;
import com.frigus.coreapi.dto.transaction.TransactionResponseDto;
import com.frigus.coreapi.enums.BillingInterval;
import com.frigus.coreapi.enums.PaymentMethod;
import com.frigus.coreapi.enums.SubscriptionStatus;
import com.frigus.coreapi.enums.TransactionStatus;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.TransactionMapper;
import com.frigus.coreapi.model.Plan;
import com.frigus.coreapi.model.Subscription;
import com.frigus.coreapi.model.Transaction;
import com.frigus.coreapi.model.TransactionEvent;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.PlanRepository;
import com.frigus.coreapi.repository.SubscriptionRepository;
import com.frigus.coreapi.repository.TransactionEventRepository;
import com.frigus.coreapi.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionQueueProducer transactionQueueProducer;
    private final PlanRepository planRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionEventRepository transactionEventRepository;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponseDto checkout(User currentUser, CheckoutRequestDto dto) {
        return checkout(currentUser, dto, null);
    }

    @Transactional
    public TransactionResponseDto checkout(User currentUser, CheckoutRequestDto dto, String idempotencyKeyHeader) {
        Plan plan = planRepository.findByPlanCode(dto.getPlanCode())
                .orElseThrow(() -> new BadRequestException("Plano não encontrado", "Plano com o código " + dto.getPlanCode() + " não foi encontrado"));

        if (Boolean.FALSE.equals(plan.getActive()) || plan.getDeletedAt() != null) {
            throw new BadRequestException("Plano inativo", "Plano com o código " + dto.getPlanCode() + " não está disponível para assinaturas");
        }

        boolean isFreePlan = plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) == 0;

        if (!isFreePlan) {
            if (dto.getPaymentMethod() == PaymentMethod.PIX && (dto.getFakePixKey() == null || dto.getFakePixKey().isBlank())) {
                throw new BadRequestException("Chave PIX obrigatória", "Para pagamento via PIX, informe a chave PIX.");
            }
            if (dto.getPaymentMethod() == PaymentMethod.CREDIT_CARD && (dto.getFakeCardLast4() == null || dto.getFakeCardLast4().isBlank())) {
                throw new BadRequestException("Cartão obrigatório", "Para pagamento com cartão, informe os últimos 4 dígitos do cartão.");
            }
        }

        String idempotencyKey = (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank())
                ? idempotencyKeyHeader.trim()
                : generateIdempotencyKey(currentUser.getId(), plan.getPlanCode());

        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingTransaction.isPresent()) {
            Transaction tx = existingTransaction.get();

            if (tx.getStatus() == TransactionStatus.PENDING || tx.getStatus() == TransactionStatus.APPROVED || tx.getStatus() == TransactionStatus.PROCESSING) {
                return transactionMapper.toDto(tx);
            }

            // Se transação anterior falhou ou foi cancelada, gera subchave para retry
            idempotencyKey = idempotencyKey + ":retry:" + UUID.randomUUID().toString().substring(0, 8);
        }

        Transaction transaction = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .user(currentUser)
                .plan(plan)
                .amount(plan.getPrice())
                .paymentMethod(dto.getPaymentMethod())
                .fakeCardLast4(dto.getFakeCardLast4())
                .fakePixKey(dto.getFakePixKey())
                .status(isFreePlan ? TransactionStatus.APPROVED : TransactionStatus.PENDING)
                .attempts(0)
                .maxAttempts(3)
                .createdAt(Instant.now())
                .queuedAt(isFreePlan ? null : Instant.now())
                .processedAt(isFreePlan ? Instant.now() : null)
                .build();

        try {
            transaction = transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            return transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .map(transactionMapper::toDto)
                    .orElseThrow(() -> e);
        }

        if (isFreePlan) {
            Subscription subscription = handleFreeSubscription(transaction);
            transaction.setSubscription(subscription);
            transaction = transactionRepository.save(transaction);

            recordEvent(transaction, TransactionStatus.APPROVED, "Plano gratuito ativado com sucesso");
            return transactionMapper.toDto(transaction);
        }

        recordEvent(transaction, TransactionStatus.PENDING, "Transação criada e aguardando processamento na fila");

        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .userId(transaction.getUser().getId())
                .planCode(transaction.getPlan().getPlanCode())
                .amount(transaction.getAmount())
                .paymentMethod(transaction.getPaymentMethod())
                .fakeCardLast4(transaction.getFakeCardLast4())
                .fakePixKey(transaction.getFakePixKey())
                .attempt(0)
                .build();

        // Enfileira após commit no banco para evitar race condition de dual-write
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    transactionQueueProducer.enqueue(payload);
                }
            });
        } else {
            transactionQueueProducer.enqueue(payload);
        }

        return transactionMapper.toDto(transaction);
    }

    public TransactionResponseDto getTransactionById(UUID transactionId, User currentUser) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada", "Transação com o id " + transactionId + " não foi encontrada"));

        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Transação não autorizada", "Transação com o id " + transactionId + " não pertence ao usuário");
        }

        return transactionMapper.toDto(transaction);
    }

    public Page<TransactionResponseDto> listTransactions(User currentUser, Pageable pageable) {
        return transactionRepository.findByUserId(currentUser.getId(), pageable)
                .map(transactionMapper::toDto);
    }

    @Transactional
    public TransactionResponseDto cancelTransaction(UUID transactionId, User currentUser) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada", "Transação com o id " + transactionId + " não foi encontrada"));

        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Transação não autorizada", "Transação com o id " + transactionId + " não pertence ao usuário");
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BadRequestException("Transação não cancelável", "Apenas transações com status PENDING podem ser canceladas.");
        }

        transaction.setStatus(TransactionStatus.CANCELED);
        transaction.setProcessedAt(Instant.now());
        transaction.setErrorMessage("Transação cancelada pelo usuário");
        transaction = transactionRepository.save(transaction);

        recordEvent(transaction, TransactionStatus.CANCELED, "Transação cancelada pelo usuário");

        return transactionMapper.toDto(transaction);
    }

    private Subscription handleFreeSubscription(Transaction transaction) {
        Subscription subscription = subscriptionRepository.findByUserId(transaction.getUser().getId())
                .orElseGet(() -> Subscription.builder()
                        .user(transaction.getUser())
                        .startedAt(Instant.now())
                        .build()
                );

        subscription.setPlan(transaction.getPlan());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(Instant.now());
        subscription.setCurrentPeriodEnd(
                transaction.getPlan().getBillingInterval() == BillingInterval.YEARLY
                        ? Instant.now().plus(365, ChronoUnit.DAYS)
                        : Instant.now().plus(30, ChronoUnit.DAYS)
        );

        return subscriptionRepository.save(subscription);
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

    private String generateIdempotencyKey(UUID userId, String planCode) {
        // bucket de 5 minutos (300 segundos) para evitar múltiplos checkouts acidentais
        long timeBucket = Instant.now().getEpochSecond() / 300;

        String raw = String.format("user:%s:plan:%s:bucket:%d", 
                userId, 
                planCode, 
                timeBucket);

        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}