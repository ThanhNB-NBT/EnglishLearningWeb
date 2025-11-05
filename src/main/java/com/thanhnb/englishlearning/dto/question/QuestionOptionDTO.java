package com.thanhnb.englishlearning.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lựa chọn cho câu hỏi trắc nghiệm")
public class QuestionOptionDTO {
    
    @Schema(description = "ID lựa chọn", example = "201")
    private Long id;

    @NotNull(message = "Question ID không được để trống")
    @Schema(description = "ID câu hỏi", example = "101")
    private Long questionId;

    @NotBlank(message = "Nội dung lựa chọn không được để trống")
    @Schema(description = "Nội dung lựa chọn", example = "goes")
    private String optionText;

    @Schema(description = "Đáp án đúng không", example = "true")
    private Boolean isCorrect = false;

    @NotNull(message = "Thứ tự không được để trống")
    @Min(value = 1, message = "Thứ tự phải >= 1")
    @Schema(description = "Thứ tự hiển thị", example = "1")
    private Integer orderIndex;
}