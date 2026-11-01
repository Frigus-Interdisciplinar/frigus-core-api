package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.ForgotPasswordRequestDto;
import com.frigus.coreapi.client.EmailDeliveryException;
import com.frigus.coreapi.dto.user.ResetPasswordRequestDto;
import com.frigus.coreapi.dto.user.VerifyPasswordRecoveryCodeRequestDto;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.utils.OneTimeTokens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordRecoveryService {
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration REQUEST_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_CODE_ATTEMPTS = 5;

    private static final String CODE_KEY_PREFIX = "password-recovery:";
    private static final String ATTEMPT_KEY_PREFIX = "password-recovery-attempts:";
    private static final String REQUEST_KEY_PREFIX = "password-request:";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final StringRedisTemplate redis;

    public void request(ForgotPasswordRequestDto request) {
        String email = normalizeEmail(request.email());
        String requestKey = REQUEST_KEY_PREFIX + OneTimeTokens.hash(email);
        if (!acquireRequestCooldown(requestKey)) return;

        users.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .ifPresent(user -> sendRecoveryCode(user, email, requestKey));
    }

    public void verify(VerifyPasswordRecoveryCodeRequestDto request) {
        User user = findActiveUser(request.email());
        validateCode(user, request.code());
    }

    @Transactional
    public void reset(ResetPasswordRequestDto request) {
        User user = lockActiveUser(request.email());
        String codeKey = validateCode(user, request.code());

        user.setHashPassword(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        users.save(user);

        redis.delete(codeKey);
        redis.delete(attemptKey(user.getId()));
    }

    private boolean acquireRequestCooldown(String requestKey) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(requestKey, "1", REQUEST_COOLDOWN)
        );
    }

    private void sendRecoveryCode(User user, String email, String requestKey) {
        String codeKey = codeKey(user.getId());
        String code = OneTimeTokens.createNumericCode();

        redis.delete(attemptKey(user.getId()));
        redis.opsForValue().set(codeKey, OneTimeTokens.hash(code), CODE_TTL);

        try {
            emailService.sendRecoveryCode(email, code);
        } catch (EmailDeliveryException exception) {
            redis.delete(codeKey);
            redis.delete(requestKey);
            log.warn(
                    "Falha no provedor de e-mail de recuperação: {}",
                    exception.getClass().getSimpleName()
            );
        }
    }

    private User findActiveUser(String email) {
        return users.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(email))
                .orElseThrow(this::invalidCode);
    }

    private User lockActiveUser(String email) {
        User reference = findActiveUser(email);
        return users.findByIdForUpdate(reference.getId()).orElseThrow(this::invalidCode);
    }

    private String validateCode(User user, String code) {
        String key = codeKey(user.getId());
        String storedHash = redis.opsForValue().get(key);
        String submittedHash = OneTimeTokens.hash(code);

        if (storedHash == null) throw invalidCode();
        if (!matches(storedHash, submittedHash)) {
            recordFailedAttempt(user, key);
            throw invalidCode();
        }

        return key;
    }

    private boolean matches(String storedHash, String submittedHash) {
        return MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                submittedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void recordFailedAttempt(User user, String codeKey) {
        String key = attemptKey(user.getId());
        Long attempts = redis.opsForValue().increment(key);
        if (attempts == null) return;

        if (attempts == 1L) redis.expire(key, CODE_TTL);
        if (attempts >= MAX_CODE_ATTEMPTS) {
            redis.delete(codeKey);
            redis.delete(key);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String codeKey(UUID userId) {
        return CODE_KEY_PREFIX + userId;
    }

    private String attemptKey(UUID userId) {
        return ATTEMPT_KEY_PREFIX + userId;
    }

    private BadRequestException invalidCode() {
        return new BadRequestException(
                "Código inválido ou expirado",
                "Confira o código ou solicite um novo"
        );
    }
}
