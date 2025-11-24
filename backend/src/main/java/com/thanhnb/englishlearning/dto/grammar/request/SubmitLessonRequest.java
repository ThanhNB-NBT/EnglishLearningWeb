package com.thanhnb.englishlearning.dto.grammar.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitLessonRequest {

    @NotNull(message = "Lesson Id không được để trống")
    @Schema(description = "ID của bài học", example = "1")
    private Long lessonId;

    @Schema(description = "Danh sách câu trả lời cho các câu hỏi thực hành")
    private List<SubmitAnswerRequest> answers;

    @Schema(description = "Thời gian đọc lý thuyết tính bằng giây", example = "30")
    private Integer readingTimeSecond;
}
