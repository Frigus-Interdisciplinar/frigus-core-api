package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.transaction.TransactionQueuePayload;
import com.frigus.coreapi.enums.PaymentMethod;
import com.frigus.coreapi.enums.SubscriptionStatus;
import com.frigus.coreapi.enums.TransactionStatus;
import com.frigus.coreapi.model.Plan;
import com.frigus.coreapi.model.Subscription;
import com.frigus.coreapi.model.Transaction;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.SubscriptionRepository;
import com.frigus.coreapi.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
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
    private TransactionRepository transactionRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private TransactionWorkerService workerService;

    private User user;
    private Plan plan;
    private Transaction pendingTransaction;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Gabriel Worker Test")
                .build();

        plan = Plan.builder()
                .id(1)
                .planCode("PRO")
                .name("Plano Pro")
                .price(new BigDecimal("99.90"))
                .build();

        pendingTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(plan)
                .amount(plan.getPrice())
                .status(TransactionStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Deve aprovar transação e criar nova Subscription caso usuário não possua")
    void shouldApproveTransactionAndCreateSubscription() {
        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .userId(user.getId())
                .planCode("PRO")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("1234")
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(eq(TransactionQueueProducer.TRANSACTION_QUEUE), eq(2L), eq(TimeUnit.SECONDS)))
                .thenReturn(payload);

        when(transactionRepository.findById(pendingTransaction.getId()))
                .thenReturn(Optional.of(pendingTransaction));
        when(subscriptionRepository.findByUserId(user.getId()))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        workerService.consumeQueue();

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());

        Transaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(savedTx.getProcessedAt()).isNotNull();
        assertThat(savedTx.getSubscription()).isNotNull();
        assertThat(savedTx.getSubscription().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(savedTx.getSubscription().getPlan()).isEqualTo(plan);
    }

    @Test
    @DisplayName("Deve aprovar transação e renovar Subscription existente")
    void shouldApproveTransactionAndRenewExistingSubscription() {
        Subscription existingSub = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.EXPIRED)
                .startedAt(Instant.now())
                .build();

        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .userId(user.getId())
                .planCode("PRO")
                .paymentMethod(PaymentMethod.PIX)
                .fakePixKey("chave-valida-pix")
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(any(), eq(2L), any())).thenReturn(payload);

        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(existingSub));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        workerService.consumeQueue();

        assertThat(existingSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(existingSub.getCurrentPeriodEnd()).isAfter(Instant.now());
        verify(subscriptionRepository).save(existingSub);
        verify(transactionRepository).save(pendingTransaction);
    }

    @Test
    @DisplayName("Deve rejeitar transação se cartão terminar em 0000")
    void shouldRejectTransactionWhenCardEndsWith0000() {
        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .userId(user.getId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("0000")
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(any(), eq(2L), any())).thenReturn(payload);
        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));

        workerService.consumeQueue();

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());

        Transaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(savedTx.getErrorMessage()).contains("Pagamento Recusado");
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar transação se chave PIX contiver 'recusar'")
    void shouldRejectTransactionWhenPixContainsRecusar() {
        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .userId(user.getId())
                .paymentMethod(PaymentMethod.PIX)
                .fakePixKey("recusar-chave-teste")
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(any(), eq(2L), any())).thenReturn(payload);
        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));

        workerService.consumeQueue();

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());

        Transaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve reprocessar transação caso status não seja PENDING")
    void shouldIgnoreTransactionIfAlreadyProcessed() {
        pendingTransaction.setStatus(TransactionStatus.APPROVED);

        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .build();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(any(), eq(2L), any())).thenReturn(payload);
        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));

        workerService.consumeQueue();

        verify(transactionRepository, never()).save(any());
        verify(subscriptionRepository, never()).save(any());
    }
}
