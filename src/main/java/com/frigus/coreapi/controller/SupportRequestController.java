package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.support.SupportRequestCreateDto;
import com.frigus.coreapi.dto.support.SupportRequestPageResponseDto;
import com.frigus.coreapi.dto.support.SupportRequestResponseDto;
import com.frigus.coreapi.service.SupportRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/support/requests")
@RequiredArgsConstructor
public class SupportRequestController {
    private final SupportRequestService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupportRequestResponseDto create(@Valid @RequestBody SupportRequestCreateDto request) {
        return service.create(request);
    }

    @GetMapping
    public SupportRequestPageResponseDto list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public SupportRequestResponseDto get(@PathVariable UUID id) {
        return service.get(id);
    }
}
