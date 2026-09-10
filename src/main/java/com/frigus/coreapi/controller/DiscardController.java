package com.frigus.coreapi.controller;

import jakarta.validation.Valid;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscardResponseDto create(@Valid @RequestBody DiscardCreateRequestDto dto) {
        return service.create(dto);
    }
}
