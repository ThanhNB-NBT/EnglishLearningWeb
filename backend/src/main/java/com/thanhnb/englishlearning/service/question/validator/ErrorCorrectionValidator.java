package com.thanhnb.englishlearning.service.question.validator;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.enums.QuestionType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ErrorCorrectionValidator implements AnswerValidator {

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.ERROR_CORRECTION;
    }

    @Override
    public QuestionResultDTO validate(Map<String, Object> meta, Object userAnswer, QuestionResultDTO.QuestionResultDTOBuilder builder) {
        String correction = (String) meta.get("correction");
        String errorText = (String) meta.get("errorText");
        String explanation = (String) meta.get("explanation");

        if (correction == null) {
            return builder.isCorrect(false).feedback("Lỗi dữ liệu: Thiếu đáp án sửa lỗi.").build();
        }

        String userStr = userAnswer != null ? String.valueOf(userAnswer).trim() : "";
        boolean isCorrect = correction.equalsIgnoreCase(userStr);

        return builder
                .correctAnswer(correction)
                .isCorrect(isCorrect)
                .feedback(isCorrect 
                    ? "Chính xác!" 
                    : String.format("Sai rồi. Lỗi sai ở '%s', sửa đúng là: %s", errorText, correction))
                .explanation(explanation)
                .build();
    }
}