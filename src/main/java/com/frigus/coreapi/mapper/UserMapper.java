package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper implements BaseMapper<User, UserResponseDto, UserRegisterRequestDto>{

    public UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId()).
                name(user.getName()).
                email(user.getEmail()).
                accountType(user.getAccountType()).
                birthDate(user.getBirthDate())
                .build();
    }

    public User toEntity(UserRegisterRequestDto dto) {
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .birthDate(dto.getBirthDate())
                .build();
    }
}
