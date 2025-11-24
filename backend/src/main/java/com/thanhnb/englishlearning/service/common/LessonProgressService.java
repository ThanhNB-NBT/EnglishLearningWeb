package com.thanhnb.englishlearning.service.common;

import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service xử lý progress và unlock logic chung cho tất cả module học tập
 * Chứa logic:
 * - Update score (keep highest)
 * - Increment attempts
 * - Mark completed
 * - Award points (only first completion)
 * - Check lesson unlock status
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LessonProgressService {

    private final UserRepository userRepository;

    /**
     * Progress update result wrapper
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

        public boolean isFirstCompletion() {
            return isFirstCompletion;
        }

        public boolean isPassed() {
            return isPassed;
        }

        public int getPointsEarned() {
            return pointsEarned;
        }

        public BigDecimal getScorePercentage() {
            return scorePercentage;
        }
    }

    /**
     * Generic progress interface
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
     * Functional interfaces for unlock logic
     */
    @FunctionalInterface
    public interface LessonOrderIndexGetter<L> {
        Integer getOrderIndex(L lesson);
    }

    @FunctionalInterface
    public interface LessonIdGetter<L> {
        Long getLessonId(L lesson);
    }

    @FunctionalInterface
    public interface ProgressCompletedChecker {
        boolean isCompleted(Long userId, Long lessonId);
    }

    /**
     * Update progress và tính điểm
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

        boolean wasAlreadyCompleted = progress.getIsCompleted() != null && progress.getIsCompleted();
        boolean isFirstCompletion = !wasAlreadyCompleted;
        BigDecimal oldScore = progress.getScorePercentage();
        BigDecimal newScore = BigDecimal.valueOf(currentScore);

        // Update score (keep highest)
        if (newScore.compareTo(progress.getScorePercentage()) > 0) {
            progress.setScorePercentage(newScore);
            log.info("Score improved: {} -> {}", oldScore, newScore);
        } else {
            log.info("Score maintained: current={}, new={}", progress.getScorePercentage(), newScore);
        }

        // Increment attempts
        Integer currentAttempts = progress.getAttempts() != null ? progress.getAttempts() : 0;
        progress.setAttempts(currentAttempts + 1);

        int pointsEarned = 0;

        // Mark completed if passed
        if (isPassed) {
            progress.setIsCompleted(true);

            if (isFirstCompletion) {
                progress.setCompletedAt(LocalDateTime.now());
            }

            // Award points only first completion
            if (isFirstCompletion) {
                pointsEarned = pointsReward;
                user.setTotalPoints(user.getTotalPoints() + pointsReward);
                userRepository.save(user);
                log.info("User {} first completed - earned {} points", user.getId(), pointsReward);
            } else {
                log.info("User {} re-completed - no additional points (attempts: {})",
                        user.getId(), progress.getAttempts());
            }
        }

        progress.setUpdatedAt(LocalDateTime.now());

        log.info("Progress updated: attempts={}, score={}, completed={}",
                progress.getAttempts(), progress.getScorePercentage(), progress.getIsCompleted());

        return new ProgressUpdateResult(isFirstCompletion, isPassed, pointsEarned, newScore);
    }

    /**
     * Initialize new progress
     */
    public <T extends LessonProgress> void initializeProgress(T progress) {
        progress.setIsCompleted(false);
        progress.setScorePercentage(BigDecimal.ZERO);
        progress.setAttempts(0);
        progress.setUpdatedAt(LocalDateTime.now());

        log.debug("Initialized new progress");
    }

    /**
     * Check if user can submit (anti-spam)
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

    /**
     * Check lesson unlock status (sequential unlock)
     * 
     * Rules:
     * 1. First lesson (orderIndex = 1) always unlocked
     * 2. Next lesson unlocked only when previous lesson completed
     * 
     * @param lesson Current lesson to check
     * @param allLessons All lessons sorted by orderIndex
     * @param userId User ID
     * @param orderIndexGetter Lambda to get orderIndex from lesson
     * @param lessonIdGetter Lambda to get lessonId from lesson
     * @param progressChecker Lambda to check if progress completed
     */
    public <L> boolean isLessonUnlocked(
            L lesson,
            List<L> allLessons,
            Long userId,
            LessonOrderIndexGetter<L> orderIndexGetter,
            LessonIdGetter<L> lessonIdGetter,
            ProgressCompletedChecker progressChecker) {

        Integer orderIndex = orderIndexGetter.getOrderIndex(lesson);

        // Rule 1: First lesson always unlocked
        if (orderIndex == 1) {
            log.debug("Lesson orderIndex={} is first, unlocked", orderIndex);
            return true;
        }

        // Rule 2: Find previous lesson
        L previousLesson = allLessons.stream()
                .filter(l -> orderIndexGetter.getOrderIndex(l).equals(orderIndex - 1))
                .findFirst()
                .orElse(null);

        if (previousLesson == null) {
            log.warn("No previous lesson found for orderIndex={}, unlocked by fallback", orderIndex);
            return true;
        }

        // Check if previous lesson completed
        Long previousLessonId = lessonIdGetter.getLessonId(previousLesson);
        boolean isPreviousCompleted = progressChecker.isCompleted(userId, previousLessonId);

        if (isPreviousCompleted) {
            log.debug("Previous lesson id={} completed, unlocked", previousLessonId);
        } else {
            log.debug("Previous lesson id={} not completed, locked", previousLessonId);
        }

        return isPreviousCompleted;
    }

    /**
     * Find next lesson in sequence
     */
    public <L> L findNextLesson(L currentLesson, List<L> allLessons,
            LessonOrderIndexGetter<L> orderIndexGetter) {
        Integer currentOrderIndex = orderIndexGetter.getOrderIndex(currentLesson);

        return allLessons.stream()
                .filter(l -> orderIndexGetter.getOrderIndex(l).equals(currentOrderIndex + 1))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find previous lesson in sequence
     */
    public <L> L findPreviousLesson(L currentLesson, List<L> allLessons,
            LessonOrderIndexGetter<L> orderIndexGetter) {
        Integer currentOrderIndex = orderIndexGetter.getOrderIndex(currentLesson);

        return allLessons.stream()
                .filter(l -> orderIndexGetter.getOrderIndex(l).equals(currentOrderIndex - 1))
                .findFirst()
                .orElse(null);
    }
}