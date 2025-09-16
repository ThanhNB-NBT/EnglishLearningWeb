package com.thanhnb.englishlearning.service;

import com.thanhnb.englishlearning.entity.OtpCode;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.OtpType;
import com.thanhnb.englishlearning.repository.OtpCodeRepository;
import com.thanhnb.englishlearning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OtpService {

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final JavaMailSender mailSender;
    
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_OTP_PER_HOUR = 5; // Giới hạn 5 OTP/giờ
    private static final int MAX_ATTEMPTS = 3; // Tối đa 3 lần thử sai

    public void generateAndSendOtp(String email, OtpType otpType, String ipAddress) {
        // KIỂM TRA SPAM
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        // 1. Kiểm tra giới hạn theo IP (10 OTP/giờ)
        long ipOtpCount = otpCodeRepository.countByIpAndCreatedAtAfter(ipAddress, oneHourAgo);
        if (ipOtpCount >= 10) {
            throw new RuntimeException("IP này đã gửi quá nhiều OTP. Vui lòng thử lại sau 1 giờ");
        }
        
        // 2. Kiểm tra spam (1 IP gửi cho nhiều email khác nhau)
        long distinctEmails = otpCodeRepository.countDistinctEmailsByIp(ipAddress, oneHourAgo);
        if (distinctEmails >= 3) {
            throw new RuntimeException("IP này đã gửi OTP cho quá nhiều email. Vui lòng thử lại sau");
        }
        // Kiểm tra email tồn tại
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Email không tồn tại trong hệ thống");
        }

        // Kiểm tra giới hạn gửi OTP (5 lần/giờ)
        long otpCount = otpCodeRepository.countByEmailAndTypeAndCreatedAtAfter(email, otpType, oneHourAgo);
        if (otpCount >= MAX_OTP_PER_HOUR) {
            throw new RuntimeException("Bạn đã gửi quá nhiều OTP. Vui lòng thử lại sau 1 giờ");
        }

        User user = userOpt.get();
        
        // Vô hiệu hóa tất cả OTP cũ của email + type này
        otpCodeRepository.markAllAsUsedByEmailAndType(email, otpType);
        
        // Tạo OTP mới
        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        OtpCode otpCode = new OtpCode(email, otp, otpType, expiresAt);
        otpCode.setIpAddress(ipAddress);
        otpCodeRepository.save(otpCode);

        // Gửi email async
        sendOtpEmailAsync(email, otp, user.getFullName(), otpType);
        
        log.info("Generated OTP for email: {} with type: {}", email, otpType, ipAddress);
    }

    public boolean verifyOtp(String email, String otp, OtpType otpType) {
        LocalDateTime now = LocalDateTime.now();
        
        // Tìm OTP hợp lệ
        Optional<OtpCode> otpCodeOpt = otpCodeRepository
            .findValidOtp(email, otp, otpType, now);
        
        if (otpCodeOpt.isEmpty()) {
            // Tìm OTP để tăng attempts
            Optional<OtpCode> latestOtp = otpCodeRepository.findLatestByEmailAndType(email, otpType);
            if (latestOtp.isPresent() && !latestOtp.get().getIsUsed()) {
                otpCodeRepository.incrementAttempts(latestOtp.get().getId());
                
                if (latestOtp.get().getAttempts() + 1 >= MAX_ATTEMPTS) {
                    // Vô hiệu hóa OTP sau khi thử sai quá nhiều
                    otpCodeRepository.markAllAsUsedByEmailAndType(email, otpType);
                    throw new RuntimeException("Bạn đã nhập sai OTP quá nhiều lần. Vui lòng yêu cầu gửi lại OTP mới");
                }
            }
            return false;
        }

        OtpCode otpCode = otpCodeOpt.get();
        
        // Đánh dấu OTP đã sử dụng
        otpCode.setIsUsed(true);
        otpCodeRepository.save(otpCode);

        // Xử lý theo type
        handleOtpVerificationSuccess(email, otpType);
        
        log.info("Successfully verified OTP for email: {} with type: {}", email, otpType);
        return true;
    }

    private void handleOtpVerificationSuccess(String email, OtpType otpType) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return;
        
        User user = userOpt.get();
        
        switch (otpType) {
            case VERIFY_EMAIL:
                user.setIsVerified(true);
                user.setIsActive(true);
                userRepository.save(user);
                break;
            case RESET_PASSWORD:
                // Không cần làm gì, chỉ verify OTP thôi
                // Logic reset password sẽ ở endpoint khác
                break;
            case CHANGE_EMAIL:
                // Logic thay đổi email sẽ ở endpoint khác
                break;
            case CHANGE_PASSWORD:
                // Logic thay đổi password sẽ ở endpoint khác
                break;
        }
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6 digit OTP
        return String.valueOf(otp);
    }

    @Async
    private void sendOtpEmailAsync(String email, String otp, String fullName, OtpType otpType) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("English Learning - Mã xác thực OTP");
            
            String content = buildEmailContent(otp, fullName, otpType);
            message.setText(content);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
        }
    }

    private String buildEmailContent(String otp, String fullName, OtpType otpType) {
        return (
                """
                Xin chào %s,
                
                Mã xác thực OTP cho %s của bạn là: %s
                
                Mã này sẽ hết hạn sau %d phút.
                Vui lòng không chia sẻ mã này với bất kỳ ai.
                
                Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.
                
                Trân trọng,
                English Learning Team""").formatted(
                fullName, otpType.getDescription(), otp, OTP_EXPIRY_MINUTES
        );
    }

    // Cleanup job - có thể chạy scheduled
    @Transactional
    public void cleanupExpiredOtp() {
        otpCodeRepository.deleteExpiredOrUsedOtp(LocalDateTime.now());
        log.info("Cleaned up expired and used OTPs");
    }
}
