package com.frigus.coreapi.dto.receipt;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public record ReceiptConfirmRequest(@NotEmpty List<@Valid Item> items) {
 public record Item(@NotNull UUID lineId, @NotBlank String action, Integer productId, Integer quantity, LocalDate expireDate) {}
}
