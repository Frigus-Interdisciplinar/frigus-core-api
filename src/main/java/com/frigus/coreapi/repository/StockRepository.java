package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Stock;
<<<<<<< HEAD
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockRepository extends BaseRepository<Stock, Integer> {
    Page<Stock> findByGroupId(UUID groupId, Pageable pageable);
=======

public interface StockRepository extends BaseRepository<Stock, Integer> {
>>>>>>> f050c93a2a16b2e7225b9b15bc1d8980692e8c86
}
