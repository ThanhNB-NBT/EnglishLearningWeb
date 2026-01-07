package com.thanhnb.englishlearning.service.user;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thanhnb.englishlearning.entity.user.UserStats;
import com.thanhnb.englishlearning.repository.user.UserStatsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StreakScheduler {
    
    private final UserStatsRepository statsRepository;

    /**
     * Check and reset streaks daily at 00:01 AM
     * 
     * OPTION B: Bulk update approach (RECOMMENDED - Much faster!)
     * 
     * Uses single SQL UPDATE query to reset all expired streaks
     */
    @Scheduled(cron = "0 1 0 * * *") // 00:01 AM every day
    @Transactional
    public void checkAndResetStreaks() {
        log.info("Starting daily streak check...");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        try {
            // BULK UPDATE: Single query resets all expired streaks
            int resetCount = statsRepository.resetExpiredStreaks(yesterday);
            
            log.info("Daily streak check completed. Reset {} streaks", resetCount);
            
            // Optional: Log statistics
            if (resetCount > 0) {
                long activeStreaks = statsRepository.countUsersWithActiveStreak();
                log.info("Active streaks remaining: {}", activeStreaks);
            }
            
        } catch (Exception e) {
            log.error("Error during streak reset: {}", e.getMessage(), e);
            // Don't throw - let it retry next day
        }
    }

    /**
     * ALTERNATIVE: Individual update approach (safer but slower)
     * 
     * Uncomment and use this if bulk update causes issues
     * 
     * @Scheduled(cron = "0 1 0 * * *")
     * @Transactional
     * public void checkAndResetStreaksIndividual() {
     *     log.info("Starting daily streak check (individual)...");
     *     
     *     LocalDate yesterday = LocalDate.now().minusDays(1);
     *     LocalDate today = LocalDate.now();
     *     
     *     // Get all users with active streaks
     *     List<UserStats> activeStreakUsers = statsRepository.findAllWithActiveStreak();
     *     
     *     int resetCount = 0;
     *     for (UserStats stats : activeStreakUsers) {
     *         LocalDate lastStreakDate = stats.getLastStreakDate();
     *         
     *         // If last streak date is not yesterday or today → Reset
     *         if (lastStreakDate == null || 
     *             (!lastStreakDate.equals(yesterday) && !lastStreakDate.equals(today))) {
     *             
     *             log.debug("Resetting streak for user {} (last streak: {})", 
     *                     stats.getUserId(), lastStreakDate);
     *             
     *             stats.resetStreak();
     *             statsRepository.save(stats);
     *             resetCount++;
     *         }
     *     }
     *     
     *     log.info("Daily streak check completed. Reset {} streaks", resetCount);
     * }
     */

    /**
     * Manual trigger for admin (for testing/debug)
     * 
     * Can be called via admin endpoint to force streak check
     */
    @Transactional
    public int manualStreakCheck() {
        log.info("Manual streak check triggered");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int resetCount = statsRepository.resetExpiredStreaks(yesterday);
        
        log.info("Manual streak check completed. Reset {} streaks", resetCount);
        return resetCount;
    }

    /**
     * Preview how many streaks will be reset
     * 
     * Useful for monitoring/admin dashboard
     */
    public int getStreaksToResetCount() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<UserStats> toReset = statsRepository.findStreaksToReset(yesterday);
        return toReset.size();
    }

    /**
     * Get streak statistics
     * 
     * For monitoring dashboard
     */
    public StreakStatistics getStatistics() {
        long totalActiveStreaks = statsRepository.countUsersWithActiveStreak();
        Double averageStreak = statsRepository.getAverageStreak();
        LocalDate today = LocalDate.now();
        List<UserStats> studiedToday = statsRepository.findUsersWhoStudiedToday(today);
        
        return StreakStatistics.builder()
                .activeStreaks(totalActiveStreaks)
                .averageStreak(averageStreak != null ? averageStreak : 0.0)
                .studiedToday(studiedToday.size())
                .build();
    }

    /**
     * DTO for streak statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class StreakStatistics {
        private long activeStreaks;
        private double averageStreak;
        private int studiedToday;
    }
}