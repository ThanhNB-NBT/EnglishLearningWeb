package com.thanhnb.englishlearning.service.ai.recommendation;

import com.thanhnb.englishlearning.entity.recommendation.AIRecommendation;
import com.thanhnb.englishlearning.repository.recommendation.AIRecommendationRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ Track effectiveness of AI recommendations
 * 
 * Features:
 * - Track which recommendations users click
 * - Track completion rate
 * - Provide analytics for improvement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationTrackingService {

    private final AIRecommendationRepository recommendationRepository;

    /**
     * Mark recommendation as shown to user
     */
    @Transactional
    public void markAsShown(Long recommendationId, Long userId) {
        AIRecommendation rec = getRecommendation(recommendationId, userId);

        if (!rec.isShown()) {
            rec.setShown(true); // ✅ Fixed: use setShown() not setIsShown()
            rec.setShownAt(LocalDateTime.now());
            recommendationRepository.save(rec);

            log.debug("📊 Recommendation {} shown to user {}", recommendationId, userId);
        }
    }

    /**
     * Mark recommendation as accepted (user clicked on it)
     */
    @Transactional
    public void markAsAccepted(Long recommendationId, Long userId) {
        AIRecommendation rec = getRecommendation(recommendationId, userId);

        rec.setIsAccepted(true);
        rec.setShown(true); // ✅ Auto-mark as shown
        if (rec.getShownAt() == null) {
            rec.setShownAt(LocalDateTime.now());
        }

        recommendationRepository.save(rec);

        log.info("✅ User {} accepted recommendation {}: {}", userId, recommendationId, rec.getTitle());
    }

    /**
     * Mark recommendation as completed (user finished the suggested lesson)
     */
    @Transactional
    public void markAsCompleted(Long recommendationId, Long userId) {
        AIRecommendation rec = getRecommendation(recommendationId, userId);

        rec.setCompleted(true); // ✅ Fixed: use setCompleted() not setIsCompleted()
        rec.setIsAccepted(true); // ✅ Auto-accept if completed
        rec.setShown(true);

        recommendationRepository.save(rec);

        log.info("🎉 User {} completed recommendation {}: {}", userId, recommendationId, rec.getTitle());
    }

    /**
     * Dismiss recommendation (user not interested)
     */
    @Transactional
    public void dismissRecommendation(Long recommendationId, Long userId) {
        AIRecommendation rec = getRecommendation(recommendationId, userId);

        // Option 1: Mark as rejected (keep for analytics)
        rec.setIsAccepted(false);
        recommendationRepository.save(rec);

        // Option 2: Delete completely (cleaner for user)
        // recommendationRepository.delete(rec);

        log.info("❌ User {} dismissed recommendation {}", userId, recommendationId);
    }

    /**
     * Get recommendation effectiveness metrics
     */
    @Transactional(readOnly = true)
    public RecommendationMetrics getMetrics(Long userId) {
        List<AIRecommendation> all = recommendationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);

        if (all.isEmpty()) {
            return RecommendationMetrics.empty();
        }

        long total = all.size();
        long shown = all.stream().filter(AIRecommendation::isShown).count();
        long accepted = all.stream()
                .filter(r -> r.getIsAccepted() != null && r.getIsAccepted())
                .count();
        long completed = all.stream().filter(AIRecommendation::isCompleted).count();
        long ignored = all.stream()
                .filter(r -> r.isShown() && (r.getIsAccepted() == null || !r.getIsAccepted()))
                .count();

        // Calculate rates based on shown (not total)
        double acceptanceRate = shown > 0 ? (double) accepted / shown * 100 : 0;
        double completionRate = accepted > 0 ? (double) completed / accepted * 100 : 0;

        return RecommendationMetrics.builder()
                .totalRecommendations(total)
                .shownCount(shown)
                .acceptedCount(accepted)
                .completedCount(completed)
                .ignoredCount(ignored)
                .acceptanceRate(acceptanceRate)
                .completionRate(completionRate)
                .build();
    }

    /**
     * Clean up expired recommendations
     */
    @Transactional
    public void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<AIRecommendation> expired = recommendationRepository
                .findByExpiresAtBeforeAndIsCompletedFalse(now);

        if (!expired.isEmpty()) {
            recommendationRepository.deleteAll(expired);
            log.info("🗑️ Cleaned up {} expired recommendations", expired.size());
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Get recommendation with security check
     */
    private AIRecommendation getRecommendation(Long recommendationId, Long userId) {
        AIRecommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));

        // Security check: ensure user owns this recommendation
        if (!rec.getUser().getId().equals(userId)) {
            throw new SecurityException("Not authorized to access this recommendation");
        }

        return rec;
    }

    // =========================================================================
    // DTO
    // =========================================================================

    @Data
    @lombok.Builder
    public static class RecommendationMetrics {
        private long totalRecommendations;
        private long shownCount;
        private long acceptedCount;
        private long completedCount;
        private long ignoredCount; // ✅ Added: shown but not accepted
        private double acceptanceRate; // % of shown that were accepted
        private double completionRate; // % of accepted that were completed

        public static RecommendationMetrics empty() {
            return RecommendationMetrics.builder()
                    .totalRecommendations(0)
                    .shownCount(0)
                    .acceptedCount(0)
                    .completedCount(0)
                    .ignoredCount(0)
                    .acceptanceRate(0.0)
                    .completionRate(0.0)
                    .build();
        }
    }
}