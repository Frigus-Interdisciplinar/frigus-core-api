package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.preferences.AppSettingsDto;
import com.frigus.coreapi.service.AppSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile/app-settings")
@RequiredArgsConstructor
public class AppSettingsController {
    private final AppSettingsService service;

    @GetMapping
    public AppSettingsDto get() {
        return service.get();
    }

    @PutMapping
    public AppSettingsDto update(@Valid @RequestBody AppSettingsDto request) {
        return service.update(request);
    }
}
