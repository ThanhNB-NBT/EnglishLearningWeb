package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.dto.question.QuestionDTO;
import com.thanhnb.englishlearning.dto.question.QuestionOptionDTO;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.entity.question.QuestionOption;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.question.QuestionOptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ✅ Abstract base service cho CRUD questions
 * - Chứa logic chung cho tất cả module (Grammar, Reading, Listening, etc.)
 * - Subclass chỉ cần implement getParentType() và validateLessonExists()
 */
@Slf4j
public abstract class BaseQuestionService {

    @Autowired
    protected QuestionRepository questionRepository;

    @Autowired
    protected QuestionOptionRepository optionRepository;

    @Autowired
    protected QuestionValidationService validationService;

    @Autowired
    protected QuestionConversionService conversionService;

    /**
     * ✅ TEMPLATE METHOD: Subclass override để chỉ định ParentType
     */
    protected abstract ParentType getParentType();

    /**
     * ✅ TEMPLATE METHOD: Subclass validate lesson exists
     */
    protected abstract void validateLessonExists(Long lessonId);

    /**
     * ✅ TEMPLATE METHOD: Reorder after delete (optional - some modules may not need)
     */
    protected void reorderAfterDelete(Long lessonId, Integer deletedOrderIndex) {
        // Default: do nothing
        // Subclass can override if needed
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📖 READ OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Get questions with pagination
     */
    public Page<QuestionDTO> getQuestionsByLessonPaginated(Long lessonId, Pageable pageable) {
        validateLessonExists(lessonId);

        return questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(
                getParentType(), lessonId, pageable)
                .map(conversionService::convertToDTO);
    }

    /**
     * ✅ Get all questions for a lesson
     */
    public List<QuestionDTO> getQuestionsByLesson(Long lessonId) {
        validateLessonExists(lessonId);

        return questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(
                getParentType(), lessonId)
                .stream()
                .map(conversionService::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Get single question by ID
     */
    public QuestionDTO getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại với id: " + id));

        if (!question.getParentType().equals(getParentType())) {
            throw new RuntimeException("Câu hỏi không thuộc module " + getParentType());
        }

        return conversionService.convertToDTO(question);
    }

    /**
     * ✅ Get next order index
     */
    public Integer getNextOrderIndex(Long lessonId) {
        validateLessonExists(lessonId);

        Integer maxOrder = questionRepository.findMaxOrderIndexByParentTypeAndParentId(
                getParentType(), lessonId);

        return maxOrder != null ? maxOrder + 1 : 1;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ✍️ CREATE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Create single question
     */
    @Transactional
    public QuestionDTO createQuestion(QuestionDTO dto) {
        validateLessonExists(dto.getLessonId());
        validationService.validateQuestionDTO(dto, getParentType());

        // Create question entity
        Question question = buildQuestionEntity(dto);
        Question savedQuestion = questionRepository.save(question);

        log.info("✅ Created {} question: {} (id={})", 
                getParentType(), savedQuestion.getQuestionText(), savedQuestion.getId());

        // Create options if needed
        if (requiresOptions(dto.getQuestionType())) {
            saveOptions(savedQuestion, dto.getOptions());
        }

        return conversionService.convertToDTO(savedQuestion);
    }

    /**
     * ✅ Create multiple questions in bulk
     */
    @Transactional
    public List<QuestionDTO> createQuestionsInBulk(Long lessonId, List<QuestionDTO> dtos) {
        validateLessonExists(lessonId);

        Long currentCount = questionRepository.countByParentTypeAndParentId(getParentType(), lessonId);
        final int[] orderIndex = { currentCount.intValue() };

        List<Question> questions = dtos.stream()
                .map(dto -> {
                    try {
                        validationService.validateQuestionDTO(dto, getParentType());
                    } catch (RuntimeException e) {
                        log.warn("⚠️ Skipping invalid question: {}", e.getMessage());
                        return null;
                    }

                    dto.setLessonId(lessonId);
                    if (dto.getOrderIndex() == null) {
                        dto.setOrderIndex(++orderIndex[0]);
                    }

                    return buildQuestionEntity(dto);
                })
                .filter(q -> q != null)
                .collect(Collectors.toList());

        List<Question> savedQuestions = questionRepository.saveAll(questions);

        // Create options
        for (int i = 0; i < savedQuestions.size(); i++) {
            Question savedQuestion = savedQuestions.get(i);
            QuestionDTO dto = findMatchingDTO(dtos, savedQuestion);

            if (dto != null && requiresOptions(dto.getQuestionType()) && dto.getOptions() != null) {
                saveOptions(savedQuestion, dto.getOptions());
            }
        }

        log.info("✅ Created {} questions in bulk for {} lesson {}", 
                questions.size(), getParentType(), lessonId);

        return savedQuestions.stream()
                .map(conversionService::convertToDTO)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔄 UPDATE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Update question
     */
    @Transactional
    public QuestionDTO updateQuestion(Long id, QuestionDTO dto) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại với id: " + id));

        if (!question.getParentType().equals(getParentType())) {
            throw new RuntimeException("Câu hỏi không thuộc module " + getParentType());
        }

        validationService.validateQuestionDTO(dto, getParentType());

        // Update question fields
        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());
        question.setExplanation(dto.getExplanation());

        if (dto.getPoints() != null) {
            question.setPoints(dto.getPoints());
        }

        if (dto.getOrderIndex() != null) {
            question.setOrderIndex(dto.getOrderIndex());
        }

        // ✅ Handle correctAnswer based on question type
        if (requiresOptions(dto.getQuestionType())) {
            // MULTIPLE_CHOICE, TRUE_FALSE: correctAnswer in options
            question.setCorrectAnswer(null);
        } else {
            // FILL_BLANK, SHORT_ANSWER, etc.: correctAnswer in field
            question.setCorrectAnswer(dto.getCorrectAnswer());
        }

        Question savedQuestion = questionRepository.save(question);

        // ✅ Update options
        updateOptions(savedQuestion, dto);

        log.info("✅ Updated {} question id={}", getParentType(), id);
        return conversionService.convertToDTO(savedQuestion);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🗑️ DELETE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Delete single question
     */
    @Transactional
    public void deleteQuestion(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại với id: " + id));

        if (!question.getParentType().equals(getParentType())) {
            throw new RuntimeException("Câu hỏi không thuộc module " + getParentType());
        }

        Long lessonId = question.getParentId();
        Integer deletedOrderIndex = question.getOrderIndex();

        // Delete options first
        optionRepository.deleteByQuestionId(id);

        // Delete question
        questionRepository.delete(question);

        log.info("✅ Deleted {} question id={} (orderIndex={})", 
                getParentType(), id, deletedOrderIndex);

        // Reorder remaining questions
        reorderAfterDelete(lessonId, deletedOrderIndex);
    }

    /**
     * ✅ Bulk delete questions
     */
    @Transactional
    public int bulkDeleteQuestions(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return 0;
        }

        List<Question> toDelete = questionRepository.findByIdIn(questionIds);
        if (toDelete.isEmpty()) {
            return 0;
        }

        // Filter by parentType
        toDelete = toDelete.stream()
                .filter(q -> q.getParentType() == getParentType())
                .collect(Collectors.toList());

        if (toDelete.isEmpty()) {
            log.warn("⚠️ No {} questions found in provided IDs", getParentType());
            return 0;
        }

        // Delete options
        optionRepository.deleteByQuestionIdIn(
                toDelete.stream().map(Question::getId).toList());

        // Group by lesson
        Map<Long, List<Question>> byLesson = toDelete.stream()
                .collect(Collectors.groupingBy(Question::getParentId));

        // Delete questions
        questionRepository.deleteAllByIdInBatch(questionIds);

        // Reindex each lesson
        int totalReindexed = 0;
        for (Map.Entry<Long, List<Question>> entry : byLesson.entrySet()) {
            Long lessonId = entry.getKey();
            List<Question> remaining = questionRepository
                    .findByParentTypeAndParentIdOrderByOrderIndexAsc(getParentType(), lessonId);

            for (int i = 0; i < remaining.size(); i++) {
                remaining.get(i).setOrderIndex(i + 1);
            }

            if (!remaining.isEmpty()) {
                questionRepository.saveAll(remaining);
                totalReindexed += remaining.size();
            }
        }

        log.info("✅ Bulk deleted {} {} questions; reindexed {} items across {} lessons",
                toDelete.size(), getParentType(), totalReindexed, byLesson.size());

        return toDelete.size();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📋 COPY OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Copy questions from one lesson to another
     */
    @Transactional
    public void copyQuestionsToLesson(Long sourceLessonId, Long targetLessonId) {
        validateLessonExists(sourceLessonId);
        validateLessonExists(targetLessonId);

        List<Question> sourceQuestions = questionRepository
                .findByParentTypeAndParentIdOrderByOrderIndexAsc(getParentType(), sourceLessonId);

        long currentCount = questionRepository.countByParentTypeAndParentId(
                getParentType(), targetLessonId);

        for (Question source : sourceQuestions) {
            // Copy question
            Question newQuestion = copyQuestion(source, targetLessonId, (int) ++currentCount);
            Question savedQuestion = questionRepository.save(newQuestion);

            // Copy options
            List<QuestionOption> sourceOptions = optionRepository
                    .findByQuestionIdOrderByOrderIndexAsc(source.getId());

            if (!sourceOptions.isEmpty()) {
                List<QuestionOption> newOptions = sourceOptions.stream()
                        .map(opt -> copyOption(opt, savedQuestion))
                        .collect(Collectors.toList());

                optionRepository.saveAll(newOptions);
            }
        }

        log.info("✅ Copied {} questions from {} lesson {} to lesson {}",
                sourceQuestions.size(), getParentType(), sourceLessonId, targetLessonId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔧 PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Build question entity from DTO
     */
    private Question buildQuestionEntity(QuestionDTO dto) {
        Question question = new Question();
        question.setParentType(getParentType());
        question.setParentId(dto.getLessonId());
        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());
        question.setExplanation(dto.getExplanation());
        question.setPoints(dto.getPoints() != null ? dto.getPoints() : 5);
        question.setCreatedAt(LocalDateTime.now());

        // ✅ Set orderIndex
        if (dto.getOrderIndex() == null) {
            long count = questionRepository.countByParentTypeAndParentId(
                    getParentType(), dto.getLessonId());
            question.setOrderIndex((int) count + 1);
        } else {
            question.setOrderIndex(dto.getOrderIndex());
        }

        // ✅ Set correctAnswer based on question type
        if (requiresOptions(dto.getQuestionType())) {
            // Options-based questions: correctAnswer stored in options table
            question.setCorrectAnswer(null);
        } else {
            // Text-based questions: correctAnswer stored in question table
            question.setCorrectAnswer(dto.getCorrectAnswer());
        }

        return question;
    }

    /**
     * ✅ Check if question type requires options
     */
    private boolean requiresOptions(QuestionType type) {
        return type == QuestionType.MULTIPLE_CHOICE || type == QuestionType.TRUE_FALSE;
    }

    /**
     * ✅ Save options for question
     */
    private void saveOptions(Question question, List<QuestionOptionDTO> optionDTOs) {
        if (optionDTOs == null || optionDTOs.isEmpty()) {
            return;
        }

        List<QuestionOption> options = optionDTOs.stream()
                .map(dto -> {
                    QuestionOption option = new QuestionOption();
                    option.setQuestion(question);
                    option.setOptionText(dto.getOptionText());
                    option.setIsCorrect(dto.getIsCorrect() != null ? dto.getIsCorrect() : false);
                    option.setOrderIndex(dto.getOrderIndex());
                    return option;
                })
                .collect(Collectors.toList());

        optionRepository.saveAll(options);
        log.debug("✅ Saved {} options for question id={}", options.size(), question.getId());
    }

    /**
     * ✅ Update options for question
     */
    private void updateOptions(Question question, QuestionDTO dto) {
        // Delete old options
        optionRepository.deleteByQuestionId(question.getId());

        // Create new options if needed
        if (requiresOptions(dto.getQuestionType()) && dto.getOptions() != null) {
            saveOptions(question, dto.getOptions());
        }
    }

    /**
     * ✅ Find matching DTO from list (for bulk operations)
     */
    private QuestionDTO findMatchingDTO(List<QuestionDTO> dtos, Question question) {
        return dtos.stream()
                .filter(dto -> dto.getQuestionText().equals(question.getQuestionText()))
                .findFirst()
                .orElse(null);
    }

    /**
     * ✅ Copy question entity
     */
    private Question copyQuestion(Question source, Long newLessonId, int newOrderIndex) {
        Question copy = new Question();
        copy.setParentType(getParentType());
        copy.setParentId(newLessonId);
        copy.setQuestionText(source.getQuestionText());
        copy.setQuestionType(source.getQuestionType());
        copy.setCorrectAnswer(source.getCorrectAnswer());
        copy.setExplanation(source.getExplanation());
        copy.setPoints(source.getPoints());
        copy.setOrderIndex(newOrderIndex);
        copy.setCreatedAt(LocalDateTime.now());
        return copy;
    }

    /**
     * ✅ Copy option entity
     */
    private QuestionOption copyOption(QuestionOption source, Question newQuestion) {
        QuestionOption copy = new QuestionOption();
        copy.setQuestion(newQuestion);
        copy.setOptionText(source.getOptionText());
        copy.setIsCorrect(source.getIsCorrect());
        copy.setOrderIndex(source.getOrderIndex());
        return copy;
    }
}