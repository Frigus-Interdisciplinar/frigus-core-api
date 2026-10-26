package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.receipt.*;
import com.frigus.coreapi.service.ReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/receipts") @RequiredArgsConstructor
public class ReceiptController {
 private final ReceiptService service;
 @PostMapping("/scan") @ResponseStatus(HttpStatus.CREATED)
 public ReceiptResponse scan(@Valid @RequestBody ReceiptScanRequest request){return service.scan(request);}
 @GetMapping("/{receiptId}")
 public ReceiptResponse get(@PathVariable UUID receiptId){return service.get(receiptId);}
 @PostMapping("/{receiptId}/confirm")
 public ReceiptResponse.ConfirmationResponse confirm(@PathVariable UUID receiptId,@Valid @RequestBody ReceiptConfirmRequest request){return service.confirm(receiptId,request);}
}
