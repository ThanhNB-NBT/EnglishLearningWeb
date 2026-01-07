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
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * ✅ Generate JWT token with role
     */
    public String generateToken(String username, String role) {
        Date expiration = new Date(System.currentTimeMillis() + jwtProperties.getExpiration());
        
        // Add ROLE_ prefix if not present
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return Jwts.builder()
                .subject(username)
                .claim("roles", List.of(roleWithPrefix))
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Backward compatibility - default to USER role
     */
    public String generateToken(String username) {
        return generateToken(username, "USER");
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    
    /**
     * ✅ Extract roles from JWT token
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List) {
                return (List<String>) rolesObj;
            }
            
            return List.of("ROLE_USER");
        } catch (Exception e) {
            log.warn("Could not extract roles from token: {}", e.getMessage());
            return List.of("ROLE_USER");
        }
    }

    public Date getIssuedAtDateFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getIssuedAt();
    }

    public Date getExpirationDateFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }

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

    public boolean isTokenIssuedBeforeLastUpdate(String token, LocalDateTime lastLoginDate) {
        if (lastLoginDate == null) {
            return false;
        }

        try {
            Date tokenIssuedAt = getIssuedAtDateFromToken(token);
            Date lastUpdateDate = Date.from(lastLoginDate.atZone(ZoneId.systemDefault()).toInstant());

            long toleranceMs = 1000;
            long timeDifferenceMs = lastUpdateDate.getTime() - tokenIssuedAt.getTime();

            boolean isInvalid = timeDifferenceMs > toleranceMs;

            if (isInvalid) {
                log.warn("Token issued at {} before last login/update at {} (diff: {} ms)",
                        tokenIssuedAt, lastUpdateDate, timeDifferenceMs);
            }

            return isInvalid;
        } catch (Exception e) {
            log.error("Error checking token issue date: {}", e.getMessage());
            return true;
        }
    }

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