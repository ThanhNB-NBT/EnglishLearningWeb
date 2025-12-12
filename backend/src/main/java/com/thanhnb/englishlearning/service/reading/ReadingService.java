package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.dto.reading.*;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.entity.reading.*;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.repository.reading.*;
import com.thanhnb.englishlearning.service.common.BaseLearningService;
import com.thanhnb.englishlearning.service.common.LessonProgressService;
import com.thanhnb.englishlearning.service.question.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reading service extends BaseLearningService
 * - Tích hợp LessonProgressService để xử lý progress và unlock
 * - Loại bỏ manual pagination (user không cần phân trang)
 * - Nhất quán với Grammar/Listening modules
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadingService extends BaseLearningService<ReadingLesson, UserReadingProgress> {

    private final ReadingLessonRepository lessonRepository;
    private final UserReadingProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final QuestionService questionService;
    private final LessonProgressService lessonProgressService;

    private static final int SUBMIT_COOLDOWN_SECONDS = 10;
    private static final double PASS_THRESHOLD = 80.0;

    @Override
    protected ParentType getParentType() {
        return ParentType.READING;
    }

    /**
     * [USER] Lấy tất cả bài đọc với progress và unlock status
     * Không phân trang, load tất cả active lessons (thường < 100 bài)
     */
    public List<ReadingLessonDTO> getAllLessonsForUser(Long userId) {
        log.info("Loading reading lessons for user {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Người dùng không tồn tại với id: " + userId);
        }

        List<ReadingLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();
        List<UserReadingProgress> allProgress = progressRepository.findByUserId(userId);

        List<ReadingLessonDTO> result = allLessons.stream()
                .map(lesson -> buildUserLessonDTO(lesson, allLessons, allProgress, userId))
                .collect(Collectors.toList());

        log.info("Loaded {} reading lessons for user {}", result.size(), userId);
        return result;
    }

    /**
     * [USER] Lấy chi tiết lesson với questions và check unlock
     */
    public ReadingLessonDTO getLessonDetail(Long lessonId, Long userId) {
        log.info("Loading lesson detail: lessonId={}, userId={}", lessonId, userId);

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Người dùng không tồn tại với id: " + userId);
        }

        ReadingLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với id: " + lessonId));

        if (!lesson.getIsActive()) {
            throw new RuntimeException("Bài đọc này hiện không khả dụng");
        }

        List<ReadingLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();
        boolean isUnlocked = checkUnlockStatus(lesson, allLessons, userId);

        if (!isUnlocked) {
            throw new RuntimeException("Bạn cần hoàn thành bài đọc trước đó để mở khóa bài này");
        }

        ReadingLessonDTO dto = convertLessonToFullDTO(lesson);

        List<QuestionResponseDTO> questionDTOs = questionService
                .convertToDTOs(
                        questionService.loadQuestionsByParent(getParentType(), lessonId),
                        false);

        dto.withQuestions(questionDTOs);

        Optional<UserReadingProgress> progress = progressRepository.findByUserIdAndLessonId(userId, lessonId);

        if (progress.isPresent()) {
            UserReadingProgress p = progress.get();
            dto.withProgress(
                    p.getIsCompleted(),
                    p.getScorePercentage().doubleValue(),
                    p.getAttempts(),
                    p.getCompletedAt());
        }

        dto.withUnlockStatus(isUnlocked, isUnlocked);

        log.info("Loaded lesson with {} questions, unlocked={}", questionDTOs.size(), isUnlocked);
        return dto;
    }

    /**
     * [USER] Submit lesson với LessonProgressService
     */
    @Transactional
    public ReadingSubmitResponse submitLesson(Long userId, Long lessonId, ReadingSubmitRequest request) {
        log.info("Submit reading lesson: userId={}, lessonId={}", userId, lessonId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với id: " + userId));

        ReadingLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với id: " + lessonId));

        if (!lesson.getIsActive()) {
            throw new RuntimeException("Bài đọc này hiện không khả dụng");
        }

        List<ReadingLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();

        if (!checkUnlockStatus(lesson, allLessons, userId)) {
            throw new RuntimeException("Bạn cần hoàn thành bài đọc trước đó để làm bài này");
        }

        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new RuntimeException("Vui lòng trả lời ít nhất 1 câu hỏi");
        }

        long expectedQuestions = questionService.countQuestionsByParent(getParentType(), lessonId);
        if (request.getAnswers().size() < expectedQuestions) {
            log.warn("User {} submitted lesson {} incomplete: {}/{} answers", 
                    userId, lessonId, request.getAnswers().size(), expectedQuestions);
        }

        UserReadingProgress progress = progressRepository
                .findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    UserReadingProgress newProgress = createNewProgress(user, lesson);
                    lessonProgressService.initializeProgress(newProgress);
                    return newProgress;
                });

        long cooldown = lessonProgressService.checkSubmitCooldown(
                progress.getUpdatedAt(), SUBMIT_COOLDOWN_SECONDS);

        if (cooldown > 0) {
            throw new RuntimeException("Vui lòng đợi " + cooldown + " giây trước khi nộp lại");
        }

        List<QuestionResultDTO> results = questionService.processAnswers(
                request.getAnswers(), getParentType());

        int totalQuestions = results.size();
        int correctCount = questionService.calculateCorrectCount(results);
        double scorePercentage = questionService.calculateScorePercentage(correctCount, totalQuestions);

        boolean isPassed = isPassed(scorePercentage, PASS_THRESHOLD);

        LessonProgressService.ProgressUpdateResult progressResult =
                lessonProgressService.updateProgress(
                        progress,
                        user,
                        scorePercentage,
                        isPassed,
                        lesson.getPointsReward());

        progressRepository.save(progress);

        Long nextLessonId = null;
        boolean hasUnlockedNext = false;

        if (progressResult.isFirstCompletion() && progressResult.isPassed()) {
            ReadingLesson nextLesson = lessonProgressService.findNextLesson(
                    lesson,
                    allLessons,
                    ReadingLesson::getOrderIndex);

            if (nextLesson != null) {
                nextLessonId = nextLesson.getId();
                hasUnlockedNext = true;
                log.info("Unlocked next lesson: {}", nextLessonId);
            }
        }

        log.info("Submit result: correct={}/{}, scorePercentage={:.2f}%, passed={}",
                correctCount, totalQuestions, scorePercentage, isPassed);

        return ReadingSubmitResponse.of(
                lessonId,
                lesson.getTitle(),
                totalQuestions,
                correctCount,
                questionService.calculateTotalScore(results),
                scorePercentage,
                isPassed,
                progressResult.getPointsEarned(),
                hasUnlockedNext,
                nextLessonId,
                results);
    }

    /**
     * [USER] Lấy danh sách bài đã hoàn thành
     */
    public List<UserReadingProgress> getCompletedLessons(Long userId) {
        log.info("Loading completed reading lessons for user {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Người dùng không tồn tại với id: " + userId);
        }

        return progressRepository.findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(userId);
    }

    /**
     * Build user lesson DTO với progress và unlock status
     */
    private ReadingLessonDTO buildUserLessonDTO(
            ReadingLesson lesson,
            List<ReadingLesson> allLessons,
            List<UserReadingProgress> allProgress,
            Long userId) {

        ReadingLessonDTO dto = convertLessonToSummaryDTO(lesson);

        Optional<UserReadingProgress> progress = allProgress.stream()
                .filter(p -> p.getLesson().getId().equals(lesson.getId()))
                .findFirst();

        if (progress.isPresent()) {
            UserReadingProgress p = progress.get();
            dto.withProgress(
                    p.getIsCompleted(),
                    p.getScorePercentage().doubleValue(),
                    p.getAttempts(),
                    p.getCompletedAt());
        }

        boolean isUnlocked = checkUnlockStatus(lesson, allLessons, userId);
        dto.setIsUnlocked(isUnlocked);
        dto.setIsAccessible(isUnlocked);

        return dto;
    }

    /**
     * Check unlock status
     */
    private boolean checkUnlockStatus(ReadingLesson lesson, List<ReadingLesson> allLessons, Long userId) {
        return lessonProgressService.isLessonUnlocked(
                lesson,
                allLessons,
                userId,
                ReadingLesson::getOrderIndex,
                ReadingLesson::getId,
                progressRepository::existsByUserIdAndLessonIdAndIsCompletedTrue);
    }

    /**
     * Tạo progress mới
     */
    private UserReadingProgress createNewProgress(User user, ReadingLesson lesson) {
        log.info("Creating new reading progress for user {} and lesson {}", user.getId(), lesson.getId());

        UserReadingProgress progress = new UserReadingProgress();
        progress.setUser(user);
        progress.setLesson(lesson);
        progress.setCreatedAt(LocalDateTime.now());

        return progress;
    }

    /**
     * Conversion methods
     */
    private ReadingLessonDTO convertLessonToSummaryDTO(ReadingLesson lesson) {
        long questionCount = questionService.countQuestionsByParent(getParentType(), lesson.getId());

        return ReadingLessonDTO.summary(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getDifficulty() != null ? lesson.getDifficulty().name() : "BEGINNER",
                lesson.getTimeLimitSeconds(),
                lesson.getOrderIndex(),
                lesson.getIsActive(),
                (int) questionCount);
    }

    private ReadingLessonDTO convertLessonToFullDTO(ReadingLesson lesson) {
        return ReadingLessonDTO.full(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getContentTranslation(),
                lesson.getDifficulty() != null ? lesson.getDifficulty().name() : "BEGINNER",
                lesson.getTimeLimitSeconds(),
                lesson.getOrderIndex(),
                lesson.getPointsReward(),
                lesson.getIsActive(),
                lesson.getCreatedAt());
    }
}