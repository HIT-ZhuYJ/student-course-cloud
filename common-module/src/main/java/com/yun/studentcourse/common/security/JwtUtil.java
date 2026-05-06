package com.yun.studentcourse.common.security;

import com.yun.studentcourse.common.RoleEnum;
import com.yun.studentcourse.common.dto.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;

public final class JwtUtil {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_RELATED_ID = "relatedId";

    private JwtUtil() {
    }

    public static String generateToken(String secret, UserContext userContext, long expirationMillis) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(expirationMillis);
        return Jwts.builder()
                .subject(userContext.getUsername())
                .claim(CLAIM_USER_ID, userContext.getUserId())
                .claim(CLAIM_USERNAME, userContext.getUsername())
                .claim(CLAIM_ROLE, userContext.getRole().name())
                .claim(CLAIM_RELATED_ID, userContext.getRelatedId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey(secret))
                .compact();
    }

    public static Claims parseClaims(String secret, String token) {
        return Jwts.parser()
                .verifyWith(secretKey(secret))
                .build()
                .parseSignedClaims(stripBearerPrefix(token))
                .getPayload();
    }

    public static UserContext parseUserContext(String secret, String token) {
        Claims claims = parseClaims(secret, token);
        return new UserContext(
                getLong(claims, CLAIM_USER_ID),
                claims.get(CLAIM_USERNAME, String.class),
                RoleEnum.valueOf(claims.get(CLAIM_ROLE, String.class)),
                getLong(claims, CLAIM_RELATED_ID)
        );
    }

    public static boolean validateToken(String secret, String token) {
        try {
            parseClaims(secret, token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public static Long getExpirationEpochMillis(String secret, String token) {
        Date expiration = parseClaims(secret, token).getExpiration();
        return expiration == null ? null : expiration.getTime();
    }

    public static String stripBearerPrefix(String token) {
        if (token == null) {
            return null;
        }
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return token.substring(7).trim();
        }
        return token.trim();
    }

    private static SecretKey secretKey(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static Long getLong(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }
}
