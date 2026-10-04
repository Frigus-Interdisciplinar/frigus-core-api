package com.frigus.coreapi.controller;
import com.frigus.coreapi.dto.user.*;
import com.frigus.coreapi.service.PasswordRecoveryService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/auth") @RequiredArgsConstructor
public class PasswordRecoveryController {
 private final PasswordRecoveryService service;
 @PostMapping("/forgot-password") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void request(@Valid @RequestBody ForgotPasswordRequestDto dto){service.request(dto);}
 @PostMapping("/reset-password") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void reset(@Valid @RequestBody ResetPasswordRequestDto dto){service.reset(dto);}
}
