package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.StockProduct;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockProductRepository extends BaseRepository<StockProduct, Integer> {
    @Query("select sp from StockProduct sp join fetch sp.product where sp.stock.id=:stockId and sp.deletedAt is null and sp.stock.deletedAt is null")
    Page<StockProduct> findByStockId(@Param("stockId") Integer stockId, Pageable pageable);

    @Query("select sp from StockProduct sp where sp.stock.id=:stockId and sp.deletedAt is null and sp.stock.deletedAt is null")
    List<StockProduct> findByStockId(@Param("stockId") Integer stockId);

    List<StockProduct> findByStockIdAndExpireDateLessThanEqual(Integer stockId, LocalDate date);

    @Query("select sp from StockProduct sp where sp.expireDate between :startDate and :endDate and sp.deletedAt is null and sp.quantity>0 and sp.stock.deletedAt is null and sp.stock.group.deletedAt is null")
    List<StockProduct> findByExpireDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    boolean existsByProductIdAndStockIdAndExpireDate(Integer productId, Integer stockId, LocalDate expireDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stockProduct from StockProduct stockProduct where stockProduct.id = :id and stockProduct.deletedAt is null and stockProduct.stock.deletedAt is null")
    Optional<StockProduct> findByIdForUpdate(@Param("id") Integer id);
    @Query("select sp from StockProduct sp join fetch sp.product p join fetch sp.stock s where s.group.id=:groupId and s.deletedAt is null and sp.deletedAt is null")
    List<StockProduct> findActiveByGroup(@Param("groupId") java.util.UUID groupId);
    @Query("select sp from StockProduct sp where sp.stock.id=:stockId and sp.stock.deletedAt is null and sp.deletedAt is null")
    List<StockProduct> findActiveByStock(@Param("stockId") Integer stockId);
    boolean existsByProductIdAndStockIdAndExpireDateAndBatchAndDeletedAtIsNull(Integer productId,Integer stockId,LocalDate expireDate,String batch);
}
