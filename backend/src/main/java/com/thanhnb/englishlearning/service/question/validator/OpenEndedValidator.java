package com.thanhnb.englishlearning.service.question.validator;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.enums.QuestionType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OpenEndedValidator implements AnswerValidator {

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.OPEN_ENDED;
    }

    @Override
    public QuestionResultDTO validate(Map<String, Object> meta, Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        String suggestedAnswer = (String) meta.get("suggestedAnswer");

        String userText = userAnswer != null ? String.valueOf(userAnswer).trim() : "";

        if (userText.isEmpty()) {
            return builder
                    .isCorrect(false)
                    .feedback("Bạn chưa nhập câu trả lời.")
                    .build();
        }

        // Logic sơ khai: Nếu có đáp án gợi ý và user nhập y hệt thì cho đúng luôn (ít
        // khi xảy ra)
        if (suggestedAnswer != null && suggestedAnswer.equalsIgnoreCase(userText)) {
            return builder.isCorrect(true).feedback("Tuyệt vời! Hoàn toàn chính xác.").build();
        }

        // Mặc định: Chờ chấm
        return builder
                .correctAnswer(suggestedAnswer)
                .isCorrect(null)
                .feedback("Câu trả lời đã được ghi nhận và đang chờ chấm điểm.")
                .build();
    }
}