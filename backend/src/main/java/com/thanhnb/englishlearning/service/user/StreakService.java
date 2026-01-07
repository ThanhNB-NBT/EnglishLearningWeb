package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.entity.user.UserStats;
import com.thanhnb.englishlearning.repository.user.UserRepository;
import com.thanhnb.englishlearning.repository.user.UserStatsRepository;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StreakService {

    private final UserRepository userRepository;
    private final UserStatsRepository statsRepository;

    /**
     * Update streak when user has activity
     * 
     * @param userId User ID
     * @return true if streak was updated, false if already has streak today
     */
    public boolean updateStreakOnActivity(Long userId) {
        // Get or create stats
        UserStats stats = statsRepository.findById(userId)
                .orElseGet(() -> createNewStats(userId));

        // Use entity method to update streak
        boolean updated = stats.updateStreakOnActivity();
        
        if (updated) {
            statsRepository.save(stats);
            log.info("User {} streak updated: {} days", userId, stats.getCurrentStreak());
        } else {
            log.debug("User {} already has streak for today", userId);
        }
        
        return updated;
    }

    /**
     * Check if user has streak today
     * 
     * @param userId User ID
     * @return true if user already studied today
     */
    public boolean hasStreakToday(Long userId) {
        UserStats stats = statsRepository.findById(userId)
                .orElseGet(() -> createNewStats(userId));

        return stats.hasStreakToday();
    }

    /**
     * Get streak information
     * 
     * @param userId User ID
     * @return StreakInfo with current streak data
     */
    public StreakInfo getStreakInfo(Long userId) {
        UserStats stats = statsRepository.findById(userId)
                .orElseGet(() -> createNewStats(userId));

        return StreakInfo.builder()
                .currentStreak(stats.getCurrentStreak())
                .longestStreak(stats.getLongestStreak())
                .lastStreakDate(stats.getLastStreakDate())
                .hasStreakToday(stats.hasStreakToday())
                .build();
    }

    /**
     * Reset streak for user (manual)
     * 
     * Can be called by admin or as penalty
     */
    public void resetStreak(Long userId) {
        UserStats stats = statsRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Stats not found for user: " + userId));
        
        stats.resetStreak();
        statsRepository.save(stats);
        
        log.info("Streak reset for user {}", userId);
    }

    /**
     * Get streak rank for user
     * 
     * Shows where user ranks compared to others
     */
    public Long getStreakRank(Long userId) {
        return statsRepository.getUserRankByStreak(userId);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create new stats for user if doesn't exist
     */
    private UserStats createNewStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        UserStats stats = UserStats.builder()
                .userId(userId)
                .user(user)
                .totalPoints(0)
                .currentStreak(0)
                .longestStreak(0)
                .totalLessonsCompleted(0)
                .grammarCompleted(0)
                .readingCompleted(0)
                .listeningCompleted(0)
                .totalStudyTimeMinutes(0)
                .build();
        
        log.warn("Creating missing stats for user: {}", userId);
        return statsRepository.save(stats);
    }

    // ==================== DTO ====================

    /**
     * StreakInfo DTO
     * 
     * Added longestStreak field
     */
    @Data
    @Builder
    public static class StreakInfo {
        private Integer currentStreak;
        private Integer longestStreak;
        private LocalDate lastStreakDate;
        private boolean hasStreakToday;
    }
}