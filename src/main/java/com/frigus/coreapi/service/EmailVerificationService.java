package com.frigus.coreapi.service;

import com.frigus.coreapi.client.EmailDeliveryException;
import com.frigus.coreapi.dto.user.EmailVerificationStatusDto;
import com.frigus.coreapi.dto.user.VerifyEmailCodeRequestDto;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ServiceUnavailableException;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.utils.OneTimeTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration REQUEST_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final String CODE_KEY_PREFIX = "email-verification:";
    private static final String ATTEMPT_KEY_PREFIX = "email-verification-attempts:";
    private static final String REQUEST_KEY_PREFIX = "email-verification-request:";

    private final GroupAccessService access;
    private final UserRepository users;
    private final EmailService email;
    private final StringRedisTemplate redis;

    @Transactional(readOnly = true)
    public EmailVerificationStatusDto status() {
        User user = access.requireCurrentUser();
        return new EmailVerificationStatusDto(user.getEmail(), user.isEmailVerified());
    }

    @Transactional
    public void requestCode() {
        User user = access.requireCurrentUser();
        if (user.isEmailVerified()) return;

        String requestKey = REQUEST_KEY_PREFIX + user.getId();
        Boolean acquired = redis.opsForValue().setIfAbsent(requestKey, "1", REQUEST_COOLDOWN);
        if (!Boolean.TRUE.equals(acquired)) return;

        String code = OneTimeTokens.createNumericCode();
        String codeKey = codeKey(user);
        String normalizedEmail = user.getEmail().toLowerCase(Locale.ROOT);
        String value = OneTimeTokens.hash(code) + ":" + OneTimeTokens.hash(normalizedEmail);
        redis.delete(attemptKey(user));
        redis.opsForValue().set(codeKey, value, CODE_TTL);

        try {
            email.sendEmailVerificationCode(user.getEmail(), code);
        } catch (EmailDeliveryException exception) {
            redis.delete(codeKey);
            redis.delete(requestKey);
            throw new ServiceUnavailableException(
                    "Falha no envio do código",
                    "Tente solicitar a confirmação novamente em alguns instantes");
        }
    }

    @Transactional
    public EmailVerificationStatusDto verify(VerifyEmailCodeRequestDto request) {
        User user = access.requireCurrentUser();
        if (user.isEmailVerified()) return new EmailVerificationStatusDto(user.getEmail(), true);

        String key = codeKey(user);
        String storedValue = redis.opsForValue().get(key);
        String[] storedParts = storedValue == null ? new String[0] : storedValue.split(":", -1);
        String submittedCodeHash = OneTimeTokens.hash(request.code());
        String currentEmailHash = OneTimeTokens.hash(user.getEmail().toLowerCase(Locale.ROOT));
        if (storedParts.length != 2
                || !matches(storedParts[0], submittedCodeHash)
                || !matches(storedParts[1], currentEmailHash)) {
            recordFailedAttempt(user, key);
            throw invalidCode();
        }

        user.setEmailVerified(true);
        users.save(user);
        redis.delete(key);
        redis.delete(attemptKey(user));
        redis.delete(REQUEST_KEY_PREFIX + user.getId());
        return new EmailVerificationStatusDto(user.getEmail(), true);
    }

    private void recordFailedAttempt(User user, String codeKey) {
        String key = attemptKey(user);
        Long attempts = redis.opsForValue().increment(key);
        if (attempts == null) return;
        if (attempts == 1L) redis.expire(key, CODE_TTL);
        if (attempts >= MAX_CODE_ATTEMPTS) {
            redis.delete(codeKey);
            redis.delete(key);
        }
    }

    private boolean matches(String stored, String submitted) {
        return MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8), submitted.getBytes(StandardCharsets.UTF_8));
    }

    private String codeKey(User user) {
        return CODE_KEY_PREFIX + user.getId();
    }

    private String attemptKey(User user) {
        return ATTEMPT_KEY_PREFIX + user.getId();
    }

    private BadRequestException invalidCode() {
        return new BadRequestException("Código inválido ou expirado", "Confira o código ou solicite um novo");
    }
}
