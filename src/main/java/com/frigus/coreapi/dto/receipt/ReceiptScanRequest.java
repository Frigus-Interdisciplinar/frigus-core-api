package com.frigus.coreapi.dto.receipt;
import jakarta.validation.constraints.*;
public record ReceiptScanRequest(@NotNull @Positive Integer stockId, @NotBlank @Size(max=4000) String qrPayload) {}
