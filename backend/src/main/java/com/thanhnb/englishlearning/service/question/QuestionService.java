package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý questions chung cho tất cả module
 * Refactored để tích hợp với:
 * - QuestionConversionService: Convert entity <-> DTO
 * - QuestionValidationService: Validate và check answers
 * - Metadata-based architecture (JSONB)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionMapper conversionService;
    private final QuestionAnswerProcessor validationService;

    // ═══════════════════════════════════════════════════════════════
    // LOAD & COUNT OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Load questions by parentType and parentId
     */
    public List<Question> loadQuestionsByParent(ParentType parentType, Long parentId) {
        return questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(parentType, parentId);
    }

    /**
     * Count questions by parentType and parentId
     */
    public long countQuestionsByParent(ParentType parentType, Long parentId) {
        return questionRepository.countByParentTypeAndParentId(parentType, parentId);
    }

    /**
     * Get single question by ID
     */
    public Question getQuestionById(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại với id: " + questionId));
    }

    // ═══════════════════════════════════════════════════════════════
    // CONVERSION OPERATIONS (Delegate to ConversionService)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Convert Question entity -> QuestionResponseDTO
     * Tự động shuffle options cho MULTIPLE_CHOICE để tránh gian lận
     * 
     * @param question          Question entity
     * @param shuffleOptions    Có shuffle options không (cho MULTIPLE_CHOICE)
     */
    public QuestionResponseDTO convertToDTO(Question question, boolean shuffleOptions) {
        QuestionResponseDTO dto = conversionService.convertToDTO(question);
        
        // Shuffle options cho MULTIPLE_CHOICE
        if (shuffleOptions && question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            dto.setMetadata(shuffleMultipleChoiceOptions(dto.getMetadata()));
            log.debug("🔀 Shuffled options for question {}", question.getId());
        }
        
        return dto;
    }

    /**
     * Convert Question entity -> QuestionResponseDTO (no shuffle)
     */
    public QuestionResponseDTO convertToDTO(Question question) {
        return conversionService.convertToDTO(question);
    }

    /**
     * Convert list of questions to DTOs
     */
    public List<QuestionResponseDTO> convertToDTOs(List<Question> questions, boolean shuffleOptions) {
        return questions.stream()
                .map(q -> convertToDTO(q, shuffleOptions))
                .collect(Collectors.toList());
    }

    /**
     * Convert list of questions to DTOs (no shuffle)
     */
    public List<QuestionResponseDTO> convertToDTOs(List<Question> questions) {
        return conversionService.convertToDTOs(questions);
    }

    // ═══════════════════════════════════════════════════════════════
    // ANSWER PROCESSING & VALIDATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Process answers và trả về kết quả chi tiết
     * 
     * @param answers            Danh sách câu trả lời của user
     * @param expectedParentType ParentType expected (GRAMMAR/READING/LISTENING)
     * @return Danh sách kết quả chi tiết từng câu
     */
    public List<QuestionResultDTO> processAnswers(
            List<SubmitAnswerRequest> answers,
            ParentType expectedParentType) {

        return answers.stream().map(answerRequest -> {
            // Load question
            Question question = getQuestionById(answerRequest.getQuestionId());

            // Validate parentType
            if (question.getParentType() != expectedParentType) {
                throw new RuntimeException(
                        String.format("Question %d không thuộc %s module",
                                question.getId(), expectedParentType));
            }

            // Validate answer using QuestionValidationService
            QuestionResultDTO result = validationService.validateAnswer(question, answerRequest.getAnswer());
            
            log.debug("Question {}: {} -> {} points",
                    question.getId(), 
                    Boolean.TRUE.equals(result.getIsCorrect()) ? "✅ Correct" : "❌ Wrong", 
                    result.getPoints());

            return result;
            
        }).collect(Collectors.toList());
    }

    /**
     * Validate answer count
     * 
     * @throws RuntimeException if answer count mismatch
     */
    public void validateAnswerCount(List<SubmitAnswerRequest> answers,
            ParentType parentType,
            Long parentId) {
        long expectedCount = countQuestionsByParent(parentType, parentId);

        if (answers == null || answers.isEmpty()) {
            throw new RuntimeException("Bài này cần có câu trả lời");
        }

        if (answers.size() < expectedCount) {
            throw new RuntimeException(
                    String.format("Vui lòng trả lời tất cả %d câu hỏi", expectedCount));
        }

        log.debug("Validated {} answers (expected: {})", answers.size(), expectedCount);
    }

    // ═══════════════════════════════════════════════════════════════
    // SCORING UTILITIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Calculate total score from results
     */
    public int calculateTotalScore(List<QuestionResultDTO> results) {
        return results.stream()
                .filter(r -> r.getPoints() != null)
                .mapToInt(QuestionResultDTO::getPoints)
                .sum();
    }

    /**
     * Calculate correct count from results
     */
    public int calculateCorrectCount(List<QuestionResultDTO> results) {
        return (int) results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                .count();
    }

    /**
     * Calculate score percentage
     */
    public double calculateScorePercentage(int correctCount, int totalQuestions) {
        if (totalQuestions == 0) {
            return 0;
        }
        return (double) correctCount / totalQuestions * 100;
    }

    /**
     * Calculate score percentage from results
     */
    public double calculateScorePercentage(List<QuestionResultDTO> results) {
        if (results == null || results.isEmpty()) {
            return 0;
        }
        int correctCount = calculateCorrectCount(results);
        return calculateScorePercentage(correctCount, results.size());
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Shuffle options trong metadata cho MULTIPLE_CHOICE
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> shuffleMultipleChoiceOptions(Map<String, Object> metadata) {
        if (metadata == null || !metadata.containsKey("options")) {
            return metadata;
        }

        try {
            // Clone metadata để không modify original
            Map<String, Object> shuffledMetadata = new HashMap<>(metadata);
            
            List<Map<String, Object>> options = (List<Map<String, Object>>) metadata.get("options");
            if (options != null && options.size() > 1) {
                List<Map<String, Object>> shuffledOptions = new ArrayList<>(options);
                Collections.shuffle(shuffledOptions);
                
                // Re-index after shuffle
                for (int i = 0; i < shuffledOptions.size(); i++) {
                    shuffledOptions.get(i).put("order", i + 1);
                }
                
                shuffledMetadata.put("options", shuffledOptions);
            }
            
            return shuffledMetadata;
        } catch (Exception e) {
            log.warn("Cannot shuffle options: {}", e.getMessage());
            return metadata;
        }
    }

    /**
     * Get questions by IDs (for bulk operations)
     */
    public List<Question> getQuestionsByIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return questionRepository.findAllById(questionIds);
    }

    /**
     * Validate if all questions belong to specific parent
     */
    public void validateQuestionsParent(List<Long> questionIds, ParentType parentType, Long parentId) {
        List<Question> questions = getQuestionsByIds(questionIds);
        
        boolean allMatch = questions.stream()
                .allMatch(q -> q.getParentType() == parentType && q.getParentId().equals(parentId));
        
        if (!allMatch) {
            throw new RuntimeException(
                    String.format("Một số câu hỏi không thuộc %s với id %d", parentType, parentId));
        }
    }
}