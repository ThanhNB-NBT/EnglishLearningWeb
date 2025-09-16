package com.thanhnb.englishlearning.entity;

import com.thanhnb.englishlearning.enums.OtpType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "otp_code", length = 6, nullable = false)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "otp_type", length = 20, nullable = false)
    private OtpType otpType;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used")
    private Boolean isUsed = false;

    @Column(name = "attempts")
    private Integer attempts = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ip_address")
    private String ipAddress;

    // Constructor để tạo OTP mới
    public OtpCode(String email, String otpCode, OtpType otpType, LocalDateTime expiresAt) {
        this.email = email;
        this.otpCode = otpCode;
        this.otpType = otpType;
        this.expiresAt = expiresAt;
        this.isUsed = false;
        this.attempts = 0;
        this.createdAt = LocalDateTime.now();
    }
}
