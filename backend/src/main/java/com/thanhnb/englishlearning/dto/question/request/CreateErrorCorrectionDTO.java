package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO cho câu hỏi Tìm lỗi sai")
public class CreateErrorCorrectionDTO extends CreateQuestionDTO {

    @NotBlank(message = "Phải chỉ rõ từ/cụm từ sai (errorText)")
    private String errorText;

    @NotBlank(message = "Phải cung cấp đáp án sửa lại (correction)")
    private String correction;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.ERROR_CORRECTION;
    }
}