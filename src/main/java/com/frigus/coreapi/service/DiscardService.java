package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.discard.DiscardCreateRequestDto;
import com.frigus.coreapi.dto.discard.DiscardResponseDto;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.mapper.DiscardMapper;
import com.frigus.coreapi.model.Discard;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.repository.DiscardRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DiscardService extends BaseService<Discard, Integer, DiscardCreateRequestDto, DiscardResponseDto, DiscardMapper, DiscardRepository> {

    private final StockProductRepository stockProductRepository;
    private final GroupAccessService groupAccessService;
    private final MoneySavingLedgerService ledger;

    @Autowired
    public DiscardService(
            DiscardRepository repository,
            DiscardMapper mapper,
            StockProductRepository stockProductRepository,
            GroupAccessService groupAccessService,
            MoneySavingLedgerService ledger) {
        super(repository, mapper);
        this.stockProductRepository = stockProductRepository;
        this.groupAccessService = groupAccessService;
        this.ledger = ledger;
    }

    public DiscardService(DiscardRepository repository, DiscardMapper mapper,
            StockProductRepository stockProductRepository) {
        this(repository, mapper, stockProductRepository, null, null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<DiscardResponseDto> findAll(Pageable pageable) {
        var user = groupAccessService.requireCurrentUser();
        Page<Discard> page = user.getRole() == Role.ADMIN
                ? repository.findActive(pageable)
                : repository.findAccessible(user.getId(), pageable);
        return page.map(mapper::toDto);
    }

    @Override
    protected void authorizeRead(Discard discard) {
        if (discard.getCanceledAt() != null) throw new NotFoundException();
        if (groupAccessService != null)
            groupAccessService.requireGroupAccess(discard.getStockProduct().getStock().getGroup().getId());
    }

    @Transactional
    public DiscardResponseDto create(DiscardCreateRequestDto dto) {
        if (dto.getReasonCode() != null && !dto.getReasonCode().equals("EXPIRED") && !dto.getReasonCode().equals("OTHER"))
            throw new BadRequestException("Motivo inválido", "Use EXPIRED ou OTHER");
        StockProduct stockProduct = stockProductRepository.findByIdForUpdate(dto.getStockProductId())
                .orElseThrow(() -> new NotFoundException(
                        "Produto do estoque não encontrado",
                        "Nenhum produto do estoque foi encontrado com o ID informado"));

        if (groupAccessService != null)
            groupAccessService.requireGroupAccess(stockProduct.getStock().getGroup().getId());

        if (dto.getQuantity() == null || dto.getQuantity() <= 0 || dto.getQuantity() > stockProduct.getQuantity()) {
            throw new BadRequestException("Quantidade inválida", "A quantidade descartada deve estar no saldo disponível");
        }
        int openingQuantity = stockProduct.getQuantity();
        stockProduct.setQuantity(stockProduct.getQuantity() - dto.getQuantity());
        Discard discard = mapper.toEntity(dto);
        discard.setStockProduct(stockProduct);
        
        discard.setDate(Instant.now());

        stockProductRepository.save(stockProduct);
        stockProductRepository.flush();
        Discard saved = repository.save(discard);
        repository.flush();
        if (ledger != null)
            ledger.recordDiscard(stockProduct, openingQuantity, dto.getQuantity(), dto.getReasonCode(), saved.getId(), saved.getDate());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Discard discard = repository.findById(id).orElseThrow(() ->
                ledger != null && ledger.hasDiscardEvent(id)
                        ? new ConflictException("Item removido", "O item de estoque não existe mais")
                        : new NotFoundException());
        authorizeRead(discard);
        if (discard.getQuantity() == null)
            throw new ConflictException("Quantidade histórica ausente", "Este descarte antigo não pode ser estornado automaticamente");
        StockProduct item = stockProductRepository.findByIdForUpdate(discard.getStockProduct().getId())
                .orElseThrow(() -> new ConflictException("Item removido", "O item de estoque não existe mais"));
        try {
            item.setQuantity(Math.addExact(item.getQuantity(), discard.getQuantity()));
        } catch (ArithmeticException exception) {
            throw new ConflictException("Saldo excedido", "O descarte não pode ser estornado");
        }
        discard.setCanceledAt(Instant.now());
        stockProductRepository.saveAndFlush(item);
        repository.saveAndFlush(discard);
        if (ledger != null) ledger.cancelDiscard(id, item.getId());
    }

}
