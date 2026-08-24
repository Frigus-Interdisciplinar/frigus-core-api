package com.frigus.coreapi.repository;

import com.frigus.coreapi.enums.SubscriptionStatus;
import com.frigus.coreapi.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends BaseRepository<Subscription, UUID> {
    Optional<Subscription> findByUserId(UUID userId);
    Optional<Subscription> findByUserIdAndDeletedAtIsNull(UUID userId);
    Page<Subscription> findByDeletedAtIsNull(Pageable pageable);
    Page<Subscription> findByStatusAndDeletedAtIsNull(SubscriptionStatus status, Pageable pageable);
    Page<Subscription> findByPlanIdAndDeletedAtIsNull(Integer planId, Pageable pageable);
    Page<Subscription> findByStatusAndPlanIdAndDeletedAtIsNull(SubscriptionStatus status, Integer planId, Pageable pageable);

    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.canceledAt IS NULL AND s.deletedAt IS NULL AND s.currentPeriodEnd <= :now AND s.plan.price > 0")
    List<Subscription> findDueForRecurringBilling(@Param("status") SubscriptionStatus status, @Param("now") Instant now);

    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.canceledAt IS NOT NULL AND s.currentPeriodEnd <= :now AND s.deletedAt IS NULL")
    List<Subscription> findExpiredCanceledSubscriptions(@Param("status") SubscriptionStatus status, @Param("now") Instant now);
}

