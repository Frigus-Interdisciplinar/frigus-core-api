package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends BaseRepository<User, UUID> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update User u set u.planSelectionRequired = false, u.pendingPlanTransactionId = null where u.id = :id")
    void completePlanSelection(@org.springframework.data.repository.query.Param("id") UUID id);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update User u set u.pendingPlanTransactionId = :transactionId where u.id = :id and u.planSelectionRequired = true")
    void setPendingPlanTransaction(@org.springframework.data.repository.query.Param("id") UUID id, @org.springframework.data.repository.query.Param("transactionId") UUID transactionId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update User u set u.pendingPlanTransactionId = null where u.id = :id and u.pendingPlanTransactionId = :transactionId")
    void clearPendingPlanTransaction(@org.springframework.data.repository.query.Param("id") UUID id, @org.springframework.data.repository.query.Param("transactionId") UUID transactionId);

    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
    @org.springframework.data.jpa.repository.Query("select distinct u from User u left join fetch u.subscription subscription left join fetch subscription.plan where u.id = :id")
    Optional<User> findByIdWithSubscriptionAndPlan(@org.springframework.data.repository.query.Param("id") UUID id);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByGoogleId(String googleId);
}
