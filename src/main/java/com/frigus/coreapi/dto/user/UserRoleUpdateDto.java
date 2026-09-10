package com.frigus.coreapi.dto.user;

import com.frigus.coreapi.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateDto {
    @NotNull(message = "A role não pode ser nula")
    private Role role;
}
