package com.frigus.coreapi.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.frigus.coreapi.enums.TransactionStatus;
import com.frigus.coreapi.model.Transaction;

public interface TransactionRepository extends BaseRepository<Transaction, UUID> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    Page<Transaction> findByUserId(UUID userId, Pageable pageable);
    Page<Transaction> findByUserIdAndStatus(UUID userId, TransactionStatus status, Pageable pageable);
}
