package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.subscription.BillingJobResultDto;
import com.frigus.coreapi.dto.subscription.SubscriptionResponseDto;
import com.frigus.coreapi.dto.subscription.SubscriptionStatusUpdateDto;
import com.frigus.coreapi.enums.SubscriptionStatus;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.SubscriptionBillingJobService;
import com.frigus.coreapi.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionBillingJobService subscriptionBillingJobService;

    @GetMapping("/me")
    public SubscriptionResponseDto getMySubscription(@AuthenticationPrincipal User currentUser) {
        return subscriptionService.getMySubscription(currentUser);
    }

    @PostMapping("/me/cancel")
    public SubscriptionResponseDto cancelMySubscription(@AuthenticationPrincipal User currentUser) {
        return subscriptionService.cancelMySubscription(currentUser);
    }

    @PostMapping("/me/reactivate")
    public SubscriptionResponseDto reactivateMySubscription(@AuthenticationPrincipal User currentUser) {
        return subscriptionService.reactivateMySubscription(currentUser);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<SubscriptionResponseDto> listSubscriptions(
            Pageable pageable,
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) Integer planId) {
        return subscriptionService.listSubscriptions(pageable, status, planId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionResponseDto getSubscriptionById(@PathVariable UUID id) {
        return subscriptionService.getSubscriptionById(id);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionResponseDto getSubscriptionByUserId(@PathVariable UUID userId) {
        return subscriptionService.getSubscriptionByUserId(userId);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionResponseDto updateSubscriptionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody SubscriptionStatusUpdateDto dto) {
        return subscriptionService.updateSubscriptionStatus(id, dto.getStatus());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionResponseDto cancelSubscriptionByAdmin(@PathVariable UUID id) {
        return subscriptionService.cancelSubscriptionByAdmin(id);
    }

    @PostMapping("/billing/run")
    @PreAuthorize("hasRole('ADMIN')")
    public BillingJobResultDto runBillingJobManually() {
        return subscriptionBillingJobService.runDailyBillingJob();
    }
}
