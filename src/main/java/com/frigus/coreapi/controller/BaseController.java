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
    public ResponseEntity<Page<TResponseDto>> findAll(Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TResponseDto> findById(@PathVariable TId id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable TId id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}