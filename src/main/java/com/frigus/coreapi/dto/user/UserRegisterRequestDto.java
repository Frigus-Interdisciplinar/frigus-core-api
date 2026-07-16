package com.frigus.coreapi.dto.user;

import lombok.*;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterRequestDto {
    private String name;
    private LocalDate birthDate;
    private String email;
    private String rawPassword;
}
