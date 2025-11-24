package com.thanhnb.englishlearning.repository;

import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

        Optional<User> findByUsername(String username);

        List<User> findByEnglishLevel(EnglishLevel englishLevel);

        Optional<User> findByEmail(String email);

        List<User> findByIsActiveTrue();

        boolean existsByUsername(String username);

        boolean existsByEmail(String email);

        @Query("SELECT u FROM User u WHERE u.totalPoints >= :minPoints ORDER BY u.totalPoints DESC")
        List<User> findTopUsersByPoints(@Param("minPoints") Integer minPoints);

        @Query("SELECT u FROM User u WHERE u.streakDays >= :minStreak ORDER BY u.streakDays DESC")
        List<User> findTopUsersByStreak(@Param("minStreak") Integer minStreak);

        List<User> findByStreakDaysGreaterThan(Integer streakDays);

        // ==================== CLEANUP METHODS (OPTIMIZED) ====================

        /**
         * ✅ OPTIMIZED: Xóa tài khoản chưa verify và được tạo trước thời điểm cutoffDate
         * FIX: Thêm null check cho createdDate để tránh xóa nhầm
         * 
         * @param cutoffDate Thời điểm cắt (ví dụ: LocalDateTime.now().minusHours(24))
         * @return Số lượng tài khoản đã xóa
         */
        @Modifying
        @Query("DELETE FROM User u WHERE u.isVerified = false " +
                        "AND u.createdAt IS NOT NULL " +
                        "AND u.createdAt < :cutoffDate")
        int deleteUnverifiedAccountsCreatedBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

        /**
         * ✅ OPTIMIZED: Đếm tổng số tài khoản chưa verify (bất kể thời gian)
         * Sử dụng cho admin dashboard
         * 
         * @return Số lượng tài khoản chưa verify
         */
        @Query("SELECT COUNT(u) FROM User u WHERE u.isVerified = false")
        long countUnverifiedUsers();

        /**
         * ✅ OPTIMIZED: Lấy danh sách tài khoản chưa verify (cho admin xem chi tiết)
         * Sắp xếp theo thời gian tạo mới nhất, null createdDate xếp cuối
         * 
         * @return Danh sách User chưa verify
         */
        @Query("SELECT u FROM User u WHERE u.isVerified = false " +
                        "ORDER BY CASE WHEN u.createdAt IS NULL THEN 1 ELSE 0 END, " +
                        "u.createdAt DESC")
        List<User> findUnverifiedUsers();

        /**
         * ✅ OPTIMIZED: Đếm số tài khoản chưa verify và cũ hơn X giờ
         * FIX: Thêm null check cho createdDate
         * Sử dụng để preview trước khi cleanup
         * 
         * @param cutoffDate Thời điểm cắt
         * @return Số lượng tài khoản sẽ bị xóa
         */
        @Query("SELECT COUNT(u) FROM User u WHERE u.isVerified = false " +
                        "AND u.createdAt IS NOT NULL " +
                        "AND u.createdAt < :cutoffDate")
        long countUnverifiedAccountsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

        /**
         * ✅ NEW: Lấy danh sách tài khoản chưa verify có createdDate null
         * Debug purpose - để admin biết có bao nhiêu tài khoản bất thường
         * 
         * @return Danh sách User có createdDate null
         */
        @Query("SELECT u FROM User u WHERE u.isVerified = false AND u.createdAt IS NULL")
        List<User> findUnverifiedUsersWithNullCreatedDate();

        /**
         * ✅ NEW: Đếm số tài khoản chưa verify có createdDate null
         * 
         * @return Số lượng tài khoản bất thường
         */
        @Query("SELECT COUNT(u) FROM User u WHERE u.isVerified = false AND u.createdAt IS NULL")
        long countUnverifiedUsersWithNullCreatedAt();

        /**
         * ✅ NEW: Fix tài khoản có createdDate null bằng cách set = current time
         * Chỉ dùng khi cần fix data lỗi
         * 
         * @return Số lượng tài khoản đã fix
         */
        @Modifying
        @Query("UPDATE User u SET u.createdAt = :currentTime " +
                        "WHERE u.createdAt IS NULL AND u.isVerified = false")
        int fixNullCreatedAt(@Param("currentTime") LocalDateTime currentTime);
}