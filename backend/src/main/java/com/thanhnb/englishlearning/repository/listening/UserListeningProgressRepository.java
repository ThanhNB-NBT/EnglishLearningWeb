package com.thanhnb.englishlearning.repository.listening;

import com.thanhnb.englishlearning.entity.listening.UserListeningProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserListeningProgressRepository extends JpaRepository<UserListeningProgress, Long> {
        // ==================== BASIC QUERIES ====================

        Optional<UserListeningProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

        List<UserListeningProgress> findByUserId(Long userId);

        List<UserListeningProgress> findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(Long userId);

        boolean existsByUserIdAndLessonIdAndIsCompletedTrue(Long userId, Long lessonId);

        // ==================== COUNT QUERIES ====================

        long countByLessonId(Long lessonId);

        long countByLessonIdAndIsCompletedTrue(Long lessonId);

        long countByIsCompletedTrue();

        // ==================== STATISTICS QUERIES ====================

        /**
         * Get average score for a lesson
         */
        @Query("SELECT AVG(p.scorePercentage) FROM UserListeningProgress p WHERE p. lesson.id = :lessonId")
        Double findAverageScoreByLessonId(@Param("lessonId") Long lessonId);

        /**
         * Get average score across all lessons
         */
        @Query("SELECT AVG(p. scorePercentage) FROM UserListeningProgress p")
        Double findAverageScore();

        /**
         * Get all scores for a lesson (for distribution calculation)
         */
        @Query("SELECT p.scorePercentage FROM UserListeningProgress p WHERE p.lesson.id = :lessonId")
        List<BigDecimal> findAllScoresByLessonId(@Param("lessonId") Long lessonId);

        // ==================== BULK DELETE ====================

        void deleteByLessonId(Long lessonId);

        List<UserListeningProgress> findAllByUserId(Long userId);

        @Query("SELECT COUNT(p) > 0 FROM UserListeningProgress p " +
                        "WHERE p.user.id = :userId AND p.lesson.id = :lessonId AND p.isCompleted = true")
        boolean existsCompletedProgress(@Param("userId") Long userId, @Param("lessonId") Long lessonId);

        @Query("SELECT COUNT(p) FROM UserListeningProgress p " +
                        "WHERE p.user.id = :userId AND p.isCompleted = true")
        long countCompletedByUserId(@Param("userId") Long userId);

        @Query("SELECT AVG(p.scorePercentage) FROM UserListeningProgress p " +
                        "WHERE p.user.id = :userId AND p.isCompleted = true")
        Double findAverageScoreByUserId(@Param("userId") Long userId);
}