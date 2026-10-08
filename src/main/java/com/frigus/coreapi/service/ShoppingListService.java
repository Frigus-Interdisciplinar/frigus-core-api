package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.shoppinglist.ShoppingListCreateRequestDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListProductCreateRequestDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListResponseDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListUpdateRequestDto;
import com.frigus.coreapi.enums.ListStatus;
import com.frigus.coreapi.enums.ProductListStatus;
import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.ShoppingListMapper;
import com.frigus.coreapi.model.ShoppingList;
import com.frigus.coreapi.model.Stock;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.repository.ShoppingListProductRepository;
import com.frigus.coreapi.repository.ShoppingListRepository;
import com.frigus.coreapi.repository.StockProductRepository;
import com.frigus.coreapi.repository.StockRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShoppingListService extends BaseService<ShoppingList, UUID, ShoppingListCreateRequestDto, ShoppingListResponseDto, ShoppingListMapper, ShoppingListRepository> {

    private final StockRepository stockRepository;
    private final GroupAccessService groupAccessService;
    private final ShoppingListProductService shoppingListProductService;
    private final ShoppingListProductRepository shoppingListProductRepository;
    private final StockProductRepository stockProductRepository;

    public ShoppingListService(ShoppingListRepository repository,
                               ShoppingListMapper mapper,
                               StockRepository stockRepository,
                               GroupAccessService groupAccessService,
                               @Lazy ShoppingListProductService shoppingListProductService,
                               ShoppingListProductRepository shoppingListProductRepository,
                               StockProductRepository stockProductRepository) {
        super(repository, mapper);
        this.stockRepository = stockRepository;
        this.groupAccessService = groupAccessService;
        this.shoppingListProductService = shoppingListProductService;
        this.shoppingListProductRepository = shoppingListProductRepository;
        this.stockProductRepository = stockProductRepository;
    }

    @Transactional
    public ShoppingListResponseDto create(ShoppingListCreateRequestDto dto) {
        Stock stock = stockRepository.findById(dto.getStockId())
                .orElseThrow(() -> new NotFoundException());

        groupAccessService.requireGroupAccess(stock.getGroup().getId());

        ShoppingList list = mapper.toEntity(dto);
        list.setStock(stock);
        list.setStatus(ListStatus.OPEN);

        list = repository.save(list);

        if (dto.getInitialItems() != null && !dto.getInitialItems().isEmpty()) {
            shoppingListProductService.addItemBatch(list.getId(), dto.getInitialItems());
        }

        return findByIdWithItems(list.getId());
    }

    @Transactional(readOnly = true)
    public ShoppingListResponseDto findByIdWithItems(UUID id) {
        ShoppingList list = repository.findById(id)
                .orElseThrow(() -> new NotFoundException());

        groupAccessService.requireGroupAccess(list.getStock().getGroup().getId());

        ShoppingListResponseDto response = mapper.toDto(list);
        
        response.setItems(shoppingListProductService.findByListId(id));
        
        response.setTotalItems(shoppingListProductRepository.countByListId(id));
        response.setPurchasedItems(shoppingListProductRepository.countByListIdAndStatus(id, ProductListStatus.PURCHASED));
        response.setPendingItems(shoppingListProductRepository.countByListIdAndStatus(id, ProductListStatus.PENDING));
        
        Double estimatedTotal = response.getItems().stream()
                .filter(item -> item.getEstimatedTotal() != null)
                .mapToDouble(item -> item.getEstimatedTotal())
                .sum();
                
        response.setEstimatedTotal(estimatedTotal);

        return response;
    }

    @Transactional(readOnly = true)
    public Page<ShoppingListResponseDto> findAll(Integer stockId, UUID groupId, ListStatus status, Pageable pageable) {
        Page<ShoppingList> page;

        if (stockId != null) {
            Stock stock = stockRepository.findById(stockId)
                    .orElseThrow(() -> new NotFoundException());
            groupAccessService.requireGroupAccess(stock.getGroup().getId());
            
            if (groupId != null && !groupId.equals(stock.getGroup().getId())) {
                throw new BadRequestException(
                        "Filtros inconsistentes", "O estoque não pertence ao grupo informado");
            }
            if (status != null) {
                page = repository.findByStockIdAndStatus(stockId, status, pageable);
            } else {
                page = repository.findByStockId(stockId, pageable);
            }
        } else if (groupId != null) {
            groupAccessService.requireGroupAccess(groupId);
            
            if (status != null) {
                page = repository.findByStock_GroupIdAndStatus(groupId, status, pageable);
            } else {
                page = repository.findByStock_GroupId(groupId, pageable);
            }
        } else {
            var user = groupAccessService.requireCurrentUser();
            page = user.getRole() == Role.ADMIN
                    ? (status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable))
                    : (status == null ? repository.findAccessible(user.getId(), pageable)
                    : repository.findAccessibleByStatus(user.getId(), status, pageable));
        }

        return page.map(list -> {
            ShoppingListResponseDto dto = mapper.toDto(list);
            dto.setTotalItems(shoppingListProductRepository.countByListId(list.getId()));
            dto.setPurchasedItems(shoppingListProductRepository.countByListIdAndStatus(list.getId(), ProductListStatus.PURCHASED));
            return dto;
        });
    }

    @Transactional
    public ShoppingListResponseDto update(UUID id, ShoppingListUpdateRequestDto dto) {
        ShoppingList list = repository.findById(id)
                .orElseThrow(() -> new NotFoundException());

        groupAccessService.requireGroupAccess(list.getStock().getGroup().getId());

        if (dto.getDate() != null) {
            list.setDate(dto.getDate());
        }
        
        if (dto.getStatus() != null) {
            list.setStatus(dto.getStatus());
        }

        list = repository.save(list);
        return findByIdWithItems(id);
    }

    @Transactional
    public void delete(UUID id) {
        ShoppingList list = repository.findById(id)
                .orElseThrow(() -> new NotFoundException());

        groupAccessService.requireGroupAccess(list.getStock().getGroup().getId());

        shoppingListProductRepository.deleteByListId(id);
        repository.delete(list);
    }

    @Transactional
    public ShoppingListResponseDto closeList(UUID id) {
        ShoppingList list = repository.findById(id)
                .orElseThrow(() -> new NotFoundException());
        groupAccessService.requireGroupAccess(list.getStock().getGroup().getId());
        
        list.setStatus(ListStatus.COMPLETED);
        repository.save(list);
        return findByIdWithItems(id);
    }

    @Transactional
    public ShoppingListResponseDto cancelList(UUID id) {
        ShoppingList list = repository.findById(id)
                .orElseThrow(() -> new NotFoundException());
        groupAccessService.requireGroupAccess(list.getStock().getGroup().getId());
        
        list.setStatus(ListStatus.CANCELED);
        repository.save(list);
        return findByIdWithItems(id);
    }

    @Transactional
    public ShoppingListResponseDto reopenList(UUID id) {
        ShoppingList list = repository.findById(id)
                .orElseThrow(() -> new NotFoundException());
        groupAccessService.requireGroupAccess(list.getStock().getGroup().getId());
        
        list.setStatus(ListStatus.OPEN);
        repository.save(list);
        return findByIdWithItems(id);
    }

    @Transactional
    public void checkAndAutoCloseList(UUID id) {
        ShoppingList list = repository.findById(id).orElse(null);
        if (list == null || list.getStatus() == ListStatus.COMPLETED || list.getStatus() == ListStatus.CANCELED) {
            return;
        }

        long totalItems = shoppingListProductRepository.countByListId(id);
        if (totalItems == 0) {
            return;
        }

        // If no items are PENDING (all are PURCHASED or REMOVED), close the list
        boolean hasPending = shoppingListProductRepository.existsByListIdAndStatusNotIn(id, 
                List.of(ProductListStatus.PURCHASED, ProductListStatus.REMOVED));

        if (!hasPending) {
            list.setStatus(ListStatus.COMPLETED);
            repository.save(list);
        }
    }

    @Transactional
    public ShoppingListResponseDto generateFromLowStock(Integer stockId) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new NotFoundException());

        groupAccessService.requireGroupAccess(stock.getGroup().getId());

        List<StockProduct> lowStockProducts = stockProductRepository.findByStockId(stockId).stream()
                .filter(sp -> sp.getQuantity() < sp.getMinimalQuantity())
                .collect(Collectors.toList());

        if (lowStockProducts.isEmpty()) {
            throw new IllegalArgumentException("No products with low stock found for this stock");
        }

        ShoppingListCreateRequestDto createDto = new ShoppingListCreateRequestDto();
        createDto.setStockId(stockId);
        
        List<ShoppingListProductCreateRequestDto> initialItems = new ArrayList<>();
        for (StockProduct sp : lowStockProducts) {
            int neededQuantity = sp.getMinimalQuantity() - sp.getQuantity();
            initialItems.add(ShoppingListProductCreateRequestDto.builder()
                    .productId(sp.getProduct().getId())
                    .quantity(neededQuantity)
                    .build());
        }
        createDto.setInitialItems(initialItems);

        return create(createDto);
    }
}
