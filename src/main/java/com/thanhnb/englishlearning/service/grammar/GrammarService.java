package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.dto.grammar.request.SubmitLessonRequest;
import com.thanhnb.englishlearning.dto.grammar.response.LessonResultResponse;
import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;
import com.thanhnb.englishlearning.entity.grammar.*;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.entity.question.QuestionOption;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.question.QuestionOptionRepository;
import com.thanhnb.englishlearning.repository.grammar.*;
import com.thanhnb.englishlearning.service.question.AnswerCheckingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarService {

    private final GrammarTopicRepository grammarTopicRepository;
    private final GrammarLessonRepository grammarLessonRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final UserGrammarProgressRepository userGrammarProgressRepository;
    private final AnswerCheckingService answerCheckingService;
    private final UserRepository userRepository;

    // ====== USER LEARNING METHODS =====

    /**
     * Lấy danh sách topics có thể truy cập với progress
     */
    public List<GrammarTopicDTO> getAccessibleTopicsForUser(Long userId) {
        List<GrammarTopic> topics = grammarTopicRepository.findByIsActiveTrueOrderByOrderIndexAsc();

        return topics.stream().map(topic -> {
            GrammarTopicDTO dto = convertTopicToDTO(topic);

            // Thêm thông tin tiến độ
            Long completedLessons = userGrammarProgressRepository.countCompletedLessonsInTopic(userId, topic.getId());
            Long totalLessons = grammarLessonRepository.countByTopicIdAndIsActive(topic.getId(), true);

            dto.setCompletedLessons(completedLessons.intValue());
            dto.setTotalLessons(totalLessons.intValue());
            dto.setIsAccessible(true);

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết topic với lessons và progress
     */
    public GrammarTopicDTO getTopicWithProgress(Long topicId, Long userId) {
        GrammarTopic topic = grammarTopicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Chủ đề không tồn tại với id: " + topicId));

        if (!topic.getIsActive()) {
            throw new RuntimeException("Chủ đề này hiện không khả dụng");
        }

        GrammarTopicDTO dto = convertTopicToDTO(topic);

        // Lấy lessons với progress
        List<GrammarLesson> lessons = grammarLessonRepository.findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(topicId);

        List<GrammarLessonDTO> lessonSummaries = lessons.stream().map(lesson -> {
            GrammarLessonDTO summary = convertLessonToSummaryDTO(lesson);

            // Thêm thông tin tiến độ
            Optional<UserGrammarProgress> progress = userGrammarProgressRepository.findByUserIdAndLessonId(userId, lesson.getId());

            summary.setIsCompleted(progress.map(UserGrammarProgress::getIsCompleted).orElse(false));
            summary.setUserScore(progress.map(p -> p.getScorePercentage().intValue()).orElse(0));

            // Kiểm tra quyền truy cập
            boolean isUnlocked = isLessonUnlocked(lesson, lessons, userId);
            summary.setIsUnlocked(isUnlocked);
            summary.setIsAccessible(isUnlocked);

            return summary;
        }).collect(Collectors.toList());

        dto.setLessons(lessonSummaries);

        // Tổng số lessons
        dto.setTotalLessons(lessons.size());
        Long completedCount = userGrammarProgressRepository.countCompletedLessonsInTopic(userId, topicId);
        dto.setCompletedLessons(completedCount.intValue());

        return dto;
    }

    /**
     * Kiểm tra lesson có được unlock không
     */
    private boolean isLessonUnlocked(GrammarLesson lesson, List<GrammarLesson> allLessons, Long userId) {
        // Lesson đầu tiên luôn unlock
        if (lesson.getOrderIndex() == 1) {
            return true;
        }

        // Tìm lesson trước đó
        GrammarLesson previousLesson = allLessons.stream()
                .filter(l -> l.getOrderIndex().equals(lesson.getOrderIndex() - 1))
                .findFirst()
                .orElse(null);

        if (previousLesson == null) {
            return true; // Không có lesson trước = unlock
        }

        // Check lesson trước đã completed chưa
        return userGrammarProgressRepository.existsByUserIdAndLessonIdAndIsCompletedTrue(
                userId, previousLesson.getId());
    }

    /**
     * Lấy nội dung lesson chi tiết
     */
    public GrammarLessonDTO getLessonContent(Long lessonId, Long userId) {
        GrammarLesson lesson = grammarLessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại với id: " + lessonId));

        if (!lesson.getIsActive()) {
            throw new RuntimeException("Bài học này hiện không khả dụng");
        }

        // Check quyền truy cập
        List<GrammarLesson> allLessons = grammarLessonRepository.findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(
                lesson.getTopic().getId());

        if (!isLessonUnlocked(lesson, allLessons, userId)) {
            throw new RuntimeException("Bạn cần hoàn thành bài học trước đó để mở khóa bài này");
        }

        GrammarLessonDTO dto = convertLessonToFullDTO(lesson);

        // Load questions từ bảng shared với ParentType.GRAMMAR
        List<Question> questions = questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(
                ParentType.GRAMMAR, lessonId);

        List<GrammarQuestionDTO> questionDTOs = questions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());

        dto.setQuestions(questionDTOs);
        dto.setQuestionCount(questionDTOs.size());

        // Thêm thông tin tiến độ
        Optional<UserGrammarProgress> progress = userGrammarProgressRepository.findByUserIdAndLessonId(userId, lessonId);
        dto.setIsCompleted(progress.map(UserGrammarProgress::getIsCompleted).orElse(false));
        dto.setUserScore(progress.map(p -> p.getScorePercentage().intValue()).orElse(0));
        dto.setUserAttempts(progress.map(UserGrammarProgress::getAttempts).orElse(0));

        // Với bài thực hành, ẩn đáp án đúng
        if (lesson.getLessonType() == LessonType.PRACTICE) {
            dto.getQuestions().forEach(q -> q.setShowCorrectAnswer(false));
        }

        return dto;
    }

    /**
     * Nộp bài và tính điểm - TỐI ƯU HÓA
     */
    @Transactional
    public LessonResultResponse submitLesson(Long userId, SubmitLessonRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với id: " + userId));

        GrammarLesson lesson = grammarLessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại với id: " + request.getLessonId()));

        // Lấy hoặc tạo progress
        UserGrammarProgress progress = userGrammarProgressRepository
                .findByUserIdAndLessonId(userId, lesson.getId())
                .orElse(new UserGrammarProgress());

        if (progress.getId() == null) {
            progress.setUser(user);
            progress.setLesson(lesson);
            progress.setCreatedAt(LocalDateTime.now());
        }

        int totalScore = 0;
        int correctAnswers = 0;
        int totalQuestions = 0;
        boolean isPassed = false;
        List<QuestionResultDTO> questionResults = null;

        // === XỬ LÝ BÀI THỰC HÀNH ===
        if (lesson.getLessonType() == LessonType.PRACTICE) {
            if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
                throw new RuntimeException("Bài thực hành cần có câu trả lời");
            }

            questionResults = processAnswers(request.getAnswers());
            totalQuestions = questionResults.size();
            correctAnswers = (int) questionResults.stream().filter(QuestionResultDTO::isCorrect).count();
            totalScore = questionResults.stream().mapToInt(QuestionResultDTO::points).sum();

            // Tính tỷ lệ đúng
            double correctRate = totalQuestions > 0 ? (double) correctAnswers / totalQuestions : 0;
            isPassed = correctRate >= 0.8; // Pass nếu đúng >= 80%

            // Cập nhật score (lưu điểm cao nhất)
            BigDecimal currentScore = BigDecimal.valueOf(correctRate * 100);
            if (currentScore.compareTo(progress.getScorePercentage()) > 0) {
                progress.setScorePercentage(currentScore);
            }
        }
        // === XỬ LÝ BÀI LÝ THUYẾT ===
        else if (lesson.getLessonType() == LessonType.THEORY) {
            if (request.getReadingTimeSecond() == null || request.getReadingTimeSecond() < lesson.getEstimatedDuration()) {
                throw new RuntimeException("Bạn cần dành ít nhất " + lesson.getEstimatedDuration() + " giây để đọc bài lý thuyết");
            }

            // Track reading time
            progress.setReadingTime((progress.getReadingTime() != null ? progress.getReadingTime() : 0) + request.getReadingTimeSecond());
            progress.setHasScrolledToEnd(true);

            totalScore = lesson.getPointsReward();
            isPassed = true;
            progress.setScorePercentage(BigDecimal.valueOf(100)); // Bài lý thuyết = 100%
        }

        // Cập nhật progress
        progress.setAttempts(progress.getAttempts() + 1);

        // Chỉ mark completed nếu pass VÀ chưa completed trước đó
        if (isPassed && !progress.getIsCompleted()) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());

            // Cộng điểm cho user (chỉ 1 lần)
            user.setTotalPoints(user.getTotalPoints() + lesson.getPointsReward());
            userRepository.save(user);

            log.info("🎉 User {} completed lesson {} - earned {} points", userId, lesson.getId(), lesson.getPointsReward());
        }

        progress.setUpdatedAt(LocalDateTime.now());
        userGrammarProgressRepository.save(progress);

        // Kiểm tra unlock lesson tiếp theo
        boolean hasUnlockedNext = false;
        Long nextLessonId = null;

        if (isPassed) {
            Optional<GrammarLesson> nextLesson = grammarLessonRepository.findNextLessonInTopic(
                    lesson.getTopic().getId(), lesson.getOrderIndex());

            if (nextLesson.isPresent()) {
                hasUnlockedNext = true;
                nextLessonId = nextLesson.get().getId();
                log.info("🔓 Unlocked next lesson: {}", nextLessonId);
            }
        }

        log.info("📊 User {} submitted lesson {}: score={}/{}, passed={}", userId, lesson.getId(), correctAnswers, totalQuestions, isPassed);

        // Sử dụng Record constructor
        return new LessonResultResponse(
                lesson.getId(),
                lesson.getTitle(),
                totalQuestions,
                correctAnswers,
                totalScore,
                isPassed ? lesson.getPointsReward() : 0,
                isPassed,
                hasUnlockedNext,
                nextLessonId,
                questionResults
        );
    }

    /**
     * Lấy random questions (cho mini quiz)
     */
    public List<GrammarQuestionDTO> getRandomQuestions(Long lessonId, Integer questionCount, Long userId) {
        GrammarLesson lesson = grammarLessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson không tồn tại"));

        if (!lesson.getIsActive()) {
            throw new RuntimeException("Lesson này hiện không khả dụng");
        }

        // Check access
        List<GrammarLesson> allLessons = grammarLessonRepository.findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(
                lesson.getTopic().getId());
        if (!isLessonUnlocked(lesson, allLessons, userId)) {
            throw new RuntimeException("Bạn chưa thể truy cập lesson này");
        }

        if (questionCount <= 0) {
            throw new IllegalArgumentException("Số lượng câu hỏi phải > 0");
        }

        long totalQuestions = questionRepository.countByParentTypeAndParentId(ParentType.GRAMMAR, lessonId);
        if (questionCount > totalQuestions) {
            questionCount = (int) totalQuestions;
        }

        List<Question> questions = questionRepository.findGrammarQuestionsByLessonId(lessonId);

        // Shuffle và lấy số lượng cần thiết
        Collections.shuffle(questions);
        questions = questions.stream().limit(questionCount).collect(Collectors.toList());

        return questions.stream()
                .map(q -> {
                    GrammarQuestionDTO dto = convertQuestionToDTO(q);
                    dto.setShowCorrectAnswer(false); // Ẩn đáp án
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy user progress summary
     */
    public List<UserGrammarProgressDTO> getUserProgressSummary(Long userId) {
        return userGrammarProgressRepository.findUserProgressWithLessonDetails(userId)
                .stream()
                .map(this::convertProgressToDTO)
                .collect(Collectors.toList());
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Xử lý câu trả lời và trả về kết quả chi tiết
     */
    private List<QuestionResultDTO> processAnswers(List<SubmitAnswerRequest> answers) {
        return answers.stream().map(answerRequest -> {
            Question question = questionRepository.findByIdWithOptions(answerRequest.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("Question không tồn tại với id: " + answerRequest.getQuestionId()));

            if (question.getParentType() != ParentType.GRAMMAR) {
                throw new RuntimeException("Question này không thuộc Grammar module");
            }

            boolean isCorrect = answerCheckingService.checkAnswer(question, answerRequest);
            int points = isCorrect ? question.getPoints() : 0;
            String hint = isCorrect ? null : answerCheckingService.generateHint(question, answerRequest);

            // Sử dụng Record constructor
            return new QuestionResultDTO(
                    question.getId(),
                    question.getQuestionText(),
                    answerRequest.getAnswer(),
                    question.getCorrectAnswer(),
                    isCorrect,
                    question.getExplanation(),
                    points,
                    hint
            );
        }).collect(Collectors.toList());
    }

    // ===== CONVERSION METHODS - TỐI ƯU HÓA =====

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

    /**
     * Convert sang Summary DTO (cho danh sách)
     */
    private GrammarLessonDTO convertLessonToSummaryDTO(GrammarLesson lesson) {
        long questionCount = questionRepository.countByParentTypeAndParentId(ParentType.GRAMMAR, lesson.getId());

        // Sử dụng factory method
        return GrammarLessonDTO.summary(
                lesson.getId(),
                lesson.getTopic().getId(),
                lesson.getTitle(),
                lesson.getLessonType(),
                lesson.getOrderIndex(),
                lesson.getPointsReward(),
                lesson.getIsActive(),
                (int) questionCount
        );
    }

    /**
     * Convert sang Full DTO (cho chi tiết)
     */
    private GrammarLessonDTO convertLessonToFullDTO(GrammarLesson lesson) {
        // Sử dụng factory method
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
                lesson.getTopic().getName()
        );
    }

    private GrammarQuestionDTO convertQuestionToDTO(Question question) {
        // Load options
        List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(question.getId());

        List<GrammarQuestionOptionDTO> optionDTOs = options.stream()
                .map(option -> new GrammarQuestionOptionDTO(
                        option.getId(),
                        question.getId(),
                        option.getOptionText(),
                        option.getIsCorrect(),
                        option.getOrderIndex()
                ))
                .collect(Collectors.toList());

        // Shuffle options cho MULTIPLE_CHOICE
        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE && !optionDTOs.isEmpty()) {
            Collections.shuffle(optionDTOs);
            // Reset order index sau shuffle
            for (int i = 0; i < optionDTOs.size(); i++) {
                optionDTOs.get(i).setOrderIndex(i + 1);
            }
        }

        GrammarQuestionDTO dto = new GrammarQuestionDTO();
        dto.setId(question.getId());
        dto.setLessonId(question.getParentId());
        dto.setQuestionText(question.getQuestionText());
        dto.setQuestionType(question.getQuestionType());
        dto.setCorrectAnswer(question.getCorrectAnswer());
        dto.setExplanation(question.getExplanation());
        dto.setPoints(question.getPoints());
        dto.setOrderIndex(question.getOrderIndex());
        dto.setCreatedAt(question.getCreatedAt());
        dto.setOptions(optionDTOs.isEmpty() ? null : optionDTOs);
        dto.setShowCorrectAnswer(true);

        return dto;
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