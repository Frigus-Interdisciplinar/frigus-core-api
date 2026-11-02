package com.frigus.coreapi.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

public final class OneTimeTokens {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int NUMERIC_CODE_BOUND = 1_000_000;

    private OneTimeTokens() { }

    public static String create() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String createNumericCode() {
        return String.format(
                Locale.ROOT,
                "%06d",
                RANDOM.nextInt(NUMERIC_CODE_BOUND)
        );
    }

    public static String hash(String token) {
        try {
            byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(tokenBytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
