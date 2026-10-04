package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockRepository extends BaseRepository<Stock, Integer> {
    @org.springframework.data.jpa.repository.Query("select s from Stock s where s.group.id=:groupId and s.deletedAt is null and s.group.deletedAt is null")
    Page<Stock> findByGroupId(@org.springframework.data.repository.query.Param("groupId") UUID groupId, Pageable pageable);
    java.util.List<Stock> findByGroupIdAndDeletedAtIsNull(java.util.UUID groupId);
    java.util.Optional<Stock> findByIdAndDeletedAtIsNull(Integer id);
}
