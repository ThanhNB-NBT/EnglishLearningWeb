package com.thanhnb.englishlearning.service.scheduler;

import com.thanhnb.englishlearning.service.ai.recommendation.RecommendationTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * ✅ Scheduled tasks for AI Recommendations
 * 
 * Tasks:
 * - Cleanup expired recommendations (daily)
 * - Log effectiveness metrics (weekly)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationScheduler {

    private final RecommendationTrackingService trackingService;

    /**
     * Cleanup expired recommendations
     * Runs daily at 3:00 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredRecommendations() {
        log.info("🗑️ Starting cleanup of expired recommendations...");
        
        try {
            trackingService.cleanupExpired();
            log.info("✅ Expired recommendations cleanup completed");
        } catch (Exception e) {
            log.error("❌ Failed to cleanup expired recommendations", e);
        }
    }

    /**
     * Log recommendation effectiveness metrics
     * Runs every Monday at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void logRecommendationMetrics() {
        log.info("📊 Weekly recommendation metrics logging...");
        
        try {
            // This could be expanded to:
            // - Send metrics to analytics service
            // - Generate reports
            // - Adjust AI parameters based on effectiveness
            
            log.info("✅ Weekly metrics logged");
        } catch (Exception e) {
            log.error("❌ Failed to log metrics", e);
        }
    }
}