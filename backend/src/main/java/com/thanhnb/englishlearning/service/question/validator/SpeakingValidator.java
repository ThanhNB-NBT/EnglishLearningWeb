package com.thanhnb.englishlearning.service.question.validator;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.enums.QuestionType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SpeakingValidator implements AnswerValidator {

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.SPEAKING;
    }

    @Override
    public QuestionResultDTO validate(Map<String, Object> meta, Object userAnswer, QuestionResultDTO.QuestionResultDTOBuilder builder) {
        String suggestedAnswer = (String) meta.get("suggestedAnswer"); // Gợi ý câu trả lời mẫu
        
        // userAnswer ở đây mong đợi là URL của file ghi âm (sau khi upload)
        String audioUrl = userAnswer != null ? String.valueOf(userAnswer).trim() : "";

        if (audioUrl.isEmpty()) {
            return builder
                    .isCorrect(false)
                    .feedback("Bạn chưa gửi bản ghi âm.")
                    .build();
        }

        // Trả về null để đánh dấu là cần chấm điểm sau (AI hoặc Giáo viên)
        // Hoặc trả về true nếu chỉ tính điểm chuyên cần (Participation)
        return builder
                .correctAnswer(suggestedAnswer)
                .isCorrect(null) 
                .feedback("Bài làm đã được gửi. Hệ thống đang xử lý đánh giá phát âm của bạn.")
                .build();
    }
}