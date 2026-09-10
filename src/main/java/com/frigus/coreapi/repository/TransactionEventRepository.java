package com.frigus.coreapi.repository;

import java.util.List;
import java.util.UUID;

import com.frigus.coreapi.model.TransactionEvent;

public interface TransactionEventRepository extends BaseRepository<TransactionEvent, Integer> {
    List<TransactionEvent> findByTransactionIdOrderByCreatedAtAsc(UUID transactionId);
}
