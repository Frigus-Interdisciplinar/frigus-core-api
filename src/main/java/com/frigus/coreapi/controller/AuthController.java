package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.LoginRequestDto;
import com.frigus.coreapi.dto.user.LoginResponseDto;
import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public UserResponseDto register(@Valid @RequestBody UserRegisterRequestDto body) {
        return authService.register(body);
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto body) {
        return authService.login(body);
    }

    @PostMapping("/refresh")
    public LoginResponseDto refresh(
            @RequestBody(required = false) Map<String, String> body) {
        return authService.refreshToken(body != null ? body.get("refreshToken") : null);
    }

    @PostMapping("/logout")
    public void logout(@RequestBody(required = false) Map<String, String> body) {
        authService.logout(body != null ? body.get("refreshToken") : null);
    }
}
