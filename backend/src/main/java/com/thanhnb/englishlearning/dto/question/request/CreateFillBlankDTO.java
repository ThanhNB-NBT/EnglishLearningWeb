package com.thanhnb.englishlearning.dto.question.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO cho câu hỏi Điền từ (Hỗ trợ cả Word Bank)")
public class CreateFillBlankDTO extends CreateQuestionDTO {

    @NotEmpty(message = "Danh sách chỗ trống (blanks) không được để trống")
    @Valid
    private List<BlankDTO> blanks;

    @Schema(description = "Danh sách từ cho trước (Word Bank). Nếu có, user sẽ kéo thả/chọn thay vì gõ.")
    private List<String> wordBank; 


    @Data
    @NoArgsConstructor // Thêm constructor này
    @AllArgsConstructor
    public static class BlankDTO {
        @NotNull(message = "Vị trí không được null")
        private Integer position;

        @NotEmpty(message = "Phải có ít nhất 1 đáp án đúng")
        private List<String> correctAnswers;
        
        // Thêm hint cho Verb Form nếu muốn gộp chung
        private String hint; 
    }
}