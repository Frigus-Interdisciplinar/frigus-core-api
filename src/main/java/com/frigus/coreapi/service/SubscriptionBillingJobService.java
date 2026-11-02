package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.subscription.BillingJobResultDto;
import com.frigus.coreapi.dto.transaction.TransactionQueuePayload;
import com.frigus.coreapi.enums.PaymentMethod;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionBillingJobService {

    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository transactionEventRepository;
    private final TransactionQueueProducer transactionQueueProducer;

    @Scheduled(cron = "${billing.job.cron:0 0 3 * * ?}")
    public void executeScheduledBillingJob() {
        log.info("Iniciando execução agendada do job diário de cobrança recorrente...");
        BillingJobResultDto result = runDailyBillingJob();
        log.info("Job diário de cobrança finalizado: {} assinaturas avaliadas, {} cobranças geradas, {} expiradas, {} erros em {}ms",
                result.getEvaluatedCount(),
                result.getTransactionsGeneratedCount(),
                result.getExpiredCount(),
                result.getErrorsCount(),
                result.getExecutionDurationMs());
    }

    @Transactional
    public BillingJobResultDto runDailyBillingJob() {
        long startTime = System.currentTimeMillis();
        Instant now = Instant.now();

        int evaluatedCount = 0;
        int transactionsGeneratedCount = 0;
        int expiredCount = 0;
        int skippedCount = 0;
        int errorsCount = 0;
        List<String> errorMessages = new ArrayList<>();

        // expiracao de assinaturas canceladas que ultrapassaram a data de término
        try {
            List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredCanceledSubscriptions(SubscriptionStatus.ACTIVE, now);
            for (Subscription expiredSub : expiredSubscriptions) {
                try {
                    expiredSub.setStatus(SubscriptionStatus.EXPIRED);
                    expiredSub.setUpdatedAt(Instant.now());
                    subscriptionRepository.save(expiredSub);
                    expiredCount++;
                    log.info("Assinatura id={} do usuário id={} expirada após cancelamento prévio",
                            expiredSub.getId(), expiredSub.getUser().getId());
                } catch (Exception e) {
                    errorsCount++;
                    String err = "Erro ao expirar assinatura id=" + expiredSub.getId() + ": " + e.getMessage();
                    errorMessages.add(err);
                    log.error(err, e);
                }
            }
        } catch (Exception e) {
            errorsCount++;
            String err = "Erro ao buscar assinaturas canceladas para expiração: " + e.getMessage();
            errorMessages.add(err);
            log.error(err, e);
        }

        // busca assinaturas ativas com periodo vencido para cobrança recorrente
        List<Subscription> dueSubscriptions = new ArrayList<>();
        try {
            dueSubscriptions = subscriptionRepository.findDueForRecurringBilling(SubscriptionStatus.ACTIVE, now);
            evaluatedCount = dueSubscriptions.size();
        } catch (Exception e) {
            errorsCount++;
            String err = "Erro ao buscar assinaturas para cobrança: " + e.getMessage();
            errorMessages.add(err);
            log.error(err, e);
        }

        for (Subscription sub : dueSubscriptions) {
            try {
                if (sub.getPlan() == null || sub.getPlan().getPrice() == null || sub.getPlan().getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    skippedCount++;
                    continue;
                }

                long periodEpoch = sub.getCurrentPeriodEnd() != null ? sub.getCurrentPeriodEnd().toEpochMilli() : now.toEpochMilli();
                String idempotencyKey = String.format("billing:sub:%s:period:%d", sub.getId(), periodEpoch);

                Optional<Transaction> existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey);
                if (existingTx.isPresent()) {
                    log.info("Cobrança recorrente para assinatura id={} já existente com chave {}, pulando", sub.getId(), idempotencyKey);
                    skippedCount++;
                    continue;
                }

                // recupera último método de pagamento aprovado do usuário para reutilizar
                PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;
                String fakeCardLast4 = "4242";
                String fakePixKey = null;

                Page<Transaction> lastApprovedTxPage = transactionRepository.findByUserIdAndStatus(
                        sub.getUser().getId(),
                        TransactionStatus.APPROVED,
                        PageRequest.of(0, 1)
                );

                if (lastApprovedTxPage.hasContent()) {
                    Transaction lastApproved = lastApprovedTxPage.getContent().get(0);
                    if (lastApproved.getPaymentMethod() != null) {
                        paymentMethod = lastApproved.getPaymentMethod();
                        fakeCardLast4 = lastApproved.getFakeCardLast4();
                        fakePixKey = lastApproved.getFakePixKey();
                    }
                }

                Transaction transaction = Transaction.builder()
                        .idempotencyKey(idempotencyKey)
                        .user(sub.getUser())
                        .subscription(sub)
                        .plan(sub.getPlan())
                        .amount(sub.getPlan().getPrice())
                        .paymentMethod(paymentMethod)
                        .fakeCardLast4(fakeCardLast4)
                        .fakePixKey(fakePixKey)
                        .status(TransactionStatus.PENDING)
                        .attempts(0)
                        .maxAttempts(3)
                        .createdAt(Instant.now())
                        .queuedAt(Instant.now())
                        .build();

                transaction = transactionRepository.save(transaction);

                TransactionEvent event = TransactionEvent.builder()
                        .transaction(transaction)
                        .status(TransactionStatus.PENDING)
                        .message("Cobrança recorrente automática gerada pelo job diário de faturamento")
                        .createdAt(Instant.now())
                        .build();
                transactionEventRepository.save(event);

                TransactionQueuePayload payload = TransactionQueuePayload.builder()
                        .transactionId(transaction.getId())
                        .idempotencyKey(transaction.getIdempotencyKey())
                        .userId(sub.getUser().getId())
                        .planCode(sub.getPlan().getPlanCode())
                        .amount(transaction.getAmount())
                        .paymentMethod(transaction.getPaymentMethod())
                        .fakeCardLast4(transaction.getFakeCardLast4())
                        .fakePixKey(transaction.getFakePixKey())
                        .attempt(0)
                        .build();

                transactionQueueProducer.enqueue(payload);
                transactionsGeneratedCount++;

                log.info("Cobrança recorrente criada com sucesso: transação id={}, assinatura id={}, valor={}",
                        transaction.getId(), sub.getId(), transaction.getAmount());

            } catch (Exception e) {
                errorsCount++;
                String err = "Erro ao processar cobrança da assinatura id=" + sub.getId() + ": " + e.getMessage();
                errorMessages.add(err);
                log.error(err, e);
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        return BillingJobResultDto.builder()
                .evaluatedCount(evaluatedCount)
                .transactionsGeneratedCount(transactionsGeneratedCount)
                .expiredCount(expiredCount)
                .skippedCount(skippedCount)
                .errorsCount(errorsCount)
                .errorMessages(errorMessages)
                .executedAt(now)
                .executionDurationMs(duration)
                .build();
    }
}
