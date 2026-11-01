package com.frigus.coreapi.repository;
import com.frigus.coreapi.model.*;
import java.util.*;
import java.time.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface BusinessExpenseRepository extends BaseRepository<BusinessExpense,UUID> {
 Page<BusinessExpense> findByGroupId(UUID groupId,Pageable pageable);
 List<BusinessExpense> findByGroupIdAndExpenseDateBetweenOrderByExpenseDateAsc(UUID groupId,LocalDate start,LocalDate end);
 Optional<BusinessExpense> findByShoppingListId(UUID listId);

}
