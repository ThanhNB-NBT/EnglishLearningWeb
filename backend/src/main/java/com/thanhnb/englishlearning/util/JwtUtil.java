package com.thanhnb.englishlearning.util;

import com.thanhnb.englishlearning.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * Generate JWT token with issued timestamp
     */
    public String generateToken(String username) {
        Date expiration = new Date(System.currentTimeMillis() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Get username from JWT token
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * ✅ NEW: Get issued date from token
     */
    public Date getIssuedAtDateFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getIssuedAt();
    }

    /**
     * Get expiration date from token
     */
    public Date getExpirationDateFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }

    /**
     * Validate JWT token (basic validation)
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * ✅ FIXED: Check if token was issued before user's last login
     * Returns true if token is INVALID (issued before last login/password
     * change/block)
     * 
     * @param token         JWT token to check
     * @param lastLoginDate User's last login date (from database)
     * @return true if token is invalid (issued before lastLoginDate)
     */
    public boolean isTokenIssuedBeforeLastUpdate(String token, LocalDateTime lastLoginDate) {
        if (lastLoginDate == null) {
            return false; // No last login recorded, token is valid
        }

        try {
            Date tokenIssuedAt = getIssuedAtDateFromToken(token);

            // Convert LocalDateTime to Date for comparison
            Date lastUpdateDate = Date.from(lastLoginDate.atZone(ZoneId.systemDefault()).toInstant());

            // ✅ FIX: Add 1-second tolerance to handle same-second timestamps
            // Token is only invalid if issued MORE THAN 1 second before last login
            long toleranceMs = 1000; // 1 second tolerance
            long timeDifferenceMs = lastUpdateDate.getTime() - tokenIssuedAt.getTime();

            // Token invalid only if issued > 1 second before lastLoginDate
            boolean isInvalid = timeDifferenceMs > toleranceMs;

            if (isInvalid) {
                log.warn("Token issued at {} before last login/update at {} (diff: {} ms)",
                        tokenIssuedAt, lastUpdateDate, timeDifferenceMs);
            } else {
                log.debug("Token timestamp check passed (issued: {}, lastLogin: {}, diff: {} ms)",
                        tokenIssuedAt, lastUpdateDate, timeDifferenceMs);
            }

            return isInvalid;
        } catch (Exception e) {
            log.error("Error checking token issue date: {}", e.getMessage());
            return true; // Assume invalid if we can't check
        }
    }

    /**
     * Hash token để lưu vào database (for blacklist)
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}