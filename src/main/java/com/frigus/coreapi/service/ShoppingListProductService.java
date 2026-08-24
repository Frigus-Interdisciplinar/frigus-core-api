package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.shopping.ShoppingListProductRequestDto;
import com.frigus.coreapi.dto.shopping.ShoppingListProductResponseDto;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.ShoppingListProductMapper;
import com.frigus.coreapi.model.ShoppingListProduct;
import com.frigus.coreapi.repository.ProductRepository;
import com.frigus.coreapi.repository.ShoppingListProductRepository;
import com.frigus.coreapi.repository.ShoppingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
    }
}
