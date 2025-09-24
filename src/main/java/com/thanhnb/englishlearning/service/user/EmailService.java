package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.enums.OtpType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service gửi email OTP sử dụng Spring Mail
 * Cấu hình SMTP từ application.yml
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Gửi OTP qua email
     * @param toEmail Email nhận
     * @param otp Mã OTP
     * @param otpType Loại OTP (VERIFY_EMAIL, RESET_PASSWORD)
     */
    public void sendOtp(String toEmail, String otp, OtpType otpType) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(getSubjectByOtpType(otpType));
            message.setText(getEmailBodyByOtpType(otp, otpType));

            mailSender.send(message);
            
            log.info("OTP email sent successfully to: {} for type: {}", toEmail, otpType);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {} - Error: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email OTP. Vui lòng thử lại sau.");
        }
    }

    /**
     * Tạo subject email theo loại OTP
     */
    private String getSubjectByOtpType(OtpType otpType) {
        return switch (otpType) {
            case VERIFY_EMAIL -> "Xác thực tài khoản - English Learning App";
            case RESET_PASSWORD -> "Đặt lại mật khẩu - English Learning App";
            default -> throw new IllegalArgumentException("Unknown OTP type: " + otpType);
        };
    }

    /**
     * Tạo nội dung email theo loại OTP
     */
    private String getEmailBodyByOtpType(String otp, OtpType otpType) {
        String baseMessage = switch (otpType) {
            case VERIFY_EMAIL -> "Chào bạn,\n\n" +
                    "Cảm ơn bạn đã đăng ký tài khoản English Learning App!\n\n" +
                    "Để hoàn tất việc đăng ký, vui lòng sử dụng mã OTP sau để xác thực email:\n\n" +
                    "Mã OTP: %s\n\n" +
                    "Mã này có hiệu lực trong 10 phút.\n\n" +
                    "Nếu bạn không thực hiện đăng ký này, vui lòng bỏ qua email này.\n\n" +
                    "Trân trọng,\nEnglish Learning Team";

            case RESET_PASSWORD -> "Chào bạn,\n\n" +
                    "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.\n\n" +
                    "Vui lòng sử dụng mã OTP sau để xác thực việc đặt lại mật khẩu:\n\n" +
                    "Mã OTP: %s\n\n" +
                    "Mã này có hiệu lực trong 10 phút.\n\n" +
                    "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này và kiểm tra bảo mật tài khoản.\n\n" +
                    "Trân trọng,\nEnglish Learning Team";
            default -> throw new IllegalArgumentException("Unknown OTP type: " + otpType);
        };

        return String.format(baseMessage, otp);
    }

    /**
     * Gửi email thông báo thành công (có thể dùng sau khi verify OTP)
     */
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Chào mừng đến với English Learning App!");
            
            String body = String.format(
                "Chào %s,\n\n" +
                "Tài khoản của bạn đã được kích hoạt thành công!\n\n" +
                "Bạn có thể bắt đầu hành trình học tiếng Anh của mình ngay bây giờ.\n\n" +
                "Chúc bạn học tập hiệu quả!\n\n" +
                "Trân trọng,\nEnglish Learning Team",
                username
            );
            
            message.setText(body);
            mailSender.send(message);
            
            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {} - Error: {}", toEmail, e.getMessage());
            // Không throw exception vì đây không phải critical error
        }
    }
}