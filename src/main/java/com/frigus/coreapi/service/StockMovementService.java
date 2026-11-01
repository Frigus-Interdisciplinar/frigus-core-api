package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.stockmovement.StockMovementCreateRequestDto;
import com.frigus.coreapi.dto.stockmovement.StockMovementResponseDto;
import com.frigus.coreapi.enums.MovementType;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.StockMovementMapper;
import com.frigus.coreapi.model.StockMovement;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.StockMovementRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class StockMovementService extends BaseService<StockMovement, Integer, StockMovementCreateRequestDto, StockMovementResponseDto, StockMovementMapper, StockMovementRepository> {
    private final StockProductRepository stockProductRepository;
    private final GroupAccessService groupAccessService;
    private final MoneySavingLedgerService ledger;

    @Autowired
    public StockMovementService(
            StockMovementRepository repository,
            StockMovementMapper mapper,
            StockProductRepository stockProductRepository,
            GroupAccessService groupAccessService,
            MoneySavingLedgerService ledger) {
        super(repository, mapper);
        this.stockProductRepository = stockProductRepository;
        this.groupAccessService = groupAccessService;
        this.ledger = ledger;
    }

    // Existing service tests exercise balance behavior without a database ledger.
    public StockMovementService(StockMovementRepository repository, StockMovementMapper mapper,
            StockProductRepository stockProductRepository, GroupAccessService groupAccessService) {
        this(repository, mapper, stockProductRepository, groupAccessService, null);
    }

    @Override
    protected void authorizeRead(StockMovement movement) {
        groupAccessService.requireGroupAccess(movement.getStockProduct().getStock().getGroup().getId());
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponseDto> findByStockProductId(Integer stockProductId, Pageable pageable) {
        StockProduct stockProduct = getRequiredStockProduct(stockProductId);
        groupAccessService.requireGroupAccess(stockProduct.getStock().getGroup().getId());
        return repository.findByStockProductIdOrderByDateDesc(stockProductId, pageable).map(this::mapToDto);
    }

    @Transactional
    public StockMovementResponseDto create(Integer stockProductId, StockMovementCreateRequestDto dto) {
        return apply(stockProductId, dto, dto.getMovementType() == MovementType.OUT ? "CONSUMPTION" : "INVENTORY");
    }

    @Transactional
    public StockMovementResponseDto createDiscard(Integer stockProductId, StockMovementCreateRequestDto dto) {
        return apply(stockProductId, dto, "DISCARD");
    }

    private StockMovementResponseDto apply(Integer stockProductId, StockMovementCreateRequestDto dto, String purpose) {
        StockProduct stockProduct = stockProductRepository.findByIdForUpdate(stockProductId)
                .orElseThrow(() -> new NotFoundException("Produto do estoque não encontrado", "O item informado não existe"));
        groupAccessService.requireGroupAccess(stockProduct.getStock().getGroup().getId());
        groupAccessService.requireGroupWriteAccess(stockProduct.getStock().getGroup().getId());
        if (dto.getReasonCode() != null &&
                (!"CONSUMED".equals(dto.getReasonCode()) || dto.getMovementType() != MovementType.OUT)) {
            throw new BadRequestException("Motivo inválido", "CONSUMED é permitido apenas para saídas OUT");
        }

        int balanceBefore = stockProduct.getQuantity();
        int balanceAfter = calculateBalanceAfter(balanceBefore, dto);
        stockProduct.setQuantity(balanceAfter);

        User user = groupAccessService.requireCurrentUser();
        StockMovement movement = mapper.toEntity(dto);
        movement.setPurpose(purpose);
        movement.setStockProduct(stockProduct);
        movement.setUser(user);
        movement.setBalanceAfter(balanceAfter);
        movement.setDate(Instant.now());

        stockProductRepository.save(stockProduct);
        stockProductRepository.flush();
        StockMovement saved = repository.save(movement);
        repository.flush();
        if (ledger != null && balanceAfter != balanceBefore) {
            ledger.recordMovement(stockProduct, balanceBefore, balanceAfter - balanceBefore,
                    dto.getReasonCode(), saved.getId(), saved.getDate());
        }
        return mapToDto(saved);
    }

    private StockProduct getRequiredStockProduct(Integer stockProductId) {
        return stockProductRepository.findById(stockProductId)
                .orElseThrow(() -> new NotFoundException("Produto do estoque não encontrado", "O item informado não existe"));
    }

    private int calculateBalanceAfter(Integer currentQuantity, StockMovementCreateRequestDto dto) {
        if (dto.getMovementType() == MovementType.ADJUSTMENT) {
            return dto.getQuantity();
        }

        if (dto.getQuantity() <= 0) {
            throw new BadRequestException("Quantidade inválida", "A quantidade deve ser maior que zero para entradas e saídas");
        }

        if (dto.getMovementType() == MovementType.OUT) {
            if (dto.getQuantity() > currentQuantity) {
                throw new BadRequestException("Saldo insuficiente", "A saída não pode deixar o estoque negativo");
            }
            return currentQuantity - dto.getQuantity();
        }

        try {
            return Math.addExact(currentQuantity, dto.getQuantity());
        } catch (ArithmeticException exception) {
            throw new BadRequestException("Quantidade inválida", "A quantidade informada excede o limite permitido");
        }
    }
}
