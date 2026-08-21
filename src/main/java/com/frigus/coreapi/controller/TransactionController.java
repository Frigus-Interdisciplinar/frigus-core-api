package com.frigus.coreapi.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frigus.coreapi.dto.transaction.CheckoutRequestDto;
import com.frigus.coreapi.dto.transaction.TransactionResponseDto;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/checkout")
    public TransactionResponseDto checkout(
            @Valid @RequestBody CheckoutRequestDto body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal User currentUser) {
        return transactionService.checkout(currentUser, body, idempotencyKey);
    }

    @GetMapping
    public Page<TransactionResponseDto> listTransactions(
            Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        return transactionService.listTransactions(currentUser, pageable);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponseDto getTransactionById(
            @PathVariable UUID transactionId,
            @AuthenticationPrincipal User currentUser) {
        return transactionService.getTransactionById(transactionId, currentUser);
    }

    @PostMapping("/{transactionId}/cancel")
    public TransactionResponseDto cancelTransaction(
            @PathVariable UUID transactionId,
            @AuthenticationPrincipal User currentUser) {
        return transactionService.cancelTransaction(transactionId, currentUser);
    }
}
