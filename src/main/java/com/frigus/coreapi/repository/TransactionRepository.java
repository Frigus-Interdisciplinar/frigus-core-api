package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Transaction;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends BaseRepository<Transaction, UUID> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
