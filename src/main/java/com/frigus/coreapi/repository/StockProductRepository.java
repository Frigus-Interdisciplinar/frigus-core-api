package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.StockProduct;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface StockProductRepository extends BaseRepository<StockProduct, Integer> {
    Page<StockProduct> findByStockId(Integer stockId, Pageable pageable);

    boolean existsByProductIdAndStockIdAndExpireDate(Integer productId, Integer stockId, LocalDate expireDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stockProduct from StockProduct stockProduct where stockProduct.id = :id")
    Optional<StockProduct> findByIdForUpdate(@Param("id") Integer id);
}
