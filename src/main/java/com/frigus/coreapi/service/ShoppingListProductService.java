package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.shoppinglist.ShoppingListProductCreateRequestDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListProductResponseDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListProductUpdateRequestDto;
import com.frigus.coreapi.enums.ProductListStatus;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.ShoppingListProductMapper;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.model.ShoppingList;
import com.frigus.coreapi.model.ShoppingListProduct;
import com.frigus.coreapi.repository.ProductRepository;
import com.frigus.coreapi.repository.ShoppingListProductRepository;
import com.frigus.coreapi.repository.ShoppingListRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShoppingListProductService extends BaseService<ShoppingListProduct, Integer, ShoppingListProductCreateRequestDto, ShoppingListProductResponseDto, ShoppingListProductMapper, ShoppingListProductRepository> {

    private final ProductRepository productRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final ShoppingListService shoppingListService;

    public ShoppingListProductService(ShoppingListProductRepository repository, 
                                      ShoppingListProductMapper mapper,
                                      ProductRepository productRepository,
                                      ShoppingListRepository shoppingListRepository,
                                      @Lazy ShoppingListService shoppingListService) {
        super(repository, mapper);
        this.productRepository = productRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.shoppingListService = shoppingListService;
    }

    @Transactional
    public ShoppingListProductResponseDto addItem(UUID listId, ShoppingListProductCreateRequestDto dto) {
        ShoppingList list = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new NotFoundException());

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new NotFoundException());

        if (repository.existsByListIdAndProductId(listId, product.getId())) {
            throw new ConflictException();
        }

        ShoppingListProduct item = mapper.toEntity(dto);
        item.setList(list);
        item.setProduct(product);
        item.setStatus(ProductListStatus.PENDING);

        item = repository.save(item);
        
        shoppingListService.checkAndAutoCloseList(listId);

        return mapper.toDto(item);
    }
    
    @Transactional
    public void addItemBatch(UUID listId, List<ShoppingListProductCreateRequestDto> items) {
        ShoppingList list = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new NotFoundException());

        for (ShoppingListProductCreateRequestDto dto : items) {
            if (!repository.existsByListIdAndProductId(listId, dto.getProductId())) {
                Product product = productRepository.findById(dto.getProductId())
                        .orElseThrow(() -> new NotFoundException());
                
                ShoppingListProduct item = mapper.toEntity(dto);
                item.setList(list);
                item.setProduct(product);
                item.setStatus(ProductListStatus.PENDING);
                repository.save(item);
            }
        }
        
        shoppingListService.checkAndAutoCloseList(listId);
    }

    public Page<ShoppingListProductResponseDto> findByListId(UUID listId, Pageable pageable) {
        return repository.findByListId(listId, pageable).map(mapper::toDto);
    }

    public List<ShoppingListProductResponseDto> findByListId(UUID listId) {
        return repository.findByListIdOrderByCreatedAtAsc(listId)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShoppingListProductResponseDto updateItem(UUID listId, Integer itemId, ShoppingListProductUpdateRequestDto dto) {
        ShoppingListProduct item = repository.findById(itemId)
                .orElseThrow(() -> new NotFoundException());

        if (!item.getList().getId().equals(listId)) {
            throw new ConflictException();
        }

        if (dto.getQuantity() != null) {
            item.setQuantity(dto.getQuantity());
        }

        if (dto.getStatus() != null) {
            item.setStatus(dto.getStatus());
        }

        item = repository.save(item);
        shoppingListService.checkAndAutoCloseList(listId);
        
        return mapper.toDto(item);
    }

    @Transactional
    public ShoppingListProductResponseDto updateStatus(UUID listId, Integer itemId, ProductListStatus status) {
        ShoppingListProduct item = repository.findById(itemId)
                .orElseThrow(() -> new NotFoundException());

        if (!item.getList().getId().equals(listId)) {
            throw new ConflictException();
        }

        item.setStatus(status);
        item = repository.save(item);
        
        shoppingListService.checkAndAutoCloseList(listId);
        
        return mapper.toDto(item);
    }

    @Transactional
    public void deleteItem(UUID listId, Integer itemId) {
        ShoppingListProduct item = repository.findById(itemId)
                .orElseThrow(() -> new NotFoundException());

        if (!item.getList().getId().equals(listId)) {
            throw new ConflictException();
        }

        repository.delete(item);
        shoppingListService.checkAndAutoCloseList(listId);
    }
}
