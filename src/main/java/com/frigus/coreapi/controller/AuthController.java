package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.LoginRequestDto;
import com.frigus.coreapi.dto.user.LoginResponseDto;
import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto body, HttpServletResponse response) {
        LoginResponseDto loginResponseDto = authService.login(body);

        setTokenCookies(response, loginResponseDto.getAccessToken(), loginResponseDto.getRefreshToken());

        return loginResponseDto;
    }

    @PostMapping("/refresh")
    public LoginResponseDto refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletResponse response) {

        String refreshToken = (refreshTokenFromCookie != null && !refreshTokenFromCookie.isBlank())
                ? refreshTokenFromCookie
                : (body != null ? body.get("refreshToken") : null);

        LoginResponseDto loginResponseDto = authService.refreshToken(refreshToken);

        setTokenCookies(response, loginResponseDto.getAccessToken(), loginResponseDto.getRefreshToken());

        return loginResponseDto;
    }

    @PostMapping("/logout")
    public void logout(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletResponse response) {

        String refreshToken = (refreshTokenFromCookie != null && !refreshTokenFromCookie.isBlank())
                ? refreshTokenFromCookie
                : (body != null ? body.get("refreshToken") : null);

        authService.logout(refreshToken);

        clearTokenCookies(response);
    }

    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .path("/")
                .maxAge(15 * 60) // 15 minutos
                .sameSite("Lax")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(30 * 24 * 60 * 60) // 30 dias
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }

    private void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }
}
