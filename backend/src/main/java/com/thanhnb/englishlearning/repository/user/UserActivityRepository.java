package com.thanhnb.englishlearning.repository.user;

import com.thanhnb.englishlearning.entity.user.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ✅ NEW REPOSITORY: UserActivityRepository
 * 
 * Purpose: Quản lý user activity & tracking data
 * 
 * Key features:
 * - Login tracking
 * - Activity monitoring
 * - Session management
 */
@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    // ==================== BASIC QUERIES ====================

    /**
     * Find activity by userId
     * (Same as findById, but explicit naming)
     */
    Optional<UserActivity> findByUserId(Long userId);

    /**
     * Check if activity record exists for user
     */
    boolean existsByUserId(Long userId);

    // ==================== LOGIN TRACKING ====================

    /**
     * Find recently active users (within X seconds)
     * 
     * @param cutoffDate Cutoff datetime
     * @return Users active after cutoffDate
     */
    @Query("SELECT a FROM UserActivity a WHERE a.lastActivityDate >= :cutoffDate " +
            "ORDER BY a.lastActivityDate DESC")
    List<UserActivity> findRecentlyActive(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find users who logged in today
     */
    @Query("SELECT a FROM UserActivity a WHERE a.lastLoginDate >= :startOfDay " +
            "ORDER BY a.lastLoginDate DESC")
    List<UserActivity> findLoggedInToday(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Find users who haven't logged in for X days
     * 
     * @param cutoffDate Cutoff datetime
     * @return Inactive users
     */
    @Query("SELECT a FROM UserActivity a WHERE a.lastLoginDate < :cutoffDate " +
            "ORDER BY a.lastLoginDate ASC")
    List<UserActivity> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find users who never logged in
     */
    @Query("SELECT a FROM UserActivity a WHERE a.lastLoginDate IS NULL")
    List<UserActivity> findNeverLoggedIn();

    // ==================== SESSION TRACKING ====================

    /**
     * Get most common login IPs (for analytics)
     * 
     * @param limit Number of results
     * @return List of [IP, count] pairs
     */
    @Query(value = "SELECT last_login_ip, COUNT(*) as count " +
            "FROM user_activity " +
            "WHERE last_login_ip IS NOT NULL " +
            "GROUP BY last_login_ip " +
            "ORDER BY count DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> getMostCommonLoginIPs(@Param("limit") int limit);

    /**
     * Find users logged in from specific IP
     * 
     * @param ip IP address
     * @return Users from this IP
     */
    @Query("SELECT a FROM UserActivity a WHERE a.lastLoginIp = :ip")
    List<UserActivity> findByLastLoginIp(@Param("ip") String ip);

    /**
     * Find users with suspicious activity (multiple IPs, etc.)
     * This is a placeholder - implement based on business rules
     */
    @Query("SELECT a FROM UserActivity a WHERE a.loginCount > :threshold " +
            "ORDER BY a.loginCount DESC")
    List<UserActivity> findHighActivityUsers(@Param("threshold") Integer threshold);

    // ==================== BATCH OPERATIONS ====================

    /**
     * ⚠️ CRITICAL: Invalidate all tokens for specific users
     * 
     * Used for:
     * - Mass password reset
     * - Security breach response
     * - Force logout all users
     * 
     * @param userIds List of user IDs
     * @return Number of records updated
     */
    @Modifying
    @Query("UPDATE UserActivity a SET a.lastLoginDate = :now " +
            "WHERE a.userId IN :userIds")
    int invalidateTokensForUsers(@Param("userIds") List<Long> userIds,
            @Param("now") LocalDateTime now);

    /**
     * Invalidate all tokens globally
     * (Emergency use only!)
     */
    @Modifying
    @Query("UPDATE UserActivity a SET a.lastLoginDate = :now")
    int invalidateAllTokens(@Param("now") LocalDateTime now);

    /**
     * Update activity timestamp for user
     */
    @Modifying
    @Query("UPDATE UserActivity a SET a.lastActivityDate = :now " +
            "WHERE a.userId = :userId")
    int updateActivity(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // ==================== STATISTICS ====================

    /**
     * Get total login count across all users
     */
    @Query("SELECT SUM(a.loginCount) FROM UserActivity a")
    Long getTotalLoginCount();

    /**
     * Get average login count per user
     */
    @Query("SELECT AVG(a.loginCount) FROM UserActivity a")
    Double getAverageLoginCount();

    /**
     * Count currently active users (active within X minutes)
     */
    @Query("SELECT COUNT(a) FROM UserActivity a WHERE a.lastActivityDate >= :cutoff")
    Long countCurrentlyActive(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Count users logged in today
     */
    @Query("SELECT COUNT(a) FROM UserActivity a WHERE a.lastLoginDate >= :startOfDay")
    Long countLoggedInToday(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Get daily active users (DAU) for specific date
     */
    @Query(value = "SELECT COUNT(DISTINCT user_id) FROM user_activity " +
            "WHERE DATE(last_activity_date) = :date", nativeQuery = true)
    Long getDailyActiveUsers(@Param("date") String date);

    /**
     * Get monthly active users (MAU)
     */
    @Query(value = "SELECT COUNT(DISTINCT user_id) FROM user_activity " +
            "WHERE last_activity_date >= :startOfMonth", nativeQuery = true)
    Long getMonthlyActiveUsers(@Param("startOfMonth") LocalDateTime startOfMonth);

    // ==================== ANALYTICS ====================

    /**
     * Get user retention rate
     * (Users who logged in again after first login)
     */
    @Query("SELECT COUNT(a) FROM UserActivity a WHERE a.loginCount > 1")
    Long countRetainedUsers();

    /**
     * Get average days between logins for a user
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (last_login_date - created_at)) / 86400) " +
            "FROM user_activity WHERE user_id = :userId AND login_count > 1", nativeQuery = true)
    Double getAverageDaysBetweenLogins(@Param("userId") Long userId);

    /**
     * Find users at risk of churning (no activity for X days)
     */
    @Query("SELECT a FROM UserActivity a " +
            "WHERE a.lastActivityDate < :cutoffDate " +
            "AND a.lastActivityDate IS NOT NULL " +
            "ORDER BY a.lastActivityDate ASC")
    List<UserActivity> findChurnRiskUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    // ==================== SECURITY ====================

    /**
     * Find multiple logins from same IP in short time
     * (Potential brute force attack)
     */
    @Query(value = "SELECT last_login_ip, COUNT(*) as count " +
            "FROM user_activity " +
            "WHERE last_login_date >= :since " +
            "AND last_login_ip IS NOT NULL " +
            "GROUP BY last_login_ip " +
            "HAVING COUNT(*) > :threshold " +
            "ORDER BY count DESC", nativeQuery = true)
    List<Object[]> findSuspiciousLoginIPs(@Param("since") LocalDateTime since,
            @Param("threshold") int threshold);

    /**
     * Find users who logged in from multiple IPs
     * (Store IP history in separate table for better tracking)
     */
    // This would require IP history table
}