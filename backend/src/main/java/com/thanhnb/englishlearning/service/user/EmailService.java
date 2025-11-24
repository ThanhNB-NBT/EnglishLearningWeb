package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.enums.OtpType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Service gửi email với HTML template
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:English Learning App}")
    private String appName;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Gửi OTP qua email với HTML template
     */
    public void sendOtp(String toEmail, String otp, OtpType otpType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(getSubjectByOtpType(otpType));
            helper.setText(getHtmlEmailBody(otp, otpType), true); // true = HTML

            mailSender.send(message);
            
            log.info("HTML OTP email sent successfully to: {} for type: {}", toEmail, otpType);
        } catch (MessagingException e) {
            log.error("Failed to send HTML OTP email to: {} - Error: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email OTP. Vui lòng thử lại sau.");
        }
    }

    /**
     * Gửi welcome email
     */
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Chào mừng đến với " + appName + "!");
            helper.setText(getWelcomeEmailHtml(username), true);

            mailSender.send(message);
            
            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {} - Error: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Base HTML template
     */
    private String getBaseHtmlTemplate(String title, String content) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background-color: #f4f4f4;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 40px auto;
                        background-color: #ffffff;
                        border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 30px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .otp-box {
                        background-color: #f8f9fa;
                        border: 2px dashed #667eea;
                        border-radius: 8px;
                        padding: 20px;
                        text-align: center;
                        margin: 30px 0;
                    }
                    .otp-code {
                        font-size: 36px;
                        font-weight: bold;
                        color: #667eea;
                        letter-spacing: 8px;
                        font-family: 'Courier New', monospace;
                    }
                    .button {
                        display: inline-block;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 14px 30px;
                        text-decoration: none;
                        border-radius: 6px;
                        margin: 20px 0;
                        font-weight: bold;
                    }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 20px 30px;
                        text-align: center;
                        font-size: 12px;
                        color: #6c757d;
                    }
                    .warning {
                        background-color: #fff3cd;
                        border-left: 4px solid #ffc107;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 4px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    <div class="content">
                        %s
                    </div>
                    <div class="footer">
                        <p>Email này được gửi từ <strong>%s</strong></p>
                        <p>Nếu bạn không thực hiện hành động này, vui lòng bỏ qua email này.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(title, content, appName);
    }

    /**
     * HTML cho OTP email (ĐÃ XÓA EMOJI)
     */
    private String getHtmlEmailBody(String otp, OtpType otpType) {
        String title;
        String message;
        
        if (otpType == OtpType.VERIFY_EMAIL) {
            title = "Xác thực Email";
            message = """
                <h2 style="color: #333;">Xin chào!</h2>
                <p>Cảm ơn bạn đã đăng ký tài khoản <strong>%s</strong>!</p>
                <p>Để hoàn tất việc đăng ký, vui lòng sử dụng mã OTP sau để xác thực email:</p>
                
                <div class="otp-box">
                    <p style="margin: 0; color: #666; font-size: 14px;">MÃ OTP CỦA BẠN</p>
                    <div class="otp-code">%s</div>
                    <p style="margin: 10px 0 0 0; color: #999; font-size: 12px;">Mã có hiệu lực trong 10 phút</p>
                </div>
                
                <div class="warning">
                    <strong>⚠️ Lưu ý:</strong> Không chia sẻ mã OTP này với bất kỳ ai!
                </div>
                """.formatted(appName, otp);
        } else {
            title = "Đặt lại Mật khẩu";
            message = """
                <h2 style="color: #333;">Yêu cầu đặt lại mật khẩu</h2>
                <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                <p>Vui lòng sử dụng mã OTP sau để xác thực việc đặt lại mật khẩu:</p>
                
                <div class="otp-box">
                    <p style="margin: 0; color: #666; font-size: 14px;">MÃ OTP CỦA BẠN</p>
                    <div class="otp-code">%s</div>
                    <p style="margin: 10px 0 0 0; color: #999; font-size: 12px;">Mã có hiệu lực trong 10 phút</p>
                </div>
                
                <div class="warning">
                    <strong>⚠️ Cảnh báo:</strong> Nếu bạn không yêu cầu đặt lại mật khẩu, 
                    vui lòng bỏ qua email này và kiểm tra bảo mật tài khoản.
                </div>
                """.formatted(otp);
        }
        
        return getBaseHtmlTemplate(title, message);
    }

    /**
     * HTML cho welcome email (ĐÃ XÓA EMOJI)
     */
    private String getWelcomeEmailHtml(String username) {
        String content = """
            <h2 style="color: #333;">Xin chào %s!</h2>
            <p>Tài khoản của bạn đã được kích hoạt thành công!</p>
            <p>Bạn có thể bắt đầu hành trình học tiếng Anh của mình ngay bây giờ.</p>
            
            <div style="text-align: center; margin: 30px 0;">
                <a href="%s/login" class="button">Đăng nhập ngay</a>
            </div>
            
            <div style="background-color: #e7f3ff; padding: 20px; border-radius: 8px; margin: 20px 0;">
                <h3 style="margin-top: 0; color: #0066cc;">Bắt đầu với chúng tôi:</h3>
                <ul style="line-height: 1.8;">
                    <li>Khám phá hàng nghìn bài học</li>
                    <li>Luyện tập với các bài kiểm tra</li>
                    <li>Theo dõi tiến độ học tập</li>
                    <li>Xây dựng thói quen học mỗi ngày</li>
                </ul>
            </div>
            
            <p style="margin-top: 30px; color: #666;">Chúc bạn học tập hiệu quả!</p>
            """.formatted(username, frontendUrl);
        
        return getBaseHtmlTemplate("Chào mừng đến với " + appName, content);
    }

    /**
     * Tạo subject email theo loại OTP
     */
    private String getSubjectByOtpType(OtpType otpType) {
        return switch (otpType) {
            case VERIFY_EMAIL -> "Xác thực tài khoản - " + appName;
            case RESET_PASSWORD -> "Đặt lại mật khẩu - " + appName;
            default -> throw new IllegalArgumentException("Unknown OTP type: " + otpType);
        };
    }
}