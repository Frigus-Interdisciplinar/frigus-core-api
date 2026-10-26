package com.frigus.coreapi.service;

import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.model.StockProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MoneySavingLedgerService {
    private final JdbcTemplate jdbc;

    public static String categoryGroup(String category) {
        return switch (category) {
            case "FRUIT", "VEGETABLE" -> "HORTIFRUTI";
            case "MEAT" -> "MEAT";
            case "DAIRY" -> "DAIRY";
            case "GRAIN" -> "GRAIN";
            case "BEVERAGE" -> "BEVERAGE";
            default -> throw new IllegalArgumentException("Categoria não alimentar: " + category);
        };
    }

    public void ensureOpening(Integer stockProductId, int openingQuantity) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM money_supplies WHERE stock_key=?", Integer.class, stockProductId);
        if (count == 0 && openingQuantity > 0) {
            addUnknown(stockProductId, openingQuantity, LocalDate.of(1900, 1, 1));
        }
    }

    public void addUnknown(Integer stockProductId, int quantity, LocalDate date) {
        if (quantity > 0) jdbc.update("INSERT INTO money_supplies(stock_key,stock_product_id,available_on,quantity,unknown_cost) VALUES (?,?,?,?,true)",
                stockProductId, stockProductId, date, quantity);
    }

    public void recordMovement(StockProduct item, int openingQuantity, int delta, String reasonCode, Integer movementId, Instant at) {
        ensureOpening(item.getId(), openingQuantity);
        LocalDate localDate = at.atZone(ZoneId.of(item.getStock().getGroup().getTimeZone())).toLocalDate();
        if (delta > 0) addUnknown(item.getId(), delta, localDate);
        if (delta < 0) {
            addExit(item, -delta, "CONSUMED".equals(reasonCode) ? "CONSUMED" : "OTHER", reasonCode, movementId, null, at);
        }
    }

    public void recordDiscard(StockProduct item, int openingQuantity, int quantity, String reasonCode, Integer discardId, Instant at) {
        ensureOpening(item.getId(), openingQuantity);
        addExit(item, quantity, "DISCARDED", reasonCode, null, discardId, at);
    }

    private void addExit(StockProduct item, int quantity, String kind, String reasonCode, Integer movementId, Integer discardId, Instant at) {
        LocalDate localDate = at.atZone(ZoneId.of(item.getStock().getGroup().getTimeZone())).toLocalDate();
        jdbc.update("""
                INSERT INTO money_exits(group_id,stock_key,stock_product_id,product_name,category,unit_of_measure,expire_date,
                  occurred_at,local_date,quantity,kind,reason_code,movement_id,discard_id)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, item.getStock().getGroup().getId(), item.getId(), item.getId(), item.getProduct().getName(),
                item.getCategory().name(), item.getProduct().getUnitOfMeasure().name(), item.getExpireDate(),
                java.sql.Timestamp.from(at), localDate, quantity, kind, reasonCode, movementId, discardId);
        rebuild(item.getId());
    }

    public void cancelDiscard(Integer discardId, Integer stockProductId) {
        jdbc.update("UPDATE money_exits SET canceled_at=now() WHERE discard_id=? AND canceled_at IS NULL", discardId);
        rebuild(stockProductId);
    }

    public boolean hasDiscardEvent(Integer discardId) {
        return jdbc.queryForObject("SELECT count(*) FROM money_exits WHERE discard_id=?", Integer.class, discardId) > 0;
    }

    public void rebuild(Integer stockProductId) {
        List<Supply> supplies = jdbc.query("""
                SELECT s.id,s.quantity,s.available_on,s.unknown_cost,l.unit_price,l.category_group
                FROM money_supplies s LEFT JOIN food_purchase_lines l ON l.id=s.purchase_line_id
                LEFT JOIN food_purchases p ON p.id=l.purchase_id
                WHERE s.stock_key=? AND (p.id IS NULL OR p.deleted_at IS NULL)
                ORDER BY s.available_on,s.id
                """, (rs, n) -> new Supply(rs.getLong(1), rs.getInt(2), rs.getDate(3).toLocalDate(),
                rs.getBoolean(4), rs.getBigDecimal(5), rs.getString(6)), stockProductId);
        List<Exit> exits = jdbc.query("""
                SELECT id,quantity,local_date FROM money_exits
                WHERE stock_key=? AND canceled_at IS NULL ORDER BY occurred_at,id
                """, (rs, n) -> new Exit(rs.getLong(1), rs.getInt(2), rs.getDate(3).toLocalDate()), stockProductId);
        jdbc.update("DELETE FROM money_allocations WHERE exit_id IN (SELECT id FROM money_exits WHERE stock_key=?)", stockProductId);
        for (Exit exit : exits) {
            int needed = exit.quantity();
            for (Supply supply : supplies) {
                if (needed == 0) break;
                if (supply.remaining == 0 || supply.date.isAfter(exit.date())) continue;
                int taken = Math.min(needed, supply.remaining);
                BigDecimal amount = supply.unknown ? null : supply.unitPrice.multiply(BigDecimal.valueOf(taken));
                jdbc.update("INSERT INTO money_allocations(exit_id,supply_id,quantity,amount,category_group) VALUES (?,?,?,?,?)",
                        exit.id(), supply.id, taken, amount, supply.categoryGroup);
                supply.remaining -= taken;
                needed -= taken;
            }
            if (needed > 0) throw new ConflictException("Saldo financeiro inconsistente", "Não foi possível atribuir a saída ao estoque disponível");
        }
    }

    private static final class Supply {
        final long id;
        int remaining;
        final LocalDate date;
        final boolean unknown;
        final BigDecimal unitPrice;
        final String categoryGroup;

        Supply(long id, int remaining, LocalDate date, boolean unknown, BigDecimal unitPrice, String categoryGroup) {
            this.id = id;
            this.remaining = remaining;
            this.date = date;
            this.unknown = unknown;
            this.unitPrice = unitPrice;
            this.categoryGroup = categoryGroup;
        }
    }

    private record Exit(long id, int quantity, LocalDate date) {}
}
