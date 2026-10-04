package com.frigus.coreapi.utils;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class OneTimeTokens {
 private static final SecureRandom RANDOM=new SecureRandom();
 private OneTimeTokens(){}
 public static String create(){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
 public static String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
