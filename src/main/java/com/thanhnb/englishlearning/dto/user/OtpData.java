package com.thanhnb.englishlearning.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO để lưu trữ thông tin OTP trong Redis
 * Thay thế cho OtpCode entity trong database
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpData {
    
    /**
     * Mã OTP (6 chữ số)
     */
    @Schema(description = "Mã OTP (6 chữ số)", example = "123456")
    private String otp;
    
    /**
     * Số lần đã thử nhập sai (tối đa 3 lần)
     */
    @Schema(description = "Số lần đã thử nhập sai (tối đa 3 lần)", example = "1")
    private int attempts;
    
    /**
     * Thời điểm tạo OTP
     */
    @Schema(description = "Thời điểm tạo OTP")
    private Long createdAt;
    
    /**
     * IP address của client tạo OTP (để tracking)
     */
    @Schema(description = "IP address của client tạo OTP (để tracking)")
    private String ipAddress;
    
    /**
     * Constructor để tạo OTP mới
     */
    public OtpData(String otp, String ipAddress) {
        this.otp = otp;
        this.attempts = 0;
        this.createdAt = System.currentTimeMillis();
        this.ipAddress = ipAddress;
    }
    
    /**
     * Tăng số lần thử sai
     */
    public void incrementAttempts() {
        this.attempts++;
    }
    
    /**
     * Kiểm tra đã hết số lần thử chưa
     */
    public boolean hasMaxAttempts() {
        return this.attempts >= 3;
    }
}