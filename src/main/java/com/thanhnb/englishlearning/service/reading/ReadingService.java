package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.dto.reading.*;
import com.thanhnb.englishlearning.dto.question.QuestionDTO;
import com.thanhnb.englishlearning.dto.question.QuestionResultDTO;
import com.thanhnb.englishlearning.entity.reading.*;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.repository.reading.*;
import com.thanhnb.englishlearning.service.common.BaseLearningService;
import com.thanhnb.englishlearning.service.question.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ✅ REFACTORED: Reading service extends BaseLearningService
 * Chỉ giữ lại logic RIÊNG của Reading module
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

    // ✅ Override: Chỉ định ParentType cho Reading
    @Override
    protected ParentType getParentType() {
        return ParentType.READING;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎓 USER LEARNING METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ [USER] Lấy tất cả bài đọc với progress + unlock status
     */
    public Page<ReadingLessonDTO> getAllLessonsForUser(Long userId, Pageable pageable) {
        log.info("📥 Loading reading lessons for user {} with pagination", userId);

        List<ReadingLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();
        List<UserReadingProgress> allProgress = progressRepository.findByUserId(userId);

        List<ReadingLessonDTO> allLessonDTOs = allLessons.stream()
                .map(lesson -> {
                    ReadingLessonDTO dto = convertLessonToSummaryDTO(lesson);

                    // Add user progress
                    Optional<UserReadingProgress> progress = allProgress.stream()
                            .filter(p -> p.getLesson().getId().equals(lesson.getId()))
                            .findFirst();

                    if (progress.isPresent()) {
                        dto.withProgress(
                                progress.get().getIsCompleted(),
                                progress.get().getScorePercentage().doubleValue(),
                                progress.get().getAttemps(),
                                progress.get().getCompletedAt());
                    }

                    // ✅ CHECK UNLOCK (dùng base method)
                    boolean isUnlocked = isLessonUnlocked(
                            lesson,
                            allLessons,
                            userId,
                            ReadingLesson::getOrderIndex,
                            ReadingLesson::getId,
                            (uId, lId) -> allProgress.stream()
                                    .anyMatch(p -> p.getLesson().getId().equals(lId)
                                            && p.getIsCompleted() != null
                                            && p.getIsCompleted()));

                    dto.setIsUnlocked(isUnlocked);
                    dto.setIsAccessible(isUnlocked);

                    return dto;
                })
                .collect(Collectors.toList());

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allLessonDTOs.size());

        List<ReadingLessonDTO> pageContent = allLessonDTOs.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allLessonDTOs.size());
    }

    /**
     * ✅ [USER] Lấy chi tiết lesson với questions + CHECK UNLOCK
     */
    public ReadingLessonDTO getLessonDetail(Long lessonId, Long userId) {
        log.info("📥 Loading lesson detail: lessonId={}, userId={}", lessonId, userId);

        ReadingLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với id: " + lessonId));

        if (!lesson.getIsActive()) {
            throw new RuntimeException("Bài đọc này hiện không khả dụng");
        }

        // ✅ CHECK UNLOCK (dùng base method)
        List<ReadingLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();
        boolean isUnlocked = checkUnlockStatus(lesson, allLessons, userId);

        if (!isUnlocked) {
            throw new RuntimeException("🔒 Bạn cần hoàn thành bài đọc trước đó để mở khóa bài này");
        }

        ReadingLessonDTO dto = convertLessonToFullDTO(lesson);

        // ✅ Load questions (dùng base method)
        List<QuestionDTO> questionDTOs = questionRepository
                .findByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType.READING, lessonId)
                .stream()
                .map(this::convertQuestionToDTO) // từ BaseLearningService
                .collect(Collectors.toList());

        dto.withQuestions(questionDTOs);

        // Load user progress
        Optional<UserReadingProgress> progress = progressRepository.findByUserIdAndLessonId(userId, lessonId);

        if (progress.isPresent()) {
            dto.withProgress(
                    progress.get().getIsCompleted(),
                    progress.get().getScorePercentage().doubleValue(),
                    progress.get().getAttemps(),
                    progress.get().getCompletedAt());
        }

        dto.withUnlockStatus(isUnlocked, isUnlocked);

        // Hide correct answers if not completed
        if (dto.getIsCompleted() == null || !dto.getIsCompleted()) {
            dto.getQuestions().forEach(q -> q.setShowCorrectAnswer(false));
        }

        log.info("✅ Loaded lesson with {} questions, unlocked={}", questionDTOs.size(), isUnlocked);
        return dto;
    }

    /**
     * ✅ [USER] Submit lesson (dùng base method cho xử lý answers)
     */
    @Transactional
    public ReadingSubmitResponse submitLesson(Long userId, Long lessonId, ReadingSubmitRequest request) {
        log.info("📤 Submit reading lesson: userId={}, lessonId={}", userId, lessonId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với id: " + userId));

        ReadingLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với id: " + lessonId));

        if (!lesson.getIsActive()) {
            throw new RuntimeException("Bài đọc này hiện không khả dụng");
        }

        // ✅ CHECK UNLOCK
        List<ReadingLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();

        if (!checkUnlockStatus(lesson, allLessons, userId)) {
            throw new RuntimeException("🔒 Bạn cần hoàn thành bài đọc trước đó để làm bài này");
        }

        // Validate answer count
        long expectedQuestions = questionService.countQuestionsByParent(ParentType.READING, lessonId);
        if (request.getAnswers() == null || request.getAnswers().size() < expectedQuestions) {
            throw new RuntimeException(String.format("Vui lòng trả lời tất cả %d câu hỏi", expectedQuestions));
        }

        // Get or create progress
        UserReadingProgress progress = progressRepository
                .findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> createNewProgress(user, lesson));

        // Track trạng thái cũ
        boolean wasAlreadyCompleted = progress.getIsCompleted() != null && progress.getIsCompleted();
        boolean isFirstCompletion = !wasAlreadyCompleted;

        // ✅ Process answers (dùng QuestionService)
        List<QuestionResultDTO> results = questionService.processAnswers(
                request.getAnswers(), ParentType.READING);

        int totalQuestions = results.size();
        int correctCount = questionService.calculateCorrectCount(results);
        double scorePercentage = questionService.calculateScorePercentage(correctCount, totalQuestions);

        // Update score (keep highest)
        BigDecimal currentScore = BigDecimal.valueOf(scorePercentage);
        if (currentScore.compareTo(progress.getScorePercentage()) > 0) {
            progress.setScorePercentage(currentScore);
            log.info("📈 Score improved: {} -> {}", progress.getScorePercentage(), currentScore);
        }

        // Update attempts
        progress.setAttemps((progress.getAttemps() != null ? progress.getAttemps() : 0) + 1);

        // ✅ Check pass (dùng base method)
        boolean passed = isPassed(scorePercentage);

        boolean hasUnlockedNext = false;
        Long nextLessonId = null;

        if (passed) {
            progress.setIsCompleted(true);
            if (!wasAlreadyCompleted) {
                progress.setCompletedAt(LocalDateTime.now());
            }

            // Award points only first completion
            if (isFirstCompletion) {
                user.setTotalPoints(user.getTotalPoints() + lesson.getPointsReward());
                userRepository.save(user);
                log.info("🎉 User {} FIRST completed reading lesson {} - earned {} points",
                        userId, lessonId, lesson.getPointsReward());

                // ✅ Find next lesson
                ReadingLesson nextLesson = findNextLesson(
                        lesson,
                        allLessons,
                        ReadingLesson::getOrderIndex);

                if (nextLesson != null) {
                    hasUnlockedNext = true;
                    nextLessonId = nextLesson.getId();
                    log.info("🔓 Unlocked next lesson: {}", nextLessonId);
                }
            }
        }

        progress.setUpdatedAt(LocalDateTime.now());
        progressRepository.save(progress);

        log.info("📊 Submit result: correct={}/{}, scorePercentage={:.2f}%, passed={}",
                correctCount, totalQuestions, scorePercentage, passed);

        // ✅ Build response
        return ReadingSubmitResponse.of(
                lessonId,
                lesson.getTitle(),
                totalQuestions,
                correctCount,
                results.stream().mapToInt(QuestionResultDTO::points).sum(),
                scorePercentage,
                passed,
                isFirstCompletion && passed ? lesson.getPointsReward() : 0,
                hasUnlockedNext,
                nextLessonId,
                results);
    }

    /**
     * [USER] Lấy danh sách bài đã hoàn thành
     */
    public List<UserReadingProgress> getCompletedLessons(Long userId) {
        log.info("📚 Loading completed reading lessons for user {}", userId);
        return progressRepository.findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(userId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔧 PRIVATE HELPER METHODS (READING-SPECIFIC)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Helper: Check unlock status (wrapper for base method)
     */
    private boolean checkUnlockStatus(ReadingLesson lesson, List<ReadingLesson> allLessons, Long userId) {
        return isLessonUnlocked(
                lesson,
                allLessons,
                userId,
                ReadingLesson::getOrderIndex,
                ReadingLesson::getId,
                (uId, lId) -> progressRepository
                        .existsByUserIdAndLessonIdAndIsCompletedTrue(uId, lId));
    }

    /**
     * Tạo progress mới
     */
    private UserReadingProgress createNewProgress(User user, ReadingLesson lesson) {
        log.info("✨ Creating new reading progress for user {} and lesson {}", user.getId(), lesson.getId());

        UserReadingProgress progress = new UserReadingProgress();
        progress.setUser(user);
        progress.setLesson(lesson);
        progress.setCreatedAt(LocalDateTime.now());
        progress.setAttemps(0);
        progress.setScorePercentage(BigDecimal.ZERO);
        progress.setIsCompleted(false);

        return progress;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔄 CONVERSION METHODS (READING-SPECIFIC)
    // ═══════════════════════════════════════════════════════════════════════

    private ReadingLessonDTO convertLessonToSummaryDTO(ReadingLesson lesson) {
        long questionCount = questionRepository.countByParentTypeAndParentId(
                ParentType.READING, lesson.getId());

        return ReadingLessonDTO.summary(
                lesson.getId(),
                lesson.getTitle(),
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
                lesson.getOrderIndex(),
                lesson.getPointsReward(),
                lesson.getIsActive(),
                lesson.getCreatedAt());
    }
}