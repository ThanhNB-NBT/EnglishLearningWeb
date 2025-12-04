package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.service.question.validator.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service điều phối việc kiểm tra đáp án.
 * Sử dụng Strategy Pattern để chọn Validator phù hợp.
 */
@Service
@Slf4j
public class QuestionAnswerProcessor {

    private final Map<QuestionType, AnswerValidator> validatorMap;

    // Constructor injection: Spring tự động inject tất cả các class implement
    // AnswerValidator
    public QuestionAnswerProcessor(List<AnswerValidator> validators) {
        // 1. Map các validator theo type chính của chúng
        this.validatorMap = validators.stream()
                .collect(Collectors.toMap(AnswerValidator::getSupportedType, Function.identity()));

        // 2. Map thủ công các type dùng chung logic (Alias)
        for (AnswerValidator v : validators) {
            if (v instanceof MultipleChoiceValidator) {
                validatorMap.put(QuestionType.TRUE_FALSE, v);
                validatorMap.put(QuestionType.COMPLETE_CONVERSATION, v);
            }
            if (v instanceof FillBlankValidator) {
                validatorMap.put(QuestionType.VERB_FORM, v);
                validatorMap.put(QuestionType.TEXT_ANSWER, v);
            }
            if (v instanceof SentenceTransformationValidator) {
                validatorMap.put(QuestionType.SENTENCE_TRANSFORMATION, v);
            }

        }
    }

    /**
     * Validate user answer and return detailed result
     */
    public QuestionResultDTO validateAnswer(Question question, Object userAnswer) {
        Map<String, Object> metadata = extractMetadata(question);

        if (metadata == null) {
            return buildErrorResult(question, "Metadata không hợp lệ hoặc bị thiếu.");
        }

        String explanation = (String) metadata.get("explanation");

        // Builder cơ bản
        QuestionResultDTO.QuestionResultDTOBuilder builder = QuestionResultDTO.builder()
                .questionId(question.getId())
                .questionText(question.getQuestionText())
                .userAnswer(userAnswer != null ? String.valueOf(userAnswer) : "")
                .points(question.getPoints())
                .explanation(explanation);

        // Lấy validator tương ứng
        AnswerValidator validator = validatorMap.get(question.getQuestionType());

        if (validator != null) {
            try {
                return validator.validate(metadata, userAnswer, builder);
            } catch (Exception e) {
                log.error("Error validating question {}: {}", question.getId(), e.getMessage(), e);
                return buildErrorResult(question, "Lỗi xử lý đáp án: " + e.getMessage());
            }
        } else {
            // Fallback cho các loại chưa có validator (VD: OPEN_ENDED)
            if (question.getQuestionType() == QuestionType.OPEN_ENDED) {
                String suggested = (String) metadata.get("suggestedAnswer");
                return builder
                        .correctAnswer(suggested)
                        .isCorrect(null) // Cần AI chấm hoặc giáo viên chấm
                        .feedback("Câu trả lời đã được ghi nhận. Chờ đánh giá.")
                        .build();
            }

            return buildErrorResult(question, "Loại câu hỏi chưa được hỗ trợ validate: " + question.getQuestionType());
        }
    }

    @Deprecated
    public boolean checkAnswer(Question question,
            com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest request) {
        QuestionResultDTO result = validateAnswer(question, request.getAnswer());
        return Boolean.TRUE.equals(result.getIsCorrect());
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMetadata(Question question) {
        Object metadata = question.getMetadata();
        if (metadata == null)
            return null;
        if (metadata instanceof Map)
            return (Map<String, Object>) metadata;
        return null;
    }

    private QuestionResultDTO buildErrorResult(Question question, String msg) {
        return QuestionResultDTO.builder()
                .questionId(question.getId())
                .questionText(question.getQuestionText())
                .isCorrect(false)
                .feedback(msg)
                .points(0)
                .build();
    }
}