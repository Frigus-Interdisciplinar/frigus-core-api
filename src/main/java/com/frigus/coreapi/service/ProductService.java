package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.product.ProductCreateRequestDto;
import com.frigus.coreapi.dto.product.ProductResponseDto;
import com.frigus.coreapi.dto.product.ProductUpdateRequestDto;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.ProductMapper;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class ProductService extends BaseService<
        Product,
        Integer,
        ProductCreateRequestDto,
        ProductResponseDto,
        ProductMapper,
        ProductRepository> {

    public ProductService(
            ProductRepository repository,
            ProductMapper mapper) {
        super(repository, mapper);
    }

    @Transactional
    public ProductResponseDto create(ProductCreateRequestDto dto) {
        Product product = mapper.toEntity(dto);
        ensureUniqueName(product.getName(), null);

        product.setCreatedAt(Instant.now());
        return mapper.toDto(repository.save(product));
    }

    @Transactional
    public ProductResponseDto update(Integer id, ProductUpdateRequestDto dto) {
        Product product = repository.findById(id).orElseThrow(NotFoundException::new);
        ensureUniqueName(dto.getName().trim(), product.getId());
        product.setName(dto.getName().trim());
        product.setCategory(dto.getCategory());
        product.setStoragePlace(dto.getStoragePlace());
        product.setUnitPrice(dto.getUnitPrice());
        product.setUnitOfMeasure(dto.getUnitOfMeasure());

        return mapper.toDto(repository.save(product));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Product product = repository.findById(id).orElseThrow(NotFoundException::new);

        if (repository.isReferenced(product.getId())) {
            throw new ConflictException(
                    "Produto possui vínculos ativos",
                    "Não é possível excluir um produto usado em estoque, receitas, listas ou solicitações");
        }

        repository.delete(product);
    }

    private void ensureUniqueName(String name, Integer currentProductId) {
        Optional<Product> duplicate = repository.findFirstByNameIgnoreCase(name);

        if (duplicate.isPresent() && !duplicate.get().getId().equals(currentProductId)) {
            throw new ConflictException(
                    "Nome de produto já cadastrado",
                    "Já existe um produto com este nome no catálogo correspondente");
        }
    }

}
