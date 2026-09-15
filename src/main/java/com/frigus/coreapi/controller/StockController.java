package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.stock.StockCreateRequestDto;
import com.frigus.coreapi.dto.stock.StockResponseDto;
import com.frigus.coreapi.dto.stock.StockUpdateRequestDto;
import com.frigus.coreapi.model.Stock;
import com.frigus.coreapi.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RestController
@RequestMapping("/stocks")
public class StockController extends BaseController<Stock, Integer, StockCreateRequestDto, StockResponseDto, StockService> {
    public StockController(StockService service) {
        super(service);
    }

    @GetMapping
    public Page<StockResponseDto> findAll(@RequestParam UUID groupId, Pageable pageable) {
        return service.findByGroupId(groupId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockResponseDto create(@Valid @RequestBody StockCreateRequestDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public StockResponseDto update(@PathVariable Integer id, @Valid @RequestBody StockUpdateRequestDto dto) {
        return service.update(id, dto);
    }
}
