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
public class ReadingComprehensionValidator implements AnswerValidator {

    private final ObjectMapper objectMapper;

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.READING_COMPREHENSION;
    }

    @Override
    @SuppressWarnings("unchecked")
    public QuestionResultDTO validate(Map<String, Object> meta, Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        List<Map<String, Object>> blanks = (List<Map<String, Object>>) meta.get("blanks");
        String explanation = (String) meta.get("explanation");

        if (blanks == null || blanks.isEmpty()) {
            return builder.isCorrect(false).feedback("Lỗi dữ liệu: Không có câu hỏi con.").build();
        }

        if (!(userAnswer instanceof Map)) {
            return builder.isCorrect(false).feedback("Lỗi định dạng câu trả lời.").build();
        }

        Map<?, ?> userAnswersMap = (Map<?, ?>) userAnswer;
        int correctCount = 0;
        int totalBlanks = blanks.size();

        for (Map<String, Object> blank : blanks) {
            Integer position = (Integer) blank.get("position");
            String correctAnswer = (String) blank.get("correctAnswer");

            if (position == null || correctAnswer == null)
                continue;

            String userVal = null;
            if (userAnswersMap.containsKey(position)) {
                userVal = String.valueOf(userAnswersMap.get(position));
            } else if (userAnswersMap.containsKey(String.valueOf(position))) {
                userVal = String.valueOf(userAnswersMap.get(String.valueOf(position)));
            }

            if (correctAnswer.equals(userVal)) {
                correctCount++;
            }
        }

        boolean isCorrect = (correctCount == totalBlanks);
        String answerJson = "";
        try {
            answerJson = objectMapper.writeValueAsString(blanks);
        } catch (JsonProcessingException e) {
        }

        return builder
                .correctAnswer(answerJson)
                .isCorrect(isCorrect)
                .feedback(String.format("Đúng %d/%d câu trong bài đọc.", correctCount, totalBlanks))
                .explanation(explanation)
                .build();
    }
}