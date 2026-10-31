package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.EmailVerificationStatusDto;
import com.frigus.coreapi.dto.user.VerifyEmailCodeRequestDto;
import com.frigus.coreapi.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile/email-verification")
@RequiredArgsConstructor
public class EmailVerificationController {
    private final EmailVerificationService service;

    @GetMapping
    public EmailVerificationStatusDto status() {
        return service.status();
    }

    @PostMapping("/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestCode() {
        service.requestCode();
    }

    @PostMapping("/verify")
    public EmailVerificationStatusDto verify(@Valid @RequestBody VerifyEmailCodeRequestDto request) {
        return service.verify(request);
    }
}
