package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.shopping.ShoppingListRequestDto;
import com.frigus.coreapi.dto.shopping.ShoppingListResponseDto;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.ShoppingListMapper;
import com.frigus.coreapi.model.ShoppingList;
import com.frigus.coreapi.repository.ShoppingListRepository;
import com.frigus.coreapi.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ShoppingListService extends BaseService<ShoppingList, java.util.UUID, ShoppingListRequestDto, ShoppingListResponseDto, ShoppingListMapper, ShoppingListRepository> {
    private final StockRepository stockRepository;

    public ShoppingListService(ShoppingListRepository repository, ShoppingListMapper mapper, StockRepository stockRepository) {
        super(repository, mapper);
        this.stockRepository = stockRepository;
    }

    @Transactional
    public ShoppingListResponseDto create(ShoppingListRequestDto dto) {
        ShoppingList list = mapper.toEntity(dto);
        list.setStock(stockRepository.findById(dto.getStockId())
                .orElseThrow(NotFoundException::new)
        );
        list.setCreatedAt(Instant.now());
        return mapper.toDto(repository.save(list));
    }

    @Transactional
    public ShoppingListResponseDto update(java.util.UUID id, ShoppingListRequestDto dto) {
        ShoppingList list = repository.findById(id).orElseThrow(NotFoundException::new);
        list.setStock(stockRepository.findById(dto.getStockId()).orElseThrow(NotFoundException::new));
        list.setDate(dto.getDate() == null ? list.getDate() : dto.getDate());
        list.setStatus(dto.getStatus() == null ? list.getStatus() : dto.getStatus());
        return mapper.toDto(repository.save(list));
    }
}
