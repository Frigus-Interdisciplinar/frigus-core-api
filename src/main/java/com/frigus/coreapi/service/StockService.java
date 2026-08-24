package com.frigus.coreapi.service;

import org.springframework.transaction.annotation.Transactional;

import com.frigus.coreapi.dto.stock.StockCreateRequestDto;
import com.frigus.coreapi.dto.stock.StockResponseDto;
import com.frigus.coreapi.mapper.StockMapper;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.Stock;
import com.frigus.coreapi.repository.StockRepository;

public class StockService extends BaseService<Stock, Integer, StockCreateRequestDto, StockResponseDto, StockMapper, StockRepository>{

    private static final GroupRepository groupRepository;

    public StockService(StockRepository repository, StockMapper mapper, GroupRepository groupRepository) {
        super(repository, mapper);
        this.groupRepository = groupRepository;
    }

    @Transactional
    public StockResponseDto create(StockCreateRequestDto dto) {
        Group group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new NotFoundException("Grupo nao encontrado", "Nenhum grupo foi encontrado com o ID informado"));
        Stock stock = mapper.toEntity(dto);
        stock.setGroup(group);
        return mapper.toDto(repository.save(stock));
    }
}
