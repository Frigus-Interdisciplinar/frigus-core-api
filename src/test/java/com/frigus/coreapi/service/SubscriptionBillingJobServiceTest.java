package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.subscription.BillingJobResultDto;
import com.frigus.coreapi.dto.transaction.TransactionQueuePayload;
import com.frigus.coreapi.enums.BillingInterval;
import com.frigus.coreapi.enums.PaymentMethod;
import com.frigus.coreapi.enums.SubscriptionStatus;
import com.frigus.coreapi.enums.TransactionStatus;
import com.frigus.coreapi.model.Plan;
import com.frigus.coreapi.model.Subscription;
import com.frigus.coreapi.model.Transaction;
import com.frigus.coreapi.model.TransactionEvent;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.SubscriptionRepository;
import com.frigus.coreapi.repository.TransactionEventRepository;
import com.frigus.coreapi.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionBillingJobServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionEventRepository transactionEventRepository;

    @Mock
    private TransactionQueueProducer transactionQueueProducer;

    @InjectMocks
    private SubscriptionBillingJobService billingJobService;

    private User user;
    private Plan paidPlan;
    private Subscription dueSubscription;
    private Subscription canceledExpiredSub;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Gabriel Recurring Test")
                .email("gabriel@recurring.com")
                .build();

        paidPlan = Plan.builder()
                .id(10)
                .planCode("FAMILY")
                .name("Frigus Família")
                .price(new BigDecimal("49.99"))
                .billingInterval(BillingInterval.MONTHLY)
                .active(true)
                .build();

        dueSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(paidPlan)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .currentPeriodStart(Instant.now().minus(30, ChronoUnit.DAYS))
                .currentPeriodEnd(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        canceledExpiredSub = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(paidPlan)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(Instant.now().minus(60, ChronoUnit.DAYS))
                .currentPeriodStart(Instant.now().minus(30, ChronoUnit.DAYS))
                .currentPeriodEnd(Instant.now().minus(2, ChronoUnit.DAYS))
                .canceledAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();
    }

    @Test
    @DisplayName("Deve processar assinaturas vencidas e gerar cobrança recorrente com payload na fila")
    void shouldProcessDueSubscriptionsAndGenerateBillingTransactions() {
        when(subscriptionRepository.findExpiredCanceledSubscriptions(eq(SubscriptionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(canceledExpiredSub));
        when(subscriptionRepository.findDueForRecurringBilling(eq(SubscriptionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(dueSubscription));
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());

        Transaction previousTx = Transaction.builder()
                .id(UUID.randomUUID())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("9876")
                .build();
        Page<Transaction> previousTxPage = new PageImpl<>(List.of(previousTx), PageRequest.of(0, 1), 1);
        when(transactionRepository.findByUserIdAndStatus(eq(user.getId()), eq(TransactionStatus.APPROVED), any(PageRequest.class)))
                .thenReturn(previousTxPage);

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });

        BillingJobResultDto result = billingJobService.runDailyBillingJob();

        assertThat(result.getEvaluatedCount()).isEqualTo(1);
        assertThat(result.getTransactionsGeneratedCount()).isEqualTo(1);
        assertThat(result.getExpiredCount()).isEqualTo(1);
        assertThat(result.getErrorsCount()).isEqualTo(0);

        // Verifica que a assinatura cancelada foi marcada como EXPIRED
        assertThat(canceledExpiredSub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(subscriptionRepository).save(canceledExpiredSub);

        // Verifica que a transação foi salva com os dados corretos e enfileirada
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getAmount()).isEqualTo(new BigDecimal("49.99"));
        assertThat(savedTx.getFakeCardLast4()).isEqualTo("9876");
        assertThat(savedTx.getStatus()).isEqualTo(TransactionStatus.PENDING);

        verify(transactionEventRepository).save(any(TransactionEvent.class));
        verify(transactionQueueProducer).enqueue(any(TransactionQueuePayload.class));
    }

    @Test
    @DisplayName("Deve evitar cobrança duplicada se chave de idempotência já existir no ciclo")
    void shouldSkipSubscriptionIfIdempotencyKeyAlreadyExists() {
        when(subscriptionRepository.findExpiredCanceledSubscriptions(eq(SubscriptionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of());
        when(subscriptionRepository.findDueForRecurringBilling(eq(SubscriptionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(dueSubscription));

        Transaction existingTx = Transaction.builder()
                .id(UUID.randomUUID())
                .status(TransactionStatus.PENDING)
                .build();
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.of(existingTx));

        BillingJobResultDto result = billingJobService.runDailyBillingJob();

        assertThat(result.getEvaluatedCount()).isEqualTo(1);
        assertThat(result.getTransactionsGeneratedCount()).isEqualTo(0);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        verify(transactionQueueProducer, never()).enqueue(any());
    }

    @Test
    @DisplayName("Deve ignorar planos com valor zero")
    void shouldSkipZeroPricePlans() {
        Plan zeroPlan = Plan.builder().id(99).price(BigDecimal.ZERO).build();
        dueSubscription.setPlan(zeroPlan);

        when(subscriptionRepository.findExpiredCanceledSubscriptions(eq(SubscriptionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of());
        when(subscriptionRepository.findDueForRecurringBilling(eq(SubscriptionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(dueSubscription));

        BillingJobResultDto result = billingJobService.runDailyBillingJob();

        assertThat(result.getTransactionsGeneratedCount()).isEqualTo(0);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve capturar erros isolados sem abortar a execução do lote")
    void shouldHandleItemErrorsGracefully() {
        when(subscriptionRepository.findExpiredCanceledSubscriptions(eq(SubscriptionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of());
        when(subscriptionRepository.findDueForRecurringBilling(eq(SubscriptionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(dueSubscription));
        when(transactionRepository.findByIdempotencyKey(anyString()))
                .thenThrow(new RuntimeException("DB Connection Timeout"));

        BillingJobResultDto result = billingJobService.runDailyBillingJob();

        assertThat(result.getErrorsCount()).isEqualTo(1);
        assertThat(result.getErrorMessages()).isNotEmpty();
        assertThat(result.getTransactionsGeneratedCount()).isEqualTo(0);
    }
}
