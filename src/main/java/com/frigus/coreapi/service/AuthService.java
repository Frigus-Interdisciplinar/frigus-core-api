package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.GoogleLoginRequestDto;
import com.frigus.coreapi.dto.user.LoginRequestDto;
import com.frigus.coreapi.dto.user.LoginResponseDto;
import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.UnauthorizedException;
import com.frigus.coreapi.mapper.UserMapper;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.frigus.coreapi.service.GoogleTokenVerifierService.GoogleUserData;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

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
                .accountType(body.getAccountType() != null ? body.getAccountType() : AccountType.DOMESTIC)
                .emailVerified(false)
                .planSelectionRequired(true)
                .role(Role.USER)
                .createdAt(Instant.now())
                .build();
        user = userRepository.save(user);

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .planSelectionRequired(user.isPlanSelectionRequired())
                .birthDate(user.getBirthDate())
                .accountType(user.getAccountType())
                .build();
    }

    public LoginResponseDto login(LoginRequestDto body) {
        return login(body, null);
    }

    public LoginResponseDto login(LoginRequestDto body, String userAgent) {
        User user = userRepository.findByEmail(body.getEmail()).orElseThrow(() -> new UnauthorizedException("Credenciais inválidas", "Email ou senha incorretos"));

        if (user.getHashPassword() == null) {
            throw new BadRequestException("Conta vinculada ao Google", "Esta conta foi cadastrada utilizando o Google. Acesse usando 'Entrar com Google' ou redefina sua senha.");
        }

        if (!passwordEncoder.matches(body.getRawPassword(), user.getHashPassword())) {
            throw new UnauthorizedException("Credenciais inválidas", "Email ou senha incorretos");
        }

        return createLoginResponse(user, userAgent);
    }

    public LoginResponseDto loginWithGoogle(GoogleLoginRequestDto body) {
        return loginWithGoogle(body, null);
    }

    public LoginResponseDto loginWithGoogle(GoogleLoginRequestDto body, String userAgent) {
        GoogleUserData googleData = googleTokenVerifierService.verify(body.getIdToken());

        User user = userRepository.findByGoogleId(googleData.getGoogleId())
                .orElseGet(() -> userRepository.findByEmail(googleData.getEmail()).orElse(null));

        if (user != null) {
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleData.getGoogleId());
            }
            user.setEmailVerified(true);
            user = userRepository.save(user);
        } else {
            user = User.builder()
                    .email(googleData.getEmail())
                    .googleId(googleData.getGoogleId())
                    .name(googleData.getName())
                    .accountType(AccountType.DOMESTIC)
                    .emailVerified(true)
                    .planSelectionRequired(true)
                    .role(Role.USER)
                    .createdAt(Instant.now())
                    .build();
            user = userRepository.save(user);
        }

        return createLoginResponse(user, userAgent);
    }

    private LoginResponseDto createLoginResponse(User user, String userAgent) {
        if (userAgent == null) {
            return LoginResponseDto.builder()
                    .accessToken(tokenProvider.generateAccessToken(user))
                    .refreshToken(refreshTokenService.createRefreshToken(user.getId(), user.getTokenVersion()))
                    .user(userMapper.toDto(user))
                    .build();
        }
        UUID sessionId = UUID.randomUUID();
        String normalizedUserAgent = userAgent.length() > 512 ? userAgent.substring(0, 512) : userAgent;
        return LoginResponseDto.builder()
                .accessToken(tokenProvider.generateAccessToken(user, sessionId))
                .refreshToken(refreshTokenService.createRefreshToken(
                        user.getId(), user.getTokenVersion(), sessionId, normalizedUserAgent))
                .user(userMapper.toDto(user))
                .build();
    }

    public LoginResponseDto refreshToken(String refreshToken) {
        UUID userId = refreshTokenService.validateAndGetUserId(refreshToken);

        if (userId == null) {
            throw new UnauthorizedException("Refresh token inválido ou expirado", "Faça login novamente");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new UnauthorizedException("Usuário não encontrado", "Usuário inválido"));
        if (!refreshTokenService.isCurrentVersion(refreshToken, user.getTokenVersion())) {
            throw new UnauthorizedException("Refresh token inválido ou expirado", "Faça login novamente");
        }

        refreshTokenService.deleteRefreshToken(refreshToken);
        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getId(), user.getTokenVersion());

        return LoginResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(userMapper.toDto(user))
                .build();
    }

    public LoginResponseDto refreshToken(String refreshToken, String userAgent) {
        UUID userId = refreshTokenService.validateAndGetUserId(refreshToken);
        if (userId == null) {
            throw new UnauthorizedException("Refresh token inválido ou expirado", "Faça login novamente");
        }

        UUID sessionId = refreshTokenService.ensureSessionId(refreshToken, userAgent);
        if (sessionId == null) {
            throw new UnauthorizedException("Refresh token inválido ou expirado", "Faça login novamente");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado", "Usuário inválido"));
        if (!refreshTokenService.isCurrentVersion(refreshToken, user.getTokenVersion())) {
            throw new UnauthorizedException("Refresh token inválido ou expirado", "Faça login novamente");
        }

        String newRefreshToken = refreshTokenService.rotateRefreshToken(
                refreshToken, user.getId(), user.getTokenVersion(), sessionId);
        return LoginResponseDto.builder()
                .accessToken(tokenProvider.generateAccessToken(user, sessionId))
                .refreshToken(newRefreshToken)
                .user(userMapper.toDto(user))
                .build();
    }

    public void logout(String refreshToken) {
        if(refreshToken != null) {
            refreshTokenService.deleteRefreshToken(refreshToken);
        }
    }
}
