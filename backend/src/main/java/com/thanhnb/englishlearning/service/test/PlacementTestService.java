package com.thanhnb.englishlearning.service.test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.topic.TopicRepository;
import com.thanhnb.englishlearning.repository.user.UserRepository;
import com.thanhnb.englishlearning.service.question.AnswerValidationService;
import com.thanhnb.englishlearning.service.question.QuestionService;
import com.thanhnb.englishlearning.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Placement Test Service
 * 
 * Handles:
 * - Generating placement test with questions from Grammar, Reading, Listening
 * - Grading test and assigning English level (A1-C1)
 * - Enforcing 24h cooldown between attempts
 * 
 * Security:
 * - @JsonView automatically hides correct answers (isCorrect, correctAnswers, etc.)
 * - QuestionService.convertToDTOsForLearning() shuffles options
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlacementTestService {

    private final GrammarLessonRepository grammarLessonRepository;
    private final ReadingLessonRepository readingLessonRepository;
    private final ListeningLessonRepository listeningLessonRepository;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AnswerValidationService answerValidationService;
    private final QuestionService questionService;

    // Configuration
    private static final int MIN_QUESTIONS_REQUIRED = 20;
    private static final int TIME_LIMIT_SECONDS = 1800; // 30 minutes
    private static final int COOLDOWN_HOURS = 24;
    
    // ══════════════════════════════════════════════════════════════
    // GET PLACEMENT TEST
    // ══════════════════════════════════════════════════════════════

    /**
     * Generate placement test for current user
     * 
     * @return Test with Grammar, Reading, and Listening sections
     * @throws IllegalStateException if cooldown not expired or insufficient questions
     */
    @Transactional(readOnly = true)
    public PlacementTestResponse getPlacementTest() {
        User currentUser = userService.getCurrentUser();
        
        // ✅ Enforce cooldown
        validateCooldown(currentUser);

        // ✅ Build test sections
        List<PlacementSection> sections = new ArrayList<>();
        sections.add(getGrammarSection());
        sections.addAll(getReadingSections());
        sections.addAll(getListeningSections());

        int totalQuestions = calculateTotalQuestions(sections);
        
        // ✅ Validate test has enough questions
        if (totalQuestions < MIN_QUESTIONS_REQUIRED) {
            throw new IllegalStateException(
                String.format("Placement test không đủ câu hỏi (có %d, cần %d)", 
                    totalQuestions, MIN_QUESTIONS_REQUIRED));
        }

        log.info("Generated placement test for user {}: {} questions in {} sections",
            currentUser.getId(), totalQuestions, sections.size());

        return PlacementTestResponse.builder()
                .sections(sections)
                .totalQuestions(totalQuestions)
                .timeLimitSeconds(TIME_LIMIT_SECONDS)
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // SECTION BUILDERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Build Grammar section (standalone questions)
     */
    private PlacementSection getGrammarSection() {
        Long grammarTopicId = getPlacementTopicId(ModuleType.GRAMMAR);
        List<GrammarLesson> lessons = grammarLessonRepository
                .findAllByTopicIdOrderByOrderIndexAsc(grammarTopicId);

        if (lessons.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy Grammar lessons cho placement test");
        }

        List<QuestionResponseDTO> allQuestions = new ArrayList<>();

        for (GrammarLesson lesson : lessons) {
            List<Question> questions = questionService.loadQuestionsByParent(
                ParentType.GRAMMAR, lesson.getId());

            // ✅ convertToDTOsForLearning() already:
            //    - Shuffles options
            //    - @JsonView hides correct answers automatically
            allQuestions.addAll(questionService.convertToDTOsForLearning(questions));
        }

        if (allQuestions.isEmpty()) {
            throw new IllegalStateException("Grammar section không có câu hỏi");
        }

        log.debug("Grammar section: {} questions from {} lessons", 
            allQuestions.size(), lessons.size());

        return PlacementSection.builder()
                .sectionType("GRAMMAR")
                .title("Grammar Section")
                .questions(allQuestions)
                .build();
    }

    /**
     * Build Reading sections (passage + questions)
     */
    private List<PlacementSection> getReadingSections() {
        Long readingTopicId = getPlacementTopicId(ModuleType.READING);
        List<ReadingLesson> lessons = readingLessonRepository
                .findAllByTopicIdOrderByOrderIndexAsc(readingTopicId);

        if (lessons.isEmpty()) {
            log.warn("No reading lessons for placement test");
            return List.of();
        }

        List<PlacementSection> sections = new ArrayList<>();

        for (ReadingLesson lesson : lessons) {
            List<Question> questions = questionService.loadQuestionsByParent(
                ParentType.READING, lesson.getId());

            if (questions.isEmpty()) {
                log.warn("Reading lesson {} has no questions", lesson.getId());
                continue;
            }

            List<QuestionResponseDTO> questionDTOs = questionService
                .convertToDTOsForLearning(questions);

            sections.add(PlacementSection.builder()
                    .sectionType("READING")
                    .title(lesson.getTitle())
                    .content(lesson.getContent())
                    .contentTranslation(lesson.getContentTranslation())
                    .questions(questionDTOs)
                    .build());
        }

        log.debug("Reading sections: {} passages", sections.size());
        return sections;
    }

    /**
     * Build Listening sections (audio + questions)
     */
    private List<PlacementSection> getListeningSections() {
        Long listeningTopicId = getPlacementTopicId(ModuleType.LISTENING);
        List<ListeningLesson> lessons = listeningLessonRepository
                .findAllByTopicIdOrderByOrderIndexAsc(listeningTopicId);

        if (lessons.isEmpty()) {
            log.warn("No listening lessons for placement test");
            return List.of();
        }

        List<PlacementSection> sections = new ArrayList<>();

        for (ListeningLesson lesson : lessons) {
            List<Question> questions = questionService.loadQuestionsByParent(
                ParentType.LISTENING, lesson.getId());

            if (questions.isEmpty()) {
                log.warn("Listening lesson {} has no questions", lesson.getId());
                continue;
            }

            List<QuestionResponseDTO> questionDTOs = questionService
                .convertToDTOsForLearning(questions);

            sections.add(PlacementSection.builder()
                    .sectionType("LISTENING")
                    .title(lesson.getTitle())
                    .audioUrl(lesson.getAudioUrl())
                    .transcript(lesson.getTranscript())
                    .transcriptTranslation(lesson.getTranscriptTranslation())
                    .questions(questionDTOs)
                    .build());
        }

        log.debug("Listening sections: {} audio clips", sections.size());
        return sections;
    }

    // ══════════════════════════════════════════════════════════════
    // SUBMIT TEST
    // ══════════════════════════════════════════════════════════════

    /**
     * Grade placement test and assign English level
     * 
     * @param submitRequest User's answers
     * @return Result with score and assigned level
     */
    @Transactional
    public PlacementResultResponse submitTest(PlacementSubmitRequest submitRequest) {
        User user = userService.getCurrentUser();

        // ✅ Validate submission
        validateSubmission(submitRequest);

        // ✅ Grade answers using existing AnswerValidationService
        int correctAnswers = 0;
        int totalQuestions = submitRequest.getAnswers().size();

        for (AnswerSubmission answer : submitRequest.getAnswers()) {
            Question question = questionRepository.findById(answer.getQuestionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Question not found: " + answer.getQuestionId()));

            // Reuse existing validation logic
            var result = answerValidationService.validateAnswer(
                question,
                answer.getSelectedOptions(),
                answer.getTextAnswer()
            );

            if (Boolean.TRUE.equals(result.getIsCorrect())) {
                correctAnswers++;
            }
        }

        // ✅ Calculate score and determine level
        double score = Math.round((correctAnswers * 100.0) / totalQuestions);
        EnglishLevel newLevel = determineLevel(score);

        // ✅ Update user level and timestamp
        user.setEnglishLevel(newLevel);
        user.setLastPlacementTestDate(LocalDateTime.now());
        userRepository.save(user);

        log.info("User {} completed placement test: Score={}%, Level={}, Correct={}/{}", 
            user.getId(), score, newLevel, correctAnswers, totalQuestions);

        return PlacementResultResponse.builder()
                .score(score)
                .correctAnswers(correctAnswers)
                .totalQuestions(totalQuestions)
                .assignedLevel(newLevel)
                .levelDescription(getLevelDescription(newLevel))
                .canRetakeAfter(LocalDateTime.now().plusHours(COOLDOWN_HOURS))
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // VALIDATION & HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Validate cooldown period between test attempts
     */
    private void validateCooldown(User user) {
        if (user.getLastPlacementTestDate() == null) {
            return; // First time taking test
        }

        Duration timeSinceLastTest = Duration.between(
            user.getLastPlacementTestDate(), 
            LocalDateTime.now()
        );
        
        if (timeSinceLastTest.toHours() < COOLDOWN_HOURS) {
            long hoursRemaining = COOLDOWN_HOURS - timeSinceLastTest.toHours();
            throw new IllegalStateException(
                String.format("Bạn chỉ có thể làm lại test sau %d giờ nữa", hoursRemaining)
            );
        }
    }

    /**
     * Validate submission format and content
     */
    private void validateSubmission(PlacementSubmitRequest request) {
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new IllegalArgumentException("Không có câu trả lời nào được submit");
        }

        // Check for duplicate question IDs
        Set<Long> questionIds = new HashSet<>();
        for (AnswerSubmission answer : request.getAnswers()) {
            if (!questionIds.add(answer.getQuestionId())) {
                throw new IllegalArgumentException(
                    "Trùng lặp câu trả lời cho question ID: " + answer.getQuestionId());
            }
        }

        log.debug("Validated {} answers for placement test", request.getAnswers().size());
    }

    /**
     * Get placement topic ID for given module type
     */
    private Long getPlacementTopicId(ModuleType moduleType) {
        return topicRepository.findPlacementTopicIdByModuleType(moduleType)
                .orElseThrow(() -> new IllegalStateException(
                        "Placement test topic not found for module: " + moduleType));
    }

    /**
     * Determine English level based on test score
     * 
     * Score ranges:
     * - 85-100%: C1 (Advanced)
     * - 70-84%:  B2 (Upper Intermediate)
     * - 55-69%:  B1 (Intermediate)
     * - 40-54%:  A2 (Elementary)
     * - 0-39%:   A1 (Beginner)
     */
    private EnglishLevel determineLevel(double score) {
        if (score >= 85) return EnglishLevel.C1;
        if (score >= 70) return EnglishLevel.B2;
        if (score >= 55) return EnglishLevel.B1;
        if (score >= 40) return EnglishLevel.A2;
        return EnglishLevel.A1;
    }

    /**
     * Get human-readable level description
     */
    private String getLevelDescription(EnglishLevel level) {
        return switch (level) {
            case A1 -> "Beginner - Can understand basic phrases and simple conversations";
            case A2 -> "Elementary - Can handle routine tasks and simple exchanges";
            case B1 -> "Intermediate - Can understand main points on familiar matters";
            case B2 -> "Upper Intermediate - Can interact fluently with native speakers";
            case C1 -> "Advanced - Can express ideas fluently and spontaneously";
            case C2 -> "Proficiency - Can understand virtually everything with ease";
            default -> "Unassigned - Please take the placement test";
        };
    }

    /**
     * Calculate total questions across all sections
     */
    private int calculateTotalQuestions(List<PlacementSection> sections) {
        return sections.stream()
                .mapToInt(s -> s.getQuestions().size())
                .sum();
    }

    // ══════════════════════════════════════════════════════════════
    // DTOs
    // ══════════════════════════════════════════════════════════════

    @lombok.Data
    @lombok.Builder
    public static class PlacementTestResponse {
        private List<PlacementSection> sections;
        private Integer totalQuestions;
        private Integer timeLimitSeconds;
    }

    @lombok.Data
    @lombok.Builder
    public static class PlacementSection {
        private String sectionType; // "GRAMMAR" | "READING" | "LISTENING"
        private String title;
        
        // For READING
        private String content;
        private String contentTranslation;
        
        // For LISTENING
        private String audioUrl;
        private String transcript;
        private String transcriptTranslation;
        
        // Questions (already secured by @JsonView)
        private List<QuestionResponseDTO> questions;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PlacementSubmitRequest {
        private List<AnswerSubmission> answers;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AnswerSubmission {
        private Long questionId;
        private List<Long> selectedOptions; // For multiple choice
        private String textAnswer;          // For text-based questions
    }

    @lombok.Data
    @lombok.Builder
    public static class PlacementResultResponse {
        private Double score;
        private Integer correctAnswers;
        private Integer totalQuestions;
        private EnglishLevel assignedLevel;
        private String levelDescription;
        private LocalDateTime canRetakeAfter;
    }
}