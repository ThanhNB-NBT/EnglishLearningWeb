package com.thanhnb.englishlearning.service.question;

import com.fasterxml.jackson.core.type.TypeReference;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Abstract base service cho CRUD questions
 * - Chứa logic chung cho tất cả module (Grammar, Reading, Listening, etc.)
 * - Subclass chỉ cần implement getParentType() và validateLessonExists()
 */
@Slf4j
public abstract class BaseQuestionService {

    @Autowired
    protected QuestionRepository questionRepository;

    @Autowired
    protected QuestionMapper conversionService;

    @Autowired
    protected QuestionMetadataValidator metadataValidator;

    @Autowired
    protected QuestionDTOBuilder dtoFactory;

    @Autowired
    protected com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * TEMPLATE METHOD: Subclass override để chỉ định ParentType
     */
    protected abstract ParentType getParentType();

    /**
     * TEMPLATE METHOD: Subclass validate lesson exists
     */
    protected abstract void validateLessonExists(Long lessonId);

    /**
     * TEMPLATE METHOD: Reorder after delete (optional)
     */
    protected void reorderAfterDelete(Long lessonId, Integer deletedOrderIndex) {
        // Default: do nothing
        // Subclass can override if needed
    }

    // ═══════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Get questions with pagination
     */
    public Page<QuestionResponseDTO> getQuestionsByLessonPaginated(Long lessonId, Pageable pageable) {
        validateLessonExists(lessonId);

        return questionRepository.findByParentTypeAndParentId(
                getParentType(), lessonId, pageable)
                .map(conversionService::convertToDTO);
    }

    /**
     * Get all questions for a lesson
     */
    public List<QuestionResponseDTO> getQuestionsByLesson(Long lessonId) {
        validateLessonExists(lessonId);

        return questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(
                getParentType(), lessonId)
                .stream()
                .map(conversionService::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get single question by ID
     */
    public QuestionResponseDTO getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại với id: " + id));

        if (!question.getParentType().equals(getParentType())) {
            throw new RuntimeException("Câu hỏi không thuộc module " + getParentType());
        }

        return conversionService.convertToDTO(question);
    }

    /**
     * Get next order index
     */
    public Integer getNextOrderIndex(Long lessonId) {
        validateLessonExists(lessonId);

        return questionRepository.findFirstByParentTypeAndParentIdOrderByOrderIndexDesc(
                getParentType(), lessonId)
                .map(q -> q.getOrderIndex() + 1)
                .orElse(1);
    }

    // ═══════════════════════════════════════════════════════════
    // CREATE OPERATIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Create single question
     */
    @Transactional
    public QuestionResponseDTO createQuestion(CreateQuestionDTO createDTO) {
        validateLessonExists(createDTO.getParentId());

        if (!createDTO.getParentType().equals(getParentType())) {
            throw new RuntimeException(String.format("Questien parentType phải là %s, không phải %s",
                    getParentType(), createDTO.getParentType()));
        }

        if (createDTO.getOrderIndex() == null || createDTO.getOrderIndex() == 0) {
            createDTO.setOrderIndex(getNextOrderIndex(createDTO.getParentId()));
            log.debug("Set orderIndex={} for new question", createDTO.getOrderIndex());
        }

        Question question = dtoFactory.createEntity(createDTO);
        Question savedQuestion = questionRepository.save(question);

        log.info("Created {} question: {} (id={})",
                getParentType(), savedQuestion.getQuestionText(), savedQuestion.getId());

        return conversionService.convertToDTO(savedQuestion);
    }

    /**
     * Create multiple questions in bulk
     */
    /**
     * Create multiple questions in bulk
     * Optimized: Only queries DB once for nextOrderIndex
     */
    @Transactional
    public List<QuestionResponseDTO> createQuestionsInBulk(Long lessonId, List<CreateQuestionDTO> createDTOs) {
        validateLessonExists(lessonId);

        if (createDTOs == null || createDTOs.isEmpty()) {
            log.warn("Attempted to create bulk questions with empty list for lesson {}", lessonId);
            return List.of();
        }

        // Query DB once for next order index
        Integer nextOrder = getNextOrderIndex(lessonId);
        log.debug("Starting bulk creation with nextOrderIndex={} for {} questions",
                nextOrder, createDTOs.size());

        AtomicInteger currentOrder = new AtomicInteger(nextOrder);

        List<Question> questions = createDTOs.stream()
                .map(dto -> {
                    // Force parentId and parentType
                    dto.setParentId(lessonId);
                    dto.setParentType(getParentType());

                    // Auto-increment orderIndex if not set
                    if (dto.getOrderIndex() == null || dto.getOrderIndex() == 0) {
                        dto.setOrderIndex(currentOrder.getAndIncrement());
                    }

                    return dtoFactory.createEntity(dto);
                })
                .collect(Collectors.toList());

        // Batch save all questions
        List<Question> savedQuestions = questionRepository.saveAll(questions);

        log.info("✅ Created {} questions in bulk for {} lesson {} (orderIndex: {} to {})",
                savedQuestions.size(), getParentType(), lessonId,
                nextOrder, nextOrder + savedQuestions.size() - 1);

        return savedQuestions.stream()
                .map(conversionService::convertToDTO)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // 🔄 UPDATE OPERATIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Update question
     */
    @Transactional
    public QuestionResponseDTO updateQuestion(Long id, Object dto) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại với id: " + id));

        if (!question.getParentType().equals(getParentType())) {
            throw new RuntimeException("Câu hỏi không thuộc module " + getParentType());
        }

        if (dto instanceof CreateQuestionDTO createDTO) {
            updateQuestionFromCreateDTO(question, createDTO);
        } else if (dto instanceof QuestionResponseDTO responseDTO) {
            updateQuestionFromResponseDTO(question, responseDTO);
        } else {
            throw new IllegalArgumentException("DTO type not supported: " + dto.getClass().getName());
        }

        Question savedQuestion = questionRepository.save(question);

        log.info("Updated {} question id={}", getParentType(), id);
        return conversionService.convertToDTO(savedQuestion);
    }

    /**
     * Update từ CreateDTO
     */
    private void updateQuestionFromCreateDTO(Question question, CreateQuestionDTO dto) {
        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());
        question.setExplanation(dto.getExplanation());

        if (dto.getPoints() != null) {
            question.setPoints(dto.getPoints());
        }

        if (dto.getOrderIndex() != null) {
            question.setOrderIndex(dto.getOrderIndex());
        }

        // Rebuild metadata
        Map<String, Object> newMetadata = dtoFactory.buildMetadata(dto);
        question.setMetadata(newMetadata);
    }

