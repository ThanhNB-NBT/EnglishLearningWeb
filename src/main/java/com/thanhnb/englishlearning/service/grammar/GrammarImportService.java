package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.dto.question.QuestionDTO;
import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.grammar.GrammarTopic;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.entity.question.QuestionOption;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.grammar.GrammarTopicRepository;
import com.thanhnb.englishlearning.repository.question.QuestionOptionRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service chuyên xử lý import grammar lessons từ AI parsing
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarImportService {

    private final GrammarTopicRepository topicRepository;
    private final GrammarLessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    /**
     * Import lessons from AI parsing service
     * OrderIndex đã được adjust bởi GrammarAIParsingService.adjustOrderIndexForTopic()
     *
     * @param topicId Target topic ID
     * @param parsedLessons Lessons từ AI parsing (orderIndex đã được adjust)
     * @return List of saved lessons with IDs and question counts
     */
    @Transactional
    public List<GrammarLessonDTO> importLessonsFromFile(Long topicId, List<GrammarLessonDTO> parsedLessons) {
        log.info("Importing {} parsed lessons to topicId={}", parsedLessons.size(), topicId);

        // Validate topic exists
        GrammarTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại với id: " + topicId));

        List<GrammarLessonDTO> savedLessons = new ArrayList<>();

        for (GrammarLessonDTO lessonDTO : parsedLessons) {
            try {
                // Validate required fields
                if (lessonDTO.getTitle() == null || lessonDTO.getTitle().trim().isEmpty()) {
                    log.warn("Skipping lesson with empty title");
                    continue;
                }

                if (lessonDTO.getLessonType() == null) {
                    log.warn("Skipping lesson '{}' with null lessonType", lessonDTO.getTitle());
                    continue;
                }

                // Ensure topicId is set
                lessonDTO.setTopicId(topicId);

                // Ensure defaults
                if (lessonDTO.getPointsReward() == null || lessonDTO.getPointsReward() == 0) {
                    lessonDTO.setPointsReward(
                        lessonDTO.getLessonType() == LessonType.THEORY ? 10 : 15
                    );
                }

                if (lessonDTO.getEstimatedDuration() == null || lessonDTO.getEstimatedDuration() == 0) {
                    int duration = lessonDTO.getLessonType() == LessonType.THEORY ? 180 : 300;
                    if (lessonDTO.getContent() != null && lessonDTO.getContent().length() > 5000) {
                        duration += 120;
                    }
                    if (lessonDTO.getQuestions() != null && lessonDTO.getQuestions().size() > 5) {
                        duration += lessonDTO.getQuestions().size() * 30;
                    }
                    lessonDTO.setEstimatedDuration(duration);
                }

                if (lessonDTO.getIsActive() == null) {
                    lessonDTO.setIsActive(true);
                }

                // Create lesson entity
                GrammarLesson lesson = new GrammarLesson();
                lesson.setTopic(topic);
                lesson.setTitle(lessonDTO.getTitle());
                lesson.setLessonType(lessonDTO.getLessonType());
                lesson.setContent(lessonDTO.getContent());
                lesson.setOrderIndex(lessonDTO.getOrderIndex());
                lesson.setEstimatedDuration(lessonDTO.getEstimatedDuration());
                lesson.setPointsReward(lessonDTO.getPointsReward());
                lesson.setIsActive(lessonDTO.getIsActive());
                lesson.setCreatedAt(LocalDateTime.now());

                GrammarLesson savedLesson = lessonRepository.save(lesson);

                // Save questions if exists (PRACTICE lesson)
                int questionCount = 0;
                if (lessonDTO.getQuestions() != null && !lessonDTO.getQuestions().isEmpty()) {
                    log.info("Creating {} questions for lesson: {}", 
                            lessonDTO.getQuestions().size(), savedLesson.getTitle());

                    for (QuestionDTO questionDTO : lessonDTO.getQuestions()) {
                        try {
                            // Validate question
                            validateQuestionDTO(questionDTO);

                            // Create question entity
                            Question question = new Question();
                            question.setParentType(ParentType.GRAMMAR);
                            question.setParentId(savedLesson.getId());
                            question.setQuestionText(questionDTO.getQuestionText());
                            question.setQuestionType(questionDTO.getQuestionType());

                            // Set correct answer (not for MULTIPLE_CHOICE)
                            if (questionDTO.getQuestionType() != QuestionType.MULTIPLE_CHOICE) {
                                question.setCorrectAnswer(questionDTO.getCorrectAnswer());
                            }

                            question.setExplanation(questionDTO.getExplanation());
                            question.setPoints(questionDTO.getPoints() != null ? questionDTO.getPoints() : 5);
                            question.setOrderIndex(
                                    questionDTO.getOrderIndex() != null ? questionDTO.getOrderIndex()
                                            : questionCount + 1);
                            question.setCreatedAt(LocalDateTime.now());

                            Question savedQuestion = questionRepository.save(question);
                            questionCount++;

                            // Create options for MULTIPLE_CHOICE
                            if (questionDTO.getQuestionType() == QuestionType.MULTIPLE_CHOICE &&
                                    questionDTO.getOptions() != null && !questionDTO.getOptions().isEmpty()) {

                                List<QuestionOption> options = questionDTO.getOptions().stream()
                                        .map(optionDTO -> {
                                            QuestionOption option = new QuestionOption();
                                            option.setQuestion(savedQuestion);
                                            option.setOptionText(optionDTO.getOptionText());
                                            option.setIsCorrect(
                                                    optionDTO.getIsCorrect() != null ? optionDTO.getIsCorrect()
                                                            : false);
                                            option.setOrderIndex(optionDTO.getOrderIndex());
                                            return option;
                                        })
                                        .collect(Collectors.toList());

                                optionRepository.saveAll(options);
                                
                                log.debug("Created {} options for question: {}", 
                                        options.size(), questionDTO.getQuestionText());
                            }

                        } catch (Exception e) {
                            log.warn("Skipping invalid question in lesson '{}': {}", 
                                    savedLesson.getTitle(), e.getMessage());
                            // Continue with next question instead of failing
                        }
                    }

                    log.info("Created {} questions for lesson: {}", questionCount, savedLesson.getTitle());
                } else {
                    log.debug("No questions for lesson: {} ({})", 
                            savedLesson.getTitle(), savedLesson.getLessonType());
                }

                // Build response DTO
                GrammarLessonDTO savedDTO = convertLessonToDTO(savedLesson);
                savedDTO.setQuestionCount(questionCount);
                savedLessons.add(savedDTO);

                log.info("Imported lesson: {} (type: {}, orderIndex: {}, {} questions)",
                        savedLesson.getTitle(),
                        savedLesson.getLessonType(),
                        savedLesson.getOrderIndex(),
                        questionCount);

            } catch (Exception e) {
                log.error("Error importing lesson '{}': {}", 
                        lessonDTO != null ? lessonDTO.getTitle() : "unknown", 
                        e.getMessage(), e);
                // Continue with next lesson instead of failing completely
            }
        }

        log.info("Successfully imported {}/{} lessons to topic '{}' (ID: {})",
                savedLessons.size(), parsedLessons.size(), topic.getName(), topicId);

        return savedLessons;
    }

    // ═════════════════════════════════════════════════════════════════════
    // VALIDATION METHODS
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Validate question DTO based on question type
     * 
     * @throws RuntimeException if validation fails
     */
    private void validateQuestionDTO(QuestionDTO dto) {
        // Check question text
        if (dto.getQuestionText() == null || dto.getQuestionText().trim().isEmpty()) {
            throw new RuntimeException("Question text không được để trống");
        }

        // Check question type
        if (dto.getQuestionType() == null) {
            throw new RuntimeException("Question type không được null");
        }

        // Validate based on question type
        switch (dto.getQuestionType()) {
            case MULTIPLE_CHOICE:
                validateMultipleChoiceQuestion(dto);
                break;

            case FILL_BLANK:
                validateFillBlankQuestion(dto);
                break;

            case TRANSLATE:
                validateTranslateQuestion(dto);
                break;

            default:
                throw new RuntimeException("Loại câu hỏi không hợp lệ: " + dto.getQuestionType());
        }
    }

    /**
     * Validate MULTIPLE_CHOICE question
     */
    private void validateMultipleChoiceQuestion(QuestionDTO dto) {
        // Must have options
        if (dto.getOptions() == null || dto.getOptions().isEmpty()) {
            throw new RuntimeException("Câu hỏi trắc nghiệm phải có ít nhất 2 lựa chọn");
        }

        // At least 2 options
        if (dto.getOptions().size() < 2) {
            throw new RuntimeException("Câu hỏi trắc nghiệm phải có ít nhất 2 lựa chọn");
        }

        // Check for empty options
        boolean hasEmptyOption = dto.getOptions().stream()
                .anyMatch(opt -> opt.getOptionText() == null || opt.getOptionText().trim().isEmpty());
        if (hasEmptyOption) {
            throw new RuntimeException("Tất cả lựa chọn phải có nội dung");
        }

        // Must have at least one correct answer
        boolean hasCorrectAnswer = dto.getOptions().stream()
                .anyMatch(opt -> opt.getIsCorrect() != null && opt.getIsCorrect());
        if (!hasCorrectAnswer) {
            throw new RuntimeException("Phải có ít nhất 1 đáp án đúng");
        }

        // Should not have more than one correct answer
        long correctCount = dto.getOptions().stream()
                .filter(opt -> opt.getIsCorrect() != null && opt.getIsCorrect())
                .count();
        if (correctCount > 1) {
            log.warn("Multiple choice question has {} correct answers (should be 1): {}", 
                    correctCount, dto.getQuestionText());
        }
    }

    /**
     * Validate FILL_BLANK question
     */
    private void validateFillBlankQuestion(QuestionDTO dto) {
        // Must have correct answer
        if (dto.getCorrectAnswer() == null || dto.getCorrectAnswer().trim().isEmpty()) {
            throw new RuntimeException("Đáp án đúng không được để trống cho câu hỏi điền từ");
        }

        // Should contain blank marker
        String questionText = dto.getQuestionText();
        if (!questionText.contains("___") && 
            !questionText.contains("(") && 
            !questionText.contains("[blank]")) {
            log.warn("FILL_BLANK question missing blank marker: {}", questionText);
        }
    }

    /**
     * Validate TRANSLATE question
     */
    private void validateTranslateQuestion(QuestionDTO dto) {
        // Must have correct answer
        if (dto.getCorrectAnswer() == null || dto.getCorrectAnswer().trim().isEmpty()) {
            throw new RuntimeException("Đáp án đúng không được để trống cho câu hỏi dịch");
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // CONVERSION METHODS
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Convert GrammarLesson entity to DTO
     */
    private GrammarLessonDTO convertLessonToDTO(GrammarLesson lesson) {
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
}