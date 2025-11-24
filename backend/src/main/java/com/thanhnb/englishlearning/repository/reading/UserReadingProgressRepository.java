package com.thanhnb.englishlearning.repository.reading;

import com.thanhnb.englishlearning.entity.reading.UserReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserReadingProgressRepository extends JpaRepository<UserReadingProgress, Long> {

        // ==================== BASIC QUERIES ====================

        /**
         * Tìm progress của user cho một lesson cụ thể
         */
        Optional<UserReadingProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

        /**
         * Tìm tất cả progress của user
         */
        List<UserReadingProgress> findByUserId(Long userId);

        /**
         * Tìm các bài đã hoàn thành của user, sắp xếp theo thời gian
         */
        List<UserReadingProgress> findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(Long userId);

        // ==================== EXISTENCE CHECKS ====================

        /**
         * Kiểm tra user đã hoàn thành lesson chưa
         */
        boolean existsByUserIdAndLessonIdAndIsCompletedTrue(Long userId, Long lessonId);

        /**
         * Kiểm tra user đã có progress cho lesson chưa
         */
        boolean existsByUserIdAndLessonId(Long userId, Long lessonId);

        // ==================== STATISTICS QUERIES (OPTIMIZED) ====================

        /**
         * Đếm số user đã hoàn thành một lesson cụ thể
         */
        @Query("SELECT COUNT(DISTINCT p.user.id) FROM UserReadingProgress p " +
                        "WHERE p.lesson.id = :lessonId AND p.isCompleted = true")
        long countCompletedUsersByLessonId(@Param("lessonId") Long lessonId);

        /**
         * Tính tổng số lần thử của tất cả user cho một lesson
         */
        @Query("SELECT COALESCE(SUM(p.attempts), 0) FROM UserReadingProgress p " +
                        "WHERE p.lesson.id = :lessonId")
        long getTotalAttemptsByLessonId(@Param("lessonId") Long lessonId);

        /**
         * Tính điểm trung bình của một lesson
         */
        @Query("SELECT COALESCE(AVG(p.scorePercentage), 0.0) FROM UserReadingProgress p " +
                        "WHERE p.lesson.id = :lessonId")
        double getAverageScoreByLessonId(@Param("lessonId") Long lessonId);

        /**
         * Đếm tổng số user khác nhau đã làm bài Reading
         */
        @Query("SELECT COUNT(DISTINCT p.user.id) FROM UserReadingProgress p")
        long countDistinctUsers();

        /**
         * Đếm tổng số bài đã hoàn thành (toàn module)
         */
        @Query("SELECT COUNT(p) FROM UserReadingProgress p WHERE p.isCompleted = true")
        long countTotalCompletions();

        /**
         * Lấy điểm trung bình của toàn module Reading
         */
        @Query("SELECT COALESCE(AVG(p.scorePercentage), 0.0) FROM UserReadingProgress p")
        double getModuleAverageScore();

        // ==================== DELETE OPERATIONS ====================

        /**
         * Xóa tất cả progress của một lesson (cascade delete)
         */
        void deleteByLessonId(Long lessonId);

        /**
         * Xóa tất cả progress của một user
         */
        void deleteByUserId(Long userId);
}