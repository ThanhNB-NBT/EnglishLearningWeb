package com.thanhnb.englishlearning.service.question.validator;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.enums.QuestionType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SentenceBuildingValidator implements AnswerValidator {

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.SENTENCE_BUILDING;
    }

    @Override
    public QuestionResultDTO validate(Map<String, Object> meta, Object userAnswer, QuestionResultDTO.QuestionResultDTOBuilder builder) {
        String correctSentence = (String) meta.get("correctSentence");
        String explanation = (String) meta.get("explanation");

        if (correctSentence == null) {
            return builder.isCorrect(false).feedback("Lỗi dữ liệu: Thiếu câu đúng.").build();
        }

        // Normalize: trim, lowercase, remove extra spaces
        String userStr = userAnswer != null 
                ? String.valueOf(userAnswer).trim().toLowerCase().replaceAll("\\s+", " ") 
                : "";
        String correctStr = correctSentence.trim().toLowerCase().replaceAll("\\s+", " ");

        boolean isCorrect = userStr.equals(correctStr);

        return builder
                .correctAnswer(correctSentence)
                .isCorrect(isCorrect)
                .feedback(isCorrect ? "Hoàn hảo!" : "Chưa chính xác. Câu đúng là: " + correctSentence)
                .explanation(explanation)
                .build();
    }
}