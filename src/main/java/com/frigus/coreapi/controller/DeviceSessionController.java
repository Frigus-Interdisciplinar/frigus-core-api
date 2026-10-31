package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.DeviceSessionResponseDto;
import com.frigus.coreapi.service.DeviceSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/profile/sessions")
@RequiredArgsConstructor
public class DeviceSessionController {
    private final DeviceSessionService service;

    @GetMapping
    public List<DeviceSessionResponseDto> list() {
        return service.list();
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID sessionId) {
        service.revoke(sessionId);
    }
}
