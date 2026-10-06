package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockRepository extends BaseRepository<Stock, Integer> {
    Page<Stock> findByGroupId(UUID groupId, Pageable pageable);
}
