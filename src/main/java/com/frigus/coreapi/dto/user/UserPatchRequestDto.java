package com.frigus.coreapi.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPatchRequestDto {
    private String name;

    @Size(max = 2048)
    @Pattern(regexp = "^https://.+$", message = "A foto do perfil deve ser uma URL HTTPS")
    private String profilePictureUrl;

    @Past(message = "A data de nascimento deve ser no passado")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate birthDate;
}
