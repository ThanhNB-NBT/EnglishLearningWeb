package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.common.SubmitResultDTO;
import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.dto.grammar.GrammarSubmitRequest;
import com.thanhnb.englishlearning.dto.grammar.GrammarSubmitResponse;
import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.grammar.UserGrammarProgress;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.grammar.UserGrammarProgressRepository;
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
 * ✅ REFACTORED: Grammar Learning Service (Full)
 * - Đã bổ sung getProgressSummary
 * - Dùng BaseLearningService cho submit
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarLearningService extends BaseLearningService<GrammarLesson, UserGrammarProgress> {

    private final GrammarLessonRepository lessonRepository;
    private final UserGrammarProgressRepository progressRepository;

    // ═══════════════════════════════════════════════════════════
    // BASE IMPLEMENTATION
    // ═══════════════════════════════════════════════════════════

    @Override
    protected ParentType getParentType() {
        return ParentType.GRAMMAR;
    }

    @Override
    protected Integer getLessonOrder(GrammarLesson l) {
        return l.getOrderIndex();
    }

    @Override
    protected Long getLessonId(GrammarLesson l) {
        return l.getId();
    }

    @Override
    protected boolean isLessonActive(GrammarLesson l) {
        return l.getIsActive();
    }

    @Override
    protected int getPointsReward(GrammarLesson l) {
        return l.getPointsReward();
    }

    @Override
    protected Long getTopicId(GrammarLesson l) {
        return l.getTopic().getId();
    }

    @Override
    protected String getTopicName(GrammarLesson l) {
        return l.getTopic().getName();
    }

    @Override
    protected EnglishLevel getLessonRequiredLevel(GrammarLesson l) {
        return l.getTopic().getLevelRequired();
    }

    @Override
    protected UserGrammarProgress createNewProgressInstance(Long userId, GrammarLesson lesson) {
        User user = userRepository.getReferenceById(userId);
        UserGrammarProgress p = new UserGrammarProgress();
        p.setUser(user);
        p.setLesson(lesson);
        p.setReadingTime(0);
        lessonProgressService.initializeProgress(p);
        return p;
    }

    @Override
    protected Optional<UserGrammarProgress> findProgress(Long userId, Long lessonId) {
        return progressRepository.findByUserIdAndLessonId(userId, lessonId);
    }

    @Override
    protected void saveProgress(UserGrammarProgress p) {
        progressRepository.save(p);
    }

    // ═══════════════════════════════════════════════════════════
    // SUBMIT LESSON
    // ═══════════════════════════════════════════════════════════

    public GrammarSubmitResponse submitLesson(Long userId, GrammarSubmitRequest request) {
        GrammarLesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<GrammarLesson> allLessons = lessonRepository
                .findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(lesson.getTopic().getId());

        validateLessonAccess(lesson, allLessons, userId,
                progressRepository::existsByUserIdAndLessonIdAndIsCompletedTrue, user.getEnglishLevel());

        if (lesson.getLessonType() == LessonType.PRACTICE) {
            // 🔥 Practice: Dùng hàm chung Base
            SubmitResultDTO res = processSubmission(userId, lesson, allLessons, request.getAnswers(),
                    ModuleType.GRAMMAR);
            return GrammarSubmitResponse.of(
                    lesson.getId(), lesson.getTitle(),
                    res.totalQuestions(), res.correctCount(), res.totalScore(), res.scorePercentage(),
                    res.isPassed(), res.pointsEarned(), res.hasUnlockedNext(), res.nextLessonId(),
                    res.results(), res.levelUpgradeResult());
        } else {
            // 🔥 Theory: Logic riêng (vì không chấm điểm câu hỏi)
            return submitTheoryLesson(userId, lesson, allLessons, request);
        }
    }

    private GrammarSubmitResponse submitTheoryLesson(Long userId, GrammarLesson lesson, List<GrammarLesson> allLessons,
            GrammarSubmitRequest request) {
        UserGrammarProgress progress = findProgress(userId, lesson.getId())
                .orElseGet(() -> createNewProgressInstance(userId, lesson));

        if (request.getReadingTimeSecond() != null) {
            int current = progress.getReadingTime() != null ? progress.getReadingTime() : 0;
            progress.setReadingTime(current + request.getReadingTimeSecond());
        }

        User user = userRepository.getReferenceById(userId);
        var updateResult = lessonProgressService.updateProgress(progress, user, 100.0, true, lesson.getPointsReward(),
                getParentType());
        saveProgress(progress);

        GrammarLesson next = findNextLesson(lesson, allLessons);
        Long nextId = next != null ? next.getId() : null;
        boolean unlocked = updateResult.isFirstCompletion() && next != null;

        var levelRes = (updateResult.isFirstCompletion())
                ? levelUpgradeService.checkAndUpgradeLevel(userId, ModuleType.GRAMMAR, lesson.getTopic().getId())
                : null;

        return GrammarSubmitResponse.of(
                lesson.getId(), lesson.getTitle(), 0, 0, 0, 100.0, true,
                updateResult.getPointsEarned(), unlocked, nextId, null, levelRes);
    }

    // ═══════════════════════════════════════════════════════════
    // DATA RETRIEVAL
    // ═══════════════════════════════════════════════════════════

    public List<GrammarLessonDTO> getAllLessonsForUser(Long userId, Long topicId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<GrammarLesson> lessons = (topicId != null)
                ? lessonRepository.findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(topicId)
                : lessonRepository.findByIsActiveTrueOrderByOrderIndexAsc();

        List<UserGrammarProgress> progressList = progressRepository.findByUserId(userId);
        Set<Long> completedIds = progressList.stream()
                .filter(UserGrammarProgress::getIsCompleted)
                .map(p -> p.getLesson().getId())
                .collect(Collectors.toSet());

        return lessons.stream().map(l -> {
            boolean unlocked = isLessonUnlocked(l, lessons, userId,
                    this::getLessonOrder, this::getLessonId,
                    (u, lid) -> completedIds.contains(lid), user.getEnglishLevel());

            Optional<UserGrammarProgress> pOpt = progressList.stream()
                    .filter(p -> p.getLesson().getId().equals(l.getId()))
                    .findFirst();

            GrammarLessonDTO dto = GrammarLessonDTO.builder()
                    .id(l.getId()).title(l.getTitle())
                    .topicId(l.getTopic().getId()).topicName(l.getTopic().getName())
                    .lessonType(l.getLessonType()).orderIndex(l.getOrderIndex())
                    .pointsReward(l.getPointsReward())
                    .requiredLevel(getLessonRequiredLevel(l))
                    .isUnlocked(unlocked).isActive(l.getIsActive())
                    .build();

            pOpt.ifPresent(p -> dto.withProgress(
                    p.getIsCompleted(),
                    p.getScorePercentage() != null ? p.getScorePercentage().intValue() : 0,
                    p.getAttempts(), p.getCompletedAt()));
            return dto;
        }).toList();
    }

    public GrammarLessonDTO getLessonDetail(Long lessonId, Long userId) {
        GrammarLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<GrammarLesson> allLessons = lessonRepository
                .findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(lesson.getTopic().getId());
        validateLessonAccess(lesson, allLessons, userId,
                progressRepository::existsByUserIdAndLessonIdAndIsCompletedTrue, user.getEnglishLevel());

        GrammarLessonDTO dto = GrammarLessonDTO.builder()
                .id(lesson.getId()).title(lesson.getTitle()).content(lesson.getContent())
                .topicId(lesson.getTopic().getId()).topicName(lesson.getTopic().getName())
                .lessonType(lesson.getLessonType()).timeLimitSeconds(lesson.getTimeLimitSeconds())
                .pointsReward(lesson.getPointsReward()).requiredLevel(getLessonRequiredLevel(lesson)).build();

        if (lesson.getLessonType() == LessonType.PRACTICE) {
            dto.withGroupedQuestions(getGroupedQuestionsForLesson(lessonId));
        }

        findProgress(userId, lessonId).ifPresent(p -> dto.withProgress(
                p.getIsCompleted(),
                p.getScorePercentage() != null ? p.getScorePercentage().intValue() : 0,
                p.getAttempts(), p.getCompletedAt()));
        return dto;
    }

    public List<UserGrammarProgress> getCompletedLessons(Long userId) {
        return progressRepository.findCompletedWithLessonByUserId(userId);
    }

    // ═══════════════════════════════════════════════════════════
    // ✅ SUMMARY & STATS (ĐÃ BỔ SUNG)
    // ═══════════════════════════════════════════════════════════

    public GrammarProgressSummary getProgressSummary(Long userId) {
        List<UserGrammarProgress> completed = progressRepository.findCompletedWithLessonByUserId(userId);

        int totalCompleted = completed.size();

        // Tính điểm trung bình (chỉ tính bài Practice có điểm)
        double avgScore = completed.stream()
                .filter(p -> p.getLesson().getLessonType() == LessonType.PRACTICE)
                .filter(p -> p.getScorePercentage() != null)
                .mapToDouble(UserGrammarProgress::getScorePercentage)
                .average()
                .orElse(0.0);

        int totalAttempts = completed.stream()
                .mapToInt(p -> p.getAttempts() == null ? 0 : p.getAttempts())
                .sum();

        // 5 bài học gần nhất
        List<RecentCompletion> recent = completed.stream()
                .sorted((p1, p2) -> p2.getCompletedAt().compareTo(p1.getCompletedAt())) // Mới nhất trước
                .limit(5)
                .map(p -> new RecentCompletion(
                        p.getLesson().getId(),
                        p.getLesson().getTitle(),
                        p.getScorePercentage() != null ? p.getScorePercentage() : 0.0,
                        p.getCompletedAt()))
                .toList();

        return GrammarProgressSummary.builder()
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
    public static class GrammarProgressSummary {
        private Long userId;
        private int totalCompleted;
        private double averageScore;
        private int totalAttempts;
        private List<RecentCompletion> recentCompletions;
    }

    public record RecentCompletion(Long lessonId, String lessonTitle, double score, LocalDateTime completedAt) {
    }
}