package com.thanhnb.englishlearning.service.common;

import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonProgressService {

    private final UserRepository userRepository;

    // =========================================================================
    // INTERFACES & DTOs
    // =========================================================================

    public interface LessonProgress {
        Boolean getIsCompleted();
        void setIsCompleted(Boolean completed);
        
        Double getScorePercentage();
        void setScorePercentage(Double score);
        
        Integer getAttempts();
        void setAttempts(Integer attempts);
        
        LocalDateTime getCompletedAt();
        void setCompletedAt(LocalDateTime completedAt);
        
        LocalDateTime getUpdatedAt();
        void setUpdatedAt(LocalDateTime updatedAt);
    }

    @lombok.Getter
    @lombok.RequiredArgsConstructor
    public static class ProgressUpdateResult {
        private final boolean isFirstCompletion;
        private final boolean isPassed;
        private final int pointsEarned;
        private final Double scorePercentage;
    }

    // =========================================================================
    // ✅ FIXED: CORE LOGIC
    // =========================================================================

    /**
     * Update progress với proper transaction và error handling
     * 
     * FIXES:
     * - Đảm bảo atomic operation (cộng điểm + update progress cùng transaction)
     * - Better logging
     * - Proper null checks
     */
    @Transactional
    public <T extends LessonProgress> ProgressUpdateResult updateProgress(
            T progress, 
            User user, 
            double currentScore, 
            boolean isPassed, 
            int pointsReward,
            ParentType lessonType) {

        if (progress == null) {
            throw new IllegalArgumentException("Progress cannot be null");
        }

        boolean wasAlreadyCompleted = Boolean.TRUE.equals(progress.getIsCompleted());
        boolean isFirstCompletion = !wasAlreadyCompleted;

        Double oldScore = progress.getScorePercentage() != null ? 
            progress.getScorePercentage() : 0.0;

        log.debug("Updating progress: oldScore={}, newScore={}, isPassed={}, isFirst={}", 
            oldScore, currentScore, isPassed, isFirstCompletion);

        // 1. Cập nhật điểm cao nhất
        if (currentScore > oldScore) {
            progress.setScorePercentage(currentScore);
            log.debug("Score improved from {} to {}", oldScore, currentScore);
        }

        // 2. Tăng số lần thử
        int currentAttempts = progress.getAttempts() != null ? progress.getAttempts() : 0;
        progress.setAttempts(currentAttempts + 1);

        int pointsEarned = 0;

        // 3. ✅ FIX: Xử lý khi qua bài - TRONG CÙNG TRANSACTION
        if (isPassed) {
            progress.setIsCompleted(true);
            
            // Nếu là lần đầu hoàn thành -> Cộng điểm & Tăng số bài học
            if (isFirstCompletion) {
                progress.setCompletedAt(LocalDateTime.now());
                pointsEarned = pointsReward;
                
                if (user != null) {
                    if (user.getStats() == null) {
                        log.error("User {} has no stats object!", user.getId());
                        throw new IllegalStateException("User stats not initialized");
                    }
                    
                    // ✅ Cộng điểm
                    user.getStats().addPoints(pointsReward);
                    
                    // ✅ Tăng số bài học theo loại
                    user.getStats().incrementLessons(lessonType);
                    
                    // ✅ Save trong cùng transaction
                    userRepository.save(user);
                    
                    log.info("User {} completed {} lesson #{}: +{} points, Total lessons: {}", 
                        user.getUsername(), 
                        lessonType, 
                        progress.getAttempts(),
                        pointsReward, 
                        user.getStats().getTotalLessonsCompleted());
                } else {
                    log.warn("User object is null when updating progress - points not awarded");
                }
            } else {
                log.debug("User already completed this lesson, no points awarded");
            }
        } else {
            log.debug("User did not pass (score: {}%), lesson not completed", currentScore);
        }

        progress.setUpdatedAt(LocalDateTime.now());
        
        return new ProgressUpdateResult(isFirstCompletion, isPassed, pointsEarned, currentScore);
    }

    /**
     * Initialize new progress object
     */
    public <T extends LessonProgress> void initializeProgress(T progress) {
        if (progress == null) {
            throw new IllegalArgumentException("Progress cannot be null");
        }
        
        progress.setIsCompleted(false);
        progress.setScorePercentage(0.0);
        progress.setAttempts(0);
        progress.setUpdatedAt(LocalDateTime.now());
        
        log.debug("Initialized new progress object");
    }
    
    /**
     * ✅ Check cooldown - SỬ DỤNG TRONG SUBMIT FLOW
     * 
     * @return seconds remaining in cooldown, 0 if no cooldown
     */
    public long checkSubmitCooldown(LocalDateTime lastUpdated, int cooldownSeconds) {
        if (lastUpdated == null) {
            return 0;
        }
        
        long secondsSinceLastSubmit = java.time.Duration
            .between(lastUpdated, LocalDateTime.now())
            .getSeconds();
        
        long remaining = secondsSinceLastSubmit < cooldownSeconds ? 
            (cooldownSeconds - secondsSinceLastSubmit) : 0;
        
        if (remaining > 0) {
            log.debug("Cooldown active: {} seconds remaining", remaining);
        }
        
        return remaining;
    }

    /**
     * ✅ NEW: Validate progress state before update
     */
    public <T extends LessonProgress> void validateProgressState(T progress) {
        if (progress == null) {
            throw new IllegalArgumentException("Progress cannot be null");
        }
        
        if (progress.getAttempts() != null && progress.getAttempts() < 0) {
            throw new IllegalStateException("Attempts cannot be negative");
        }
        
        if (progress.getScorePercentage() != null && 
            (progress.getScorePercentage() < 0 || progress.getScorePercentage() > 100)) {
            throw new IllegalStateException("Score percentage must be between 0 and 100");
        }
    }
}