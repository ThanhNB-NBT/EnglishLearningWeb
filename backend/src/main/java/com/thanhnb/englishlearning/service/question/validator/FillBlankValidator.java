package com.thanhnb.englishlearning.service.question.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.enums.QuestionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FillBlankValidator implements AnswerValidator {

    private final ObjectMapper objectMapper;

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.FILL_BLANK;
    }

    @Override
    @SuppressWarnings("unchecked")
    public QuestionResultDTO validate(Map<String, Object> meta, Object userAnswer, QuestionResultDTO.QuestionResultDTOBuilder builder) {
        List<Map<String, Object>> blanks = (List<Map<String, Object>>) meta.get("blanks");
        String explanation = (String) meta.get("explanation");
        
        // Lấy tùy chọn caseSensitive (mặc định false nếu không có)
        Boolean caseSensitive = (Boolean) meta.getOrDefault("caseSensitive", false);

        if (blanks == null || blanks.isEmpty()) {
            return builder.isCorrect(false).feedback("Lỗi dữ liệu: Không có chỗ trống/câu trả lời.").build();
        }

        // Convert user answer to Map
        Map<?, ?> userAnswersMap;
        if (userAnswer instanceof Map) {
            userAnswersMap = (Map<?, ?>) userAnswer;
        } else if (blanks.size() == 1) {
            // Fallback quan trọng cho Text Answer: FE có thể gửi string đơn thay vì Map
            userAnswersMap = Map.of("1", String.valueOf(userAnswer));
        } else {
            return builder.isCorrect(false).feedback("Định dạng câu trả lời không hợp lệ.").build();
        }

        int correctCount = 0;
        int totalBlanks = blanks.size();

        for (Map<String, Object> blank : blanks) {
            Integer position = (Integer) blank.get("position");
            Object correctAnswersObj = blank.get("correctAnswers");
            
            List<String> correctAnswers;
            if (correctAnswersObj instanceof List) {
                correctAnswers = (List<String>) correctAnswersObj;
            } else {
                correctAnswers = List.of(String.valueOf(correctAnswersObj));
            }

            // Lấy câu trả lời của user (linh hoạt key int/string)
            String userBlankAnswer = null;
            if (userAnswersMap.containsKey(position)) {
                userBlankAnswer = String.valueOf(userAnswersMap.get(position)).trim();
            } else if (userAnswersMap.containsKey(String.valueOf(position))) {
                userBlankAnswer = String.valueOf(userAnswersMap.get(String.valueOf(position))).trim();
            }

            if (userBlankAnswer != null) {
                String finalUserAnswer = userBlankAnswer;
                
                // Logic so sánh (có hỗ trợ caseSensitive)
                boolean match = correctAnswers.stream()
                        .anyMatch(ans -> {
                            String answer = ans.trim();
                            return caseSensitive 
                                ? answer.equals(finalUserAnswer) 
                                : answer.equalsIgnoreCase(finalUserAnswer);
                        });
                        
                if (match) correctCount++;
            }
        }

        boolean isCorrect = (correctCount == totalBlanks);
        String correctAnswerJson = "";
        try {
            correctAnswerJson = objectMapper.writeValueAsString(blanks);
        } catch (JsonProcessingException e) {
            correctAnswerJson = "Error processing answers";
        }

        return builder
                .correctAnswer(correctAnswerJson)
                .isCorrect(isCorrect)
                .feedback(isCorrect 
                    ? "Chính xác! Bạn làm đúng tất cả." 
                    : String.format("Bạn trả lời đúng %d/%d ý.", correctCount, totalBlanks))
                .explanation(explanation)
                .build();
    }
}