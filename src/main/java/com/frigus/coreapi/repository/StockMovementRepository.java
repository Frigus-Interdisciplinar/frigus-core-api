package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockMovementRepository extends BaseRepository<StockMovement, Integer> {
    Page<StockMovement> findByStockProductIdOrderByDateDesc(Integer stockProductId, Pageable pageable);
}
