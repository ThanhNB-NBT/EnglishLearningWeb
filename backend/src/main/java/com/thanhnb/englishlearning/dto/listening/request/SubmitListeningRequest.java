package com.thanhnb.englishlearning.dto.listening.request;

import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SubmitListeningRequest {
    
    @NotNull(message = "Lesson ID is required")
    private Long lessonId;
    
    @NotEmpty(message = "Answers cannot be empty")
    @Valid
    private List<SubmitAnswerRequest> answers;
}