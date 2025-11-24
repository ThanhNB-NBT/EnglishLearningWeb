package com.thanhnb.englishlearning.service.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.QuestionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Single Responsibility: Process and validate user answers for all question types
 * 
 * Features:
 * - Check if answer is correct
 * - Generate feedback and hints
 * - Calculate points
 * - Return detailed results
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionAnswerProcessor {

    private final ObjectMapper objectMapper;

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API - Main validation method
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validate user answer and return detailed result
     * 
     * @param question Question entity
     * @param userAnswer User's submitted answer
     * @return QuestionResultDTO with validation result, feedback, hint
     */
    public QuestionResultDTO validateAnswer(Question question, Object userAnswer) {
        Map<String, Object> metadata = extractMetadata(question);
        
        if (metadata == null) {
            return buildErrorResult(question, "Metadata không hợp lệ");
        }

        QuestionResultDTO.QuestionResultDTOBuilder builder = QuestionResultDTO.builder()
            .questionId(question.getId())
            .questionText(question.getQuestionText())
            .userAnswer(userAnswer != null ? String.valueOf(userAnswer) : "")
            .explanation(question.getExplanation())
            .points(question.getPoints());

        return switch (question.getQuestionType()) {
            case MULTIPLE_CHOICE, TRUE_FALSE -> 
                validateMultipleChoice(metadata, userAnswer, builder);
            case FILL_BLANK, VERB_FORM, SHORT_ANSWER, ERROR_CORRECTION -> 
                validateTextAnswer(metadata, userAnswer, builder);
            case MATCHING -> 
                validateMatching(metadata, userAnswer, builder);
            case SENTENCE_BUILDING -> 
                validateSentenceBuilding(metadata, userAnswer, builder);
            case COMPLETE_CONVERSATION -> 
                validateConversation(metadata, userAnswer, builder);
            case PRONUNCIATION -> 
                validatePronunciation(metadata, userAnswer, builder);
            case READING_COMPREHENSION -> 
                validateReadingComprehension(metadata, userAnswer, builder);
            case OPEN_ENDED -> 
                validateOpenEnded(metadata, userAnswer, builder);
        };
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use validateAnswer() instead
     */
    @Deprecated
    public boolean checkAnswer(Question question, SubmitAnswerRequest request) {
        QuestionResultDTO result = validateAnswer(question, request.getAnswer());
        return Boolean.TRUE.equals(result.getIsCorrect());
    }

    /**
     * Generate hint when answer is wrong
     */
    public String generateHint(Question question) {
        if (question.getQuestionType() == QuestionType.OPEN_ENDED) {
            return "Câu trả lời của bạn cần được đánh giá bởi AI/giáo viên.";
        }

        Map<String, Object> metadata = extractMetadata(question);
        if (metadata != null && metadata.containsKey("hint")) {
            String hint = (String) metadata.get("hint");
            if (hint != null && !hint.isEmpty()) {
                return hint;
            }
        }

        if (question.getExplanation() != null && !question.getExplanation().isEmpty()) {
            return question.getExplanation();
        }

        return "Hãy xem lại đáp án đúng và thử lại!";
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE VALIDATION METHODS - One per question type
    // ═══════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private QuestionResultDTO validateMultipleChoice(
            Map<String, Object> meta, 
            Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        
        List<Map<String, Object>> options = (List<Map<String, Object>>) meta.get("options");

        // Validation: options exist
        if (options == null || options.isEmpty()) {
            return builder
                .correctAnswer("")
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Không có tùy chọn nào được định nghĩa cho câu hỏi này.")
                .build();
        }

        // Find correct option
        Map<String, Object> correctOption = options.stream()
            .filter(o -> Boolean.TRUE.equals(o.get("isCorrect")))
            .findFirst()
            .orElse(null);

        if (correctOption == null) {
            return builder
                .correctAnswer("")
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Không có tùy chọn đúng nào được định nghĩa.")
                .build();
        }
        
        String correctText = (String) correctOption.get("text");

        // Check if user answered
        if (userAnswer == null) {
            return builder
                .correctAnswer(correctText)
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Bạn chưa chọn đáp án nào.")
                .build();
        }

        // Compare answers
        String userAnswerStr = String.valueOf(userAnswer);
        boolean isCorrect = correctText.equalsIgnoreCase(userAnswerStr);
        
        return builder
            .correctAnswer(correctText)
            .isCorrect(isCorrect)
            .hint((String) meta.get("hint"))
            .feedback(isCorrect ? "Chính xác!" : "Sai rồi. Đáp án đúng là: " + correctText)
            .build();
    }

    private QuestionResultDTO validateTextAnswer(
            Map<String, Object> meta,
            Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        
        String correctAnswer = (String) meta.get("correctAnswer");

        // Validation: correct answer exists
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            return builder
                .correctAnswer("")
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Không có đáp án đúng nào được định nghĩa.")
                .build();
        }

        Boolean caseSensitive = (Boolean) meta.getOrDefault("caseSensitive", false);
        String userAnswerStr = userAnswer != null ? String.valueOf(userAnswer).trim() : "";
        
        // Support multiple correct answers separated by "|"
        String[] possibleAnswers = correctAnswer.split("\\|");
        boolean isCorrect = Arrays.stream(possibleAnswers)
            .map(String::trim)
            .filter(a -> !a.isEmpty())
            .anyMatch(a -> caseSensitive 
                ? a.equals(userAnswerStr) 
                : a.equalsIgnoreCase(userAnswerStr));
        
        return builder
            .correctAnswer(correctAnswer)
            .isCorrect(isCorrect)
            .hint((String) meta.get("hint"))
            .feedback(isCorrect ? "Đúng rồi!" : "Chưa chính xác. Đáp án: " + correctAnswer)
            .build();
    }

    @SuppressWarnings("unchecked")
    private QuestionResultDTO validateMatching(
            Map<String, Object> meta,
            Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        
        List<Map<String, Object>> pairs = (List<Map<String, Object>>) meta.get("pairs");
        
        // Validation: pairs exist
        if (pairs == null || pairs.isEmpty()) {
            return builder
                .correctAnswer("")
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Không có cặp nào được định nghĩa.")
                .build();
        }

        // Validation: user answer format
        if (!(userAnswer instanceof Map)) {
            return builder
                .correctAnswer(convertToJsonString(pairs))
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Định dạng câu trả lời không hợp lệ (phải là Map)")
                .build();
        }
        
        Map<String, String> userPairs;
        try {
            userPairs = (Map<String, String>) userAnswer;
        } catch (ClassCastException e) {
            return builder
                .correctAnswer(convertToJsonString(pairs))
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Định dạng câu trả lời không hợp lệ (phải là Map<String, String>)")
                .build();
        }

        // Count correct pairs
        int correctCount = 0;
        int totalPairs = pairs.size();

        for (Map<String, Object> pair : pairs) {
            String left = (String) pair.get("left");
            String right = (String) pair.get("right");
            
            if (left == null || right == null) {
                log.warn("Invalid pair: {}", pair);
                continue;
            }

            String userRight = userPairs.get(left);
            if (right.equals(userRight)) {
                correctCount++;
            }
        }
        
        boolean isCorrect = correctCount == totalPairs;

        return builder
            .correctAnswer(convertToJsonString(pairs))
            .isCorrect(isCorrect)
            .hint((String) meta.get("hint"))
            .feedback(String.format("Bạn ghép đúng %d/%d cặp", correctCount, totalPairs))
            .build();
    }

    private QuestionResultDTO validateSentenceBuilding(
            Map<String, Object> meta,
            Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        
        String correctSentence = (String) meta.get("correctSentence");

        // Validation: correct sentence exists
        if (correctSentence == null || correctSentence.trim().isEmpty()) {
            return builder
                .correctAnswer("")
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Không có câu đúng nào được định nghĩa.")
                .build();
        }

        // Normalize both sentences
        String userAnswerStr = userAnswer != null 
            ? String.valueOf(userAnswer).trim().toLowerCase().replaceAll("\\s+", " ")
            : "";
        String correctNormalized = correctSentence.trim().toLowerCase().replaceAll("\\s+", " ");
        
        boolean isCorrect = userAnswerStr.equals(correctNormalized);
        
        return builder
            .correctAnswer(correctSentence)
            .isCorrect(isCorrect)
            .hint((String) meta.get("hint"))
            .feedback(isCorrect ? "Hoàn hảo!" : "Câu đúng: " + correctSentence)
            .build();
    }

    private QuestionResultDTO validateConversation(
            Map<String, Object> meta,
            Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        
        // Conversation uses same validation as text answer
        return validateTextAnswer(meta, userAnswer, builder);
    }

    @SuppressWarnings("unchecked")
    private QuestionResultDTO validatePronunciation(
            Map<String, Object> meta,
            Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        
        List<Map<String, Object>> correctClassifications = 
            (List<Map<String, Object>>) meta.get("correctClassifications");
        
        // Validation: classifications exist
        if (correctClassifications == null || correctClassifications.isEmpty()) {
            return builder
                .correctAnswer("")
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Không có phân loại đúng nào được định nghĩa.")
                .build();
        }

        // Validation: user answer format
        if (!(userAnswer instanceof Map)) {
            return builder
                .correctAnswer(convertToJsonString(correctClassifications))
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Định dạng câu trả lời không hợp lệ")
                .build();
        }
        
        Map<String, String> userClassifications;
        try {
            userClassifications = (Map<String, String>) userAnswer;
        } catch (ClassCastException e) {
            return builder
                .correctAnswer(convertToJsonString(correctClassifications))
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Định dạng câu trả lời không hợp lệ")
                .build();
        }

        // Count correct classifications
        int correctCount = 0;
        int totalWords = correctClassifications.size();
        
        for (Map<String, Object> classification : correctClassifications) {
            String word = (String) classification.get("word");
            String category = (String) classification.get("category");
            
            if (word == null || category == null) {
                log.warn("Invalid classification: {}", classification);
                continue;
            }
            
            String userCategory = userClassifications.get(word);
            if (category.equals(userCategory)) {
                correctCount++;
            }
        }
        
        boolean isCorrect = correctCount == totalWords;
        
        return builder
            .correctAnswer(convertToJsonString(correctClassifications))
            .isCorrect(isCorrect)
            .hint((String) meta.get("hint"))
            .feedback(String.format("Phân loại đúng %d/%d từ", correctCount, totalWords))
            .build();
    }

    @SuppressWarnings("unchecked")
    private QuestionResultDTO validateReadingComprehension(
            Map<String, Object> meta,
            Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        
        List<Map<String, Object>> blanks = (List<Map<String, Object>>) meta.get("blanks");
        
        // Validation: blanks exist
        if (blanks == null || blanks.isEmpty()) {
            return builder
                .correctAnswer("")
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Không có chỗ trống nào được định nghĩa.")
                .build();
        }

        // Validation: user answer format
        if (!(userAnswer instanceof Map)) {
            return builder
                .correctAnswer(convertToJsonString(blanks))
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Định dạng câu trả lời không hợp lệ")
                .build();
        }
        
        Map<?, ?> rawUserAnswers;
        try {
            rawUserAnswers = (Map<?, ?>) userAnswer;
        } catch (ClassCastException e) {
            return builder
                .correctAnswer(convertToJsonString(blanks))
                .isCorrect(false)
                .hint((String) meta.get("hint"))
                .feedback("Định dạng câu trả lời không hợp lệ")
                .build();
        }
        
        // Count correct blanks
        int correctCount = 0;
        int totalBlanks = blanks.size();
        
        for (Map<String, Object> blank : blanks) {
            Integer position = (Integer) blank.get("position");
            String correctAnswer = (String) blank.get("correctAnswer");
            
            if (position == null || correctAnswer == null) {
                log.warn("Invalid blank: {}", blank);
                continue;
            }

            // Handle both String and Integer keys
            String userBlankAnswer = null;
            if (rawUserAnswers.containsKey(position)) {
                userBlankAnswer = String.valueOf(rawUserAnswers.get(position));
            } else if (rawUserAnswers.containsKey(String.valueOf(position))) {
                userBlankAnswer = String.valueOf(rawUserAnswers.get(String.valueOf(position)));
            }

            if (correctAnswer.equals(userBlankAnswer)) {
                correctCount++;
            }
        }

        boolean isCorrect = correctCount == totalBlanks;

        return builder
            .correctAnswer(convertToJsonString(blanks))
            .isCorrect(isCorrect)
            .hint((String) meta.get("hint"))
            .feedback(String.format("Điền đúng %d/%d chỗ trống", correctCount, totalBlanks))
            .build();
    }

    private QuestionResultDTO validateOpenEnded(
            Map<String, Object> meta,
            Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        
        Object suggestedAnswer = meta.get("suggestedAnswer");
        
        return builder
            .correctAnswer(suggestedAnswer != null ? String.valueOf(suggestedAnswer) : "")
            .isCorrect(null) // Requires AI/teacher evaluation
            .hint((String) meta.get("hint"))
            .feedback("Câu trả lời của bạn đã được ghi nhận. Cần AI đánh giá.")
            .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Extract metadata from Question entity
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMetadata(Question question) {
        Object metadata = question.getMetadata();

        if (metadata == null) {
            log.warn("Question {} has no metadata", question.getId());
            return null;
        }

        if (metadata instanceof Map) {
            return (Map<String, Object>) metadata;
        }

        log.warn("Metadata is not a Map for question {} (type: {})",
                question.getId(), metadata.getClass().getName());
        return null;
    }

    /**
     * Convert object to JSON string safely
     */
    private String convertToJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON: {}", e.getMessage());
            return obj != null ? obj.toString() : "";
        }
    }

    /**
     * Build error result when metadata is invalid
     */
    private QuestionResultDTO buildErrorResult(Question question, String errorMessage) {
        return QuestionResultDTO.builder()
            .questionId(question.getId())
            .questionText(question.getQuestionText())
            .userAnswer("")
            .correctAnswer("")
            .isCorrect(false)
            .feedback(errorMessage)
            .hint(null)
            .explanation(question.getExplanation())
            .points(0)
            .build();
    }
}