package com.frigus.coreapi.dto.user;

import com.frigus.coreapi.enums.AccountType;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {
    private UUID id;
    private String name;
    private String email;
    private AccountType accountType;
    private LocalDate birthDate;
}
