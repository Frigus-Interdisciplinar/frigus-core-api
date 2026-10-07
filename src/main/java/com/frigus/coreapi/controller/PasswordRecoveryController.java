package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.ForgotPasswordRequestDto;
import com.frigus.coreapi.dto.user.ResetPasswordRequestDto;
import com.frigus.coreapi.dto.user.VerifyPasswordRecoveryCodeRequestDto;
import com.frigus.coreapi.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordRecoveryController {
    private final PasswordRecoveryService service;

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void request(@Valid @RequestBody ForgotPasswordRequestDto request) {
        service.request(request);
    }

    @PostMapping("/verify-reset-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody VerifyPasswordRecoveryCodeRequestDto request) {
        service.verify(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequestDto request) {
        service.reset(request);
    }
}
