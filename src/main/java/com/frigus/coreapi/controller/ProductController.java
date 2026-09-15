package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.product.ProductCreateRequestDto;
import com.frigus.coreapi.dto.product.ProductResponseDto;
import com.frigus.coreapi.dto.product.ProductUpdateRequestDto;
import com.frigus.coreapi.model.Product;
import com.frigus.coreapi.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController extends BaseController<
        Product,
        Integer,
        ProductCreateRequestDto,
        ProductResponseDto,
        ProductService> {

    public ProductController(ProductService service) {
        super(service);
    }

    @GetMapping
    @Override
    public Page<ProductResponseDto> findAll(Pageable pageable) {
        return super.findAll(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponseDto create(@Valid @RequestBody ProductCreateRequestDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponseDto update(@PathVariable Integer id, @Valid @RequestBody ProductUpdateRequestDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void delete(@PathVariable Integer id) {
        super.delete(id);
    }
}
