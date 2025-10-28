package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.GrammarQuestionDTO;
import com.thanhnb.englishlearning.dto.grammar.GrammarQuestionOptionDTO;
import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.entity.question.QuestionOption;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.question.QuestionOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarQuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final GrammarLessonRepository lessonRepository;
    private final GrammarOrderService orderService;

    // ===== READ OPERATIONS =====

    public Page<GrammarQuestionDTO> getQuestionsByLessonPaginated(Long lessonId, Pageable pageable) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new RuntimeException("Bài học không tồn tại");
        }

        return questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(
                ParentType.GRAMMAR, lessonId, pageable)
                .map(this::convertToDTO);
    }

    // ===== CREATE =====

    public GrammarQuestionDTO createQuestion(GrammarQuestionDTO dto) {
        if (!lessonRepository.existsById(dto.getLessonId())) {
            throw new RuntimeException("Bài học không tồn tại");
        }

        // Validate
        validateQuestionDTO(dto);

        // Create question
        Question question = new Question();
        question.setParentType(ParentType.GRAMMAR);
        question.setParentId(dto.getLessonId());
        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());

        if (dto.getQuestionType() != QuestionType.MULTIPLE_CHOICE) {
            question.setCorrectAnswer(dto.getCorrectAnswer());
        }

        question.setExplanation(dto.getExplanation());
        question.setPoints(dto.getPoints() != null ? dto.getPoints() : 5);

        if (dto.getOrderIndex() == null) {
            long currentCount = questionRepository.countByParentTypeAndParentId(
                    ParentType.GRAMMAR, dto.getLessonId());
            question.setOrderIndex((int) currentCount + 1);
        } else {
            question.setOrderIndex(dto.getOrderIndex());
        }

        question.setCreatedAt(LocalDateTime.now());

        Question savedQuestion = questionRepository.save(question);
        log.info("✅ Created new Grammar Question: {}", savedQuestion.getQuestionText());

        // Create options
        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            List<QuestionOption> options = dto.getOptions().stream()
                    .map(optionDTO -> {
                        QuestionOption option = new QuestionOption();
                        option.setQuestion(savedQuestion);
                        option.setOptionText(optionDTO.getOptionText());
                        option.setIsCorrect(optionDTO.getIsCorrect() != null ? optionDTO.getIsCorrect() : false);
                        option.setOrderIndex(optionDTO.getOrderIndex());
                        return option;
                    }).collect(Collectors.toList());

            optionRepository.saveAll(options);
            log.info("✅ Created {} options for question id={}", options.size(), savedQuestion.getId());
        }

        return convertToDTO(savedQuestion);
    }

    // ===== UPDATE =====

    public GrammarQuestionDTO updateQuestion(Long id, GrammarQuestionDTO dto) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại"));

        if (!question.getParentType().equals(ParentType.GRAMMAR)) {
            throw new RuntimeException("Câu hỏi không thuộc bài học ngữ pháp");
        }

        // Validate
        validateQuestionDTO(dto);

        // Update question
        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());

        if (dto.getQuestionType() != QuestionType.MULTIPLE_CHOICE) {
            question.setCorrectAnswer(dto.getCorrectAnswer());
        } else {
            question.setCorrectAnswer(null);
        }

        question.setExplanation(dto.getExplanation());

        if (dto.getPoints() != null) {
            question.setPoints(dto.getPoints());
        }

        if (dto.getOrderIndex() != null) {
            question.setOrderIndex(dto.getOrderIndex());
        }

        Question savedQuestion = questionRepository.save(question);

        // Update options
        if (dto.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            optionRepository.deleteByQuestionId(id);

            if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
                List<QuestionOption> options = dto.getOptions().stream()
                        .map(optionDTO -> {
                            QuestionOption option = new QuestionOption();
                            option.setQuestion(savedQuestion);
                            option.setOptionText(optionDTO.getOptionText());
                            option.setIsCorrect(optionDTO.getIsCorrect() != null ? optionDTO.getIsCorrect() : false);
                            option.setOrderIndex(optionDTO.getOrderIndex());
                            return option;
                        }).collect(Collectors.toList());

                optionRepository.saveAll(options);
                log.info("✅ Updated {} options for question id={}", options.size(), id);
            }
        } else {
            optionRepository.deleteByQuestionId(id);
        }

        log.info("✅ Updated grammar question ID: {}", id);
        return convertToDTO(savedQuestion);
    }

    // ===== DELETE =====

    public void deleteQuestion(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại"));

        if (!question.getParentType().equals(ParentType.GRAMMAR)) {
            throw new RuntimeException("Câu hỏi không thuộc bài học ngữ pháp");
        }

        Long lessonId = question.getParentId();
        Integer deletedOrderIndex = question.getOrderIndex();

        // Delete options first
        optionRepository.deleteByQuestionId(id);

        questionRepository.delete(question);
        log.info("✅ Deleted Grammar Question: {} (orderIndex: {})",
                question.getQuestionText(), deletedOrderIndex);

        // Auto reorder
        orderService.reorderQuestionsAfterDelete(lessonId, deletedOrderIndex);
    }

    @Transactional
    public int bulkDeleteQuestions(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty())
            return 0;

        List<Question> toDelete = questionRepository.findByIdIn(questionIds);
        if (toDelete.isEmpty())
            return 0;

        // Xóa options trước
        optionRepository.deleteByQuestionIdIn(toDelete.stream().map(Question::getId).toList());

        // Group theo lesson
        Map<Long, List<Question>> byLesson = toDelete.stream()
                .collect(Collectors.groupingBy(Question::getParentId));

        // Xóa questions theo batch
        questionRepository.deleteAllByIdInBatch(questionIds);

        int totalReindexed = 0;
        // Reindex mỗi lesson một lần
        for (Map.Entry<Long, List<Question>> e : byLesson.entrySet()) {
            Long lessonId = e.getKey();
            List<Question> remaining = questionRepository
                    .findByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType.GRAMMAR, lessonId);

            for (int i = 0; i < remaining.size(); i++) {
                remaining.get(i).setOrderIndex(i + 1);
            }
            if (!remaining.isEmpty()) {
                questionRepository.saveAll(remaining);
                totalReindexed += remaining.size();
            }
        }
        log.info("✅ Bulk deleted {} questions; reindexed {} remaining items across {} lessons",
                toDelete.size(), totalReindexed, byLesson.size());
        return toDelete.size();
    }

    // ===== GET NEXT ORDER INDEX =====

    public Integer getNextOrderIndex(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new RuntimeException("Bài học không tồn tại");
        }

        Integer maxOrder = questionRepository.findMaxOrderIndexByParentTypeAndParentId(
                ParentType.GRAMMAR, lessonId);

        if (maxOrder != null) {
            log.info("✅ Max question orderIndex in lesson {}: {}", lessonId, maxOrder);
            return maxOrder + 1;
        }

        log.info("⚠️ No questions found in lesson {}, returning 1", lessonId);
        return 1;
    }

    // Thêm vào cuối class GrammarQuestionService

    // ===== BULK OPERATIONS =====

    /**
     * Tạo nhiều questions cùng lúc
     */
    @Transactional
    public List<GrammarQuestionDTO> createQuestionsInBulk(Long lessonId, List<GrammarQuestionDTO> questionDTOs) {
        GrammarLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại"));

        Long currentCount = questionRepository.countByParentTypeAndParentId(ParentType.GRAMMAR, lessonId);
        final int[] orderIndex = { currentCount.intValue() };

        List<Question> questions = questionDTOs.stream()
                .map(dto -> {
                    try {
                        validateQuestionDTO(dto);
                    } catch (RuntimeException e) {
                        log.warn("⚠️ Skipping invalid question: {}", e.getMessage());
                        return null;
                    }

                    Question question = new Question();
                    question.setParentType(ParentType.GRAMMAR);
                    question.setParentId(lessonId);
                    question.setQuestionText(dto.getQuestionText());
                    question.setQuestionType(dto.getQuestionType());

                    if (dto.getQuestionType() != QuestionType.MULTIPLE_CHOICE) {
                        question.setCorrectAnswer(dto.getCorrectAnswer());
                    }

                    question.setExplanation(dto.getExplanation());
                    question.setPoints(dto.getPoints() != null ? dto.getPoints() : 5);
                    question.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : ++orderIndex[0]);
                    question.setCreatedAt(LocalDateTime.now());
                    return question;
                })
                .filter(q -> q != null)
                .collect(Collectors.toList());

        List<Question> savedQuestions = questionRepository.saveAll(questions);

        // Create options
        for (int i = 0; i < savedQuestions.size(); i++) {
            Question savedQuestion = savedQuestions.get(i);

            GrammarQuestionDTO dto = questionDTOs.stream()
                    .filter(d -> d.getQuestionText().equals(savedQuestion.getQuestionText()))
                    .findFirst()
                    .orElse(null);

            if (dto != null &&
                    dto.getQuestionType() == QuestionType.MULTIPLE_CHOICE &&
                    dto.getOptions() != null && !dto.getOptions().isEmpty()) {

                List<QuestionOption> options = dto.getOptions().stream()
                        .map(optionDTO -> {
                            QuestionOption option = new QuestionOption();
                            option.setQuestion(savedQuestion);
                            option.setOptionText(optionDTO.getOptionText());
                            option.setIsCorrect(optionDTO.getIsCorrect() != null ? optionDTO.getIsCorrect() : false);
                            option.setOrderIndex(optionDTO.getOrderIndex());
                            return option;
                        })
                        .collect(Collectors.toList());

                optionRepository.saveAll(options);
                log.info("✅ Created {} options for question id={}", options.size(), savedQuestion.getId());
            }
        }

        log.info("✅ Created {} new questions for lesson: {}", questions.size(), lesson.getTitle());
        return questions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Copy questions từ lesson này sang lesson khác
     */
    @Transactional
    public void copyQuestionsToLesson(Long sourceLessonId, Long targetLessonId) {
        if (!lessonRepository.existsById(sourceLessonId)) {
            throw new RuntimeException("Source lesson không tồn tại");
        }
        if (!lessonRepository.existsById(targetLessonId)) {
            throw new RuntimeException("Target lesson không tồn tại");
        }

        List<Question> sourceQuestions = questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(
                ParentType.GRAMMAR, sourceLessonId);

        long currentCount = questionRepository.countByParentTypeAndParentId(ParentType.GRAMMAR, targetLessonId);

        for (Question source : sourceQuestions) {
            Question newQuestion = new Question();
            newQuestion.setParentType(ParentType.GRAMMAR);
            newQuestion.setParentId(targetLessonId);
            newQuestion.setQuestionText(source.getQuestionText());
            newQuestion.setQuestionType(source.getQuestionType());
            newQuestion.setCorrectAnswer(source.getCorrectAnswer());
            newQuestion.setExplanation(source.getExplanation());
            newQuestion.setPoints(source.getPoints());
            newQuestion.setOrderIndex((int) ++currentCount);
            newQuestion.setCreatedAt(LocalDateTime.now());

            Question savedQuestion = questionRepository.save(newQuestion);

            // Copy options
            List<QuestionOption> sourceOptions = optionRepository
                    .findByQuestionIdOrderByOrderIndexAsc(source.getId());
            if (!sourceOptions.isEmpty()) {
                List<QuestionOption> newOptions = sourceOptions.stream()
                        .map(sourceOption -> {
                            QuestionOption newOption = new QuestionOption();
                            newOption.setQuestion(savedQuestion);
                            newOption.setOptionText(sourceOption.getOptionText());
                            newOption.setIsCorrect(sourceOption.getIsCorrect());
                            newOption.setOrderIndex(sourceOption.getOrderIndex());
                            return newOption;
                        })
                        .collect(Collectors.toList());

                optionRepository.saveAll(newOptions);
            }
        }

        log.info("✅ Copied {} questions from lesson {} to lesson {}",
                sourceQuestions.size(), sourceLessonId, targetLessonId);
    }

    // ===== VALIDATION =====

    private void validateQuestionDTO(GrammarQuestionDTO dto) {
        switch (dto.getQuestionType()) {
            case MULTIPLE_CHOICE:
                if (dto.getOptions() == null || dto.getOptions().size() < 2) {
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

    private GrammarQuestionDTO convertToDTO(Question question) {
        List<QuestionOption> options = optionRepository.findByQuestionIdOrderByOrderIndexAsc(question.getId());

        List<GrammarQuestionOptionDTO> optionDTOs = options.stream()
                .map(option -> new GrammarQuestionOptionDTO(
                        option.getId(),
                        question.getId(),
                        option.getOptionText(),
                        option.getIsCorrect(),
                        option.getOrderIndex()))
                .collect(Collectors.toList());

        GrammarQuestionDTO dto = new GrammarQuestionDTO(
                question.getId(),
                question.getParentId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getCorrectAnswer(),
                question.getExplanation(),
                question.getPoints(),
                question.getOrderIndex(),
                question.getCreatedAt(),
                optionDTOs.isEmpty() ? null : optionDTOs,
                true);

        dto.setOrderIndex(question.getOrderIndex());
        return dto;
    }
}