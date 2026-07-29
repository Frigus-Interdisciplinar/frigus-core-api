package com.frigus.coreapi.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frigus.coreapi.enums.AccountType;
import lombok.*;

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

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate birthDate;
}
