package com.frigus.coreapi.repository;
import com.frigus.coreapi.model.*;
import java.util.*;
import java.time.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface ShoppingListRepository extends BaseRepository<ShoppingList,UUID> {
 Page<ShoppingList> findByStockId(Integer stockId,Pageable pageable);
 Page<ShoppingList> findByStockIdAndStatus(Integer stockId,com.frigus.coreapi.enums.ListStatus status,Pageable pageable);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select l from ShoppingList l where l.id=:id")
 Optional<ShoppingList> lockById(@Param("id") UUID id);
 @Query("select count(l) from ShoppingList l where l.stock.group.id=:groupId and l.status=com.frigus.coreapi.enums.ListStatus.COMPLETED and l.completedAt>=:start and l.completedAt<:end")
 long countCompleted(@Param("groupId") UUID groupId,@Param("start") Instant start,@Param("end") Instant end);

}
