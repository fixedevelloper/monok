package com.monokek.identity.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Issues and validates the stateless access tokens that replace Laravel
 * Sanctum's {@code personal_access_tokens}. One token == one logged-in
 * device/session, exactly like {@code $user->createToken($deviceName)}.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl-minutes}") long ttlMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMinutes = ttlMinutes;
    }

    public String generateToken(Long userId, UUID userUuid, String name, List<String> roles, List<String> permissions) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("uuid", userUuid.toString())
                .claim("name", name)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(ttlMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }
}
