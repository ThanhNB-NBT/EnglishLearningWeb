package com.thanhnb.englishlearning.dto.question;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    
    @NotNull(message = "Question Id không được để trống")
    @Schema(description = "ID của câu hỏi", example = "1")
    private Long questionId;

    @NotNull(message = "Câu trả lời không được để trống")
    @Schema(description = "Câu trả lời của người dùng", example = "Đáp án A")
    private String answer; // Người dùng trả lời

    @Schema(description = "ID của lựa chọn được chọn", example = "2")
    private Long selectedOptionId; // ID của lựa chọn được chọn, nếu có

}
