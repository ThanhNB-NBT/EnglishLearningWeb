package com.thanhnb.englishlearning.service.question.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.enums.QuestionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PronunciationValidator implements AnswerValidator {

    private final ObjectMapper objectMapper;

    @Override
    public QuestionType getSupportedType() {
        return QuestionType.PRONUNCIATION;
    }

    @Override
    @SuppressWarnings("unchecked")
    public QuestionResultDTO validate(Map<String, Object> meta, Object userAnswer, QuestionResultDTO.QuestionResultDTOBuilder builder) {
        // 1. Lấy danh sách phân loại đúng từ metadata
        List<Map<String, String>> classifications = (List<Map<String, String>>) meta.get("classifications");
        String explanation = (String) meta.get("explanation");

        if (classifications == null || classifications.isEmpty()) {
            return builder.isCorrect(false).feedback("Lỗi dữ liệu: Không có thông tin phân loại.").build();
        }

        // 2. Chuyển đổi list classifications sang Map để dễ so sánh (Key: Word -> Value: Category)
        Map<String, String> correctMap = new HashMap<>();
        for (Map<String, String> item : classifications) {
            String word = item.get("word");
            String category = item.get("category");
            if (word != null && category != null) {
                correctMap.put(word.trim().toLowerCase(), category.trim());
            }
        }

        // 3. Xử lý câu trả lời của user (Mong đợi Map: Word -> Category)
        if (!(userAnswer instanceof Map)) {
            return builder.isCorrect(false).feedback("Định dạng câu trả lời không hợp lệ.").build();
        }

        Map<?, ?> userMap = (Map<?, ?>) userAnswer;
        int correctCount = 0;
        int totalItems = correctMap.size();

        // 4. So sánh từng từ
        for (Map.Entry<String, String> entry : correctMap.entrySet()) {
            String wordKey = entry.getKey();
            String correctCategory = entry.getValue();

            // Lấy category user đã chọn cho từ này
            Object userCatObj = userMap.get(wordKey);
            // Fallback: nếu userMap dùng key nguyên bản (chưa lowercase) thì thử tìm
            if (userCatObj == null) {
                userCatObj = findUserValueIgnoreCase(userMap, wordKey);
            }

            if (userCatObj != null) {
                String userCategory = String.valueOf(userCatObj).trim();
                if (correctCategory.equalsIgnoreCase(userCategory)) {
                    correctCount++;
                }
            }
        }

        // 5. Kết quả
        boolean isCorrect = (correctCount == totalItems);
        String jsonAnswer = "";
        try {
            // Trả về full list classifications để FE hiển thị đúng
            jsonAnswer = objectMapper.writeValueAsString(classifications);
        } catch (JsonProcessingException e) {
            log.error("Error serializing pronunciation answers", e);
        }

        return builder
                .correctAnswer(jsonAnswer)
                .isCorrect(isCorrect)
                .feedback(isCorrect
                        ? "Tuyệt vời! Bạn đã phân loại đúng tất cả các từ."
                        : String.format("Bạn phân loại đúng %d/%d từ.", correctCount, totalItems))
                .explanation(explanation)
                .build();
    }

    // Helper để tìm value trong map mà không phân biệt hoa thường ở Key (nếu cần)
    private Object findUserValueIgnoreCase(Map<?, ?> map, String key) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (String.valueOf(entry.getKey()).equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}