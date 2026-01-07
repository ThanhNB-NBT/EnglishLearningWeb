package com.thanhnb.englishlearning.dto.question.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    
    @NotNull(message = "Question Id không được để trống")
    @Schema(description = "ID của câu hỏi", example = "1")
    private Long questionId;

    @Schema(description = "Danh sách ID đáp án đã chọn (cho câu trắc nghiệm)", example = "[1, 2]")
    private List<Long> selectedOptions;

    @Schema(description = "Câu trả lời dạng text (cho điền từ, viết lại câu)", example = "I go to school")
    private String textAnswer;
}