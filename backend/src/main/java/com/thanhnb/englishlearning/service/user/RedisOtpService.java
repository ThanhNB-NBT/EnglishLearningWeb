package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.dto.user.OtpData;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.OtpType;
import com.thanhnb.englishlearning.exception.*;
import com.thanhnb.englishlearning.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service xử lý OTP sử dụng Redis thay vì database
 * Lợi ích: Tự động expire, performance cao, rate limiting dễ dàng
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisOtpService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    
    // Cấu hình
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final int RATE_LIMIT_MAX = 5;
    private static final int RATE_LIMIT_WINDOW_HOURS = 1;
    
    /**
     * Tạo và gửi OTP
     * @param email Email nhận OTP
     * @param otpType Loại OTP (VERIFY_EMAIL, RESET_PASSWORD)
     * @param ipAddress IP của client (để tracking)
     */
    public void generateAndSendOtp(String email, OtpType otpType, String ipAddress) {
        // Kiểm tra rate limit (tối đa 5 OTP/giờ)
        if (isRateLimited(email, otpType)) {
            throw new RateLimitExceededException(
                "Bạn đã vượt quá giới hạn " + RATE_LIMIT_MAX + " OTP trong " + RATE_LIMIT_WINDOW_HOURS + " giờ. Vui lòng thử lại sau."
            );
        }
        
        // Tạo OTP 6 số ngẫu nhiên
        String otp = generateSecureOtp();
        String otpKey = buildOtpKey(email, otpType);
        
        // Lưu vào Redis với TTL
        OtpData otpData = new OtpData(otp, ipAddress);
        
        try {
            redisTemplate.opsForValue().set(otpKey, otpData, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to store OTP in Redis for email: {}", email, e);
            throw new RuntimeException("Không thể tạo mã OTP. Vui lòng thử lại sau.");
        }
        
        // Cập nhật counter rate limit
        updateRateLimit(email, otpType);
        
        // Gửi email
        try {
            emailService.sendOtp(email, otp, otpType);
        } catch (Exception e) {
            // Nếu gửi email thất bại, xóa OTP đã tạo
            redisTemplate.delete(otpKey);
            log.error("Failed to send OTP email to: {}", email, e);
            throw new EmailSendFailedException("Không thể gửi email OTP. Vui lòng kiểm tra lại địa chỉ email.");
        }
        
        log.info("OTP sent successfully to email: {} for type: {} from IP: {}", email, otpType, ipAddress);
    }
    
    /**
     * Xác thực OTP
     * @param email Email cần verify
     * @param inputOtp OTP người dùng nhập
     * @param otpType Loại OTP
     * @return true nếu OTP hợp lệ
     */
    public boolean verifyOtp(String email, String inputOtp, OtpType otpType) {
        String otpKey = buildOtpKey(email, otpType);
        
        Object storedObj = null;
        try {
            storedObj = redisTemplate.opsForValue().get(otpKey);
        } catch (Exception e) {
            log.error("Failed to retrieve OTP from Redis for email: {}", email, e);
            throw new RuntimeException("Lỗi hệ thống khi xác thực OTP. Vui lòng thử lại.");
        }
        
        // Kiểm tra OTP có tồn tại không (có thể đã expire)
        if (storedObj == null) {
            log.warn("OTP verification failed - OTP not found or expired for email: {}", email);
            throw new OtpExpiredException("Mã OTP không tồn tại hoặc đã hết hạn. Vui lòng yêu cầu mã mới.");
        }
        
        // Type safety check
        if (!(storedObj instanceof OtpData)) {
            log.error("Invalid OTP data type in Redis for email: {}", email);
            redisTemplate.delete(otpKey);
            throw new RuntimeException("Dữ liệu OTP không hợp lệ. Vui lòng yêu cầu mã mới.");
        }
        
        OtpData storedOtp = (OtpData) storedObj;
        
        // Kiểm tra số lần thử
        if (storedOtp.hasMaxAttempts()) {
            redisTemplate.delete(otpKey);
            log.warn("OTP verification failed - Max attempts reached for email: {}", email);
            throw new OtpMaxAttemptsException("Đã vượt quá " + MAX_OTP_ATTEMPTS + " lần thử sai. Vui lòng tạo mã OTP mới.");
        }
        
        // Kiểm tra OTP có đúng không
        if (!storedOtp.getOtp().equals(inputOtp)) {
            storedOtp.incrementAttempts();
            int remainingAttempts = MAX_OTP_ATTEMPTS - storedOtp.getAttempts();
            
            // Cập nhật lại số lần thử trong Redis
            try {
                redisTemplate.opsForValue().set(otpKey, storedOtp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.error("Failed to update OTP attempts in Redis", e);
            }
            
            log.warn("OTP verification failed - Invalid OTP for email: {} (attempt: {})", 
                    email, storedOtp.getAttempts());
            
            throw new OtpInvalidException("Mã OTP không chính xác. Còn lại " + remainingAttempts + " lần thử.");
        }
        
        // OTP đúng - xóa khỏi Redis
        try {
            redisTemplate.delete(otpKey);
        } catch (Exception e) {
            log.error("Failed to delete OTP from Redis after verification", e);
        }
        
        // Xử lý business logic tùy theo loại OTP
        if (otpType == OtpType.VERIFY_EMAIL) {
            activateUserAccount(email);
        }
        
        log.info("OTP verification successful for email: {} type: {}", email, otpType);
        return true;
    }
    
    /**
     * Kiểm tra rate limit (tối đa X OTP/giờ cho mỗi email)
     */
    private boolean isRateLimited(String email, OtpType otpType) {
        String rateLimitKey = buildRateLimitKey(email, otpType);
        
        try {
            Object countObj = redisTemplate.opsForValue().get(rateLimitKey);
            
            if (countObj != null) {
                int count = ((Number) countObj).intValue();
                return count >= RATE_LIMIT_MAX;
            }
        } catch (Exception e) {
            log.error("Error checking rate limit for email: {}", email, e);
            // Nếu lỗi, cho phép gửi (fail open)
            return false;
        }
        
        return false;
    }
    
    /**
     * Cập nhật counter rate limit
     */
    private void updateRateLimit(String email, OtpType otpType) {
        String rateLimitKey = buildRateLimitKey(email, otpType);
        
        try {
            Long currentCount = redisTemplate.opsForValue().increment(rateLimitKey);
            
            if (currentCount != null && currentCount == 1) {
                // Lần đầu tiên - set expire
                redisTemplate.expire(rateLimitKey, RATE_LIMIT_WINDOW_HOURS, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.error("Error updating rate limit for email: {}", email, e);
        }
    }
    
    /**
     * Tạo OTP 6 số bảo mật
     */
    private String generateSecureOtp() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }
    
    /**
     * Tạo Redis key cho OTP
     * Pattern: otp:VERIFY_EMAIL:user@example.com
     */
    private String buildOtpKey(String email, OtpType otpType) {
        return "otp:" + otpType.name() + ":" + email.toLowerCase();
    }
    
    /**
     * Tạo Redis key cho rate limiting
     * Pattern: rate_limit:VERIFY_EMAIL:user@example.com
     */
    private String buildRateLimitKey(String email, OtpType otpType) {
        return "rate_limit:" + otpType.name() + ":" + email.toLowerCase();
    }
    
    /**
     * Kích hoạt tài khoản user sau khi verify email thành công
     */
    private void activateUserAccount(String email) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setIsActive(true);
                user.setIsVerified(true);
                userRepository.save(user);
                log.info("User account activated for email: {}", email);
            } else {
                log.error("User not found for email: {} during account activation", email);
                throw new UserNotFoundException("Không tìm thấy tài khoản để kích hoạt");
            }
        } catch (Exception e) {
            log.error("Error activating user account for email: {}", email, e);
            throw new RuntimeException("Không thể kích hoạt tài khoản. Vui lòng liên hệ hỗ trợ.");
        }
    }
    
    /**
     * Lấy thông tin OTP còn lại (dùng cho debug/admin)
     */
    public OtpInfo getOtpInfo(String email, OtpType otpType) {
        String otpKey = buildOtpKey(email, otpType);
        String rateLimitKey = buildRateLimitKey(email, otpType);
        
        boolean exists = Boolean.TRUE.equals(redisTemplate.hasKey(otpKey));
        Long ttl = exists ? redisTemplate.getExpire(otpKey, TimeUnit.SECONDS) : null;
        
        Object countObj = redisTemplate.opsForValue().get(rateLimitKey);
        int rateLimitCount = countObj != null ? ((Number) countObj).intValue() : 0;
        
        return new OtpInfo(exists, ttl, rateLimitCount, RATE_LIMIT_MAX - rateLimitCount);
    }
    
    /**
     * Inner class cho OTP info
     */
    public static class OtpInfo {
        private final boolean exists;
        private final Long ttlSeconds;
        private final int rateLimitUsed;
        private final int rateLimitRemaining;
        
        public OtpInfo(boolean exists, Long ttlSeconds, int rateLimitUsed, int rateLimitRemaining) {
            this.exists = exists;
            this.ttlSeconds = ttlSeconds;
            this.rateLimitUsed = rateLimitUsed;
            this.rateLimitRemaining = rateLimitRemaining;
        }
        
        // Getters
        public boolean isExists() { return exists; }
        public Long getTtlSeconds() { return ttlSeconds; }
        public int getRateLimitUsed() { return rateLimitUsed; }
        public int getRateLimitRemaining() { return rateLimitRemaining; }
    }
}