package com.frigus.coreapi.controller;

import com.frigus.coreapi.service.BaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public abstract class BaseController<
    TModel,
    TId,
    TRequestDto,
    TResponseDto,
    TService extends BaseService<TModel, TId, TRequestDto, TResponseDto, ?, ?>> {

    protected final TService service;

    protected BaseController(TService service) {
        this.service = service;
    }

    @GetMapping
    public Page<TResponseDto> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    public TResponseDto findById(@PathVariable TId id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable TId id) {
        service.delete(id);
    }
}