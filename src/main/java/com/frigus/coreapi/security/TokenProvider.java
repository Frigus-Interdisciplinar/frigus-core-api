package com.frigus.coreapi.security;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.frigus.coreapi.model.User;

import java.util.Date;
import java.util.UUID;

@Component
public class TokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-time}")
    private Long expirationTime;

    public String generateAccessToken(User user) {
        Date issuedAt = new Date();
        Date expirationDate = new Date(issuedAt.getTime() + expirationTime);
        return JWT.create()
                .withSubject(user.getId().toString())
                .withIssuedAt(issuedAt)
                .withExpiresAt(expirationDate)
                .withIssuer("frigus")
                .withClaim("plan", user.getPlanNameFromUser())
                .sign(Algorithm.HMAC256(secret));
    }

    public String validateAccessTokenAndGetSubject(String accessToken) {
        try {
            return JWT.require(Algorithm.HMAC256(secret))
            .withIssuer("frigus")
            .build()
            .verify(accessToken)
            .getSubject();
        } catch(JWTVerificationException e) {
            return null;
        }
    }

    public DecodedJWT validateAccessToken(String accessToken) {
        try {
            return JWT.require(Algorithm.HMAC256(secret))
            .withIssuer("frigus")
            .build()
            .verify(accessToken);
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
