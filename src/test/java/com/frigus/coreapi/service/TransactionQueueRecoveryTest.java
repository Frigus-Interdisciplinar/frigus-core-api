package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.transaction.TransactionQueuePayload;
import com.frigus.coreapi.enums.BillingInterval;
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
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Real JDBC commits/rollbacks and Spring transaction interception; repositories use
 * small H2-backed fixtures so no production database or scheduled worker is touched.
 * The subscription fixture models the existing derived DeletedAtIsNull query.
 */
class TransactionQueueRecoveryTest {
    private JdbcTemplate jdbc;
    private TransactionRepository transactions;
    private SubscriptionRepository subscriptions;
    private TransactionEventRepository events;
    private TransactionProcessorService processor;
    private final Plan plan = Plan.builder().id(1).planCode("PLUS")
            .price(new BigDecimal("19.90")).billingInterval(BillingInterval.MONTHLY).build();

    @BeforeEach
    void prepareTransactionalFixtures() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table queue_transactions (id varchar(36) primary key, user_id varchar(36), "
                + "status varchar(24), attempts int, max_attempts int, subscription_id varchar(36))");
        jdbc.execute("create table queue_subscriptions (id varchar(36) primary key, user_id varchar(36), "
                + "status varchar(24), deleted_at timestamp)");
        jdbc.execute("create table queue_events (transaction_id varchar(36), status varchar(24))");
        transactions = mock(TransactionRepository.class);
        subscriptions = mock(SubscriptionRepository.class);
        events = mock(TransactionEventRepository.class);

