package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.moneysaving.MoneySavingDtos;
import com.frigus.coreapi.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MoneySavingReportService {
    private static final String EVENT_ROWS = """
            WITH events AS (
              SELECT p.group_id, 'P:' || l.id AS id, l.id AS sort_id, 'PURCHASE' AS kind,
                p.purchase_date AS event_date, l.product_name, l.category, l.category_group,
                l.unit_of_measure, l.expire_date, l.quantity,
                l.unit_price * l.quantity AS amount, true AS verified, NULL::varchar AS reason_code
              FROM food_purchase_lines l JOIN food_purchases p ON p.id=l.purchase_id
              WHERE p.deleted_at IS NULL AND p.group_id=:groupId AND p.purchase_date>=:fromDate AND p.purchase_date<:toDate
              UNION ALL
              SELECT e.group_id, 'E:' || e.id AS id, e.id AS sort_id, e.kind,
                e.local_date AS event_date, e.product_name, e.category,
                CASE WHEN count(DISTINCT a.category_group)>1 THEN NULL
                  ELSE COALESCE(MAX(a.category_group), CASE WHEN e.category IN ('FRUIT','VEGETABLE') THEN 'HORTIFRUTI' ELSE e.category END) END,
                e.unit_of_measure, e.expire_date, e.quantity,
                CASE WHEN count(a.id)=0 OR count(a.id) FILTER (WHERE a.amount IS NULL)>0
                  THEN NULL ELSE sum(a.amount) END AS amount,
                (count(a.id)>0 AND count(a.id) FILTER (WHERE a.amount IS NULL)=0) AS verified,
                e.reason_code
              FROM money_exits e LEFT JOIN money_allocations a ON a.exit_id=e.id
              WHERE e.canceled_at IS NULL AND e.kind IN ('CONSUMED','DISCARDED')
                AND e.group_id=:groupId AND e.local_date>=:fromDate AND e.local_date<:toDate
              GROUP BY e.id
            )
            """;
    private static final List<String> CATEGORIES = List.of("HORTIFRUTI", "MEAT", "DAIRY", "GRAIN", "BEVERAGE");
    private final NamedParameterJdbcTemplate jdbc;
    private final GroupAccessService access;

    @Transactional(readOnly = true)
    public MoneySavingDtos.Summary summary(UUID groupId, LocalDate from, LocalDate to) {
        access.requireGroupAccess(groupId);
        validateDates(from, to);
        LocalDate previousFrom = previousStart(from, to);
        YearMonth lastMonth = YearMonth.from(to.minusDays(1));
        LocalDate sixMonthsFrom = lastMonth.minusMonths(5).atDay(1);
        LocalDate earliest = from.isBefore(previousFrom) ? from : previousFrom;
        if (sixMonthsFrom.isBefore(earliest)) earliest = sixMonthsFrom;
        List<Row> rows = fetch(groupId, earliest, to);
        Totals current = totals(rows, from, to);
        Totals previous = totals(rows, previousFrom, from);
        BigDecimal percent = previous.spent.signum() == 0 ? null : current.spent.subtract(previous.spent)
                .multiply(BigDecimal.valueOf(100)).divide(previous.spent, 1, RoundingMode.HALF_UP);
        List<MoneySavingDtos.CategoryTotal> categories = categoryTotals(groupId, from, to);
        List<MoneySavingDtos.MonthPoint> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = lastMonth.minusMonths(i);
            Totals monthly = totals(rows, month.atDay(1), month.plusMonths(1).atDay(1));
            months.add(new MoneySavingDtos.MonthPoint(month.toString(), monthly.spent, monthly.saved, monthly.lost));
        }
        return new MoneySavingDtos.Summary(new MoneySavingDtos.Metric(current.spent, current.purchaseActions),
                new MoneySavingDtos.Metric(current.saved, current.consumedActions),
                new MoneySavingDtos.Metric(current.lost, current.discardActions), previous.spent, percent,
                current.verifiedActions > 0, categories, months);
    }

    @Transactional(readOnly = true)
    public MoneySavingDtos.EventPage events(UUID groupId, LocalDate from, LocalDate to,
            String category, String kind, int page, int size) {
        access.requireGroupAccess(groupId);
        validateDates(from, to);
        if (category != null && !CATEGORIES.contains(category)) throw bad("Categoria inválida");
        if (kind != null && !List.of("PURCHASE", "CONSUMED", "DISCARDED").contains(kind)) throw bad("Tipo inválido");
        if (page < 0 || size < 1 || size > 100) throw bad("Paginação inválida");
        MapSqlParameterSource params = parameters(groupId, from, to)
                .addValue("category", category).addValue("kind", kind)
                .addValue("limit", size).addValue("offset", (long) page * size);
        String filter = " WHERE 1=1";
        if (category != null) filter += " AND (ev.category_group=:category OR (ev.kind<>'PURCHASE' AND EXISTS (" +
                "SELECT 1 FROM money_allocations a WHERE a.exit_id=substring(ev.id from 3)::bigint AND a.category_group=:category)))";
        if (kind != null) filter += " AND ev.kind=:kind";
        Long total = jdbc.queryForObject(EVENT_ROWS + "SELECT count(*) FROM events ev" + filter, params, Long.class);
        List<Row> rows = jdbc.query(EVENT_ROWS + "SELECT * FROM events ev" + filter +
                " ORDER BY event_date DESC, sort_id DESC, kind DESC LIMIT :limit OFFSET :offset", params, (rs, n) -> row(rs));
        List<MoneySavingDtos.Event> content = rows.stream().map(row -> new MoneySavingDtos.Event(
                row.id(), row.kind(), row.date(), row.productName(), row.category(), row.categoryGroup(),
                row.unit(), row.quantity(), row.amount(), row.verified(), included(row), row.reasonCode(),
                categoryAmounts(row))).toList();
        return new MoneySavingDtos.EventPage(content, total, page, size, (int) ((total + size - 1) / size));
    }

    private List<Row> fetch(UUID groupId, LocalDate from, LocalDate to) {
        return jdbc.query(EVENT_ROWS + "SELECT * FROM events ORDER BY event_date,sort_id", parameters(groupId, from, to),
                (rs, n) -> row(rs));
    }

    private List<MoneySavingDtos.CategoryAmount> categoryAmounts(Row row) {
        if (row.amount() == null) return List.of();
        if ("PURCHASE".equals(row.kind()))
            return List.of(new MoneySavingDtos.CategoryAmount(row.categoryGroup(), row.amount()));
        long exitId = Long.parseLong(row.id().substring(2));
        return jdbc.getJdbcTemplate().query("""
                SELECT category_group,sum(amount) FROM money_allocations
                WHERE exit_id=? AND amount IS NOT NULL GROUP BY category_group ORDER BY category_group
                """, (rs, n) -> new MoneySavingDtos.CategoryAmount(rs.getString(1), rs.getBigDecimal(2)), exitId);
    }

    private List<MoneySavingDtos.CategoryTotal> categoryTotals(UUID groupId, LocalDate from, LocalDate to) {
        var sums = jdbc.query("""
                SELECT category_group,
                  COALESCE(sum(amount) FILTER (WHERE kind='PURCHASE'),0) AS spent,
                  COALESCE(sum(amount) FILTER (WHERE kind='CONSUMED'),0) AS saved,
                  COALESCE(sum(amount) FILTER (WHERE kind='DISCARDED'),0) AS lost
                FROM (
                  SELECT l.category_group,'PURCHASE' AS kind,l.unit_price*l.quantity AS amount
                  FROM food_purchase_lines l JOIN food_purchases p ON p.id=l.purchase_id
                  WHERE p.group_id=:groupId AND p.deleted_at IS NULL AND p.purchase_date>=:fromDate AND p.purchase_date<:toDate
                  UNION ALL
                  SELECT a.category_group,e.kind,a.amount
                  FROM money_exits e JOIN money_allocations a ON a.exit_id=e.id
                  WHERE e.group_id=:groupId AND e.canceled_at IS NULL
                    AND e.local_date>=:fromDate AND e.local_date<:toDate
                    AND a.amount IS NOT NULL
                    AND NOT EXISTS (SELECT 1 FROM money_allocations unknown
                      WHERE unknown.exit_id=e.id AND unknown.amount IS NULL)
                    AND ((e.kind='CONSUMED' AND e.local_date<=e.expire_date)
                      OR (e.kind='DISCARDED' AND e.reason_code='EXPIRED' AND e.local_date>e.expire_date))
                ) valued
                GROUP BY category_group
                """, parameters(groupId, from, to), (rs, n) -> new MoneySavingDtos.CategoryTotal(
                rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4)));
        var byCategory = new java.util.HashMap<String, MoneySavingDtos.CategoryTotal>();
        sums.forEach(sum -> byCategory.put(sum.category(), sum));
        return CATEGORIES.stream().map(category -> byCategory.getOrDefault(category,
                new MoneySavingDtos.CategoryTotal(category, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))).toList();
    }

    private Row row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Row(rs.getString("id"), rs.getString("kind"), rs.getDate("event_date").toLocalDate(),
                rs.getString("product_name"), rs.getString("category"), rs.getString("category_group"),
                rs.getString("unit_of_measure"), rs.getDate("expire_date").toLocalDate(), rs.getInt("quantity"),
                rs.getBigDecimal("amount"), rs.getBoolean("verified"), rs.getString("reason_code"));
    }

    private MapSqlParameterSource parameters(UUID groupId, LocalDate from, LocalDate to) {
        return new MapSqlParameterSource().addValue("groupId", groupId).addValue("fromDate", from).addValue("toDate", to);
    }

    private Totals totals(List<Row> rows, LocalDate from, LocalDate to) {
        Totals totals = new Totals();
        for (Row row : rows) {
            if (row.date().isBefore(from) || !row.date().isBefore(to)) continue;
            if ("PURCHASE".equals(row.kind())) {
                totals.purchaseActions++;
                totals.spent = totals.spent.add(row.amount());
                totals.verifiedActions++;
            } else if ("CONSUMED".equals(row.kind()) && !row.date().isAfter(row.expiry())) {
                totals.consumedActions++;
                if (row.amount() != null) { totals.saved = totals.saved.add(row.amount()); totals.verifiedActions++; }
            } else if ("DISCARDED".equals(row.kind()) && "EXPIRED".equals(row.reasonCode()) && row.date().isAfter(row.expiry())) {
                totals.discardActions++;
                if (row.amount() != null) { totals.lost = totals.lost.add(row.amount()); totals.verifiedActions++; }
            }
        }
        return totals;
    }

    private boolean included(Row row) {
        if (row.amount() == null) return false;
        if ("PURCHASE".equals(row.kind())) return true;
        if ("CONSUMED".equals(row.kind())) return !row.date().isAfter(row.expiry());
        return "DISCARDED".equals(row.kind()) && "EXPIRED".equals(row.reasonCode()) && row.date().isAfter(row.expiry());
    }

    private LocalDate previousStart(LocalDate from, LocalDate to) {
        if (from.getDayOfMonth() == 1 && to.equals(from.plusMonths(1))) return from.minusMonths(1);
        return from.minusDays(ChronoUnit.DAYS.between(from, to));
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from == null || to == null || !from.isBefore(to)) throw bad("Intervalo de datas inválido");
    }

    private BadRequestException bad(String message) { return new BadRequestException(message, message); }
    private record Row(String id, String kind, LocalDate date, String productName, String category,
                       String categoryGroup, String unit, LocalDate expiry, int quantity,
                       BigDecimal amount, boolean verified, String reasonCode) {}
    private static final class Totals {
        BigDecimal spent = BigDecimal.ZERO;
        BigDecimal saved = BigDecimal.ZERO;
        BigDecimal lost = BigDecimal.ZERO;
        long purchaseActions;
        long consumedActions;
        long discardActions;
        long verifiedActions;
    }
}
