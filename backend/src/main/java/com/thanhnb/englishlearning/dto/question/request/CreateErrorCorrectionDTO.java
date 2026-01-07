package com.thanhnb.englishlearning.dto.question.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO cho câu hỏi Tìm lỗi sai")
public class CreateErrorCorrectionDTO extends QuestionData {

    @NotBlank(message = "Phải chỉ rõ từ/cụm từ sai (errorText)")
    @JsonView(Views.Public.class)
    private String errorText;

    @NotBlank(message = "Phải cung cấp đáp án sửa lại (correction)")
    @JsonView(Views.Admin.class)
    private String correction;
}