package com.frigus.coreapi.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.frigus.coreapi.dto.discard.DiscardCreateRequestDto;
import com.frigus.coreapi.dto.discard.DiscardResponseDto;
import com.frigus.coreapi.model.Discard;
import com.frigus.coreapi.service.DiscardService;

@RestController
@RequestMapping("/discard")
public class DiscardController extends BaseController<Discard, Integer, DiscardCreateRequestDto, DiscardResponseDto, DiscardService> {

    public DiscardController(DiscardService service) {
        super(service);
    }

    @GetMapping
    @Override
    public Page<DiscardResponseDto> findAll(Pageable pageable) {
        return super.findAll(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscardResponseDto create(@Valid @RequestBody DiscardCreateRequestDto dto) {
        return service.create(dto);
    }
}
