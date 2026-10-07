package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.ForgotPasswordRequestDto;
import com.frigus.coreapi.dto.user.ResetPasswordRequestDto;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.utils.OneTimeTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock EmailService email;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> values;
    @InjectMocks PasswordRecoveryService service;

    private User user;

    @BeforeEach
    void setup() {
        user = User.builder().id(UUID.randomUUID()).email("ana@example.test").hashPassword("old").build();
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void requestStoresOnlyCodeHashForTenMinutesAndSendsCode() {
        when(values.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(60)))).thenReturn(true);
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull(user.getEmail())).thenReturn(Optional.of(user));

        service.request(new ForgotPasswordRequestDto("ANA@example.test"));

        ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        String key = "password-recovery:" + user.getId();
        verify(values).set(eq(key), storedHash.capture(), eq(Duration.ofMinutes(10)));
        verify(email).sendRecoveryCode(eq(user.getEmail()), code.capture());
        assertThat(code.getValue()).matches("\\d{6}");
        assertThat(storedHash.getValue()).isEqualTo(OneTimeTokens.hash(code.getValue()));
    }

    @Test
    void resetChangesPasswordConsumesCodeAndRevokesPreviouslyIssuedSessions() {
        String key = "password-recovery:" + user.getId();
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(values.get(key)).thenReturn(OneTimeTokens.hash("123456"));
        when(encoder.encode("NewPass123!")).thenReturn("new-hash");

        service.reset(new ResetPasswordRequestDto(user.getEmail(), "123456", "NewPass123!"));

        assertThat(user.getHashPassword()).isEqualTo("new-hash");
        assertThat(user.getTokenVersion()).isEqualTo(1);
        verify(redis).delete(key);
        verify(users).save(user);
    }

    @Test
    void missingExpiredCodeCannotChangePassword() {
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(values.get("password-recovery:" + user.getId())).thenReturn(null);

        assertThatThrownBy(() -> service.reset(new ResetPasswordRequestDto(user.getEmail(), "123456", "NewPass123!")))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(encoder);
        verify(users, never()).save(any());
    }

    @Test
    void repeatedRecoveryRequestDoesNotSendMoreMail() {
        when(values.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(60)))).thenReturn(false);

        service.request(new ForgotPasswordRequestDto(user.getEmail()));

        verifyNoInteractions(users, email);
    }
}
