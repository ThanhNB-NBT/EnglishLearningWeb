package com.thanhnb.englishlearning.service.question.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.enums.QuestionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SentenceTransformationValidator implements AnswerValidator {

    private final ObjectMapper objectMapper;

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.SENTENCE_TRANSFORMATION;
    }

    @Override
    @SuppressWarnings("unchecked")
    public QuestionResultDTO validate(Map<String, Object> meta, Object userAnswer, QuestionResultDTO.QuestionResultDTOBuilder builder) {
        // 1. Lấy dữ liệu từ metadata
        String originalSentence = (String) meta.get("originalSentence");
        List<String> correctAnswers = (List<String>) meta.get("correctAnswers");
        String explanation = (String) meta.get("explanation");

        if (correctAnswers == null || correctAnswers.isEmpty()) {
            return builder.isCorrect(false).feedback("Lỗi dữ liệu: Không có đáp án mẫu.").build();
        }

        // 2. Xử lý câu trả lời của user
        String userStr = userAnswer != null ? String.valueOf(userAnswer).trim() : "";

        if (userStr.isEmpty()) {
            return builder
                    .correctAnswer(correctAnswers.get(0))
                    .isCorrect(false)
                    .feedback("Bạn chưa nhập câu trả lời.")
                    .build();
        }

        // 3. Logic so sánh (Normalized: lowercase + remove extra spaces + remove ending dot)
        String normalizedUser = normalizeSentence(userStr);
        
        boolean isCorrect = correctAnswers.stream()
                .map(this::normalizeSentence)
                .anyMatch(ans -> ans.equals(normalizedUser));

        // 4. Chuẩn bị JSON đáp án để trả về FE (nếu cần hiển thị tất cả các cách đúng)
        String correctAnswersJson;
        try {
            correctAnswersJson = objectMapper.writeValueAsString(correctAnswers);
        } catch (JsonProcessingException e) {
            correctAnswersJson = correctAnswers.get(0);
        }

        return builder
                .correctAnswer(correctAnswersJson)
                .isCorrect(isCorrect)
                .feedback(isCorrect 
                    ? "Chính xác! Kỹ năng viết lại câu của bạn rất tốt." 
                    : "Chưa chính xác. Xem đáp án đúng nhé.")
                .explanation(explanation)
                .build();
    }

    /**
     * Hàm chuẩn hóa câu để so sánh "lỏng" tay hơn
     */
    private String normalizeSentence(String input) {
        if (input == null) return "";
        // 1. Trim & Lowercase
        String s = input.trim().toLowerCase();
        // 2. Thay thế nhiều khoảng trắng thành 1
        s = s.replaceAll("\\s+", " ");
        // 3. Bỏ dấu chấm câu ở cuối (nếu có) để user quên chấm vẫn tính đúng
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }
}