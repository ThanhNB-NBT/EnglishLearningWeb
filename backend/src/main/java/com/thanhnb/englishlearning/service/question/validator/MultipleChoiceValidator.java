// File: backend/src/main/java/com/thanhnb/englishlearning/service/question/validator/MultipleChoiceValidator.java
package com.thanhnb.englishlearning.service.question.validator;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.enums.QuestionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MultipleChoiceValidator implements AnswerValidator {

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.MULTIPLE_CHOICE;
    }

    // True/False dùng chung logic này, ta có thể đăng ký thêm hoặc handle riêng nếu muốn strict
    public boolean supports(QuestionType type) {
        return type == QuestionType.MULTIPLE_CHOICE || type == QuestionType.TRUE_FALSE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public QuestionResultDTO validate(Map<String, Object> meta, Object userAnswer, QuestionResultDTO.QuestionResultDTOBuilder builder) {
        List<Map<String, Object>> options = (List<Map<String, Object>>) meta.get("options");
        String explanation = (String) meta.get("explanation");

        if (options == null || options.isEmpty()) {
            return builder.isCorrect(false).feedback("Lỗi dữ liệu: Không có tùy chọn.").build();
        }

        // Tìm đáp án đúng trong metadata
        Map<String, Object> correctOption = options.stream()
                .filter(o -> Boolean.TRUE.equals(o.get("isCorrect")))
                .findFirst()
                .orElse(null);

        if (correctOption == null) {
            return builder.isCorrect(false).feedback("Lỗi dữ liệu: Không có đáp án đúng.").build();
        }

        String correctText = String.valueOf(correctOption.get("text"));

        if (userAnswer == null) {
            return builder.correctAnswer(correctText).isCorrect(false).feedback("Bạn chưa chọn đáp án.").build();
        }

        String userAnswerStr = String.valueOf(userAnswer);
        boolean isCorrect = correctText.equalsIgnoreCase(userAnswerStr);

        return builder
                .correctAnswer(correctText)
                .isCorrect(isCorrect)
                .feedback(isCorrect ? "Chính xác!" : "Sai rồi. Đáp án đúng là: " + correctText)
                .explanation(explanation)
                .build();
    }
}