    /**
     * Update từ ResponseDTO (backward compatibility)
     */
    private void updateQuestionFromResponseDTO(Question question, QuestionResponseDTO dto) {
        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());
        question.setExplanation(dto.getExplanation());

        if (dto.getPoints() != null) {
            question.setPoints(dto.getPoints());
        }

        if (dto.getOrderIndex() != null) {
            question.setOrderIndex(dto.getOrderIndex());
        }

        if (dto.getMetadata() != null) {
            question.setMetadata(dto.getMetadata());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DELETE OPERATIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Delete single question
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

        questionRepository.delete(question);

        log.info("Deleted {} question id={} (orderIndex={})",
                getParentType(), id, deletedOrderIndex);

        reorderAfterDelete(lessonId, deletedOrderIndex);
    }

    /**
     * Bulk delete questions
     */
    @Transactional
    public int bulkDeleteQuestions(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return 0;
        }

        List<Question> toDelete = questionRepository.findAllById(questionIds).stream()
                .filter(q -> q.getParentType() == getParentType())
                .collect(Collectors.toList());

        if (toDelete.isEmpty()) {
            log.warn("No {} questions found in provided IDs", getParentType());
            return 0;
        }

        // Group by lesson for reindexing
        Map<Long, List<Question>> byLesson = toDelete.stream()
                .collect(Collectors.groupingBy(Question::getParentId));

        // Delete questions
        questionRepository.deleteAll(toDelete);

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

        log.info("Bulk deleted {} {} questions; reindexed {} items across {} lessons",
                toDelete.size(), getParentType(), totalReindexed, byLesson.size());

        return toDelete.size();
    }

    // ═══════════════════════════════════════════════════════════
    // COPY OPERATIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Copy questions from one lesson to another
     */
    @Transactional
    public void copyQuestionsToLesson(Long sourceLessonId, Long targetLessonId) {
        validateLessonExists(sourceLessonId);
        validateLessonExists(targetLessonId);

        List<Question> sourceQuestions = questionRepository
                .findByParentTypeAndParentIdOrderByOrderIndexAsc(getParentType(), sourceLessonId);

        Integer nextOrder = getNextOrderIndex(targetLessonId);

        List<Question> copiedQuestions = sourceQuestions.stream()
                .map(source -> copyQuestion(source, targetLessonId, nextOrder))
                .collect(Collectors.toList());

        questionRepository.saveAll(copiedQuestions);

        log.info("Copied {} questions from {} lesson {} to lesson {}",
                sourceQuestions.size(), getParentType(), sourceLessonId, targetLessonId);
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * Copy question entity
     */
    private Question copyQuestion(Question source, Long newLessonId, int newOrderIndex) {
        Question copy = new Question();
        copy.setParentType(getParentType());
        copy.setParentId(newLessonId);
        copy.setQuestionText(source.getQuestionText());
        copy.setQuestionType(source.getQuestionType());
        copy.setExplanation(source.getExplanation());
        copy.setPoints(source.getPoints());
        copy.setOrderIndex(newOrderIndex);
        Object sourceMetadata = source.getMetadata();
        if (sourceMetadata instanceof Map) {
            try {
                String json = objectMapper.writeValueAsString(sourceMetadata);
                Map<String, Object> clonedMetadata = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                copy.setMetadata(clonedMetadata);
            } catch (Exception e) {
                log.error("Failed to clone metadata, using shallow copy", e);
                @SuppressWarnings("unchecked")
                Map<String, Object> metadataMap = (Map<String, Object>) sourceMetadata;
                copy.setMetadata(metadataMap);
            }
        } else {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadataMap = sourceMetadata != null ? (Map<String, Object>) sourceMetadata : null;
            copy.setMetadata(metadataMap);
        }
        copy.setCreatedAt(LocalDateTime.now());

        log.debug("Copied question id={} to lesson={} with orderIndex={}",
                source.getId(), newLessonId, newOrderIndex);
        return copy;
    }
}