package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.dto.grammar.GrammarQuestionDTO;
import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.grammar.GrammarTopic;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.entity.question.QuestionOption;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service chuyên xử lý import data từ PDF/external sources
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
     * Import lessons từ PDF parsing result (lessons + questions)
     */
    public List<GrammarLessonDTO> importLessonsFromPDF(Long topicId, List<GrammarLessonDTO> lessonDTOs) {
        GrammarTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại với id: " + topicId));

        List<GrammarLessonDTO> savedLessons = new ArrayList<>();

        for (GrammarLessonDTO lessonDTO : lessonDTOs) {
            try {
                lessonDTO.setTopicId(topicId);

                // Create lesson
                GrammarLesson lesson = new GrammarLesson();
                lesson.setTopic(topic);
                lesson.setTitle(lessonDTO.getTitle());
                lesson.setLessonType(lessonDTO.getLessonType());
                lesson.setContent(lessonDTO.getContent());
                lesson.setOrderIndex(lessonDTO.getOrderIndex());
                lesson.setEstimatedDuration(
                        lessonDTO.getEstimatedDuration() != null ? lessonDTO.getEstimatedDuration() : 30);
                lesson.setPointsReward(lessonDTO.getPointsReward() != null ? lessonDTO.getPointsReward() : 10);
                lesson.setIsActive(lessonDTO.getIsActive() != null ? lessonDTO.getIsActive() : true);
                lesson.setCreatedAt(LocalDateTime.now());

                GrammarLesson savedLesson = lessonRepository.save(lesson);

                int questionCount = 0;

                // Create questions
                if (lessonDTO.getQuestions() != null && !lessonDTO.getQuestions().isEmpty()) {
                    for (GrammarQuestionDTO questionDTO : lessonDTO.getQuestions()) {
                        try {
                            validateQuestionDTO(questionDTO);

                            Question question = new Question();
                            question.setParentType(ParentType.GRAMMAR);
                            question.setParentId(savedLesson.getId());
                            question.setQuestionText(questionDTO.getQuestionText());
                            question.setQuestionType(questionDTO.getQuestionType());

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

                            // Create options
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
                            }
                        } catch (Exception e) {
                            log.warn("⚠️ Skipping invalid question: {}", e.getMessage());
                        }
                    }

                    log.info("✅ Created {} questions for lesson: {}", questionCount, savedLesson.getTitle());
                }

                GrammarLessonDTO savedDTO = convertLessonToDTO(savedLesson);
                savedDTO.setQuestionCount(questionCount);
                savedLessons.add(savedDTO);

                log.info("✅ Imported lesson: {} (type: {}, {} questions)",
                        savedLesson.getTitle(), savedLesson.getLessonType(), questionCount);

            } catch (Exception e) {
                log.error("❌ Failed to import lesson: {}", lessonDTO.getTitle(), e);
            }
        }

        log.info("✅ Successfully imported {} lessons for topic: {}", savedLessons.size(), topic.getName());

        return savedLessons;
    }

    // ===== VALIDATION =====

    private void validateQuestionDTO(GrammarQuestionDTO dto) {
        switch (dto.getQuestionType()) {
            case MULTIPLE_CHOICE:
                if (dto.getOptions() == null || dto.getOptions().isEmpty()) {
                    throw new RuntimeException("Câu hỏi trắc nghiệm phải có ít nhất 2 lựa chọn");
                }
                if (dto.getOptions().size() < 2) {
                    throw new RuntimeException("Câu hỏi trắc nghiệm phải có ít nhất 2 lựa chọn");
                }

                boolean hasEmptyOption = dto.getOptions().stream()
                        .anyMatch(opt -> opt.getOptionText() == null || opt.getOptionText().trim().isEmpty());
                if (hasEmptyOption) {
                    throw new RuntimeException("Tất cả lựa chọn phải có nội dung");
                }

                boolean hasCorrectAnswer = dto.getOptions().stream()
                        .anyMatch(opt -> opt.getIsCorrect() != null && opt.getIsCorrect());
                if (!hasCorrectAnswer) {
                    throw new RuntimeException("Phải có ít nhất 1 đáp án đúng");
                }
                break;

            case FILL_BLANK:
            case TRANSLATE:
                if (dto.getCorrectAnswer() == null || dto.getCorrectAnswer().trim().isEmpty()) {
                    throw new RuntimeException("Đáp án đúng không được để trống cho câu hỏi " +
                            (dto.getQuestionType() == QuestionType.FILL_BLANK ? "điền từ" : "dịch câu"));
                }
                break;

            default:
                throw new RuntimeException("Loại câu hỏi không hợp lệ");
        }
    }

    // ===== CONVERSION =====

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