        when(transactions.findByIdForUpdate(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return jdbc.query("select * from queue_transactions where id = ? for update", (rs, row) -> {
                String subscriptionId = rs.getString("subscription_id");
                Subscription subscription = subscriptionId == null ? null : Subscription.builder()
                        .id(UUID.fromString(subscriptionId)).status(SubscriptionStatus.ACTIVE).build();
                return Transaction.builder().id(id)
                        .user(User.builder().id(UUID.fromString(rs.getString("user_id"))).build())
                        .plan(plan).status(TransactionStatus.valueOf(rs.getString("status")))
                        .attempts(rs.getInt("attempts")).maxAttempts(rs.getInt("max_attempts"))
                        .subscription(subscription).build();
            }, id.toString()).stream().findFirst();
        });
        when(transactions.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            jdbc.update("update queue_transactions set status = ?, attempts = ? where id = ?",
                    transaction.getStatus().name(), transaction.getAttempts(), transaction.getId().toString());
            return transaction;
        });
        when(events.save(any(TransactionEvent.class))).thenAnswer(invocation -> {
            TransactionEvent event = invocation.getArgument(0);
            jdbc.update("insert into queue_events values (?, ?)", event.getTransaction().getId().toString(),
                    event.getStatus().name());
            return event;
        });
        when(subscriptions.findByUserIdAndDeletedAtIsNull(any())).thenAnswer(invocation -> {
            UUID userId = invocation.getArgument(0);
            List<Subscription> matches = jdbc.query(
                    "select * from queue_subscriptions where user_id = ? and deleted_at is null",
                    (rs, row) -> Subscription.builder().id(UUID.fromString(rs.getString("id")))
                            .user(User.builder().id(userId).build()).plan(plan)
                            .status(SubscriptionStatus.valueOf(rs.getString("status"))).build(),
                    userId.toString());
            if (matches.size() > 1) {
                throw new IncorrectResultSizeDataAccessException(1, matches.size());
            }
            return matches.stream().findFirst();
        });
        // lenient because the ambiguity tests must not reach any subscription write.
        lenient().when(subscriptions.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(new DataSourceTransactionManager(dataSource));
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory proxy = new ProxyFactory(new TransactionProcessorService(transactions, subscriptions, events));
        proxy.addAdvice(interceptor);
        processor = (TransactionProcessorService) proxy.getProxy();
    }

    @Test
    void oneLiveAndOneDeletedSubscriptionSelectsOnlyLiveRecord() {
        UUID owner = UUID.randomUUID();
        UUID liveId = subscription(owner, SubscriptionStatus.ACTIVE, false);
        subscription(owner, SubscriptionStatus.EXPIRED, true);
        UUID id = transaction(owner, null);

        processor.processTransaction(payload(id));

        assertThat(status(id)).isEqualTo("APPROVED");
        assertThat(attempts(id)).isEqualTo(1);
        var written = org.mockito.ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptions).save(written.capture());
        assertThat(written.getValue().getId()).isEqualTo(liveId);
        verify(subscriptions).findByUserIdAndDeletedAtIsNull(owner);
        verify(subscriptions, never()).findByUserId(any());
    }

    @Test
    void ambiguousSubscriptionsRollBackAndFailuresCommitUntilExactlyThreeThenStop() {
        UUID owner = UUID.randomUUID();
        UUID activeId = subscription(owner, SubscriptionStatus.ACTIVE, false);
        subscription(owner, SubscriptionStatus.ACTIVE, false);
        UUID id = transaction(owner, activeId);

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThatThrownBy(() -> processor.processTransaction(payload(id)))
                    .isInstanceOf(IncorrectResultSizeDataAccessException.class);
            // Both the PROCESSING status and its counter/event were rolled back.
            assertThat(status(id)).isEqualTo("PENDING");
            assertThat(attempts(id)).isEqualTo(attempt - 1);
            assertThat(eventCount(id)).isZero();

            boolean retry = processor.handleProcessingFailure(payload(id), "ambiguous subscription");
            assertThat(retry).isEqualTo(attempt < 3);
            assertThat(attempts(id)).isEqualTo(attempt);
        }

        assertThat(status(id)).isEqualTo("ERROR");
        assertThat(eventCount(id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from queue_subscriptions where id = ?",
                String.class, activeId.toString())).isEqualTo("ACTIVE");
        verify(subscriptions, never()).save(any());
        processor.processTransaction(payload(id));
        assertThat(processor.handleProcessingFailure(payload(id), "late duplicate")).isFalse();
        assertThat(attempts(id)).isEqualTo(3);
        assertThat(eventCount(id)).isEqualTo(1);
    }

    @Test
    void differentQueuedTransactionIdsKeepIndependentDurableFailureBudgets() {
        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();
        for (UUID owner : List.of(firstOwner, secondOwner)) {
            subscription(owner, SubscriptionStatus.ACTIVE, false);
            subscription(owner, SubscriptionStatus.CANCELED, false);
        }
        UUID first = transaction(firstOwner, null);
        UUID second = transaction(secondOwner, null);

        failAndRecord(first);
        failAndRecord(second);
        failAndRecord(first);

        assertThat(attempts(first)).isEqualTo(2);
        assertThat(attempts(second)).isEqualTo(1);
        assertThat(status(first)).isEqualTo("PENDING");
        assertThat(status(second)).isEqualTo("PENDING");
        verify(subscriptions, never()).save(any());
        verify(subscriptions, times(2)).findByUserIdAndDeletedAtIsNull(firstOwner);
        verify(subscriptions).findByUserIdAndDeletedAtIsNull(secondOwner);
    }

    @ParameterizedTest
    @EnumSource(value = TransactionStatus.class, names = {"APPROVED", "REJECTED", "ERROR", "CANCELED", "PROCESSING"})
    void lateFailureOrQueueReplayDoesNotModifyNonPendingTransaction(TransactionStatus status) {
        UUID id = transaction(UUID.randomUUID(), null);
        jdbc.update("update queue_transactions set status = ?, attempts = 2 where id = ?",
                status.name(), id.toString());

        processor.processTransaction(payload(id));
        assertThat(processor.handleProcessingFailure(payload(id), "late failure")).isFalse();

        assertThat(status(id)).isEqualTo(status.name());
        assertThat(attempts(id)).isEqualTo(2);
        assertThat(eventCount(id)).isZero();
        verifyNoInteractions(subscriptions);
    }

    @Test
    void explicitGatewayRejectionKeepsExistingDelinquentRule() {
        UUID owner = UUID.randomUUID();
        UUID activeId = subscription(owner, SubscriptionStatus.ACTIVE, false);
        UUID id = transaction(owner, activeId);
        TransactionQueuePayload payload = TransactionQueuePayload.builder().transactionId(id)
                .fakeCardLast4("0000").build();

        processor.processTransaction(payload);

        assertThat(status(id)).isEqualTo("REJECTED");
        var written = org.mockito.ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptions).save(written.capture());
        assertThat(written.getValue().getStatus()).isEqualTo(SubscriptionStatus.DELINQUENT);
    }

    private void failAndRecord(UUID id) {
        assertThatThrownBy(() -> processor.processTransaction(payload(id)))
                .isInstanceOf(IncorrectResultSizeDataAccessException.class);
        assertThat(processor.handleProcessingFailure(payload(id), "ambiguous subscription")).isTrue();
    }

    private UUID transaction(UUID owner, UUID subscriptionId) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into queue_transactions values (?, ?, 'PENDING', 0, 3, ?)",
                id.toString(), owner.toString(), subscriptionId == null ? null : subscriptionId.toString());
        return id;
    }

    private UUID subscription(UUID owner, SubscriptionStatus status, boolean deleted) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into queue_subscriptions values (?, ?, ?, ?)", id.toString(), owner.toString(),
                status.name(), deleted ? Timestamp.from(Instant.now()) : null);
        return id;
    }

    private TransactionQueuePayload payload(UUID id) {
        return TransactionQueuePayload.builder().transactionId(id).fakeCardLast4("4242").build();
    }

    private int attempts(UUID id) {
        return jdbc.queryForObject("select attempts from queue_transactions where id = ?", Integer.class, id.toString());
    }

    private String status(UUID id) {
        return jdbc.queryForObject("select status from queue_transactions where id = ?", String.class, id.toString());
    }

    private int eventCount(UUID id) {
        return jdbc.queryForObject("select count(*) from queue_events where transaction_id = ?", Integer.class, id.toString());
    }
}
