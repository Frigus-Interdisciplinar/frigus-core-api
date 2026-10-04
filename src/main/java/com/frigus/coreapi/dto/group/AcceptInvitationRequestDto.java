package com.frigus.coreapi.dto.group;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record AcceptInvitationRequestDto(@NotBlank @Size(max=128) String token) { }
