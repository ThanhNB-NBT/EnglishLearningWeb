package com.thanhnb.englishlearning.dto.grammar;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Grammar question option details")
public class GrammarQuestionOptionDTO {
    @Schema(description = "ID của lựa chọn", example = "201")
    private Long id;

    @NotNull(message = "Question Id không được để trống")
    @Schema(description = "ID của câu hỏi", example = "101")
    private Long questionId;

    @NotBlank(message = "Nội dung lựa chọn không được để trống")
    @Schema(description = "Nội dung của lựa chọn", example = "I am happy")
    private String optionText;

    @Schema(description = "Trạng thái của lựa chọn", example = "true")
    private Boolean isCorrect = false;

    @NotNull(message = "Thứ tự không được để trống")
    @Min(value = 1, message = "Thứ tự phải lớn hơn 0")
    @Schema(description = "Thứ tự của lựa chọn", example = "1")
    private Integer orderIndex;
}