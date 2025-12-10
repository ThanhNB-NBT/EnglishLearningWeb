package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.dto.grammar.request.SubmitLessonRequest;
import com.thanhnb.englishlearning.dto.grammar.response.LessonResultResponse;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
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

import io.jsonwebtoken.lang.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REFACTORED: Grammar service - Metadata-based Architecture
 * - Không còn QuestionOption entity
 * - Dùng QuestionResponseDTO (có metadata JSONB)
 * - Tích hợp với QuestionService, QuestionConversionService,
 * QuestionValidationService
 * - Auto shuffle options cho MULTIPLE_CHOICE
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarService extends BaseLearningService<GrammarLesson, UserGrammarProgress> {

        private final GrammarTopicRepository grammarTopicRepository;
        private final GrammarLessonRepository grammarLessonRepository;
        private final UserGrammarProgressRepository userGrammarProgressRepository;
        private final UserRepository userRepository;
        private final QuestionService questionService;
        private final LessonProgressService progressService;

        private static final int SUBMIT_COOLDOWN_SECONDS = 10;
        private static final double PASS_THRESHOLD = 80.0;

        @Override
        protected ParentType getParentType() {
                return ParentType.GRAMMAR;
        }

        // ═══════════════════════════════════════════════════════════════════════
        // USER LEARNING METHODS
        // ═══════════════════════════════════════════════════════════════════════

        /**
         * [USER] Lấy tất cả topics với lessons và progress
         */
        public List<GrammarTopicDTO> getAccessibleTopicsForUser(Long userId) {
                log.info("Loading all topics with lessons for user {}", userId);

                // Lấy tất cả topics active
                List<GrammarTopic> topics = grammarTopicRepository.findByIsActiveTrueOrderByOrderIndexAsc();
                if (topics.isEmpty())
                        return Collections.emptyList();

                List<Long> topicIds = topics.stream().map(GrammarTopic::getId).toList();

                // Lấy tất cả progress của user trong các topics này
                List<UserGrammarProgress> allProgress = userGrammarProgressRepository.findByUserIdAndTopicIdIn(userId,
                                topicIds);

                // Gom nhóm progress theo topicId
                Map<Long, List<UserGrammarProgress>> progressByTopicMap = allProgress.stream()
                                .collect(Collectors.groupingBy(p -> p.getLesson().getTopic().getId()));

                // Map dữ liệu (Xử lí progress cho từng lesson trong topic)
                return topics.stream().map(topic -> {
                        GrammarTopicDTO dto = convertTopicToDTO(topic);

                        // Lấy list progress tương ứng với topic từ map
                        List<UserGrammarProgress> topicProgress = progressByTopicMap.getOrDefault(topic.getId(),
                                        Collections.emptyList());

                        long completedLessons = topicProgress.stream().filter(UserGrammarProgress::getIsCompleted)
                                        .count();
                        int totalLessons = topic.getLessons().size();

                        dto.setCompletedLessons((int) completedLessons);
                        dto.setTotalLessons(totalLessons);
                        dto.setIsAccessible(true);

                        List<GrammarLesson> lessons = grammarLessonRepository
                                        .findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(topic.getId());

                        log.debug("Topic '{}' has {} lessons", topic.getName(), lessons.size());

                        List<GrammarLessonDTO> lessonSummaries = lessons.stream().map(lesson -> {
                                GrammarLessonDTO summary = convertLessonToSummaryDTO(lesson);

                                Optional<UserGrammarProgress> progress = userGrammarProgressRepository
                                                .findByUserIdAndLessonId(userId, lesson.getId());

                                summary.setIsCompleted(progress.map(UserGrammarProgress::getIsCompleted).orElse(false));
                                summary.setUserScore(progress.map(p -> p.getScorePercentage().intValue()).orElse(0));
                                summary.setUserAttempts(progress.map(UserGrammarProgress::getAttempts).orElse(0));

                                boolean isUnlocked = checkUnlockStatus(lesson, lessons, userId);
                                summary.setIsUnlocked(isUnlocked);
                                summary.setIsAccessible(isUnlocked);

                                return summary;
                        }).collect(Collectors.toList());

                        dto.setLessons(lessonSummaries);

                        log.debug("Topic '{}': {}/{} lessons completed",
                                        topic.getName(), completedLessons, totalLessons);

                        return dto;
                }).collect(Collectors.toList());
        }

        /**
         * [USER] Lấy chi tiết 1 topic với progress
         */
        public GrammarTopicDTO getTopicWithProgress(Long topicId, Long userId) {
                log.info("Loading topic {} for user {}", topicId, userId);

                GrammarTopic topic = grammarTopicRepository.findById(topicId)
                                .orElseThrow(() -> new RuntimeException("Chủ đề không tồn tại với id: " + topicId));

                if (!topic.getIsActive()) {
                        throw new RuntimeException("Chủ đề này hiện không khả dụng");
                }

                GrammarTopicDTO dto = convertTopicToDTO(topic);

                List<GrammarLesson> lessons = grammarLessonRepository
                                .findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(topicId);

                List<GrammarLessonDTO> lessonSummaries = lessons.stream().map(lesson -> {
                        GrammarLessonDTO summary = convertLessonToSummaryDTO(lesson);

                        Optional<UserGrammarProgress> progress = userGrammarProgressRepository
                                        .findByUserIdAndLessonId(userId, lesson.getId());

                        summary.setIsCompleted(progress.map(UserGrammarProgress::getIsCompleted).orElse(false));
                        summary.setUserScore(progress.map(p -> p.getScorePercentage().intValue()).orElse(0));
                        summary.setUserAttempts(progress.map(UserGrammarProgress::getAttempts).orElse(0));

                        boolean isUnlocked = checkUnlockStatus(lesson, lessons, userId);
                        summary.setIsUnlocked(isUnlocked);
                        summary.setIsAccessible(isUnlocked);

                        return summary;
                }).collect(Collectors.toList());

                dto.setLessons(lessonSummaries);
                dto.setTotalLessons(lessons.size());

                Long completedCount = userGrammarProgressRepository
                                .countCompletedLessonsInTopic(userId, topicId);
                dto.setCompletedLessons(completedCount.intValue());

                return dto;
        }

        /**
         * [USER] Lấy nội dung lesson chi tiết với questions (metadata-based)
         * Auto shuffle options cho MULTIPLE_CHOICE
         */
        public GrammarLessonDTO getLessonContent(Long lessonId, Long userId) {
                log.info("Loading lesson content: lessonId={}, userId={}", lessonId, userId);

                GrammarLesson lesson = grammarLessonRepository.findById(lessonId)
                                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại với id: " + lessonId));

                if (!lesson.getIsActive()) {
                        throw new RuntimeException("Bài học này hiện không khả dụng");
                }

                List<GrammarLesson> allLessons = grammarLessonRepository
                                .findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(lesson.getTopic().getId());

                if (!checkUnlockStatus(lesson, allLessons, userId)) {
                        throw new RuntimeException("Bạn cần hoàn thành bài học trước đó để mở khóa bài này");
                }

                GrammarLessonDTO dto = convertLessonToFullDTO(lesson);

                // Load questions (metadata-based) với shuffle options
                List<Question> questions = questionService.loadQuestionsByParent(
                                ParentType.GRAMMAR, lessonId);

                // Convert to QuestionResponseDTO (auto shuffle options cho MULTIPLE_CHOICE)
                List<QuestionResponseDTO> questionDTOs = questionService.convertToDTOs(questions, true);

                dto.setQuestions(questionDTOs);
                dto.setQuestionCount(questionDTOs.size());

                Optional<UserGrammarProgress> progress = userGrammarProgressRepository
                                .findByUserIdAndLessonId(userId, lessonId);

                dto.setIsCompleted(progress.map(UserGrammarProgress::getIsCompleted).orElse(false));
                dto.setUserScore(progress.map(p -> p.getScorePercentage().intValue()).orElse(0));
                dto.setUserAttempts(progress.map(UserGrammarProgress::getAttempts).orElse(0));

                log.info("Loaded lesson with {} questions", questionDTOs.size());
                return dto;
        }

        /**
         * [USER] Nộp bài và tính điểm - Metadata-based validation
         */
        @Transactional
        public LessonResultResponse submitLesson(Long userId, SubmitLessonRequest request) {
                if (request.getLessonId() == null) {
                        throw new RuntimeException("Lesson ID không được để trống");
                }

                log.info("Submit lesson: userId={}, lessonId={}", userId, request.getLessonId());

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với id: " + userId));

                GrammarLesson lesson = grammarLessonRepository.findById(request.getLessonId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Bài học không tồn tại với id: " + request.getLessonId()));

                log.info("Lesson type: {}", lesson.getLessonType());

                List<GrammarLesson> allLessons = grammarLessonRepository
                                .findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(lesson.getTopic().getId());

                if (!checkUnlockStatus(lesson, allLessons, userId)) {
                        throw new RuntimeException("Bài học này chưa được mở khóa. Vui lòng hoàn thành bài trước đó.");
                }

                UserGrammarProgress progress = userGrammarProgressRepository
                                .findByUserIdAndLessonId(userId, lesson.getId())
                                .orElseGet(() -> createNewProgress(user, lesson));

                if (progress.getId() != null) {
                        long cooldown = progressService.checkSubmitCooldown(
                                        progress.getUpdatedAt(), SUBMIT_COOLDOWN_SECONDS);

                        if (cooldown > 0) {
                                throw new RuntimeException("Vui lòng đợi " + cooldown + " giây trước khi nộp lại");
                        }
                }

                int totalScore = 0;
                int correctAnswers = 0;
                int totalQuestions = 0;
                boolean isPassed = false;
                List<QuestionResultDTO> questionResults = null;
                double scorePercentage = 0;

                if (lesson.getLessonType() == LessonType.PRACTICE) {
                        // Validate answer count
                        questionService.validateAnswerCount(
                                        request.getAnswers(), ParentType.GRAMMAR, lesson.getId());

                        // Process answers
                        questionResults = questionService.processAnswers(
                                        request.getAnswers(), ParentType.GRAMMAR);

                        totalQuestions = questionResults.size();
                        correctAnswers = questionService.calculateCorrectCount(questionResults);
                        totalScore = questionService.calculateTotalScore(questionResults);
                        scorePercentage = questionService.calculateScorePercentage(
                                        correctAnswers, totalQuestions);

                        isPassed = isPassed(scorePercentage, PASS_THRESHOLD);

                } else if (lesson.getLessonType() == LessonType.THEORY) {
                        if (request.getReadingTimeSecond() == null
                                        || request.getReadingTimeSecond() < lesson.getTimeLimitSeconds()) {
                                throw new RuntimeException(
                                                "Bạn cần dành ít nhất " + lesson.getTimeLimitSeconds()
                                                                + " giây để đọc bài lý thuyết");
                        }

                        Integer currentReadingTime = progress.getReadingTime() != null
                                        ? progress.getReadingTime()
                                        : 0;
                        progress.setReadingTime(currentReadingTime + request.getReadingTimeSecond());
                        progress.setHasScrolledToEnd(true);

                        totalScore = lesson.getPointsReward();
                        scorePercentage = 100.0;
                        isPassed = true;
                }

                LessonProgressService.ProgressUpdateResult result = progressService.updateProgress(
                                progress,
                                user,
                                scorePercentage,
                                isPassed,
                                lesson.getPointsReward());

                UserGrammarProgress savedProgress = userGrammarProgressRepository.save(progress);
                log.info("Progress saved: attempts={}, score={}, completed={}",
                                savedProgress.getAttempts(), savedProgress.getScorePercentage(),
                                savedProgress.getIsCompleted());

                boolean hasUnlockedNext = false;
                Long nextLessonId = null;

                if (result.isFirstCompletion() && isPassed) {
                        GrammarLesson nextLesson = findNextLesson(
                                        lesson, allLessons, GrammarLesson::getOrderIndex);

                        if (nextLesson != null) {
                                hasUnlockedNext = true;
                                nextLessonId = nextLesson.getId();
                                log.info("Unlocked next lesson: {}", nextLessonId);
                        }
                }

                log.info("Submit result: correct={}/{}, passed={}, pointsEarned={}",
                                correctAnswers, totalQuestions, isPassed, result.getPointsEarned());

                return new LessonResultResponse(
                                lesson.getId(),
                                lesson.getTitle(),
                                totalQuestions,
                                correctAnswers,
                                totalScore,
                                result.getPointsEarned(),
                                isPassed,
                                hasUnlockedNext,
                                nextLessonId,
                                questionResults);
        }

        public List<UserGrammarProgressDTO> getUserProgressSummary(Long userId) {
                log.info("Loading user progress summary for user {}", userId);

                return userGrammarProgressRepository.findUserProgressWithLessonDetails(userId)
                                .stream()
                                .map(this::convertProgressToDTO)
                                .collect(Collectors.toList());
        }

        // ═══════════════════════════════════════════════════════════════════════
        // PRIVATE HELPER METHODS
        // ═══════════════════════════════════════════════════════════════════════

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
                                                .existsByUserIdAndLessonIdAndIsCompletedTrue(uId, lId));
        }

        private UserGrammarProgress createNewProgress(User user, GrammarLesson lesson) {
                log.info("Creating new grammar progress for user {} and lesson {}",
                                user.getId(), lesson.getId());

                UserGrammarProgress progress = new UserGrammarProgress();
                progress.setUser(user);
                progress.setLesson(lesson);
                progress.setCreatedAt(LocalDateTime.now());
                progress.setReadingTime(0);
                progress.setHasScrolledToEnd(false);

                progressService.initializeProgress(progress);

                return progress;
        }

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
                                lesson.getTimeLimitSeconds(),
                                lesson.getIsActive(),
                                lesson.getCreatedAt(),
                                lesson.getMetadata(),
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
}