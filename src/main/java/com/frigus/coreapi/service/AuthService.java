package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto register(UserRegisterRequestDto body) {
        if (userRepository.existsByEmail(body.getEmail())) {
            throw new BadRequestException("Email já cadastrado", "O email digitado já está cadastrado");
        }

        if (body.getBirthDate().isAfter(LocalDate.now().minusYears(16))) {
            throw new BadRequestException("Idade mínima não atingida", "A idade mínima é de 16 anos");
        }

        String hashPassword = passwordEncoder.encode(body.getRawPassword());
        User user = User.builder()
                .email(body.getEmail())
                .hashPassword(hashPassword)
                .birthDate(body.getBirthDate())
                .name(body.getName())
                .accountType(AccountType.DOMESTIC)
                .build();
        user = userRepository.save(user);

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .accountType(user.getAccountType())
                .build();
    }
}