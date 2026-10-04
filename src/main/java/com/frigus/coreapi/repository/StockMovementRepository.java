package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockMovementRepository extends BaseRepository<StockMovement, Integer> {
    Page<StockMovement> findByStockProductIdOrderByDateDesc(Integer stockProductId, Pageable pageable);
    @org.springframework.data.jpa.repository.Query("select m.stockProduct.product.unitOfMeasure, sum(m.quantity) from StockMovement m where m.stockProduct.stock.group.id=:groupId and m.movementType=com.frigus.coreapi.enums.MovementType.OUT and m.purpose='CONSUMPTION' and m.date>=:start and m.date<:end group by m.stockProduct.product.unitOfMeasure")
    java.util.List<Object[]> consumedByUnit(@org.springframework.data.repository.query.Param("groupId") java.util.UUID groupId,@org.springframework.data.repository.query.Param("start") java.time.Instant start,@org.springframework.data.repository.query.Param("end") java.time.Instant end);
}
