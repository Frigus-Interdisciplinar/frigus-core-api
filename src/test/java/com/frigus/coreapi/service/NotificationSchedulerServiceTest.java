package com.frigus.coreapi.service;

import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.enums.ListStatus;
import com.frigus.coreapi.enums.NotificationType;
import com.frigus.coreapi.enums.ProductListStatus;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.model.ShoppingList;
import com.frigus.coreapi.model.Stock;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.ShoppingListProductRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerServiceTest {
    @Mock
    private StockProductRepository stockProductRepository;
    @Mock
    private ShoppingListProductRepository shoppingListProductRepository;
    @Mock
    private NotificationService notificationService;

    private NotificationSchedulerService scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NotificationSchedulerService(
                stockProductRepository, shoppingListProductRepository, notificationService,
                org.mockito.Mockito.mock(com.frigus.coreapi.repository.GroupRepository.class),org.mockito.Mockito.mock(StockSummaryService.class));
        ReflectionTestUtils.setField(scheduler, "expirationWarningDays", 3);
        ReflectionTestUtils.setField(scheduler, "shoppingListReminderDelayDays", 3);
    }

    @Test
    void expirationJobNotifiesForProductsInWarningWindow() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
        LocalDate expiresOn = today.plusDays(2);
        UUID groupId = UUID.randomUUID();
        Group group = domesticGroup(groupId);
        StockProduct product = StockProduct.builder()
                .id(42)
                .product(Product.builder().name("Leite").build())
                .stock(Stock.builder().group(group).build())
                .expireDate(expiresOn)
                .build();
        when(stockProductRepository.findByExpireDateBetween(today, today.plusDays(3)))
                .thenReturn(List.of(product));

        scheduler.notifyProductsNearExpiration();

        verify(notificationService).notifyDomesticOrCommercialGroup(
                group,
                NotificationType.PRODUCT_NEAR_EXPIRATION,
                "Produto próximo da validade",
                "Leite vence em " + expiresOn + ".",
                "42",
                "near-expiration:42:" + expiresOn);
    }

    @Test
    void shoppingListJobQueriesOldPendingItemsAndNotifiesGroup() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
        UUID groupId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        Group group = domesticGroup(groupId);
        ShoppingList list = ShoppingList.builder()
                .id(listId)
                .stock(Stock.builder().group(group).build())
                .build();
        when(shoppingListProductRepository.findListsWithPendingProductsCreatedOnOrBefore(
                eq(ProductListStatus.PENDING), eq(ListStatus.OPEN), any(Instant.class)))
                .thenReturn(List.of(list));

        scheduler.remindOpenShoppingLists();

        verify(shoppingListProductRepository).findListsWithPendingProductsCreatedOnOrBefore(
                ProductListStatus.PENDING,
                ListStatus.OPEN,
                today.minusDays(2).atStartOfDay(java.time.ZoneId.of("America/Sao_Paulo")).toInstant());
        verify(notificationService).notifyDomesticOrCommercialGroup(
                group,
                NotificationType.SHOPPING_LIST_REMINDER,
                "Lembrete da lista de compras",
                "Ainda há itens adicionados há alguns dias que não foram comprados.",
                listId.toString(),
                "shopping-list-reminder:" + listId + ":" + today);
    }

    private Group domesticGroup(UUID id) {
        return Group.builder()
                .id(id)
                .name("Casa")
                .owner(User.builder().accountType(AccountType.DOMESTIC).build())
                .build();
    }
}
