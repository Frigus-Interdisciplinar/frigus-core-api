package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.moneysaving.MoneySavingDtos;
import com.frigus.coreapi.service.FoodPurchaseService;
import com.frigus.coreapi.service.MoneySavingReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/groups/{groupId}")
public class MoneySavingController {
    private final FoodPurchaseService purchases;
    private final MoneySavingReportService reports;

    @PostMapping("/food-purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public MoneySavingDtos.Purchase create(@PathVariable UUID groupId,
            @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody MoneySavingDtos.PurchaseInput input) {
        return purchases.create(groupId, idempotencyKey, input);
    }

    @GetMapping("/food-purchases/{purchaseId}")
    public MoneySavingDtos.Purchase get(@PathVariable UUID groupId, @PathVariable UUID purchaseId) {
        return purchases.get(groupId, purchaseId);
    }

    @PutMapping("/food-purchases/{purchaseId}")
    public MoneySavingDtos.Purchase update(@PathVariable UUID groupId, @PathVariable UUID purchaseId,
            @RequestBody MoneySavingDtos.PurchaseInput input) {
        return purchases.update(groupId, purchaseId, input);
    }

    @DeleteMapping("/food-purchases/{purchaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID groupId, @PathVariable UUID purchaseId) {
        purchases.delete(groupId, purchaseId);
    }

    @GetMapping("/money-saving/summary")
    public MoneySavingDtos.Summary summary(@PathVariable UUID groupId,
            @RequestParam("from") LocalDate from, @RequestParam("to") LocalDate to) {
        return reports.summary(groupId, from, to);
    }

    @GetMapping("/money-saving/events")
    public MoneySavingDtos.EventPage events(@PathVariable UUID groupId,
            @RequestParam("from") LocalDate from, @RequestParam("to") LocalDate to,
            @RequestParam(required = false) String category, @RequestParam(required = false) String kind,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return reports.events(groupId, from, to, category, kind, page, size);
    }
}
