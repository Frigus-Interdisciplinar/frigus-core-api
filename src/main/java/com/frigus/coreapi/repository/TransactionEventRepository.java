package com.frigus.coreapi.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.frigus.coreapi.model.TransactionEvent;

@Repository
public interface TransactionEventRepository extends BaseRepository<TransactionEvent, Integer> {
    List<TransactionEvent> findByTransactionIdOrderByCreatedAtAsc(UUID transactionId);
}
