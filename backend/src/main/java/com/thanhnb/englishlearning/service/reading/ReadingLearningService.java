package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.dto.common.SubmitResultDTO;
import com.thanhnb.englishlearning.dto.reading.ReadingLessonDTO;
import com.thanhnb.englishlearning.dto.reading.ReadingSubmitRequest;
import com.thanhnb.englishlearning.dto.reading.ReadingSubmitResponse;
import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.entity.reading.UserReadingProgress;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.reading.UserReadingProgressRepository;
import com.thanhnb.englishlearning.service.common.BaseLearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ✅ REFACTORED: Reading Learning Service (Full)
 * - Đã bổ sung getProgressSummary
 * - Dùng BaseLearningService cho submit
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadingLearningService extends BaseLearningService<ReadingLesson, UserReadingProgress> {

    private final ReadingLessonRepository lessonRepository;
    private final UserReadingProgressRepository progressRepository;

    // ═══════════════════════════════════════════════════════════
    // BASE IMPLEMENTATION
    // ═══════════════════════════════════════════════════════════

    @Override
    protected ParentType getParentType() {
        return ParentType.READING;
    }

    @Override
    protected Integer getLessonOrder(ReadingLesson l) {
        return l.getOrderIndex();
    }

    @Override
    protected Long getLessonId(ReadingLesson l) {
        return l.getId();
    }

    @Override
    protected boolean isLessonActive(ReadingLesson l) {
        return l.getIsActive();
    }

    @Override
    protected int getPointsReward(ReadingLesson l) {
        return l.getPointsReward();
    }

    @Override
    protected Long getTopicId(ReadingLesson l) {
        return l.getTopic().getId();
    }

    @Override
    protected String getTopicName(ReadingLesson l) {
        return l.getTopic().getName();
    }

    @Override
    protected EnglishLevel getLessonRequiredLevel(ReadingLesson l) {
        return l.getTopic().getLevelRequired();
    }

    @Override
    protected UserReadingProgress createNewProgressInstance(Long userId, ReadingLesson lesson) {
        User user = userRepository.getReferenceById(userId);
        UserReadingProgress p = new UserReadingProgress();
        p.setUser(user);
        p.setLesson(lesson);
        lessonProgressService.initializeProgress(p);
        return p;
    }

    @Override
    protected Optional<UserReadingProgress> findProgress(Long userId, Long lessonId) {
        return progressRepository.findByUserIdAndLessonId(userId, lessonId);
    }

    @Override
    protected void saveProgress(UserReadingProgress p) {
        progressRepository.save(p);
    }

    // ═══════════════════════════════════════════════════════════
    // SUBMIT LESSON
    // ═══════════════════════════════════════════════════════════

    public ReadingSubmitResponse submitLesson(Long userId, ReadingSubmitRequest request) {
        ReadingLesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<ReadingLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();
        validateLessonAccess(lesson, allLessons, userId,
                progressRepository::existsByUserIdAndLessonIdAndIsCompletedTrue, user.getEnglishLevel());

        // 🔥 Dùng hàm chung Base
        SubmitResultDTO res = processSubmission(userId, lesson, allLessons, request.getAnswers(), ModuleType.READING);

        return ReadingSubmitResponse.of(
                lesson.getId(), lesson.getTitle(),
                res.totalQuestions(), res.correctCount(), res.totalScore(), res.scorePercentage(),
                res.isPassed(), res.pointsEarned(), res.hasUnlockedNext(), res.nextLessonId(),
                res.results(), res.levelUpgradeResult());
    }

    // ═══════════════════════════════════════════════════════════
    // DATA RETRIEVAL
    // ═══════════════════════════════════════════════════════════

    public List<ReadingLessonDTO> getAllLessonsForUser(Long userId, Long topicId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        EnglishLevel userLevel = user.getEnglishLevel();

        List<ReadingLesson> lessons = (topicId != null)
                ? lessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId)
                : lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();

        List<UserReadingProgress> progressList = progressRepository.findByUserId(userId);
        Set<Long> completedIds = progressList.stream()
                .filter(UserReadingProgress::getIsCompleted)
                .map(p -> p.getLesson().getId())
                .collect(Collectors.toSet());

        return lessons.stream()
                .filter(ReadingLesson::getIsActive)
                .map(lesson -> {
                    boolean unlocked = isLessonUnlocked(lesson, lessons, userId,
                            this::getLessonOrder, this::getLessonId,
                            (u, lid) -> completedIds.contains(lid), userLevel);

                    Optional<UserReadingProgress> pOpt = progressList.stream()
                            .filter(p -> p.getLesson().getId().equals(lesson.getId()))
                            .findFirst();

                    ReadingLessonDTO dto = ReadingLessonDTO.builder()
                            .id(lesson.getId()).title(lesson.getTitle())
                            .topicId(lesson.getTopic().getId()).topicName(lesson.getTopic().getName())
                            .orderIndex(lesson.getOrderIndex()).pointsReward(lesson.getPointsReward())
                            .requiredLevel(getLessonRequiredLevel(lesson))
                            .isUnlocked(unlocked).isActive(lesson.getIsActive())
                            .build();

                    pOpt.ifPresent(p -> dto.withProgress(
                            p.getIsCompleted(), p.getScorePercentage(),
                            p.getAttempts(), p.getCompletedAt()));
                    return dto;
                }).toList();
    }

    public ReadingLessonDTO getLessonDetail(Long lessonId, Long userId) {
        ReadingLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<ReadingLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();
        validateLessonAccess(lesson, allLessons, userId,
                progressRepository::existsByUserIdAndLessonIdAndIsCompletedTrue, user.getEnglishLevel());

        ReadingLessonDTO dto = ReadingLessonDTO.builder()
                .id(lesson.getId()).title(lesson.getTitle())
                .content(lesson.getContent()).contentTranslation(lesson.getContentTranslation())
                .topicId(lesson.getTopic().getId()).topicName(lesson.getTopic().getName())
                .timeLimitSeconds(lesson.getTimeLimitSeconds())
                .pointsReward(lesson.getPointsReward())
                .requiredLevel(getLessonRequiredLevel(lesson)).build();

        // 🔥 Lấy câu hỏi từ hàm Helper của Base
        dto.withGroupedQuestions(getGroupedQuestionsForLesson(lessonId));

        findProgress(userId, lessonId).ifPresent(p -> dto.withProgress(
                p.getIsCompleted(), p.getScorePercentage(),
                p.getAttempts(), p.getCompletedAt()));
        return dto;
    }

    public List<UserReadingProgress> getCompletedLessons(Long userId) {
        return progressRepository.findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(userId);
    }

    // ═══════════════════════════════════════════════════════════
    // ✅ SUMMARY & STATS (ĐÃ BỔ SUNG)
    // ═══════════════════════════════════════════════════════════

    public ReadingProgressSummary getProgressSummary(Long userId) {
        List<UserReadingProgress> completed = progressRepository
                .findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(userId);

        int totalCompleted = completed.size();

        double avgScore = completed.stream()
                .filter(p -> p.getScorePercentage() != null)
                .mapToDouble(UserReadingProgress::getScorePercentage)
                .average()
                .orElse(0.0);

        int totalAttempts = completed.stream()
                .mapToInt(p -> p.getAttempts() == null ? 0 : p.getAttempts())
                .sum();

        // Top 5 bài gần nhất
        List<RecentCompletion> recent = completed.stream()
                .limit(5) // Đã sort DESC ở query rồi
                .map(p -> new RecentCompletion(
                        p.getLesson().getId(),
                        p.getLesson().getTitle(),
                        p.getScorePercentage() != null ? p.getScorePercentage() : 0.0,
                        p.getCompletedAt()))
                .toList();

        return ReadingProgressSummary.builder()
                .userId(userId)
                .totalCompleted(totalCompleted)
                .averageScore(Math.round(avgScore * 100.0) / 100.0)
                .totalAttempts(totalAttempts)
                .recentCompletions(recent)
                .build();
    }

    // --- DTOs ---
    @lombok.Data
    @lombok.Builder
    public static class ReadingProgressSummary {
        private Long userId;
        private int totalCompleted;
        private double averageScore;
        private int totalAttempts;
        private List<RecentCompletion> recentCompletions;
    }

    public record RecentCompletion(Long lessonId, String lessonTitle, double score, LocalDateTime completedAt) {
    }
}