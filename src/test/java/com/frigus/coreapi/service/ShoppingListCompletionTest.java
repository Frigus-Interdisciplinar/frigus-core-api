package com.frigus.coreapi.service;

import com.frigus.coreapi.enums.ListStatus;
import com.frigus.coreapi.enums.ProductListStatus;
import com.frigus.coreapi.mapper.ShoppingListMapper;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.ShoppingList;
import com.frigus.coreapi.model.Stock;
import com.frigus.coreapi.repository.ShoppingListProductRepository;
import com.frigus.coreapi.repository.ShoppingListRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import com.frigus.coreapi.repository.StockRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShoppingListCompletionTest {
    @Test
    void autoCompletingAfterLastItemSetsCompletionTimeForReports() {
        UUID listId = UUID.randomUUID();
        ShoppingListRepository lists = mock(ShoppingListRepository.class);
        ShoppingListMapper mapper = mock(ShoppingListMapper.class);
        StockRepository stocks = mock(StockRepository.class);
        GroupAccessService access = mock(GroupAccessService.class);
        ShoppingListProductService items = mock(ShoppingListProductService.class);
        ShoppingListProductRepository listProducts = mock(ShoppingListProductRepository.class);
        StockProductRepository stockProducts = mock(StockProductRepository.class);
        ShoppingListService service = new ShoppingListService(
                lists, mapper, stocks, access, items, listProducts, stockProducts);
        ShoppingList list = ShoppingList.builder().id(listId).status(ListStatus.OPEN).build();
        when(lists.findById(listId)).thenReturn(Optional.of(list));
        when(listProducts.countByListId(listId)).thenReturn(1L);
        when(listProducts.existsByListIdAndStatusNotIn(listId, List.of(ProductListStatus.PURCHASED, ProductListStatus.REMOVED)))
                .thenReturn(false);
        when(lists.save(any(ShoppingList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.checkAndAutoCloseList(listId);

        assertThat(list.getStatus()).isEqualTo(ListStatus.COMPLETED);
        assertThat(list.getCompletedAt()).isNotNull();
    }

    @Test
    void completesListAndPersistsSupplierAndCompletionTime() {
        UUID listId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        ShoppingListRepository lists = mock(ShoppingListRepository.class);
        ShoppingListMapper mapper = mock(ShoppingListMapper.class);
        StockRepository stocks = mock(StockRepository.class);
        GroupAccessService access = mock(GroupAccessService.class);
        ShoppingListProductService items = mock(ShoppingListProductService.class);
        ShoppingListProductRepository listProducts = mock(ShoppingListProductRepository.class);
        StockProductRepository stockProducts = mock(StockProductRepository.class);
        ShoppingListService service = new ShoppingListService(
                lists, mapper, stocks, access, items, listProducts, stockProducts);

        ShoppingList list = ShoppingList.builder()
                .id(listId)
                .stock(Stock.builder().id(12).group(Group.builder().id(groupId).build()).build())
                .status(ListStatus.OPEN)
                .build();
        when(lists.findById(listId)).thenReturn(Optional.of(list));
        when(lists.save(any(ShoppingList.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(list)).thenAnswer(invocation -> com.frigus.coreapi.dto.shoppinglist.ShoppingListResponseDto.builder()
                .id(listId)
                .supplier(list.getSupplier())
                .completedAt(list.getCompletedAt())
                .build());
        when(items.findByListId(listId)).thenReturn(List.of());
        when(listProducts.countByListId(listId)).thenReturn(0L);
        when(listProducts.countByListIdAndStatus(listId, ProductListStatus.PURCHASED)).thenReturn(0L);
        when(listProducts.countByListIdAndStatus(listId, ProductListStatus.PENDING)).thenReturn(0L);

        var response = service.closeList(listId, "  Mercado QA  ");

        assertThat(list.getStatus()).isEqualTo(ListStatus.COMPLETED);
        assertThat(list.getSupplier()).isEqualTo("Mercado QA");
        assertThat(list.getCompletedAt()).isNotNull();
        assertThat(response.getSupplier()).isEqualTo("Mercado QA");
        assertThat(response.getCompletedAt()).isNotNull();
    }
}
