// File: backend/src/main/java/com/thanhnb/englishlearning/service/question/validator/AnswerValidator.java
package com.thanhnb.englishlearning.service.question.validator;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.enums.QuestionType;

import java.util.Map;

public interface AnswerValidator {
    /**
     * Trả về loại câu hỏi mà validator này xử lý
     */
    QuestionType getSupportedType();

    /**
     * Validate câu trả lời
     * @param metadata Metadata của câu hỏi
     * @param userAnswer Câu trả lời của user
     * @param builder Builder đã có sẵn thông tin cơ bản (id, text, points)
     * @return Kết quả chi tiết
     */
    QuestionResultDTO validate(Map<String, Object> metadata, Object userAnswer, QuestionResultDTO.QuestionResultDTOBuilder builder);
}