package com.frigus.coreapi.integration;

import com.frigus.coreapi.dto.moneysaving.MoneySavingDtos;
import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.UnitOfMeasure;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.model.Stock;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.FoodPurchaseService;
import com.frigus.coreapi.service.GroupAccessService;
import com.frigus.coreapi.service.MoneySavingLedgerService;
import com.frigus.coreapi.service.MoneySavingReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class MoneySavingPersistenceTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("money_saving_test")
            .withStartupTimeout(java.time.Duration.ofMinutes(3))
            .withCopyFileToContainer(MountableFile.forHostPath("db/script.sql"), "/docker-entrypoint-initdb.d/001-schema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("db/migrations/006_money_saving.sql"), "/test/006_money_saving.sql");

    JdbcTemplate jdbc;
    TransactionTemplate tx;
    FoodPurchaseService purchases;
    MoneySavingLedgerService ledger;
    MoneySavingReportService reports;
    UUID groupId;
    int stockId;
    int productId;
    int itemId;
    User user;
    HikariDataSource source;

    @BeforeEach
    void setup() {
        source = new HikariDataSource();
        source.setJdbcUrl(POSTGRES.getJdbcUrl() + "&sslmode=disable");
        source.setUsername(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        source.setMaximumPoolSize(4);
        jdbc = new JdbcTemplate(source);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.execute("TRUNCATE users, products, recipes RESTART IDENTITY CASCADE");
        UUID userId = jdbc.queryForObject("""
                INSERT INTO users(name,email,hash_password,birth_date,role,account_type)
                VALUES ('Test','money@test.local','hash','1990-01-01','USER','DOMESTIC') RETURNING id
                """, UUID.class);
        user = User.builder().id(userId).build();
        groupId = jdbc.queryForObject("INSERT INTO groups(owner_id,name) VALUES (?, 'Home') RETURNING id", UUID.class, userId);
        jdbc.update("INSERT INTO user_groups(user_id,group_id) VALUES (?,?)", userId, groupId);
        stockId = jdbc.queryForObject("INSERT INTO stocks(group_id,name) VALUES (?, 'Fridge') RETURNING id", Integer.class, groupId);
        productId = jdbc.queryForObject("""
                INSERT INTO products(name,category,storage_place,unit_price,unit_of_measure)
                VALUES ('Milk','DAIRY','FRIDGE',99.00,'UNIT') RETURNING id
                """, Integer.class);
        itemId = jdbc.queryForObject("""
                INSERT INTO stock_products(product_id,stock_id,quantity,expire_date,category)
                VALUES (?, ?, 5, '2099-01-01', 'DAIRY') RETURNING id
                """, Integer.class, productId, stockId);
        GroupAccessService access = new GroupAccessService(null, null) {
            @Override public void requireGroupAccess(UUID id) {
                if (!groupId.equals(id)) throw new IllegalArgumentException("Other group");
            }
            @Override public User requireCurrentUser() { return user; }
        };
        ledger = new MoneySavingLedgerService(jdbc);
        purchases = new FoodPurchaseService(jdbc, access, ledger, new ObjectMapper());
        reports = new MoneySavingReportService(new NamedParameterJdbcTemplate(source), access);
    }

    @AfterEach
    void closePool() {
        if (source != null) source.close();
    }

    @Test
    void purchaseFifoEditAndPaginationAgreeWithManualAmounts() {
        LocalDate day = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        MoneySavingDtos.PurchaseInput input = new MoneySavingDtos.PurchaseInput(day, "USER_CONFIRMED", null, null,
                List.of(new MoneySavingDtos.PurchaseLineInput(null, stockId, productId, LocalDate.of(2099, 1, 1),
                        2, new BigDecimal("4.00"), null)));
        MoneySavingDtos.Purchase purchase = tx.execute(status -> purchases.create(groupId, "receipt-a", input));
        assertThat(tx.execute(status -> purchases.create(groupId, "receipt-a", input)).id()).isEqualTo(purchase.id());
        assertThat(balance()).isEqualTo(7);
        StockProduct item = item(itemId, LocalDate.of(2099, 1, 1));
        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE stock_products SET quantity=quantity-6 WHERE id=?", itemId);
            ledger.recordMovement(item, 7, -6, "CONSUMED", null, Instant.now());
        });
        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE stock_products SET quantity=quantity-1 WHERE id=?", itemId);
            ledger.recordMovement(item, 1, -1, "CONSUMED", null, Instant.now());
        });
        MoneySavingDtos.Summary before = reports.summary(groupId, day, day.plusDays(1));
        assertThat(before.spending().amount()).isEqualByComparingTo("8.00");
        assertThat(before.saving().amount()).isEqualByComparingTo("4.00");
        assertThat(before.saving().actions()).isEqualTo(2);
        assertThat(reports.events(groupId, day, day.plusDays(1), null, "CONSUMED", 0, 1).totalElements()).isEqualTo(2);
        var corrected = new MoneySavingDtos.PurchaseInput(day, "USER_CONFIRMED", null, 1,
                List.of(new MoneySavingDtos.PurchaseLineInput(purchase.lines().get(0).id(), null, null, null,
                        2, new BigDecimal("5.00"), null)));
        tx.execute(status -> purchases.update(groupId, purchase.id(), corrected));
        MoneySavingDtos.Summary after = reports.summary(groupId, day, day.plusDays(1));
        assertThat(after.spending().amount()).isEqualByComparingTo("10.00");
        assertThat(after.saving().amount()).isEqualByComparingTo("5.00");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM food_purchase_revisions", Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> purchases.delete(groupId, purchase.id())))
                .hasMessageContaining("saídas vinculadas");
    }

    @Test
    void expiredDiscardCanBeCanceledWithoutChangingPurchaseSpending() {
        LocalDate day = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        LocalDate expiry = day.minusDays(1);
        var input = new MoneySavingDtos.PurchaseInput(day.minusDays(2), "USER_CONFIRMED", null, null,
                List.of(new MoneySavingDtos.PurchaseLineInput(null, stockId, productId, expiry,
                        2, new BigDecimal("3.00"), null)));
        var purchase = tx.execute(status -> purchases.create(groupId, "receipt-b", input));
        int expiredId = purchase.lines().get(0).stockProductId();
        StockProduct item = item(expiredId, expiry);
        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE stock_products SET quantity=quantity-1 WHERE id=?", expiredId);
            ledger.recordDiscard(item, 2, 1, "EXPIRED", null, Instant.now());
        });
        var before = reports.summary(groupId, day, day.plusDays(1));
        assertThat(before.loss().amount()).isEqualByComparingTo("3.00");
        assertThat(before.loss().actions()).isEqualTo(1);
        long exitId = jdbc.queryForObject("SELECT id FROM money_exits WHERE stock_product_id=?", Long.class, expiredId);
        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE stock_products SET quantity=quantity+1 WHERE id=?", expiredId);
            jdbc.update("UPDATE money_exits SET canceled_at=now() WHERE id=?", exitId);
            ledger.rebuild(expiredId);
        });
        assertThat(reports.summary(groupId, day, day.plusDays(1)).loss().amount()).isEqualByComparingTo("0.00");
        assertThat(jdbc.queryForObject("SELECT quantity FROM stock_products WHERE id=?", Integer.class, expiredId)).isEqualTo(2);
    }

    @Test
    void localDateControlsExpirationAndMonthlySeries() {
        LocalDate expiry = LocalDate.of(2025, 12, 31);
        var input = new MoneySavingDtos.PurchaseInput(LocalDate.of(2025, 12, 30), "USER_CONFIRMED", null, null,
                List.of(new MoneySavingDtos.PurchaseLineInput(null, stockId, productId, expiry,
                        2, new BigDecimal("10.00"), null)));
        var purchase = tx.execute(status -> purchases.create(groupId, "year-boundary", input));
        int id = purchase.lines().get(0).stockProductId();
        StockProduct item = item(id, expiry);
        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE stock_products SET quantity=quantity-1 WHERE id=?", id);
            ledger.recordMovement(item, 2, -1, "CONSUMED", null, Instant.parse("2026-01-01T02:00:00Z"));
        });
        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE stock_products SET quantity=quantity-1 WHERE id=?", id);
            ledger.recordDiscard(item, 1, 1, "EXPIRED", null, Instant.parse("2026-01-02T02:00:00Z"));
        });
        var december = reports.summary(groupId, LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 1));
        var january = reports.summary(groupId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));
        assertThat(december.saving().amount()).isEqualByComparingTo("10.00");
        assertThat(december.loss().amount()).isEqualByComparingTo("0.00");
        assertThat(january.saving().amount()).isEqualByComparingTo("0.00");
        assertThat(january.loss().amount()).isEqualByComparingTo("10.00");
        assertThat(december.months().get(5).month()).isEqualTo("2025-12");
        assertThat(december.months().get(5).spent()).isEqualByComparingTo("20.00");
    }

    @Test
    void purchaseCorrectionsRemainPossibleAfterStockItemRemoval() {
        LocalDate day = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        LocalDate expiry = LocalDate.of(2098, 1, 1);
        var input = new MoneySavingDtos.PurchaseInput(day, "USER_CONFIRMED", null, null,
                List.of(new MoneySavingDtos.PurchaseLineInput(null, stockId, productId, expiry,
                        2, new BigDecimal("3.00"), null)));
        var purchase = tx.execute(status -> purchases.create(groupId, "removed-item", input));
        int id = purchase.lines().get(0).stockProductId();
        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE stock_products SET quantity=quantity-1 WHERE id=?", id);
            ledger.recordMovement(item(id, expiry), 2, -1, "CONSUMED", null, Instant.now());
        });
        jdbc.update("DELETE FROM stock_products WHERE id=?", id);
        var corrected = new MoneySavingDtos.PurchaseInput(day, "USER_CONFIRMED", null, 1,
                List.of(new MoneySavingDtos.PurchaseLineInput(purchase.lines().get(0).id(), null, null, null,
                        2, new BigDecimal("4.00"), null)));
        tx.execute(status -> purchases.update(groupId, purchase.id(), corrected));
        var summary = reports.summary(groupId, day, day.plusDays(1));
        assertThat(summary.spending().amount()).isEqualByComparingTo("8.00");
        assertThat(summary.saving().amount()).isEqualByComparingTo("4.00");
        assertThat(purchases.get(groupId, purchase.id()).lines().get(0).stockProductId()).isNull();
    }

    @Test
    void moneySavingMigrationCanBeReappliedWithoutChangingExistingRows() throws Exception {
        int before = jdbc.queryForObject("SELECT count(*) FROM stock_products", Integer.class);
        for (int run = 0; run < 2; run++) {
            var command = POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", POSTGRES.getDatabaseName(),
                    "-v", "ON_ERROR_STOP=1", "-f", "/test/006_money_saving.sql");
            assertThat(command.getExitCode()).as(command.getStderr()).isZero();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_products", Integer.class)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_trigger WHERE tgname='trg_stock_movements_apply'", Integer.class)).isZero();
    }

    @Test
    void simultaneousRetriesCreateOnePurchaseAndOneStockIncrease() throws Exception {
        LocalDate day = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        var input = new MoneySavingDtos.PurchaseInput(day, "USER_CONFIRMED", null, null,
                List.of(new MoneySavingDtos.PurchaseLineInput(null, stockId, productId, LocalDate.of(2099, 1, 1),
                        2, new BigDecimal("4.00"), null)));
        var start = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                start.await();
                return tx.execute(status -> purchases.create(groupId, "same-request", input)).id();
            });
            var second = executor.submit(() -> {
                start.await();
                return tx.execute(status -> purchases.create(groupId, "same-request", input)).id();
            });
            start.countDown();
            assertThat(first.get()).isEqualTo(second.get());
        } finally {
            executor.shutdownNow();
        }
        assertThat(balance()).isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM food_purchases", Integer.class)).isEqualTo(1);
    }

    @Test
    void mixedCostLotsSplitCategoryTotalsAndMatchEitherCategoryFilter() {
        LocalDate day = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        for (var spec : List.of(new String[] {"lot-dairy", "DAIRY", "4.00"},
                new String[] {"lot-beverage", "BEVERAGE", "6.00"})) {
            var input = new MoneySavingDtos.PurchaseInput(day, "USER_CONFIRMED", null, null,
                    List.of(new MoneySavingDtos.PurchaseLineInput(null, stockId, productId, LocalDate.of(2099, 1, 1),
                            1, new BigDecimal(spec[2]), spec[1])));
            tx.execute(status -> purchases.create(groupId, spec[0], input));
        }
        var item = item(itemId, LocalDate.of(2099, 1, 1));
        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE stock_products SET quantity=quantity-5 WHERE id=?", itemId);
            ledger.recordMovement(item, 7, -5, null, null, Instant.now());
        });
        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE stock_products SET quantity=quantity-2 WHERE id=?", itemId);
            ledger.recordMovement(item, 2, -2, "CONSUMED", null, Instant.now());
        });
        var summary = reports.summary(groupId, day, day.plusDays(1));
        assertThat(summary.saving().amount()).isEqualByComparingTo("10.00");
        assertThat(summary.categories().stream().filter(c -> c.category().equals("DAIRY")).findFirst().orElseThrow().saved())
                .isEqualByComparingTo("4.00");
        assertThat(summary.categories().stream().filter(c -> c.category().equals("BEVERAGE")).findFirst().orElseThrow().saved())
                .isEqualByComparingTo("6.00");
        var dairy = reports.events(groupId, day, day.plusDays(1), "DAIRY", "CONSUMED", 0, 10);
        var beverage = reports.events(groupId, day, day.plusDays(1), "BEVERAGE", "CONSUMED", 0, 10);
        assertThat(dairy.totalElements()).isEqualTo(1);
        assertThat(beverage.totalElements()).isEqualTo(1);
        assertThat(dairy.content().get(0).categoryGroup()).isNull();
        assertThat(dairy.content().get(0).categoryAmounts()).hasSize(2);
    }

    private int balance() { return jdbc.queryForObject("SELECT quantity FROM stock_products WHERE id=?", Integer.class, itemId); }

    private StockProduct item(int id, LocalDate expiry) {
        return StockProduct.builder().id(id).expireDate(expiry).category(Category.DAIRY)
                .product(Product.builder().id(productId).name("Milk").unitOfMeasure(UnitOfMeasure.UNIT).build())
                .stock(Stock.builder().id(stockId).group(Group.builder().id(groupId).timeZone("America/Sao_Paulo").build()).build())
                .build();
    }
}
