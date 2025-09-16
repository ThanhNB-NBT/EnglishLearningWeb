package com.thanhnb.englishlearning.repository;

import com.thanhnb.englishlearning.entity.OtpCode;
import com.thanhnb.englishlearning.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    
    // Tìm OTP hợp lệ (chưa sử dụng và chưa hết hạn)
    @Query("SELECT o FROM OtpCode o WHERE o.email = :email " +
        "AND o.otpCode = :otpCode " +
        "AND o.otpType = :otpType " + 
        "AND o.isUsed = false " +
        "AND o.expiresAt > :now")
    Optional<OtpCode> findValidOtp(
        @Param("email") String email,
        @Param("otpCode") String otpCode, 
        @Param("otpType") OtpType otpType,
        @Param("now") LocalDateTime now
    );
    
    // Tìm OTP mới nhất của email với type cụ thể
    @Query("SELECT o FROM OtpCode o WHERE o.email = :email AND o.otpType = :type AND o.isUsed = false ORDER BY o.createdAt DESC")
    Optional<OtpCode> findLatestByEmailAndType(@Param("email") String email, @Param("type") OtpType type);
    
    // Xóa OTP đã hết hạn hoặc đã sử dụng
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :now OR o.isUsed = true")
    void deleteExpiredOrUsedOtp(@Param("now") LocalDateTime now);
    
    // Đánh dấu tất cả OTP cũ của email + type là đã sử dụng
    @Modifying
    @Transactional
    @Query("UPDATE OtpCode o SET o.isUsed = true WHERE o.email = :email AND o.otpType = :type AND o.isUsed = false")
    void markAllAsUsedByEmailAndType(@Param("email") String email, @Param("type") OtpType type);
    
    // Đếm số lần gửi OTP trong khoảng thời gian
    @Query("SELECT COUNT(o) FROM OtpCode o WHERE o.email = :email AND o.otpType = :type AND o.createdAt > :since")
    long countByEmailAndTypeAndCreatedAtAfter(@Param("email") String email, @Param("type") OtpType type, @Param("since") LocalDateTime since);
    
    // Tăng số lần thử
    @Modifying
    @Transactional
    @Query("UPDATE OtpCode o SET o.attempts = o.attempts + 1 WHERE o.id = :id")
    void incrementAttempts(@Param("id") Long id);

    // Đếm số OTP từ cùng IP trong 1 giờ (chống spam)
    @Query("SELECT COUNT(o) FROM OtpCode o WHERE o.ipAddress = :ip AND o.createdAt > :since")
    long countByIpAndCreatedAtAfter(@Param("ip") String ip, @Param("since") LocalDateTime since);
    
    // Đếm số email khác nhau từ cùng IP (phát hiện spam)
    @Query("SELECT COUNT(DISTINCT o.email) FROM OtpCode o WHERE o.ipAddress = :ip AND o.createdAt > :since")
    long countDistinctEmailsByIp(@Param("ip") String ip, @Param("since") LocalDateTime since);
}
