package com.thanhnb.englishlearning.dto.question.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi trắc nghiệm (MULTIPLE_CHOICE)")
public class CreateMultipleChoiceDTO extends CreateQuestionDTO {

    @NotNull(message = "Options không được null")
    @Size(min = 2, message = "Cần ít nhất 2 options")
    private List<OptionDTO> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Đáp án cho câu hỏi trắc nghiệm")
    public static class OptionDTO {
        @NotBlank(message = "Text không được trống")
        @Schema(description = "Nội dung đáp án", example = "Đáp án A")
        private String text;

        @Schema(description = "Đáp án đúng hay không", example = "true")
        private Boolean isCorrect = false;
        
        @NotNull
        @Min(value = 1, message = "Order => 1")
        private Integer order;

        public Boolean getIsCorrect() {
            return isCorrect;
        }
        
        public void setIsCorrect(Boolean isCorrect) {
            this.isCorrect = isCorrect != null ? isCorrect : false;
        }
    }
}