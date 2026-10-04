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
    private final com.frigus.coreapi.repository.GroupRepository groups;
    private final StockSummaryService summary;
    @Value("${app.time-zone:America/Sao_Paulo}") private String timeZone="America/Sao_Paulo";

    @Value("${notifications.expiration.warning-days:3}")
    private int expirationWarningDays;

    @Value("${notifications.shopping-list-reminder.delay-days:3}")
    private int shoppingListReminderDelayDays;

    @Scheduled(cron = "${notifications.expiration.cron:0 0 9 * * *}",zone="${app.time-zone:America/Sao_Paulo}")
    @Transactional
    public void notifyProductsNearExpiration() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of(timeZone));
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

    @Scheduled(cron = "${notifications.shopping-list-reminder.cron:0 0 10 * * *}",zone="${app.time-zone:America/Sao_Paulo}")
    @Transactional
    public void remindOpenShoppingLists() {
        LocalDate dueDate = LocalDate.now(java.time.ZoneId.of(timeZone)).minusDays(shoppingListReminderDelayDays);
        for (ShoppingList list : shoppingListProductRepository.findListsWithPendingProductsCreatedOnOrBefore(
                ProductListStatus.PENDING, ListStatus.OPEN,
                dueDate.plusDays(1).atStartOfDay(java.time.ZoneId.of(timeZone)).toInstant())) {
            notificationService.notifyDomesticOrCommercialGroup(
                    list.getStock().getGroup(),
                    NotificationType.SHOPPING_LIST_REMINDER,
                    "Lembrete da lista de compras",
                    "Ainda há itens adicionados há alguns dias que não foram comprados.",
                    list.getId().toString(),
                    "shopping-list-reminder:" + list.getId() + ":" + LocalDate.now(java.time.ZoneId.of(timeZone)));
        }
    }

    @Scheduled(cron="${notifications.weekly-summary.cron:0 0 9 * * MON}",zone="${app.time-zone:America/Sao_Paulo}")
    @Transactional
    public void notifyWeeklySummary(){
        for(var group:groups.findByDeletedAtIsNull()){
            var data=summary.summarize(group.getId());
            notificationService.notifyDomesticOrCommercialGroup(group,NotificationType.WEEKLY_SUMMARY,"Resumo semanal do estoque",
                data.productCount()+" produtos no estoque; "+data.nearExpiration().size()+" itens próximos da validade; "+data.pendingShoppingItems()+" compras pendentes.",
                group.getId().toString(),"weekly-summary:"+group.getId()+":"+data.weekStart());
        }
    }
}
