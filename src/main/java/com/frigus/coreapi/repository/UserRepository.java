package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.User;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends BaseRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id=:id")
    Optional<User> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

    @Query("select distinct u from User u left join fetch u.subscription subscription " +
            "left join fetch subscription.plan where u.id = :id")
    Optional<User> findByIdWithSubscriptionAndPlan(@Param("id") UUID id);
}
