package com.thanhnb.englishlearning.dto.listening.request;

import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SubmitListeningRequest {
    
    @NotNull(message = "Danh sách câu trả lời không được để trống")
    @Schema(description = "Danh sách câu trả lời cho các câu hỏi")
    private List<SubmitAnswerRequest> answers;
}