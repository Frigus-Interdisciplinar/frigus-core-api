package com.frigus.coreapi.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.frigus.coreapi.enums.TransactionStatus;
import com.frigus.coreapi.model.Transaction;

public interface TransactionRepository extends BaseRepository<Transaction, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transaction t where t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") UUID id);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    Page<Transaction> findByUserId(UUID userId, Pageable pageable);
    Page<Transaction> findByUserIdAndStatus(UUID userId, TransactionStatus status, Pageable pageable);
}
