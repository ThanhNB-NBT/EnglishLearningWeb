package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.dto.grammar.request.SubmitLessonRequest;
import com.thanhnb.englishlearning.dto.grammar.response.LessonResultResponse;
import com.thanhnb.englishlearning.dto.question.QuestionDTO;
import com.thanhnb.englishlearning.dto.question.QuestionResultDTO;
import com.thanhnb.englishlearning.entity.grammar.*;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.repository.grammar.*;
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
 * ✅ REFACTORED: Grammar service - CHỈ chứa logic RIÊNG của Grammar
 * Logic chung đã extract sang:
 * - BaseLearningService: Unlock logic, scoring
 * - QuestionService: Question processing
 * - LessonProgressService: Progress tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarService extends BaseLearningService<GrammarLesson, UserGrammarProgress> {

    // ===== REPOSITORIES =====
    private final GrammarTopicRepository grammarTopicRepository;
    private final GrammarLessonRepository grammarLessonRepository;
    private final UserGrammarProgressRepository userGrammarProgressRepository;
    private final UserRepository userRepository;

    // ===== SHARED SERVICES =====
    private final QuestionService questionService;
    private final LessonProgressService progressService;

    // ===== CONSTANTS =====
    private static final int SUBMIT_COOLDOWN_SECONDS = 30;
    private static final double PASS_THRESHOLD = 80.0;

    // ✅ Override: Chỉ định ParentType cho Grammar
    @Override
    protected ParentType getParentType() {
        return ParentType.GRAMMAR;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎓 USER LEARNING METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ [USER] Lấy tất cả topics với lessons và progress
     * Dùng cho: Sidebar, Topic List, Overview
     */
    public List<GrammarTopicDTO> getAccessibleTopicsForUser(Long userId) {
        log.info("📥 Loading all topics with lessons for user {}", userId);

        List<GrammarTopic> topics = grammarTopicRepository.findByIsActiveTrueOrderByOrderIndexAsc();

        return topics.stream().map(topic -> {
            GrammarTopicDTO dto = convertTopicToDTO(topic);

            // Calculate topic progress
            Long completedLessons = userGrammarProgressRepository
                    .countCompletedLessonsInTopic(userId, topic.getId());
            Long totalLessons = grammarLessonRepository
                    .countByTopicIdAndIsActive(topic.getId(), true);

            dto.setCompletedLessons(completedLessons.intValue());
            dto.setTotalLessons(totalLessons.intValue());
            dto.setIsAccessible(true);

            // Load lessons với progress
            List<GrammarLesson> lessons = grammarLessonRepository
                    .findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(topic.getId());

            log.debug("📚 Topic '{}' has {} lessons", topic.getName(), lessons.size());

            List<GrammarLessonDTO> lessonSummaries = lessons.stream().map(lesson -> {
                GrammarLessonDTO summary = convertLessonToSummaryDTO(lesson);

                // Get progress for each lesson
                Optional<UserGrammarProgress> progress = userGrammarProgressRepository
                        .findByUserIdAndLessonId(userId, lesson.getId());

                summary.setIsCompleted(progress.map(UserGrammarProgress::getIsCompleted).orElse(false));
                summary.setUserScore(progress.map(p -> p.getScorePercentage().intValue()).orElse(0));
                summary.setUserAttempts(progress.map(UserGrammarProgress::getAttempts).orElse(0));

                // ✅ Check unlock status (dùng base method)
                boolean isUnlocked = checkUnlockStatus(lesson, lessons, userId);
                summary.setIsUnlocked(isUnlocked);
                summary.setIsAccessible(isUnlocked);

                return summary;
            }).collect(Collectors.toList());

            dto.setLessons(lessonSummaries);

            log.debug("✅ Topic '{}': {}/{} lessons completed",
                    topic.getName(), completedLessons, totalLessons);

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * ✅ [USER] Lấy chi tiết 1 topic với progress
     */
    public GrammarTopicDTO getTopicWithProgress(Long topicId, Long userId) {
        log.info("📥 Loading topic {} for user {}", topicId, userId);

        GrammarTopic topic = grammarTopicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Chủ đề không tồn tại với id: " + topicId));

        if (!topic.getIsActive()) {
            throw new RuntimeException("Chủ đề này hiện không khả dụng");
        }

        GrammarTopicDTO dto = convertTopicToDTO(topic);

        // Lấy lessons với progress
        List<GrammarLesson> lessons = grammarLessonRepository
                .findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(topicId);

        List<GrammarLessonDTO> lessonSummaries = lessons.stream().map(lesson -> {
            GrammarLessonDTO summary = convertLessonToSummaryDTO(lesson);

            // Thêm thông tin tiến độ
            Optional<UserGrammarProgress> progress = userGrammarProgressRepository
                    .findByUserIdAndLessonId(userId, lesson.getId());

            summary.setIsCompleted(progress.map(UserGrammarProgress::getIsCompleted).orElse(false));
            summary.setUserScore(progress.map(p -> p.getScorePercentage().intValue()).orElse(0));
            summary.setUserAttempts(progress.map(UserGrammarProgress::getAttempts).orElse(0));

            // ✅ Kiểm tra quyền truy cập (dùng base method)
            boolean isUnlocked = checkUnlockStatus(lesson, lessons, userId);
            summary.setIsUnlocked(isUnlocked);
            summary.setIsAccessible(isUnlocked);

            return summary;
        }).collect(Collectors.toList());

        dto.setLessons(lessonSummaries);

        // Tổng số lessons
        dto.setTotalLessons(lessons.size());
        Long completedCount = userGrammarProgressRepository
                .countCompletedLessonsInTopic(userId, topicId);
        dto.setCompletedLessons(completedCount.intValue());

        return dto;
    }

    /**
     * ✅ [USER] Lấy nội dung lesson chi tiết với questions
     */
    public GrammarLessonDTO getLessonContent(Long lessonId, Long userId) {
        log.info("📥 Loading lesson content: lessonId={}, userId={}", lessonId, userId);

        GrammarLesson lesson = grammarLessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại với id: " + lessonId));

        if (!lesson.getIsActive()) {
            throw new RuntimeException("Bài học này hiện không khả dụng");
        }

        // ✅ Check quyền truy cập (dùng base method)
        List<GrammarLesson> allLessons = grammarLessonRepository
                .findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(lesson.getTopic().getId());

        if (!checkUnlockStatus(lesson, allLessons, userId)) {
            throw new RuntimeException("🔒 Bạn cần hoàn thành bài học trước đó để mở khóa bài này");
        }

        GrammarLessonDTO dto = convertLessonToFullDTO(lesson);

        // ✅ Load questions (dùng QuestionService)
        List<Question> questions = questionService.loadQuestionsByParent(
                ParentType.GRAMMAR, lessonId);

        List<QuestionDTO> questionDTOs = questions.stream()
                .map(q -> questionService.convertToDTO(q))
                .collect(Collectors.toList());

        dto.setQuestions(questionDTOs);
        dto.setQuestionCount(questionDTOs.size());

        // Thêm thông tin tiến độ
        Optional<UserGrammarProgress> progress = userGrammarProgressRepository
                .findByUserIdAndLessonId(userId, lessonId);

        dto.setIsCompleted(progress.map(UserGrammarProgress::getIsCompleted).orElse(false));
        dto.setUserScore(progress.map(p -> p.getScorePercentage().intValue()).orElse(0));
        dto.setUserAttempts(progress.map(UserGrammarProgress::getAttempts).orElse(0));

        // ✅ Với bài PRACTICE, ẩn đáp án đúng
        if (lesson.getLessonType() == LessonType.PRACTICE) {
            dto.getQuestions().forEach(q -> q.setShowCorrectAnswer(false));
        }

        log.info("✅ Loaded lesson with {} questions", questionDTOs.size());
        return dto;
    }

    /**
     * ✅ [USER] Nộp bài và tính điểm - REFACTORED với shared services
     */
    @Transactional
    public LessonResultResponse submitLesson(Long userId, SubmitLessonRequest request) {
        if (request.getLessonId() == null) {
            throw new RuntimeException("Lesson ID không được để trống");
        }

        log.info("📤 Submit lesson: userId={}, lessonId={}", userId, request.getLessonId());

        // ===== LOAD ENTITIES =====
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với id: " + userId));

        GrammarLesson lesson = grammarLessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại với id: " + request.getLessonId()));

        log.info("📝 Lesson type: {}", lesson.getLessonType());

        // ===== VALIDATE UNLOCK =====
        List<GrammarLesson> allLessons = grammarLessonRepository
                .findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(lesson.getTopic().getId());

        if (!checkUnlockStatus(lesson, allLessons, userId)) {
            throw new RuntimeException("🔒 Bài học này chưa được mở khóa. Vui lòng hoàn thành bài trước đó.");
        }

        // ===== GET OR CREATE PROGRESS =====
        UserGrammarProgress progress = userGrammarProgressRepository
                .findByUserIdAndLessonId(userId, lesson.getId())
                .orElseGet(() -> createNewProgress(user, lesson));

        // ===== ANTI-SPAM CHECK =====
        long cooldown = progressService.checkSubmitCooldown(
                progress.getUpdatedAt(), SUBMIT_COOLDOWN_SECONDS);
        
        if (cooldown > 0) {
            throw new RuntimeException(
                    "Vui lòng đợi " + cooldown + " giây trước khi nộp lại");
        }

        // ===== PROCESS BASED ON LESSON TYPE =====
        int totalScore = 0;
        int correctAnswers = 0;
        int totalQuestions = 0;
        boolean isPassed = false;
        List<QuestionResultDTO> questionResults = null;
        double scorePercentage = 0;

        if (lesson.getLessonType() == LessonType.PRACTICE) {
            // ✅ BÀI PRACTICE: Process answers
            questionService.validateAnswerCount(
                    request.getAnswers(), ParentType.GRAMMAR, lesson.getId());

            questionResults = questionService.processAnswers(
                    request.getAnswers(), ParentType.GRAMMAR);

            totalQuestions = questionResults.size();
            correctAnswers = questionService.calculateCorrectCount(questionResults);
            totalScore = questionService.calculateTotalScore(questionResults);
            scorePercentage = questionService.calculateScorePercentage(
                    correctAnswers, totalQuestions);

            isPassed = isPassed(scorePercentage, PASS_THRESHOLD);

        } else if (lesson.getLessonType() == LessonType.THEORY) {
            // ✅ BÀI THEORY: Check reading time
            if (request.getReadingTimeSecond() == null 
                    || request.getReadingTimeSecond() < lesson.getEstimatedDuration()) {
                throw new RuntimeException(
                        "Bạn cần dành ít nhất " + lesson.getEstimatedDuration() 
                        + " giây để đọc bài lý thuyết");
            }

            // Update reading time
            Integer currentReadingTime = progress.getReadingTime() != null 
                    ? progress.getReadingTime() : 0;
            progress.setReadingTime(currentReadingTime + request.getReadingTimeSecond());
            progress.setHasScrolledToEnd(true);

            totalScore = lesson.getPointsReward();
            scorePercentage = 100.0;
            isPassed = true;
        }

        // ===== UPDATE PROGRESS (dùng LessonProgressService) =====
        LessonProgressService.ProgressUpdateResult result = progressService.updateProgress(
                progress,
                user,
                scorePercentage,
                isPassed,
                lesson.getPointsReward()
        );

        // ===== SAVE PROGRESS =====
        UserGrammarProgress savedProgress = userGrammarProgressRepository.save(progress);
        log.info("✅ Progress saved: attempts={}, score={}, completed={}",
                savedProgress.getAttempts(), savedProgress.getScorePercentage(), 
                savedProgress.getIsCompleted());

        // ===== CHECK UNLOCK NEXT LESSON =====
        boolean hasUnlockedNext = false;
        Long nextLessonId = null;

        if (result.isFirstCompletion() && isPassed) {
            GrammarLesson nextLesson = findNextLesson(
                    lesson, allLessons, GrammarLesson::getOrderIndex);

            if (nextLesson != null) {
                hasUnlockedNext = true;
                nextLessonId = nextLesson.getId();
                log.info("🔓 Unlocked next lesson: {}", nextLessonId);
            }
        }

        log.info("📊 Submit result: correct={}/{}, passed={}, pointsEarned={}",
                correctAnswers, totalQuestions, isPassed, result.getPointsEarned());

        // ===== BUILD RESPONSE =====
        return new LessonResultResponse(
                lesson.getId(),
                lesson.getTitle(),
                totalQuestions,
                correctAnswers,
                totalScore,
                result.getPointsEarned(), // CHỈ > 0 nếu first completion
                isPassed,
                hasUnlockedNext,
                nextLessonId,
                questionResults
        );
    }

    /**
     * ✅ [USER] Lấy user progress summary
     */
    public List<UserGrammarProgressDTO> getUserProgressSummary(Long userId) {
        log.info("📚 Loading user progress summary for user {}", userId);
        
        return userGrammarProgressRepository.findUserProgressWithLessonDetails(userId)
                .stream()
                .map(this::convertProgressToDTO)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔧 PRIVATE HELPER METHODS (GRAMMAR-SPECIFIC)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Helper: Check unlock status (wrapper for base method)
     */
    private boolean checkUnlockStatus(
            GrammarLesson lesson, 
            List<GrammarLesson> allLessons, 
            Long userId) {

        return isLessonUnlocked(
                lesson,
                allLessons,
                userId,
                GrammarLesson::getOrderIndex,
                GrammarLesson::getId,
                (uId, lId) -> userGrammarProgressRepository
                        .existsByUserIdAndLessonIdAndIsCompletedTrue(uId, lId)
        );
    }

    /**
     * Tạo progress mới cho user
     */
    private UserGrammarProgress createNewProgress(User user, GrammarLesson lesson) {
        log.info("✨ Creating new grammar progress for user {} and lesson {}", 
                user.getId(), lesson.getId());

        UserGrammarProgress progress = new UserGrammarProgress();
        progress.setUser(user);
        progress.setLesson(lesson);
        progress.setCreatedAt(LocalDateTime.now());
        progress.setReadingTime(0);
        progress.setHasScrolledToEnd(false);

        // ✅ Initialize progress (dùng LessonProgressService)
        progressService.initializeProgress(progress);

        return progress;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔄 CONVERSION METHODS (GRAMMAR-SPECIFIC)
    // ═══════════════════════════════════════════════════════════════════════

    private GrammarTopicDTO convertTopicToDTO(GrammarTopic topic) {
        GrammarTopicDTO dto = new GrammarTopicDTO();
        dto.setId(topic.getId());
        dto.setName(topic.getName());
        dto.setDescription(topic.getDescription());
        dto.setLevelRequired(topic.getLevelRequired());
        dto.setOrderIndex(topic.getOrderIndex());
        dto.setIsActive(topic.getIsActive());
        dto.setCreatedAt(topic.getCreatedAt());
        return dto;
    }

    private GrammarLessonDTO convertLessonToSummaryDTO(GrammarLesson lesson) {
        long questionCount = questionService.countQuestionsByParent(
                ParentType.GRAMMAR, lesson.getId());

        return GrammarLessonDTO.summary(
                lesson.getId(),
                lesson.getTopic().getId(),
                lesson.getTitle(),
                lesson.getLessonType(),
                lesson.getOrderIndex(),
                lesson.getPointsReward(),
                lesson.getIsActive(),
                (int) questionCount);
    }

    private GrammarLessonDTO convertLessonToFullDTO(GrammarLesson lesson) {
        return GrammarLessonDTO.full(
                lesson.getId(),
                lesson.getTopic().getId(),
                lesson.getTitle(),
                lesson.getLessonType(),
                lesson.getContent(),
                lesson.getOrderIndex(),
                lesson.getPointsReward(),
                lesson.getEstimatedDuration(),
                lesson.getIsActive(),
                lesson.getCreatedAt(),
                lesson.getTopic().getName());
    }

    private UserGrammarProgressDTO convertProgressToDTO(UserGrammarProgress progress) {
        UserGrammarProgressDTO dto = new UserGrammarProgressDTO();
        dto.setId(progress.getId());
        dto.setUserId(progress.getUser().getId());
        dto.setLessonId(progress.getLesson().getId());
        dto.setLessonTitle(progress.getLesson().getTitle());
        dto.setTopicName(progress.getLesson().getTopic().getName());
        dto.setIsCompleted(progress.getIsCompleted());
        dto.setScorePercentage(progress.getScorePercentage().intValue());
        dto.setAttempts(progress.getAttempts());
        dto.setCompletedAt(progress.getCompletedAt());
        dto.setCreatedAt(progress.getCreatedAt());
        dto.setUpdatedAt(progress.getUpdatedAt());
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔧 IMPLEMENT LessonProgressService.LessonProgress (for UserGrammarProgress)
    // ═══════════════════════════════════════════════════════════════════════
    // Note: UserGrammarProgress entity cần implement interface này
    // Hoặc tạo adapter wrapper
}