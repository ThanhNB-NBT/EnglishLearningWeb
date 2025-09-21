package com.thanhnb.englishlearning.service;

import com.thanhnb.englishlearning.dto.OtpData;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.OtpType;
import com.thanhnb.englishlearning.repository.UserRepository;
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
    
    /**
     * Tạo và gửi OTP
     * @param email Email nhận OTP
     * @param otpType Loại OTP (VERIFY_EMAIL, RESET_PASSWORD)
     * @param ipAddress IP của client (để tracking)
     */
    public void generateAndSendOtp(String email, OtpType otpType, String ipAddress) {
        // Kiểm tra rate limit (tối đa 5 OTP/giờ)
        if (isRateLimited(email, otpType)) {
            throw new RuntimeException("Bạn đã vượt quá giới hạn 5 OTP trong 1 giờ. Vui lòng thử lại sau.");
        }
        
        // Tạo OTP 6 số ngẫu nhiên
        String otp = generateSecureOtp();
        String otpKey = buildOtpKey(email, otpType);
        
        // Lưu vào Redis với TTL 10 phút
        OtpData otpData = new OtpData(otp, ipAddress);
        redisTemplate.opsForValue().set(otpKey, otpData, 10, TimeUnit.MINUTES);
        
        // Cập nhật counter rate limit
        updateRateLimit(email, otpType);
        
        // Gửi email
        emailService.sendOtp(email, otp, otpType);
        
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
        OtpData storedOtp = (OtpData) redisTemplate.opsForValue().get(otpKey);
        
        // Kiểm tra OTP có tồn tại không (có thể đã expire)
        if (storedOtp == null) {
            log.warn("OTP verification failed - OTP not found or expired for email: {}", email);
            throw new RuntimeException("Mã OTP không tồn tại hoặc đã hết hạn");
        }
        
        // Kiểm tra số lần thử
        if (storedOtp.hasMaxAttempts()) {
            redisTemplate.delete(otpKey);
            log.warn("OTP verification failed - Max attempts reached for email: {}", email);
            throw new RuntimeException("Đã vượt quá 3 lần thử sai. Vui lòng tạo mã OTP mới");
        }
        
        // Kiểm tra OTP có đúng không
        if (!storedOtp.getOtp().equals(inputOtp)) {
            storedOtp.incrementAttempts();
            // Cập nhật lại số lần thử trong Redis
            redisTemplate.opsForValue().set(otpKey, storedOtp, 10, TimeUnit.MINUTES);
            
            log.warn("OTP verification failed - Invalid OTP for email: {} (attempt: {})", 
                    email, storedOtp.getAttempts());
            throw new RuntimeException("Mã OTP không chính xác. Còn lại " + 
                    (3 - storedOtp.getAttempts()) + " lần thử");
        }
        
        // OTP đúng - xóa khỏi Redis
        redisTemplate.delete(otpKey);
        
        // Xử lý business logic tùy theo loại OTP
        if (otpType == OtpType.VERIFY_EMAIL) {
            activateUserAccount(email);
        }
        
        log.info("OTP verification successful for email: {} type: {}", email, otpType);
        return true;
    }
    
    /**
     * Kiểm tra rate limit (tối đa 5 OTP/giờ cho mỗi email)
     */
    private boolean isRateLimited(String email, OtpType otpType) {
        String rateLimitKey = buildRateLimitKey(email, otpType);
        String countStr = (String) redisTemplate.opsForValue().get(rateLimitKey);
        
        if (countStr != null) {
            try {
                int currentCount = Integer.parseInt(countStr);
                return currentCount >= 5;
            } catch (NumberFormatException e) {
                log.warn("Invalid rate limit format: {}, deleting key", countStr);
                redisTemplate.delete(rateLimitKey); // Xóa key lỗi
                return false;
            }
        }
        return false;
    }
    
    /**
     * Cập nhật counter rate limit
     */
    private void updateRateLimit(String email, OtpType otpType) {
        String rateLimitKey = buildRateLimitKey(email, otpType);
        
        // Tăng counter và set expire 1 giờ
        Long currentCount = redisTemplate.opsForValue().increment(rateLimitKey);
        if (currentCount == 1) {
            // Lần đầu tiên - set expire
            redisTemplate.expire(rateLimitKey, 1, TimeUnit.HOURS);
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
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIsActive(true);
            user.setIsVerified(true);
            userRepository.save(user);
            log.info("User account activated for email: {}", email);
        } else {
            log.error("User not found for email: {} during account activation", email);
        }
    }
}