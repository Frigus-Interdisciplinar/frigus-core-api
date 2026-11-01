package com.frigus.coreapi.service;

import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.model.StockProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.UUID;

/** Attaches a confirmed purchase to a newly persisted lot; never adds its stock quantity again. */
@Service
public class StockPurchaseRecorder {
    private final JdbcTemplate jdbc;
    private final MoneySavingLedgerService ledger;
    private final Clock clock;

    @Autowired
    public StockPurchaseRecorder(JdbcTemplate jdbc, MoneySavingLedgerService ledger) {
        this(jdbc, ledger, Clock.systemUTC());
    }

    StockPurchaseRecorder(JdbcTemplate jdbc, MoneySavingLedgerService ledger, Clock clock) {
        this.jdbc = jdbc;
        this.ledger = ledger;
        this.clock = clock;
    }

    /** Same first lock as FoodPurchaseService; acquire it before inserting the lot. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockGroup(UUID groupId) {
        jdbc.queryForObject("SELECT id FROM groups WHERE id=? AND deleted_at IS NULL FOR UPDATE", UUID.class, groupId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordCreatedLot(StockProduct lot) {
        BigDecimal price = lot.getPurchaseUnitPrice();
        if (price == null || lot.getQuantity() == null || lot.getQuantity() == 0) return;
        String categoryGroup;
        try {
            categoryGroup = MoneySavingLedgerService.categoryGroup(lot.getCategory().name());
        } catch (IllegalArgumentException exception) {
            // Non-food products retain their lot price but do not enter food spending/saving.
            return;
        }
        if (price.signum() <= 0 || price.stripTrailingZeros().scale() > 2 ||
                price.compareTo(new BigDecimal("99999999.99")) > 0 || lot.getQuantity() < 0)
            throw new BadRequestException("Preço ou quantidade inválidos", "Informe quantidade e preço de aquisição válidos");
        if (lot.getId() == null || lot.getId() <= 0 || lot.getProduct().getUnitOfMeasure() == null)
            throw new IllegalStateException("Persist the complete stock lot before recording its purchase");

        UUID groupId = lot.getStock().getGroup().getId();
        String key = "stock-product/" + lot.getId();
        String hash = hash(groupId + "/" + lot.getId() + "/" + lot.getStock().getId() + "/" +
                lot.getProduct().getId() + "/" + lot.getQuantity() + "/" + lot.getExpireDate() + "/" +
                lot.getCategory().name() + "/" + lot.getProduct().getUnitOfMeasure().name() + "/" +
                price.stripTrailingZeros().toPlainString());
        var prior = jdbc.query("SELECT request_hash,deleted_at FROM food_purchases WHERE group_id=? AND idempotency_key=?",
                (rs, n) -> new PreviousPurchase(rs.getString(1), rs.getObject(2) != null), groupId, key);
        if (!prior.isEmpty()) {
            if (prior.get(0).deleted() || !hash.equals(prior.get(0).hash()))
                throw new ConflictException("Compra do lote já registrada", "O lote já possui outro registro de aquisição");
            return;
        }

        LocalDate purchaseDate = LocalDate.now(clock.withZone(ZoneId.of(lot.getStock().getGroup().getTimeZone())));
        UUID purchaseId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO food_purchases(id,group_id,purchase_date,source,idempotency_key,request_hash)
                VALUES (?,?,?,'USER_CONFIRMED',?,?)
                """, purchaseId, groupId, purchaseDate, key, hash);
        var lineKey = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO food_purchase_lines(purchase_id,stock_key,stock_product_id,product_id,product_name,category,
                      category_group,unit_of_measure,expire_date,quantity,unit_price)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """, new String[]{"id"});
            statement.setObject(1, purchaseId);
            statement.setInt(2, lot.getId());
            statement.setInt(3, lot.getId());
            statement.setInt(4, lot.getProduct().getId());
            statement.setString(5, lot.getProduct().getName());
            statement.setString(6, lot.getCategory().name());
            statement.setString(7, categoryGroup);
            statement.setString(8, lot.getProduct().getUnitOfMeasure().name());
            statement.setObject(9, lot.getExpireDate());
            statement.setInt(10, lot.getQuantity());
            statement.setBigDecimal(11, price);
            return statement;
        }, lineKey);
        Number lineId = lineKey.getKey();
        if (lineId == null) throw new IllegalStateException("Purchase line was not persisted");
        jdbc.update("""
                INSERT INTO money_supplies(stock_key,stock_product_id,purchase_line_id,available_on,quantity,unknown_cost)
                VALUES (?,?,?,?,?,false)
                """, lot.getId(), lot.getId(), lineId.longValue(), purchaseDate, lot.getQuantity());
        // No ensureOpening here: these exact units are already represented by the known-cost supply.
        ledger.rebuild(lot.getId());
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record PreviousPurchase(String hash, boolean deleted) {}
}
