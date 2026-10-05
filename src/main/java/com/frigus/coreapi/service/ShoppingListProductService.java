package com.frigus.coreapi.service;

<<<<<<< HEAD
import com.frigus.coreapi.dto.shoppinglist.ShoppingListProductCreateRequestDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListProductResponseDto;
import com.frigus.coreapi.dto.shoppinglist.ShoppingListProductUpdateRequestDto;
import com.frigus.coreapi.enums.ProductListStatus;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.ShoppingListProductMapper;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.model.ShoppingList;
=======
import com.frigus.coreapi.dto.shopping.ShoppingListProductRequestDto;
import com.frigus.coreapi.dto.shopping.ShoppingListProductResponseDto;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.ShoppingListProductMapper;
>>>>>>> f050c93a2a16b2e7225b9b15bc1d8980692e8c86
import com.frigus.coreapi.model.ShoppingListProduct;
import com.frigus.coreapi.repository.ProductRepository;
import com.frigus.coreapi.repository.ShoppingListProductRepository;
import com.frigus.coreapi.repository.ShoppingListRepository;
<<<<<<< HEAD
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
=======
>>>>>>> f050c93a2a16b2e7225b9b15bc1d8980692e8c86
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
<<<<<<< HEAD
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
=======

@Service
public class ShoppingListProductService extends BaseService<ShoppingListProduct, Integer, ShoppingListProductRequestDto, ShoppingListProductResponseDto, ShoppingListProductMapper, ShoppingListProductRepository> {
    private final ShoppingListRepository shoppingListRepository;
    private final ProductRepository productRepository;

    public ShoppingListProductService(
            ShoppingListProductRepository repository,
            ShoppingListProductMapper mapper,
            ShoppingListRepository shoppingListRepository,
            ProductRepository productRepository) {
        super(repository, mapper);
        this.shoppingListRepository = shoppingListRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ShoppingListProductResponseDto create(ShoppingListProductRequestDto dto) {
        ShoppingListProduct item = mapper.toEntity(dto);
        item.setList(shoppingListRepository.findById(dto.getListId()).orElseThrow(NotFoundException::new));
        item.setProduct(productRepository.findById(dto.getProductId()).orElseThrow(NotFoundException::new));
        return mapper.toDto(repository.save(item));
    }

    @Transactional
    public ShoppingListProductResponseDto update(Integer id, ShoppingListProductRequestDto dto) {
        ShoppingListProduct item = repository.findById(id).orElseThrow(NotFoundException::new);
        item.setList(shoppingListRepository.findById(dto.getListId()).orElseThrow(NotFoundException::new));
        item.setProduct(productRepository.findById(dto.getProductId()).orElseThrow(NotFoundException::new));
        item.setStatus(dto.getStatus() == null ? item.getStatus() : dto.getStatus());
        item.setQuantity(dto.getQuantity());
        return mapper.toDto(repository.save(item));
    }

    @Transactional(readOnly = true)
    public List<ShoppingListProductResponseDto> findByListId(UUID listId) {
        if (!shoppingListRepository.existsById(listId)) {
            throw new NotFoundException();
        }
        return repository.findByListId(listId).stream().map(mapper::toDto).toList();
>>>>>>> f050c93a2a16b2e7225b9b15bc1d8980692e8c86
    }
}
