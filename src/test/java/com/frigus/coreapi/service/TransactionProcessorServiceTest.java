package com.frigus.coreapi.service;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TransactionEventRepository transactionEventRepository;

    @InjectMocks
    private TransactionProcessorService processorService;

    private User user;
    private Plan monthlyPlan;
    private Plan yearlyPlan;
    private Transaction pendingTransaction;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Gabriel Processor Test")
                .build();

        monthlyPlan = Plan.builder()
                .id(1)
                .planCode("PRO_MONTHLY")
                .name("Plano Pro Mensal")
                .price(new BigDecimal("99.90"))
                .billingInterval(BillingInterval.MONTHLY)
                .build();

        yearlyPlan = Plan.builder()
                .id(2)
                .planCode("PRO_YEARLY")
                .name("Plano Pro Anual")
                .price(new BigDecimal("999.00"))
                .billingInterval(BillingInterval.YEARLY)
                .build();

        pendingTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(monthlyPlan)
                .amount(monthlyPlan.getPrice())
                .status(TransactionStatus.PENDING)
                .attempts(0)
                .maxAttempts(3)
                .build();
    }

    @Test
    @DisplayName("Deve aprovar transação mensal e criar nova assinatura")
    void shouldApproveMonthlyTransactionAndCreateSubscription() {
        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .userId(user.getId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("1234")
                .build();

        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        processorService.processTransaction(payload);

        assertThat(pendingTransaction.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(pendingTransaction.getAttempts()).isEqualTo(1);
        assertThat(pendingTransaction.getProcessedAt()).isNotNull();
        assertThat(pendingTransaction.getSubscription()).isNotNull();
        assertThat(pendingTransaction.getSubscription().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        verify(transactionEventRepository, atLeast(2)).save(any(TransactionEvent.class));
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Deve aprovar transação anual e calcular 365 dias de vigência")
    void shouldApproveYearlyTransactionAndCalculate365Days() {
        pendingTransaction.setPlan(yearlyPlan);
        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .userId(user.getId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("1234")
                .build();

        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        processorService.processTransaction(payload);

        assertThat(pendingTransaction.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        Subscription sub = pendingTransaction.getSubscription();
        assertThat(sub.getCurrentPeriodEnd()).isAfter(Instant.now().plus(360, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("Deve somar dias adicionais ao término atual se assinatura já estiver ativa")
    void shouldExtendFromCurrentPeriodEndIfAlreadyActive() {
        Instant futureEnd = Instant.now().plus(20, ChronoUnit.DAYS);
        Subscription existingActiveSub = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(monthlyPlan)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.now().minus(10, ChronoUnit.DAYS))
                .currentPeriodEnd(futureEnd)
                .build();

        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .userId(user.getId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("1234")
                .build();

        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(existingActiveSub));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        processorService.processTransaction(payload);

        assertThat(existingActiveSub.getCurrentPeriodEnd()).isAfter(futureEnd.plus(29, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("Deve rejeitar transação se cartão for 0000")
    void shouldRejectWhenCardIs0000() {
        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .userId(user.getId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("0000")
                .build();

        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));

        processorService.processTransaction(payload);

        assertThat(pendingTransaction.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(pendingTransaction.getErrorMessage()).contains("Pagamento Recusado");
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar transação se PIX contiver 'recusar'")
    void shouldRejectWhenPixContainsRecusar() {
        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .userId(user.getId())
                .paymentMethod(PaymentMethod.PIX)
                .fakePixKey("recusar@chave.com")
                .build();

        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));

        processorService.processTransaction(payload);

        assertThat(pendingTransaction.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve ignorar transação caso não seja PENDING")
    void shouldIgnoreIfAlreadyProcessed() {
        pendingTransaction.setStatus(TransactionStatus.APPROVED);

        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .build();

        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));

        processorService.processTransaction(payload);

        verify(subscriptionRepository, never()).save(any());
        verify(transactionEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("handleProcessingFailure deve retornar true quando attempts < maxAttempts")
    void shouldReturnTrueWhenAttemptsUnderMax() {
        pendingTransaction.setAttempts(1);
        pendingTransaction.setMaxAttempts(3);

        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .build();

        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));

        boolean shouldRetry = processorService.handleProcessingFailure(payload, "Timeout de banco");

        assertThat(shouldRetry).isTrue();
        assertThat(pendingTransaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    @DisplayName("handleProcessingFailure deve marcar status como ERROR quando atingir maxAttempts")
    void shouldMarkAsErrorWhenAttemptsReachMax() {
        pendingTransaction.setAttempts(3);
        pendingTransaction.setMaxAttempts(3);

        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(pendingTransaction.getId())
                .build();

        when(transactionRepository.findById(pendingTransaction.getId())).thenReturn(Optional.of(pendingTransaction));

        boolean shouldRetry = processorService.handleProcessingFailure(payload, "Falha fatal de conexao");

        assertThat(shouldRetry).isFalse();
        assertThat(pendingTransaction.getStatus()).isEqualTo(TransactionStatus.ERROR);
        assertThat(pendingTransaction.getErrorMessage()).contains("Falha definitiva após 3 tentativas");
        verify(transactionEventRepository).save(any(TransactionEvent.class));
    }
}
