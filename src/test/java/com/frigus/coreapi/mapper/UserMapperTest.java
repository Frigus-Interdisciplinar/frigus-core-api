package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {
    private final UserMapper mapper = new UserMapper();

    @Test
    void mapsUsersInBothDirections() {
        User user = User.builder().id(UUID.randomUUID()).name("Ana").email("ana@frigus.com")
                .birthDate(LocalDate.of(1990, 1, 1)).accountType(AccountType.DOMESTIC).build();
        var response = mapper.toDto(user);
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
        assertThat(response.getAccountType()).isEqualTo(AccountType.DOMESTIC);

        var request = UserRegisterRequestDto.builder().name("Bia").email("bia@frigus.com")
                .birthDate(LocalDate.of(1991, 2, 2)).build();
        var entity = mapper.toEntity(request);
        assertThat(entity.getName()).isEqualTo("Bia");
        assertThat(entity.getBirthDate()).isEqualTo(request.getBirthDate());
    }
}
