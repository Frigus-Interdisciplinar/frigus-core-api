package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.discard.DiscardCreateRequestDto;
import com.frigus.coreapi.dto.discard.DiscardResponseDto;
import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.DiscardMapper;
import com.frigus.coreapi.model.Discard;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.repository.DiscardRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DiscardService extends BaseService<Discard, Integer, DiscardCreateRequestDto, DiscardResponseDto, DiscardMapper, DiscardRepository> {

    private final StockProductRepository stockProductRepository;
    private final GroupAccessService groupAccessService;

    public DiscardService(
            DiscardRepository repository,
            DiscardMapper mapper,
            StockProductRepository stockProductRepository,
            GroupAccessService groupAccessService) {
        super(repository, mapper);
        this.stockProductRepository = stockProductRepository;
        this.groupAccessService = groupAccessService;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<DiscardResponseDto> findAll(Pageable pageable) {
        var user = groupAccessService.requireCurrentUser();
        Page<Discard> page = user.getRole() == Role.ADMIN
                ? repository.findAll(pageable)
                : repository.findAccessible(user.getId(), pageable);
        return page.map(mapper::toDto);
    }

    @Override
    protected void authorizeRead(Discard discard) {
        groupAccessService.requireGroupAccess(discard.getStockProduct().getStock().getGroup().getId());
    }

    @Override
    protected boolean requiresAuthorizedDelete() {
        return true;
    }

    @Transactional
    public DiscardResponseDto create(DiscardCreateRequestDto dto) {
        StockProduct stockProduct = stockProductRepository.findById(dto.getStockProductId())
                .orElseThrow(() -> new NotFoundException(
                        "Produto do estoque não encontrado",
                        "Nenhum produto do estoque foi encontrado com o ID informado"));

        groupAccessService.requireGroupAccess(stockProduct.getStock().getGroup().getId());

        if (dto.getQuantity() <= 0 || dto.getQuantity() > stockProduct.getQuantity()) {
            throw new IllegalArgumentException("A quantidade é inválida");
        }
        stockProduct.setQuantity(stockProduct.getQuantity() - dto.getQuantity());
        Discard discard = mapper.toEntity(dto);
        discard.setStockProduct(stockProduct);
        
        discard.setDate(Instant.now());

        return mapper.toDto(repository.save(discard));
    }

}
