package com.frigus.coreapi.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frigus.coreapi.dto.transaction.CheckoutRequestDto;
import com.frigus.coreapi.dto.transaction.TransactionResponseDto;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequiredArgsConstructor
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("checkout")
    public TransactionResponseDto checkout(@Valid @RequestBody CheckoutRequestDto body, @AuthenticationPrincipal User currentUser) {
        return transactionService.checkout(currentUser, body);
    }
    
    @GetMapping("{transactionId}")
    public TransactionResponseDto getTransactionById(@PathVariable UUID transactionId, @AuthenticationPrincipal User currentUser) {
        return transactionService.getTransactionById(transactionId, currentUser);
    }
}
