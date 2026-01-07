package com.thanhnb.englishlearning.repository.user;

import com.thanhnb.englishlearning.entity.user.UserStats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ✅ NEW REPOSITORY: UserStatsRepository
 * 
 * Purpose: Quản lý user statistics & gamification data
 * 
 * Key features:
 * - Leaderboards (points, streak)
 * - Streak management
 * - Stats queries
 */
@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {

    // ==================== BASIC QUERIES ====================

    /**
     * Find stats by userId
     * (Same as findById, but explicit naming)
     */
    Optional<UserStats> findByUserId(Long userId);

    /**
     * Check if stats exists for user
     */
    boolean existsByUserId(Long userId);

    // ==================== LEADERBOARD QUERIES ====================

    /**
     * Get top users by points
     * 
     * @param minPoints Minimum points threshold
     * @param pageable  Pagination
     * @return Top users ordered by points DESC
     */
    @Query("SELECT s FROM UserStats s WHERE s.totalPoints >= :minPoints " +
            "ORDER BY s.totalPoints DESC")
    List<UserStats> findTopByPoints(@Param("minPoints") Integer minPoints, Pageable pageable);

    /**
     * Get top users by current streak
     * 
     * @param minStreak Minimum streak threshold
     * @param pageable  Pagination
     * @return Top users ordered by streak DESC
     */
    @Query("SELECT s FROM UserStats s WHERE s.currentStreak >= :minStreak " +
            "ORDER BY s.currentStreak DESC")
    List<UserStats> findTopByStreak(@Param("minStreak") Integer minStreak, Pageable pageable);

    /**
     * Get top users by longest streak ever
     */
    @Query("SELECT s FROM UserStats s WHERE s.longestStreak >= :minStreak " +
            "ORDER BY s.longestStreak DESC")
    List<UserStats> findTopByLongestStreak(@Param("minStreak") Integer minStreak, Pageable pageable);

    /**
     * Get top users by total lessons completed
     */
    @Query("SELECT s FROM UserStats s WHERE s.totalLessonsCompleted >= :minLessons " +
            "ORDER BY s.totalLessonsCompleted DESC")
    List<UserStats> findTopByLessonsCompleted(@Param("minLessons") Integer minLessons, Pageable pageable);

    // ==================== STREAK MANAGEMENT ====================

    /**
     * ⚠️ CRITICAL QUERY: Find all users with active streak (current_streak > 0)
     * 
     * Dùng cho StreakScheduler để check và reset streak
     * 
     * @return List of UserStats with active streak
     */
    @Query("SELECT s FROM UserStats s WHERE s.currentStreak > 0")
    List<UserStats> findAllWithActiveStreak();

    /**
     * Find users whose streak should be reset
     * (last_streak_date NOT IN [yesterday, today])
     * 
     * @param yesterday Yesterday's date
     * @param today     Today's date
     * @return Users whose streak needs reset
     */
    @Query("SELECT s FROM UserStats s WHERE s.currentStreak > 0 " +
            "AND s.lastStreakDate IS NOT NULL " +
            "AND s.lastStreakDate < :yesterday")
    List<UserStats> findStreaksToReset(@Param("yesterday") LocalDate yesterday);

    /**
     * Reset all expired streaks
     * 
     * @param yesterday Yesterday's date
     * @return Number of streaks reset
     */
    @Modifying
    @Query("UPDATE UserStats s SET s.currentStreak = 0, s.lastStreakDate = NULL " +
            "WHERE s.currentStreak > 0 " +
            "AND s.lastStreakDate IS NOT NULL " +
            "AND s.lastStreakDate < :yesterday")
    int resetExpiredStreaks(@Param("yesterday") LocalDate yesterday);

    /**
     * Find users who studied today
     */
    @Query("SELECT s FROM UserStats s WHERE s.lastStreakDate = :today")
    List<UserStats> findUsersWhoStudiedToday(@Param("today") LocalDate today);

    // ==================== STATS QUERIES ====================

    /**
     * Get total points distributed across all users
     */
    @Query("SELECT SUM(s.totalPoints) FROM UserStats s")
    Long getTotalPointsAcrossAllUsers();

    /**
     * Get average points per user
     */
    @Query("SELECT AVG(s.totalPoints) FROM UserStats s")
    Double getAveragePointsPerUser();

    /**
     * Get count of users with active streak
     */
    @Query("SELECT COUNT(s) FROM UserStats s WHERE s.currentStreak > 0")
    Long countUsersWithActiveStreak();

    /**
     * Get average streak
     */
    @Query("SELECT AVG(s.currentStreak) FROM UserStats s WHERE s.currentStreak > 0")
    Double getAverageStreak();

    /**
     * Find users by module completion
     * 
     * @param moduleType   "GRAMMAR", "READING", "LISTENING"
     * @param minCompleted Minimum completed lessons
     * @return Users who completed at least minCompleted lessons in module
     */
    @Query("SELECT s FROM UserStats s " +
            "WHERE (CASE " +
            "  WHEN :moduleType = 'GRAMMAR' THEN s.grammarCompleted " +
            "  WHEN :moduleType = 'READING' THEN s.readingCompleted " +
            "  WHEN :moduleType = 'LISTENING' THEN s.listeningCompleted " +
            "  ELSE 0 END) >= :minCompleted")
    List<UserStats> findByModuleCompletion(@Param("moduleType") String moduleType,
            @Param("minCompleted") Integer minCompleted);

    // ==================== BATCH UPDATE OPERATIONS ====================

    /**
     * Bulk add points to multiple users
     * (Useful for events/rewards)
     * 
     * @param userIds List of user IDs
     * @param points  Points to add
     */
    @Modifying
    @Query("UPDATE UserStats s SET s.totalPoints = s.totalPoints + :points " +
            "WHERE s.userId IN :userIds")
    int bulkAddPoints(@Param("userIds") List<Long> userIds, @Param("points") Integer points);

    /**
     * Recalculate lesson completion for user
     */
    @Modifying
    @Query(value = "UPDATE user_stats s SET " +
            "grammar_completed = (SELECT COUNT(*) FROM user_grammar_progress p " +
            "                     WHERE p.user_id = s.user_id AND p.is_completed = true), " +
            "reading_completed = (SELECT COUNT(*) FROM user_reading_progress p " +
            "                     WHERE p.user_id = s.user_id AND p.is_completed = true), " +
            "listening_completed = (SELECT COUNT(*) FROM user_listening_progress p " +
            "                       WHERE p.user_id = s.user_id AND p.is_completed = true), " +
            "total_lessons_completed = grammar_completed + reading_completed + listening_completed " +
            "WHERE s.user_id = :userId", nativeQuery = true)
    int recalculateLessonCompletion(@Param("userId") Long userId);

    // ==================== ANALYTICS ====================

    /**
     * Get user rank by points
     * 
     * @param userId User ID to get rank for
     * @return Rank (1-based)
     */
    @Query("SELECT COUNT(s) + 1 FROM UserStats s " +
            "WHERE s.totalPoints > (SELECT s2.totalPoints FROM UserStats s2 WHERE s2.userId = :userId)")
    Long getUserRankByPoints(@Param("userId") Long userId);

    /**
     * Get user rank by streak
     */
    @Query("SELECT COUNT(s) + 1 FROM UserStats s " +
            "WHERE s.currentStreak > (SELECT s2.currentStreak FROM UserStats s2 WHERE s2.userId = :userId)")
    Long getUserRankByStreak(@Param("userId") Long userId);

    /**
     * Get percentile for user's points
     * 
     * @param userId User ID
     * @return Percentile (0-100)
     */
    @Query(value = "SELECT PERCENT_RANK() OVER (ORDER BY total_points) * 100 " +
            "FROM user_stats WHERE user_id = :userId", nativeQuery = true)
    Double getUserPointsPercentile(@Param("userId") Long userId);
}