package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.stock.StockCreateRequestDto;
import com.frigus.coreapi.dto.stock.StockResponseDto;
import com.frigus.coreapi.dto.stock.StockUpdateRequestDto;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.StockMapper;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.Stock;
import com.frigus.coreapi.repository.GroupRepository;
import com.frigus.coreapi.repository.StockRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.util.UUID;

@Service
public class StockService extends BaseService<Stock, Integer, StockCreateRequestDto, StockResponseDto, StockMapper, StockRepository> {
    private final GroupRepository groupRepository;
    private final GroupAccessService groupAccessService;

    public StockService(
            StockRepository repository,
            StockMapper mapper,
            GroupRepository groupRepository,
            GroupAccessService groupAccessService) {
        super(repository, mapper);
        this.groupRepository = groupRepository;
        this.groupAccessService = groupAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockResponseDto> findAll(Pageable pageable, MultiValueMap<String, String> filters) {
        UUID groupId = requiredUuidFilter(filters, "groupId");
        return findByGroupId(groupId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockResponseDto> findByGroupId(UUID groupId, Pageable pageable) {
        groupAccessService.requireGroupAccess(groupId);
        return repository.findByGroupId(groupId, pageable).map(this::mapToDto);
    }

    @Override
    protected void authorizeRead(Stock stock) {
        groupAccessService.requireGroupAccess(stock.getGroup().getId());
    }

    @Override
    protected boolean requiresAuthorizedDelete() {
        return true;
    }

    @Transactional
    public StockResponseDto create(StockCreateRequestDto dto) {
        Group group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new NotFoundException("Grupo não encontrado", "O grupo informado não existe"));
        groupAccessService.requireGroupAccess(group.getId());

        Stock stock = mapper.toEntity(dto);
        stock.setGroup(group);
        return mapToDto(repository.save(stock));
    }

    @Transactional
    public StockResponseDto update(Integer id, StockUpdateRequestDto dto) {
        Stock stock = getRequiredEntity(id);
        authorizeRead(stock);

        Group targetGroup = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new NotFoundException("Grupo não encontrado", "O grupo informado não existe"));
        groupAccessService.requireGroupAccess(targetGroup.getId());

        stock.setGroup(targetGroup);
        stock.setName(dto.getName().trim());
        return mapToDto(repository.save(stock));
    }

    private UUID requiredUuidFilter(MultiValueMap<String, String> filters, String name) {
        String rawValue = filters.getFirst(name);
        if (rawValue == null || rawValue.isBlank()) {
            throw new BadRequestException("Filtro obrigatório ausente", "O filtro " + name + " é obrigatório");
        }
        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Filtro inválido", "O filtro " + name + " é inválido");
        }
    }
}
