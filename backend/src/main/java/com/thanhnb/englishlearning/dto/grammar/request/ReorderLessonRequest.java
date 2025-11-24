package com.thanhnb.englishlearning.dto.grammar.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderLessonRequest {
    @NotNull(message = "Vị trí chèn không được để trống")
    @Min(value = 1, message = "Vị trí chèn phải lớn hơn hoặc bằng 1")
    private Integer insertPosition;

    // ID của lesson cần exclude (dùng khi edit)
    // Nếu null là đang tạo mới
    private Long excludeLessonId;
}
