package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.dto.grammar.request.SubmitAnswerRequest;
import com.thanhnb.englishlearning.dto.grammar.request.SubmitLessonRequest;
import com.thanhnb.englishlearning.dto.grammar.response.LessonResultResponse;
import com.thanhnb.englishlearning.entity.grammar.*;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.repository.grammar.*;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.enums.QuestionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final GrammarQuestionRepository grammarQuestionRepository;
    private final UserGrammarProgressRepository userGrammarProgressRepository;
    private final AnswerCheckingService answerCheckingService;
    private final UserRepository userRepository;

    // ====== USER LEARNING METHODS =====
    // Lấy danh sách topic 
    public List<GrammarTopicDTO> getAccessibleTopicsForUser( Long userId ) {

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

    // Lấy chi tiết topic với danh sách lesson và progress
    public GrammarTopicDTO getTopicWithProgress(Long topicId, Long userId){
        GrammarTopic topic = grammarTopicRepository.findByIdWithLessons(topicId)
                .orElseThrow(() -> new RuntimeException("Chủ đề không tồn tại id: " + topicId));

        GrammarTopicDTO dto = convertTopicToDTO(topic);
        // Lấy lesson theo tiến độ
        List<GrammarLesson> lessons = grammarLessonRepository.findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(topicId);
        List<GrammarLessonSummaryDTO> lessonSummaries = lessons.stream().map(lesson -> {
            GrammarLessonSummaryDTO summary = convertLessonToSummaryDTO(lesson);

            // Thêm thông tin tiến độ
            Optional<UserGrammarProgress> progress = userGrammarProgressRepository.findByUserIdAndLessonId(userId, lesson.getId());
            summary.setIsCompleted(progress.map(UserGrammarProgress::getIsCompleted).orElse(false));
            summary.setUserScore(progress.map(UserGrammarProgress::getScore).orElse(0));

            // Kiểm tra quyền truy cập
            summary.setIsAccessible(true);
            summary.setIsUnlocked(isLessonUnlocked(lesson, lessons, userId));

            return summary;

        }).collect(Collectors.toList());

        dto.setLessons(lessonSummaries);
        return dto;
    }

    // Kiểm tra bài học có được mở khóa không
    private boolean isLessonUnlocked(GrammarLesson lesson, List<GrammarLesson> allLessons, Long userId) {
        // Lesson đầu tiên luôn unlock
        if (lesson.getOrderIndex() == 1) {
            return true;
        }
        
        // Tìm lesson trước đó
        GrammarLesson previousLesson = allLessons.stream()
                .filter(l -> l.getOrderIndex() == lesson.getOrderIndex() - 1)
                .findFirst()
                .orElse(null);
                
        if (previousLesson == null) {
            return true; // Không có lesson trước = unlock
        }
        
        // Check lesson trước đã completed chưa
        return userGrammarProgressRepository.existsByUserIdAndLessonIdAndIsCompletedTrue(
                userId, previousLesson.getId());
    }

    // Lấy nội dung lesson chi tiết
    public GrammarLessonDTO getLessonContent(Long lessonId, Long userId) {

        GrammarLesson lesson = grammarLessonRepository.findByIdWithQuestions(lessonId)
                                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại id: " + lessonId));

        if (!canUserAccessLesson(lesson, userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập bài học này.");
        }

        GrammarLessonDTO dto = convertLessonToDTO(lesson);

        // Thêm thông tin tiến độ
        Optional<UserGrammarProgress> progress = userGrammarProgressRepository.findByUserIdAndLessonId(userId, lessonId);
        dto.setIsCompleted(progress.map(UserGrammarProgress::getIsCompleted).orElse(false));
        dto.setUserScore(progress.map(UserGrammarProgress::getScore).orElse(0));
        dto.setUserAttempts(progress.map(UserGrammarProgress::getAttempts).orElse(0));

        // Với phần thực hành(PRACTICE), không hiển thị đáp án đúng
        if (lesson.getLessonType() == LessonType.PRACTICE) {
            dto.getQuestions().forEach(q -> q.setShowCorrectAnswer(false));
        }

        return dto;
    }

    // Nộp bài tập và tính điểm
    public LessonResultResponse submitLesson(Long userId, SubmitLessonRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với id: " + userId));
        
        GrammarLesson lesson = grammarLessonRepository.findByIdWithQuestions(request.getLessonId())
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại id: " + request.getLessonId()));
        
        // Kiểm tra thời gian đọc của bài lý thuyết
        if (lesson.getLessonType() == LessonType.THEORY) {
            if (request.getReadingTimeSecond() == null || request.getReadingTimeSecond() < 30) {
                throw new RuntimeException("Bạn cần dành ít nhất 30 giây để đọc bài lý thuyết.");
            }
        }

        // Lấy hoặc tạo mới tiến độ học
        UserGrammarProgress progress = userGrammarProgressRepository.findByUserIdAndLessonId(userId, lesson.getId())
                .orElse(new UserGrammarProgress());

        if (progress.getId() == null) {
            progress.setUser(user);
            progress.setLesson(lesson);
        }

        // Xử lý câu trả lời và tính điểm
        int score = 0;
        int correctAnswers = 0;
        int totalQuestions = 0;
        boolean isPassed = false;
        List<QuestionResultDTO> questionResults = null;

        if (lesson.getLessonType() == LessonType.PRACTICE && request.getAnswers() != null) {
            questionResults = processAnswers(request.getAnswers());
            totalQuestions = questionResults.size();
            correctAnswers = (int) questionResults.stream().mapToLong(r -> r.getIsCorrect() ? 1 : 0).sum();
            score = questionResults.stream().mapToInt(QuestionResultDTO::getPoints).sum();

            double correctRate = totalQuestions > 0 ? (double) correctAnswers / totalQuestions : 0;
            isPassed = correctRate >= 0.7; // Qua bài nếu đúng >= 70%
        } else if (lesson.getLessonType() == LessonType.THEORY) {
            score = lesson.getPointsReward(); // Bài lý thuyết không có câu hỏi, cho điểm tối đa
            isPassed = true; // Bài lý thuyết chỉ cần đọc đủ thời gian là qua
        }

        // Cập nhật tiến độ học
        progress.setScore(Math.max(progress.getScore(), score)); // Lưu điểm cao nhất
        progress.setAttempts(progress.getAttempts() + 1);
        progress.setIsCompleted(isPassed);
        
        if (isPassed) {
            progress.setCompletedAt(LocalDateTime.now());

            if (!progress.getIsCompleted()) {
                user.setTotalPoints(user.getTotalPoints() + lesson.getPointsReward());
                userRepository.save(user);
            }
        }

        progress.setUpdatedAt(LocalDateTime.now());

        userGrammarProgressRepository.save(progress);

        // Kiểm tra mở khóa bài học tiếp theo
        boolean hasUnlockedNext = false;
        Long nextLessonId = null;

        if (isPassed) {
            Optional<GrammarLesson> nextLesson = grammarLessonRepository.findNextLessonInTopic(
                lesson.getTopic().getId(), lesson.getOrderIndex());
            hasUnlockedNext = nextLesson.isPresent();
            nextLessonId = nextLesson.map(GrammarLesson::getId).orElse(null);
        }

        // String resultMessage = isPassed ? "Bạn đã hoàn thành bài học!" : "Bạn cần đúng ít nhất 70% để hoàn thành bài.";

        log.info("User {} submitted lesson {}: score={}, passed={}, unlockedNext={}",
                user.getId(), lesson.getId(), score, isPassed, hasUnlockedNext);

        return new LessonResultResponse(
            lesson.getId(),
            lesson.getTitle(),
            totalQuestions,
            correctAnswers,
            score,
            isPassed ? lesson.getPointsReward() : 0,
            isPassed,
            hasUnlockedNext,
            nextLessonId,
            questionResults
        );
    }

    public List<GrammarQuestionDTO> getPracticeQuestions(Long lessonId, Long userId) {
        GrammarLesson lesson = grammarLessonRepository.findByIdWithQuestions(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson không tồn tại"));

        if (!canUserAccessLesson(lesson, userId)) {
            throw new RuntimeException("Bạn chưa thể truy cập lesson này");
        }

        return lesson.getQuestions().stream().map(question -> {
            GrammarQuestionDTO dto = convertQuestionToDTO(question);
            dto.setShowCorrectAnswer(false); // Ẩn đáp án đúng
            return dto;
        }).collect(Collectors.toList());
    }

    public List<GrammarQuestionDTO> getRandomQuestions(Long lessonId, int questionCount) {
        if (questionCount <= 0) {
            throw new IllegalArgumentException("Số lượng câu hỏi phải lớn hơn 0");
        }

        long totalQuestions = grammarQuestionRepository.countByLessonId(lessonId);
        if ( questionCount > totalQuestions ) {
            questionCount = (int) totalQuestions; // Giới hạn số câu hỏi tối đa
        }

        List<GrammarQuestion> questions = grammarQuestionRepository.findRandomQuestionsByLessonId(lessonId, questionCount);
        return questions.stream().map(this::convertQuestionToDTO).collect(Collectors.toList());
    }

    // ===== PRIVATE HELPER METHODS =====

    private boolean canUserAccessLesson(GrammarLesson lesson, Long userId) {
        return isLessonUnlocked(lesson, grammarLessonRepository.findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(userId), userId);
    }

    // Xử lý câu trả lời và trả về kết quả chi tiết
    private List<QuestionResultDTO> processAnswers(List<SubmitAnswerRequest> answers) {
        return answers.stream().map(answerRequest -> {
            GrammarQuestion question = grammarQuestionRepository.findByIdWithOptions(answerRequest.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("Question không tồn tại"));

            boolean isCorrect = answerCheckingService.checkAnswer(question, answerRequest);
            int points = isCorrect ? question.getPoints() : 0;
            String hint = isCorrect ? null : answerCheckingService.generateHint(question, answerRequest);

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

    // ===== CONVERT METHODS =====
    private GrammarTopicDTO convertTopicToDTO(GrammarTopic topic) {
        return new GrammarTopicDTO(
            topic.getId(),
            topic.getName(),
            topic.getDescription(),
            topic.getLevelRequired(),
            topic.getOrderIndex(),
            topic.getIsActive(),
            topic.getCreatedAt(),
            null,
            null,
            null,
            null
        );
    }

    private GrammarLessonDTO convertLessonToDTO(GrammarLesson lesson) {
        List<GrammarQuestionDTO> questions = lesson.getQuestions().stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());

        return new GrammarLessonDTO(
                lesson.getId(),
                lesson.getTopic().getId(),
                lesson.getTitle(),
                lesson.getLessonType(),
                lesson.getContent(),
                lesson.getOrderIndex(),
                lesson.getPointsRequired(),
                lesson.getPointsReward(),
                lesson.getIsActive(),
                lesson.getCreatedAt(),
                lesson.getTopic().getName(),
                questions,
                questions.size(),
                null, // isCompleted - will be set separately
                null, // isAccessible
                null, // isUnlocked
                null, // userScore
                null  // userAttempts
        );
    }

    private GrammarLessonSummaryDTO convertLessonToSummaryDTO(GrammarLesson lesson) {
        return new GrammarLessonSummaryDTO(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getLessonType(),
                lesson.getOrderIndex(),
                lesson.getPointsRequired(),
                lesson.getPointsReward(),
                lesson.getIsActive(),
                lesson.getQuestions().size(),
                null, // isCompleted
                null, // isAccessible
                null, // isUnlocked
                null  // userScore
        );
    }

    private GrammarQuestionDTO convertQuestionToDTO(GrammarQuestion question) {
        List<GrammarQuestionOptionDTO> options = question.getOptions().stream()
                .map(this::convertOptionToDTO)
                .collect(Collectors.toList());

        // Shuffle options for multiple choice questions để randomize thứ tự
        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE && !options.isEmpty()) {
            Collections.shuffle(options);
            // Reset order index after shuffle
            for (int i = 0; i < options.size(); i++) {
                options.get(i).setOrderIndex(i + 1);
            }
        }

        return new GrammarQuestionDTO(
                question.getId(),
                question.getLesson().getId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getCorrectAnswer(),
                question.getExplanation(),
                question.getPoints(),
                question.getCreatedAt(),
                options,
                true // showCorrectAnswer - will be modified based on context
        );
    }

    private GrammarQuestionOptionDTO convertOptionToDTO(GrammarQuestionOption option) {
        return new GrammarQuestionOptionDTO(
                option.getId(),
                option.getQuestion().getId(),
                option.getOptionText(),
                option.getIsCorrect(),
                option.getOrderIndex()
        );
    }
}
