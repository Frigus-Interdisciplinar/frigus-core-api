package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.LoginRequestDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenProvider tokenProvider;
    @Mock private UserMapper userMapper;
    @Mock private RefreshTokenService refreshTokenService;
    @InjectMocks private AuthService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).name("Ana").email("ana@frigus.com")
                .birthDate(LocalDate.of(1990, 1, 1)).hashPassword("hash").role(Role.USER)
                .accountType(AccountType.DOMESTIC).build();
    }

    @Test
    void registersAnEligibleNewUserWithDefaultAccountData() {
        UserRegisterRequestDto request = UserRegisterRequestDto.builder().name("Ana").email(user.getEmail())
                .birthDate(user.getBirthDate()).rawPassword("Senha@123").build();
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponseDto result = service.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(saved.getValue().getAccountType()).isEqualTo(AccountType.DOMESTIC);
        assertThat(result.getId()).isEqualTo(user.getId());
    }

    @Test
    void rejectsDuplicateAndUnderageRegistrations() {
        UserRegisterRequestDto duplicate = UserRegisterRequestDto.builder().email(user.getEmail())
                .birthDate(LocalDate.now().minusYears(20)).build();
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);
        assertThatThrownBy(() -> service.register(duplicate)).isInstanceOf(BadRequestException.class)
                .hasMessage("Email já cadastrado");

        UserRegisterRequestDto underage = UserRegisterRequestDto.builder().email("new@frigus.com")
                .birthDate(LocalDate.now().minusYears(15)).build();
        assertThatThrownBy(() -> service.register(underage)).isInstanceOf(BadRequestException.class)
                .hasMessage("Idade mínima não atingida");
        verify(userRepository, never()).save(any());
    }

    @Test
    void logsInOnlyWithValidCredentials() {
        UserResponseDto dto = UserResponseDto.builder().id(user.getId()).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha@123", "hash")).thenReturn(true);
        when(tokenProvider.generateAccessToken(user)).thenReturn("access");
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn("refresh");
        when(userMapper.toDto(user)).thenReturn(dto);

        var result = service.login(LoginRequestDto.builder().email(user.getEmail()).rawPassword("Senha@123").build());

        assertThat(result.getAccessToken()).isEqualTo("access");
        assertThat(result.getRefreshToken()).isEqualTo("refresh");

        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        assertThatThrownBy(() -> service.login(LoginRequestDto.builder().email(user.getEmail()).rawPassword("wrong").build()))
                .isInstanceOf(BadRequestException.class).hasMessage("Senha incorreta");
        when(userRepository.findByEmail("missing@frigus.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(LoginRequestDto.builder().email("missing@frigus.com").rawPassword("x").build()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rotatesRefreshTokenAndRejectsInvalidOrUnknownUsers() {
        UserResponseDto dto = UserResponseDto.builder().id(user.getId()).build();
        when(refreshTokenService.validateAndGetUserId("old")).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn("new-access");
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn("new-refresh");
        when(userMapper.toDto(user)).thenReturn(dto);

        var result = service.refreshToken("old");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenService).deleteRefreshToken("old");

        when(refreshTokenService.validateAndGetUserId("invalid")).thenReturn(null);
        assertThatThrownBy(() -> service.refreshToken("invalid")).isInstanceOf(UnauthorizedException.class);
        when(refreshTokenService.validateAndGetUserId("missing-user")).thenReturn(UUID.randomUUID());
        assertThatThrownBy(() -> service.refreshToken("missing-user")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logsOutOnlyWhenThereIsAToken() {
        service.logout("refresh");
        service.logout(null);
        verify(refreshTokenService).deleteRefreshToken("refresh");
    }
}
