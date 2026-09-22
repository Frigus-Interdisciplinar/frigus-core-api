package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.stockproduct.StockProductCreateRequestDto;
import com.frigus.coreapi.dto.stockproduct.StockProductResponseDto;
import com.frigus.coreapi.dto.stockproduct.StockProductUpdateRequestDto;
import com.frigus.coreapi.model.StockProduct;
import com.frigus.coreapi.service.StockProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/stock-products")
public class StockProductController extends BaseController<StockProduct, Integer, StockProductCreateRequestDto, StockProductResponseDto, StockProductService> {
    public StockProductController(StockProductService service) {
        super(service);
    }

    @GetMapping
    public Page<StockProductResponseDto> findAll(@RequestParam Integer stockId, Pageable pageable) {
        return service.findByStockId(stockId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockProductResponseDto create(@Valid @RequestBody StockProductCreateRequestDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public StockProductResponseDto update(@PathVariable Integer id, @Valid @RequestBody StockProductUpdateRequestDto dto) {
        return service.update(id, dto);
    }
}
