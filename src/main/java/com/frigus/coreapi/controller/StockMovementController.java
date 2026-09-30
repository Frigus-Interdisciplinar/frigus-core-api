package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.stockmovement.StockMovementCreateRequestDto;
import com.frigus.coreapi.dto.stockmovement.StockMovementResponseDto;
import com.frigus.coreapi.service.StockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stock-products/{stockProductId}/movements")
public class StockMovementController {
    private final StockMovementService stockMovementService;

    @GetMapping
    public Page<StockMovementResponseDto> findAll(@PathVariable Integer stockProductId, Pageable pageable) {
        return stockMovementService.findByStockProductId(stockProductId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponseDto create(
            @PathVariable Integer stockProductId,
            @Valid @RequestBody StockMovementCreateRequestDto dto) {
        return stockMovementService.create(stockProductId, dto);
    }
}
