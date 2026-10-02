package com.frigus.coreapi.service;

import com.frigus.coreapi.enums.NotificationType;
import com.frigus.coreapi.enums.ProductListStatus;
import com.frigus.coreapi.enums.ListStatus;
import com.frigus.coreapi.model.ShoppingList;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.repository.ShoppingListProductRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class NotificationSchedulerService {
    private final StockProductRepository stockProductRepository;
    private final ShoppingListProductRepository shoppingListProductRepository;
    private final NotificationService notificationService;

    @Value("${notifications.expiration.warning-days:3}")
    private int expirationWarningDays;

    @Value("${notifications.shopping-list-reminder.delay-days:3}")
    private int shoppingListReminderDelayDays;

    @Scheduled(cron = "${notifications.expiration.cron:0 0 9 * * *}")
    @Transactional
    public void notifyProductsNearExpiration() {
        LocalDate today = LocalDate.now();
        LocalDate warningLimit = today.plusDays(expirationWarningDays);
        for (StockProduct stockProduct : stockProductRepository.findByExpireDateBetween(today, warningLimit)) {
            String productName = stockProduct.getProduct().getName();
            notificationService.notifyDomesticOrCommercialGroup(
                    stockProduct.getStock().getGroup(),
                    NotificationType.PRODUCT_NEAR_EXPIRATION,
                    "Produto próximo da validade",
                    productName + " vence em " + stockProduct.getExpireDate() + ".",
                    String.valueOf(stockProduct.getId()),
                    "near-expiration:" + stockProduct.getId() + ":" + stockProduct.getExpireDate());
        }
    }

    @Scheduled(cron = "${notifications.shopping-list-reminder.cron:0 0 10 * * *}")
    @Transactional
    public void remindOpenShoppingLists() {
        LocalDate dueDate = LocalDate.now().minusDays(shoppingListReminderDelayDays);
        for (ShoppingList list : shoppingListProductRepository.findListsWithPendingProductsCreatedOnOrBefore(
                ProductListStatus.PENDING, ListStatus.OPEN,
                dueDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())) {
            notificationService.notifyDomesticOrCommercialGroup(
                    list.getStock().getGroup(),
                    NotificationType.SHOPPING_LIST_REMINDER,
                    "Lembrete da lista de compras",
                    "Ainda há itens adicionados há alguns dias que não foram comprados.",
                    list.getId().toString(),
                    "shopping-list-reminder:" + list.getId() + ":" + LocalDate.now());
        }
    }
}
