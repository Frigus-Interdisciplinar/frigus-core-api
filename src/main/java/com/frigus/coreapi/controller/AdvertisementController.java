package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.advertisement.AdvertisementRequestDto;
import com.frigus.coreapi.dto.advertisement.AdvertisementResponseDto;
import com.frigus.coreapi.dto.advertisement.AdvertisementStatusRequestDto;
import com.frigus.coreapi.service.AdvertisementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/advertisements")
@RequiredArgsConstructor
public class AdvertisementController {
    private final AdvertisementService service;

    @GetMapping("/active")
    public Page<AdvertisementResponseDto> active(Pageable pageable) { return service.listActive(pageable); }

    @PostMapping("/{id}/impressions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void impression(@PathVariable UUID id) { service.recordImpression(id); }

    @PostMapping("/{id}/clicks")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void click(@PathVariable UUID id) { service.recordClick(id); }

    @GetMapping("/mine")
    public Page<AdvertisementResponseDto> mine(Pageable pageable) { return service.listMine(pageable); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdvertisementResponseDto create(@Valid @RequestBody AdvertisementRequestDto request) { return service.create(request); }

    @PutMapping("/{id}")
    public AdvertisementResponseDto update(@PathVariable UUID id, @Valid @RequestBody AdvertisementRequestDto request) { return service.update(id, request); }

    @PatchMapping("/{id}/status")
    public AdvertisementResponseDto status(@PathVariable UUID id, @Valid @RequestBody AdvertisementStatusRequestDto request) { return service.changeStatus(id, request.status()); }
}
