package com.thanhnb.englishlearning.dto.listening;

import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ✅ Request nộp bài Listening
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Listening Submit Request - Nộp bài nghe")
public class ListeningSubmitRequest {
    
    @Valid
    @NotNull(message = "Danh sách câu trả lời không được để trống")
    @Schema(description = "Danh sách câu trả lời cho các câu hỏi")
    private List<SubmitAnswerRequest> answers;
}