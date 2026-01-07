package com.thanhnb.englishlearning.dto.question.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO tạo/cập nhật Task Group")
public class CreateTaskGroupDTO {

    @NotBlank(message = "Task name không được để trống")
    @Schema(description = "Tên task", example = "Task 1: Multiple Choice")
    private String taskName;

    @Schema(description = "Hướng dẫn cho task này", example = "Choose the correct answer (A, B, C or D)")
    private String instruction;

    @Positive(message = "Order index phải là số dương")
    @Schema(description = "Thứ tự hiển thị (tùy chọn, tự động nếu null)", example = "1")
    private Integer orderIndex;
}