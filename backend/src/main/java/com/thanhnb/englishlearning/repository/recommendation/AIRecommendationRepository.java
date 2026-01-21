package com.thanhnb.englishlearning.repository.recommendation;

import com.thanhnb.englishlearning.entity.recommendation.AIRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ Enhanced AI Recommendation Repository
 */
@Repository
public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {

        /**
         * Find active (non-expired, non-completed) recommendations for user
         */
        List<AIRecommendation> findByUserIdAndExpiresAtAfterAndIsCompletedFalseOrderByPriorityDesc(
                        Long userId, LocalDateTime now);

        /**
         * Find all recommendations for user (for metrics)
         */
        List<AIRecommendation> findByUserIdOrderByCreatedAtDesc(Long userId);

        /**
         * Find expired recommendations that are not completed
         */
        List<AIRecommendation> findByExpiresAtBeforeAndIsCompletedFalse(LocalDateTime now);

        /**
         * Find recommendations by type and user
         */
        List<AIRecommendation> findByUserIdAndType(Long userId, String type);

        /**
         * Find shown but not accepted recommendations (user ignored them)
         */
        @Query("SELECT r FROM AIRecommendation r WHERE r.user.id = :userId " +
                        "AND r.isShown = true AND r.isAccepted = false " +
                        "AND r.isCompleted = false")
        List<AIRecommendation> findIgnoredRecommendations(@Param("userId") Long userId);

        /**
         * Find recommendations for specific lesson
         */
        List<AIRecommendation> findByUserIdAndTargetLessonId(Long userId, Long lessonId);

        /**
         * Find recommendations for specific topic
         */
        List<AIRecommendation> findByUserIdAndTargetTopicId(Long userId, Long topicId);

        /**
         * Count active recommendations for user
         */
        @Query("SELECT COUNT(r) FROM AIRecommendation r WHERE r.user.id = :userId " +
                        "AND r.expiresAt > :now AND r.isCompleted = false")
        long countActiveRecommendations(@Param("userId") Long userId, @Param("now") LocalDateTime now);

        /**
         * Get acceptance rate for user
         */
        @Query("SELECT CAST(SUM(CASE WHEN r.isAccepted = true THEN 1 ELSE 0 END) AS double) / COUNT(r) " +
                        "FROM AIRecommendation r WHERE r.user.id = :userId AND r.isShown = true")
        Double getAcceptanceRate(@Param("userId") Long userId);

        /**
         * Get completion rate for user (among accepted recommendations)
         */
        @Query("SELECT CAST(SUM(CASE WHEN r.isCompleted = true THEN 1 ELSE 0 END) AS double) / COUNT(r) " +
                        "FROM AIRecommendation r WHERE r.user.id = :userId AND r.isAccepted = true")
        Double getCompletionRate(@Param("userId") Long userId);

        /**
         * Delete old recommendations (cleanup)
         */
        void deleteByUserIdAndCreatedAtBefore(Long userId, LocalDateTime before);

        /**
         * Find most effective recommendation types for user
         */
        @Query("SELECT r.type, COUNT(r) as count, " +
                        "SUM(CASE WHEN r.isCompleted = true THEN 1 ELSE 0 END) as completed " +
                        "FROM AIRecommendation r WHERE r.user.id = :userId " +
                        "GROUP BY r.type ORDER BY completed DESC")
        List<Object[]> findMostEffectiveTypes(@Param("userId") Long userId);
}