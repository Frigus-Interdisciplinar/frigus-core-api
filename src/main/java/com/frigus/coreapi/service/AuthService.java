package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.LoginRequestDto;
import com.frigus.coreapi.dto.user.LoginResponseDto;
import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.UnauthorizedException;
import com.frigus.coreapi.mapper.UserMapper;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;

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

    public LoginResponseDto login(LoginRequestDto body) {
        User user = userRepository.findByEmail(body.getEmail()).orElseThrow(() -> new UnauthorizedException("Credenciais inválidas", "Email ou senha incorretos"));

        if (!passwordEncoder.matches(body.getRawPassword(), user.getHashPassword())) {
            throw new BadRequestException("Senha incorreta", "Senha incorreta");
        }

        return LoginResponseDto.builder()
                .accessToken(tokenProvider.generateAccessToken(user))
                .refreshToken(refreshTokenService.createRefreshToken(user.getId()))
                .user(userMapper.toDto(user))
                .build();
    }

    public LoginResponseDto refreshToken(String refreshToken) {
        UUID userId = refreshTokenService.validateAndGetUserId(refreshToken);

        if (userId == null) {
            throw new UnauthorizedException("Refresh token inválido ou expirado", "Faça login novamente");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new UnauthorizedException("Usuário não encontrado", "Usuário inválido"));

        refreshTokenService.deleteRefreshToken(refreshToken);
        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

        return LoginResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(userMapper.toDto(user))
                .build();
    }

    public void logout(String refreshToken) {
        if(refreshToken != null) {
            refreshTokenService.deleteRefreshToken(refreshToken);
        }
    }

    // TODO: implementar recuperacao de senha
}