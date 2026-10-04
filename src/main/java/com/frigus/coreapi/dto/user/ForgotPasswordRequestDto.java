package com.frigus.coreapi.dto.user;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record ForgotPasswordRequestDto(@NotBlank @Email @Size(max=255) String email) { }
