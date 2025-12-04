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
public class MatchingValidator implements AnswerValidator {

    private final ObjectMapper objectMapper;

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.MATCHING;
    }

    @Override
    @SuppressWarnings("unchecked")
    public QuestionResultDTO validate(Map<String, Object> meta, Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        List<Map<String, Object>> pairs = (List<Map<String, Object>>) meta.get("pairs");
        String explanation = (String) meta.get("explanation");

        if (pairs == null || pairs.isEmpty()) {
            return builder.isCorrect(false).feedback("Lỗi dữ liệu: Không có cặp nối.").build();
        }

        if (!(userAnswer instanceof Map)) {
            return builder.isCorrect(false).feedback("Lỗi định dạng câu trả lời.").build();
        }

        Map<String, String> userPairs;
        try {
            userPairs = (Map<String, String>) userAnswer;
        } catch (ClassCastException e) {
            return builder.isCorrect(false).feedback("Lỗi định dạng Map.").build();
        }

        int correctCount = 0;
        int totalPairs = pairs.size();

        for (Map<String, Object> pair : pairs) {
            String left = (String) pair.get("left");
            String right = (String) pair.get("right");

            if (left == null || right == null)
                continue;

            String userRight = userPairs.get(left);
            if (right.equals(userRight)) {
                correctCount++;
            }
        }

        boolean isCorrect = (correctCount == totalPairs);
        String jsonAnswer = "";
        try {
            jsonAnswer = objectMapper.writeValueAsString(pairs);
        } catch (JsonProcessingException e) {
            log.error("Error serializing matching pairs", e);
        }

        return builder
                .correctAnswer(jsonAnswer)
                .isCorrect(isCorrect)
                .feedback(isCorrect
                        ? "Tuyệt vời! Bạn đã ghép đúng tất cả."
                        : String.format("Bạn ghép đúng %d/%d cặp.", correctCount, totalPairs))
                .explanation(explanation)
                .build();
    }
}