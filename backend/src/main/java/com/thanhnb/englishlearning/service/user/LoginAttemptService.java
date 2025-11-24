package com.thanhnb.englishlearning.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service quản lý login attempts và account lockout
 * Chặn tài khoản sau 5 lần đăng nhập sai trong 15 phút
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Cấu hình
    private static final int MAX_ATTEMPTS = 5;
    private static final int ATTEMPT_WINDOW_MINUTES = 15;
    private static final int BLOCK_DURATION_MINUTES = 15;
    
    /**
     * Ghi nhận đăng nhập thất bại
     */
    public void loginFailed(String usernameOrEmail) {
        String key = buildAttemptsKey(usernameOrEmail);
        
        Long attempts = redisTemplate.opsForValue().increment(key);
        
        if (attempts == null) {
            attempts = 1L;
            redisTemplate.opsForValue().set(key, attempts);
        }
        
        // Set TTL cho lần đầu tiên
        if (attempts == 1) {
            redisTemplate.expire(key, ATTEMPT_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        
        // Nếu vượt quá số lần cho phép, khóa tài khoản
        if (attempts >= MAX_ATTEMPTS) {
            blockAccount(usernameOrEmail);
            log.warn("Account blocked due to too many failed login attempts: {}", usernameOrEmail);
        } else {
            log.info("Login failed for: {} - Attempt {}/{}", usernameOrEmail, attempts, MAX_ATTEMPTS);
        }
    }
    
    /**
     * Ghi nhận đăng nhập thành công (xóa counter)
     */
    public void loginSucceeded(String usernameOrEmail) {
        String attemptsKey = buildAttemptsKey(usernameOrEmail);
        String blockKey = buildBlockKey(usernameOrEmail);
        
        // Xóa counter và block flag
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(blockKey);
        
        log.debug("Login attempts cleared for: {}", usernameOrEmail);
    }
    
    /**
     * Kiểm tra tài khoản có bị khóa không
     */
    public boolean isBlocked(String usernameOrEmail) {
        String key = buildBlockKey(usernameOrEmail);
        Boolean isBlocked = redisTemplate.hasKey(key);
        
        if (Boolean.TRUE.equals(isBlocked)) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
            log.debug("Account {} is blocked for {} more minutes", usernameOrEmail, ttl);
            return true;
        }
        
        return false;
    }
    
    /**
     * Lấy số lần đăng nhập sai còn lại
     */
    public int getRemainingAttempts(String usernameOrEmail) {
        String key = buildAttemptsKey(usernameOrEmail);
        Object attempts = redisTemplate.opsForValue().get(key);
        
        if (attempts == null) {
            return MAX_ATTEMPTS;
        }
        
        int currentAttempts = ((Number) attempts).intValue();
        return Math.max(0, MAX_ATTEMPTS - currentAttempts);
    }
    
    /**
     * Lấy thời gian còn lại của block (phút)
     */
    public Long getBlockTimeRemaining(String usernameOrEmail) {
        String key = buildBlockKey(usernameOrEmail);
        return redisTemplate.getExpire(key, TimeUnit.MINUTES);
    }
    
    /**
     * Khóa tài khoản
     */
    private void blockAccount(String usernameOrEmail) {
        String key = buildBlockKey(usernameOrEmail);
        redisTemplate.opsForValue().set(key, "blocked", BLOCK_DURATION_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * Mở khóa tài khoản thủ công (dùng cho admin)
     */
    public void unblockAccount(String usernameOrEmail) {
        String attemptsKey = buildAttemptsKey(usernameOrEmail);
        String blockKey = buildBlockKey(usernameOrEmail);
        
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(blockKey);
        
        log.info("Account manually unblocked: {}", usernameOrEmail);
    }
    
    /**
     * Tạo Redis key cho attempts counter
     */
    private String buildAttemptsKey(String usernameOrEmail) {
        return "login_attempts:" + usernameOrEmail.toLowerCase();
    }
    
    /**
     * Tạo Redis key cho block flag
     */
    private String buildBlockKey(String usernameOrEmail) {
        return "account_blocked:" + usernameOrEmail.toLowerCase();
    }
    
    /**
     * Lấy thống kê (dùng cho monitoring)
     */
    public LoginAttemptStats getStats(String usernameOrEmail) {
        int remainingAttempts = getRemainingAttempts(usernameOrEmail);
        boolean isBlocked = isBlocked(usernameOrEmail);
        Long blockTimeRemaining = isBlocked ? getBlockTimeRemaining(usernameOrEmail) : null;
        
        return new LoginAttemptStats(
            usernameOrEmail,
            remainingAttempts,
            isBlocked,
            blockTimeRemaining
        );
    }
    
    /**
     * Inner class cho stats
     */
    public static class LoginAttemptStats {
        private final String identifier;
        private final int remainingAttempts;
        private final boolean blocked;
        private final Long blockTimeRemainingMinutes;
        
        public LoginAttemptStats(String identifier, int remainingAttempts, 
                                boolean blocked, Long blockTimeRemainingMinutes) {
            this.identifier = identifier;
            this.remainingAttempts = remainingAttempts;
            this.blocked = blocked;
            this.blockTimeRemainingMinutes = blockTimeRemainingMinutes;
        }
        
        // Getters
        public String getIdentifier() { return identifier; }
        public int getRemainingAttempts() { return remainingAttempts; }
        public boolean isBlocked() { return blocked; }
        public Long getBlockTimeRemainingMinutes() { return blockTimeRemainingMinutes; }
    }
}