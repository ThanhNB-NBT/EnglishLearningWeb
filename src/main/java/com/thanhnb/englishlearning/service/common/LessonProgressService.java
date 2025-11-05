package com.thanhnb.englishlearning.service.common;

import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ✅ Service xử lý progress chung cho tất cả module học tập
 * Chứa logic:
 * - Update score (keep highest)
 * - Increment attempts
 * - Mark completed
 * - Award points (only first completion)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LessonProgressService {

    private final UserRepository userRepository;

    /**
     * ✅ Progress update result wrapper
     */
    public static class ProgressUpdateResult {
        private final boolean isFirstCompletion;
        private final boolean isPassed;
        private final int pointsEarned;
        private final BigDecimal scorePercentage;

        public ProgressUpdateResult(boolean isFirstCompletion, boolean isPassed, 
                                   int pointsEarned, BigDecimal scorePercentage) {
            this.isFirstCompletion = isFirstCompletion;
            this.isPassed = isPassed;
            this.pointsEarned = pointsEarned;
            this.scorePercentage = scorePercentage;
        }

        public boolean isFirstCompletion() { return isFirstCompletion; }
        public boolean isPassed() { return isPassed; }
        public int getPointsEarned() { return pointsEarned; }
        public BigDecimal getScorePercentage() { return scorePercentage; }
    }

    /**
     * ✅ Generic progress interface
     */
    public interface LessonProgress {
        Boolean getIsCompleted();
        void setIsCompleted(Boolean completed);
        BigDecimal getScorePercentage();
        void setScorePercentage(BigDecimal score);
        Integer getAttempts();
        void setAttempts(Integer attempts);
        LocalDateTime getCompletedAt();
        void setCompletedAt(LocalDateTime completedAt);
        LocalDateTime getUpdatedAt();
        void setUpdatedAt(LocalDateTime updatedAt);
    }

    /**
     * ✅ Update progress và tính điểm
     * 
     * @param progress Progress entity (Grammar/Reading/Listening)
     * @param user User entity
     * @param currentScore Điểm hiện tại (0-100)
     * @param isPassed Pass hay không
     * @param pointsReward Điểm thưởng của bài học
     * @return ProgressUpdateResult với thông tin chi tiết
     */
    @Transactional
    public <T extends LessonProgress> ProgressUpdateResult updateProgress(
            T progress,
            User user,
            double currentScore,
            boolean isPassed,
            int pointsReward) {

        // Track trạng thái cũ
        boolean wasAlreadyCompleted = progress.getIsCompleted() != null && progress.getIsCompleted();
        boolean isFirstCompletion = !wasAlreadyCompleted;
        BigDecimal oldScore = progress.getScorePercentage();
        BigDecimal newScore = BigDecimal.valueOf(currentScore);

        // ✅ 1. Update score (LUÔN giữ điểm cao nhất)
        if (newScore.compareTo(progress.getScorePercentage()) > 0) {
            progress.setScorePercentage(newScore);
            log.info("📈 Score improved: {} -> {}", oldScore, newScore);
        } else {
            log.info("📊 Score maintained: current={}, new={}", progress.getScorePercentage(), newScore);
        }

        // ✅ 2. Increment attempts (LUÔN tăng)
        Integer currentAttempts = progress.getAttempts() != null ? progress.getAttempts() : 0;
        progress.setAttempts(currentAttempts + 1);

        int pointsEarned = 0;

        // ✅ 3. Mark completed nếu pass
        if (isPassed) {
            progress.setIsCompleted(true);
            
            // Set completedAt chỉ lần đầu
            if (isFirstCompletion) {
                progress.setCompletedAt(LocalDateTime.now());
            }

            // ✅ 4. Award points CHỈ lần đầu complete
            if (isFirstCompletion) {
                pointsEarned = pointsReward;
                user.setTotalPoints(user.getTotalPoints() + pointsReward);
                userRepository.save(user);
                log.info("🎉 User {} FIRST completed - earned {} points", 
                        user.getId(), pointsReward);
            } else {
                log.info("♻️ User {} re-completed - no additional points (attempts: {})",
                        user.getId(), progress.getAttempts());
            }
        }

        // ✅ 5. Update timestamp
        progress.setUpdatedAt(LocalDateTime.now());

        log.info("✅ Progress updated: attempts={}, score={}, completed={}", 
                progress.getAttempts(), progress.getScorePercentage(), progress.getIsCompleted());

        return new ProgressUpdateResult(isFirstCompletion, isPassed, pointsEarned, newScore);
    }

    /**
     * ✅ Initialize new progress
     */
    public <T extends LessonProgress> void initializeProgress(T progress) {
        progress.setIsCompleted(false);
        progress.setScorePercentage(BigDecimal.ZERO);
        progress.setAttempts(0);
        progress.setUpdatedAt(LocalDateTime.now());
        
        log.debug("✨ Initialized new progress");
    }

    /**
     * ✅ Check if user can submit (anti-spam)
     * 
     * @param lastUpdated Last submit time
     * @param cooldownSeconds Minimum seconds between submissions
     * @return Remaining cooldown seconds (0 if can submit)
     */
    public long checkSubmitCooldown(LocalDateTime lastUpdated, int cooldownSeconds) {
        if (lastUpdated == null) {
            return 0;
        }

        long secondsSinceLastSubmit = java.time.Duration.between(
                lastUpdated, LocalDateTime.now()).getSeconds();

        if (secondsSinceLastSubmit < cooldownSeconds) {
            return cooldownSeconds - secondsSinceLastSubmit;
        }

        return 0;
    }
}