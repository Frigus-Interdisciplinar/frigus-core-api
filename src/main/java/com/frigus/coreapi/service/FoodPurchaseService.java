package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.moneysaving.MoneySavingDtos;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodPurchaseService {
    private final JdbcTemplate jdbc;
    private final GroupAccessService access;
    private final MoneySavingLedgerService ledger;
    private final ObjectMapper json;

    @Transactional
    public MoneySavingDtos.Purchase create(UUID groupId, String key, MoneySavingDtos.PurchaseInput input) {
        access.requireGroupAccess(groupId);
        validate(input, false);
        if (key == null || key.isBlank() || key.length() > 160)
            throw bad("Chave de idempotência obrigatória");
        lockGroup(groupId);
        String hash = hash(input);
        List<Idempotent> prior = jdbc.query("SELECT id,request_hash FROM food_purchases WHERE group_id=? AND idempotency_key=?",
                (rs, n) -> new Idempotent((UUID) rs.getObject(1), rs.getString(2)), groupId, key);
        if (!prior.isEmpty()) {
            if (!prior.get(0).hash().equals(hash)) throw conflict("Chave de idempotência usada com outra compra");
            Integer deleted = jdbc.queryForObject("SELECT count(*) FROM food_purchases WHERE id=? AND deleted_at IS NOT NULL",
                    Integer.class, prior.get(0).id());
            if (deleted > 0) throw conflict("Chave de idempotência pertence a uma compra excluída");
            return get(groupId, prior.get(0).id());
        }
        String receipt = cleanReference(input.receiptReference());
        if (receipt != null && jdbc.queryForObject("SELECT count(*) FROM food_purchases WHERE group_id=? AND receipt_reference=? AND deleted_at IS NULL",
                Integer.class, groupId, receipt) > 0) throw conflict("Nota já registrada neste grupo");
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO food_purchases(id,group_id,purchase_date,source,receipt_reference,idempotency_key,request_hash)
                VALUES (?,?,?,?,?,?,?)
                """, id, groupId, input.purchaseDate(), input.source(), receipt, key, hash);
        for (MoneySavingDtos.PurchaseLineInput line : input.lines()) {
            ProductInfo product = product(line.productId());
            String category = validatedCategory(line.category() == null ? product.category() : line.category());
            checkStock(groupId, line.stockId());
            Integer itemId = findOrCreateItem(line.stockId(), line.productId(), line.expireDate(), category);
            int before = lockItemQuantity(itemId);
            if ((long) before + line.quantity() > Integer.MAX_VALUE) throw conflict("Quantidade excede o limite do estoque");
            ledger.ensureOpening(itemId, before);
            jdbc.update("UPDATE stock_products SET quantity=quantity+? WHERE id=?", line.quantity(), itemId);
            Long lineId = jdbc.queryForObject("""
                    INSERT INTO food_purchase_lines(purchase_id,stock_key,stock_product_id,product_id,product_name,category,
                      category_group,unit_of_measure,expire_date,quantity,unit_price)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?) RETURNING id
                    """, Long.class, id, itemId, itemId, line.productId(), product.name(), category,
                    MoneySavingLedgerService.categoryGroup(category), product.unit(), line.expireDate(), line.quantity(), line.unitPrice());
            jdbc.update("INSERT INTO money_supplies(stock_key,stock_product_id,purchase_line_id,available_on,quantity,unknown_cost) VALUES (?,?,?,?,?,false)",
                    itemId, itemId, lineId, input.purchaseDate(), line.quantity());
            ledger.rebuild(itemId);
        }
        return get(groupId, id);
    }

    @Transactional(readOnly = true)
    public MoneySavingDtos.Purchase get(UUID groupId, UUID purchaseId) {
        access.requireGroupAccess(groupId);
        List<PurchaseHead> heads = jdbc.query("""
                SELECT id,purchase_date,source,receipt_reference,version FROM food_purchases
                WHERE id=? AND group_id=? AND deleted_at IS NULL
                """, (rs, n) -> new PurchaseHead((UUID) rs.getObject(1), rs.getDate(2).toLocalDate(),
                rs.getString(3), rs.getString(4), rs.getInt(5)), purchaseId, groupId);
        if (heads.isEmpty()) throw new NotFoundException("Compra não encontrada", "A compra informada não existe neste grupo");
        PurchaseHead head = heads.get(0);
        List<MoneySavingDtos.PurchaseLine> lines = jdbc.query("""
                SELECT id,stock_key,stock_product_id,product_name,category,category_group,unit_of_measure,expire_date,quantity,unit_price
                FROM food_purchase_lines WHERE purchase_id=? ORDER BY id
                """, (rs, n) -> new MoneySavingDtos.PurchaseLine(rs.getLong(1), rs.getInt(2), (Integer) rs.getObject(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getDate(8).toLocalDate(), rs.getInt(9),
                rs.getBigDecimal(10), rs.getBigDecimal(10).multiply(BigDecimal.valueOf(rs.getInt(9)))), purchaseId);
        BigDecimal total = lines.stream().map(MoneySavingDtos.PurchaseLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MoneySavingDtos.Purchase(head.id(), head.date(), head.source(), head.reference(), head.version(), total, lines);
    }

    @Transactional
    public MoneySavingDtos.Purchase update(UUID groupId, UUID purchaseId, MoneySavingDtos.PurchaseInput input) {
        access.requireGroupAccess(groupId);
        validate(input, true);
        lockGroup(groupId);
        MoneySavingDtos.Purchase old = get(groupId, purchaseId);
        if (input.version() != old.version()) throw conflict("Versão da compra desatualizada");
        if (input.lines().size() != old.lines().size()) throw bad("A correção deve preservar as linhas da compra");
        Set<Long> expected = new HashSet<>();
        old.lines().forEach(line -> expected.add(line.id()));
        Set<Long> received = new HashSet<>();
        input.lines().forEach(line -> received.add(line.id()));
        if (!expected.equals(received)) throw bad("IDs das linhas não correspondem à compra");
        String receipt = cleanReference(input.receiptReference());
        if (receipt != null && jdbc.queryForObject("""
                SELECT count(*) FROM food_purchases WHERE group_id=? AND receipt_reference=? AND id<>? AND deleted_at IS NULL
                """, Integer.class, groupId, receipt, purchaseId) > 0) throw conflict("Nota já registrada neste grupo");
        jdbc.update("INSERT INTO food_purchase_revisions(purchase_id,previous_version,previous_state,changed_by) VALUES (?,?,?::jsonb,?)",
                purchaseId, old.version(), json.writeValueAsString(old), access.requireCurrentUser().getId());
        jdbc.update("""
                UPDATE food_purchases SET purchase_date=?, source=?, receipt_reference=?, version=version+1,updated_at=now()
                WHERE id=?
                """, input.purchaseDate(), input.source(), receipt, purchaseId);
        Set<Integer> affectedItems = new HashSet<>();
        for (MoneySavingDtos.PurchaseLineInput changed : input.lines()) {
            MoneySavingDtos.PurchaseLine previous = old.lines().stream().filter(line -> line.id().equals(changed.id())).findFirst().orElseThrow();
            if (changed.productId() != null || changed.stockId() != null || changed.expireDate() != null)
                throw bad("Produto, estoque e validade não podem mudar nesta correção");
            String category = validatedCategory(changed.category() == null ? previous.category() : changed.category());
            int allocated = jdbc.queryForObject("""
                    SELECT COALESCE(sum(a.quantity),0) FROM money_allocations a JOIN money_supplies s ON s.id=a.supply_id
                    WHERE s.purchase_line_id=?
                    """, Integer.class, changed.id());
            if (changed.quantity() < allocated) throw conflict("Quantidade menor que as unidades já alocadas");
            Integer itemId = previous.stockProductId();
            if (itemId == null && changed.quantity() != previous.quantity())
                throw conflict("Item removido: quantidade não pode ser corrigida");
            if (itemId != null) {
                int balance = lockItemQuantity(itemId);
                int delta = changed.quantity() - previous.quantity();
                if ((long) balance + delta < 0 || (long) balance + delta > Integer.MAX_VALUE)
                    throw conflict("Saldo inválido para corrigir a compra");
                jdbc.update("UPDATE stock_products SET quantity=quantity+? WHERE id=?", delta, itemId);
                affectedItems.add(itemId);
            }
            jdbc.update("""
                    UPDATE food_purchase_lines SET quantity=?,unit_price=?,category=?,category_group=? WHERE id=?
                    """, changed.quantity(), changed.unitPrice(), category, MoneySavingLedgerService.categoryGroup(category), changed.id());
            jdbc.update("UPDATE money_supplies SET quantity=?,available_on=? WHERE purchase_line_id=?",
                    changed.quantity(), input.purchaseDate(), changed.id());
            affectedItems.add(previous.stockKey());
        }
        affectedItems.forEach(ledger::rebuild);
        return get(groupId, purchaseId);
    }

    @Transactional
    public void delete(UUID groupId, UUID purchaseId) {
        access.requireGroupAccess(groupId);
        lockGroup(groupId);
        MoneySavingDtos.Purchase purchase = get(groupId, purchaseId);
        jdbc.update("INSERT INTO food_purchase_revisions(purchase_id,previous_version,previous_state,changed_by) VALUES (?,?,?::jsonb,?)",
                purchaseId, purchase.version(), json.writeValueAsString(purchase), access.requireCurrentUser().getId());
        for (MoneySavingDtos.PurchaseLine line : purchase.lines()) {
            Integer used = jdbc.queryForObject("""
                    SELECT count(*) FROM money_allocations a JOIN money_supplies s ON s.id=a.supply_id
                    WHERE s.purchase_line_id=?
                    """, Integer.class, line.id());
            if (used > 0) throw conflict("Compra com saídas vinculadas não pode ser excluída");
        }
        for (MoneySavingDtos.PurchaseLine line : purchase.lines()) {
            Integer itemId = line.stockProductId();
            if (itemId == null) throw conflict("Item removido: compra não pode ser excluída");
            int balance = lockItemQuantity(itemId);
            if (balance < line.quantity()) throw conflict("Saldo insuficiente para excluir a compra");
            jdbc.update("UPDATE stock_products SET quantity=quantity-? WHERE id=?", line.quantity(), itemId);
            jdbc.update("DELETE FROM money_supplies WHERE purchase_line_id=?", line.id());
            ledger.rebuild(itemId);
        }
        jdbc.update("UPDATE food_purchases SET deleted_at=now(),version=version+1,updated_at=now() WHERE id=?", purchaseId);
    }

    private void validate(MoneySavingDtos.PurchaseInput input, boolean updating) {
        if (input == null || input.purchaseDate() == null || input.lines() == null || input.lines().isEmpty())
            throw bad("Data e linhas da compra são obrigatórias");
        if (!"USER_CONFIRMED".equals(input.source()) && !"RECEIPT_CAPTURED".equals(input.source()))
            throw bad("Origem da compra inválida");
        if (updating && input.version() == null) throw bad("Versão da compra obrigatória");
        if (input.receiptReference() != null && input.receiptReference().length() > 160) throw bad("Identificador da nota muito longo");
        for (MoneySavingDtos.PurchaseLineInput line : input.lines()) {
            if (line == null || line.quantity() == null || line.quantity() <= 0 || line.unitPrice() == null ||
                    line.unitPrice().signum() <= 0 || line.unitPrice().scale() > 2 ||
                    line.unitPrice().precision() - line.unitPrice().scale() > 8)
                throw bad("Quantidade e preço unitário devem ser positivos, com até duas casas decimais");
            if (!updating && (line.stockId() == null || line.productId() == null || line.expireDate() == null))
                throw bad("Produto, estoque e validade são obrigatórios");
            if (updating && line.id() == null) throw bad("ID da linha obrigatório na correção");
        }
    }

    private void lockGroup(UUID id) {
        jdbc.queryForObject("SELECT id FROM groups WHERE id=? AND deleted_at IS NULL FOR UPDATE", UUID.class, id);
    }

    private void checkStock(UUID groupId, Integer stockId) {
        List<UUID> groups = jdbc.query("SELECT group_id FROM stocks WHERE id=? AND deleted_at IS NULL",
                (rs, n) -> (UUID) rs.getObject(1), stockId);
        if (groups.isEmpty() || !groupId.equals(groups.get(0))) throw new NotFoundException("Estoque não encontrado", "Estoque fora do grupo informado");
    }

    private ProductInfo product(Integer id) {
        List<ProductInfo> found = jdbc.query("SELECT name,category::text,unit_of_measure::text FROM products WHERE id=?",
                (rs, n) -> new ProductInfo(rs.getString(1), rs.getString(2), rs.getString(3)), id);
        if (found.isEmpty()) throw new NotFoundException("Produto não encontrado", "O produto informado não existe");
        return found.get(0);
    }

    private Integer findOrCreateItem(Integer stockId, Integer productId, LocalDate expiry, String category) {
        List<Integer> ids = jdbc.query("SELECT id FROM stock_products WHERE stock_id=? AND product_id=? AND expire_date=? FOR UPDATE",
                (rs, n) -> rs.getInt(1), stockId, productId, expiry);
        if (!ids.isEmpty()) return ids.get(0);
        return jdbc.queryForObject("""
                INSERT INTO stock_products(product_id,stock_id,quantity,expire_date,category)
                VALUES (?,?,0,?,?::category_enum) RETURNING id
                """, Integer.class, productId, stockId, expiry, category);
    }

    private int lockItemQuantity(Integer id) {
        List<Integer> balances = jdbc.query("SELECT quantity FROM stock_products WHERE id=? FOR UPDATE", (rs, n) -> rs.getInt(1), id);
        if (balances.isEmpty()) throw conflict("Item de estoque removido");
        return balances.get(0);
    }

    private String validatedCategory(String category) {
        try {
            MoneySavingLedgerService.categoryGroup(category);
            return category;
        } catch (RuntimeException exception) {
            throw bad("Categoria alimentar inválida");
        }
    }

    private String cleanReference(String reference) {
        return reference == null || reference.isBlank() ? null : reference.trim();
    }

    private String hash(MoneySavingDtos.PurchaseInput input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private BadRequestException bad(String message) { return new BadRequestException(message, message); }
    private ConflictException conflict(String message) { return new ConflictException(message, message); }
    private record Idempotent(UUID id, String hash) {}
    private record ProductInfo(String name, String category, String unit) {}
    private record PurchaseHead(UUID id, LocalDate date, String source, String reference, int version) {}
}
