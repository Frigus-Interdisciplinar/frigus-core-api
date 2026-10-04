package com.frigus.coreapi.dto.user;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record ResetPasswordRequestDto(@NotBlank @Size(max=128) String token, @NotBlank @Size(min=8,max=128) String newPassword) { }
