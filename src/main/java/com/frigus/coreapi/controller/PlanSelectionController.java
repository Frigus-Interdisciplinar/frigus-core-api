package com.frigus.coreapi.controller;
import com.frigus.coreapi.dto.transaction.*;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.TransactionService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class PlanSelectionController {
 private final TransactionService service;
 @PostMapping("/plans/select") @PreAuthorize("isAuthenticated()")
 public TransactionResponseDto select(@AuthenticationPrincipal User user,@Valid @RequestBody CheckoutRequestDto dto,@RequestHeader(value="Idempotency-Key",required=false) String key){return service.checkout(user,dto,key);}
}
