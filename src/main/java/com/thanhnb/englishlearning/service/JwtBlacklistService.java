// File: JwtBlacklistService.java
package com.thanhnb.englishlearning.service;

import com.thanhnb.englishlearning.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtBlacklistService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;
    
    /**
     * Thêm JWT token vào blacklist (logout)
     */
    public void blacklistToken(String token) {
        try {
            // Lấy expiration time từ JWT
            Date expiration = jwtUtil.getExpirationDateFromToken(token);
            long currentTime = System.currentTimeMillis();
            long ttlMillis = expiration.getTime() - currentTime;
            
            // Chỉ blacklist nếu token chưa hết hạn
            if (ttlMillis > 0) {
                String blacklistKey = buildBlacklistKey(token);
                
                // Lưu vào Redis với TTL = thời gian còn lại của JWT
                redisTemplate.opsForValue().set(
                    blacklistKey, 
                    "revoked", 
                    ttlMillis, 
                    TimeUnit.MILLISECONDS
                );
                
                String username = jwtUtil.getUsernameFromToken(token);
                log.info("Token blacklisted successfully for user: {} (TTL: {} ms)", username, ttlMillis);
            } else {
                log.warn("Token is already expired, no need to blacklist");
            }
        } catch (Exception e) {
            log.error("Error blacklisting token: {}", e.getMessage());
            throw new RuntimeException("Failed to blacklist token");
        }
    }
    
    /**
     * Kiểm tra token có bị blacklist không
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String blacklistKey = buildBlacklistKey(token); 
            boolean isBlacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
            
            if (isBlacklisted) {
                log.debug("Token is blacklisted");
            }
            
            return isBlacklisted;
        } catch (Exception e) {
            log.error("Error checking token blacklist status: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Tạo Redis key cho blacklist 
     */
    private String buildBlacklistKey(String token) {
        String tokenHash = jwtUtil.hashToken(token);
        return "blacklist:" + tokenHash; 
    }
    
    /**
     * Blacklist tất cả token của một user (logout all devices)
     */
    public void blacklistAllUserTokens(String username) {
        // Implementation phức tạp - có thể để sau
        log.info("blacklistAllUserTokens called for user: {}", username);
        log.warn("Full implementation requires tracking all user tokens");
    }
    
    /**
     * Lấy thống kê blacklist
     */
    public long getBlacklistedTokenCount() {
        try {
            return redisTemplate.keys("blacklist:*").size();
        } catch (Exception e) {
            log.error("Error getting blacklisted token count: {}", e.getMessage());
            return 0;
        }
    }
}