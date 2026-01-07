package com.thanhnb.englishlearning.dto.reading;

import lombok.Data;
import java.util.List;

import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;

import jakarta.validation.Valid;

@Data
public class ReadingSubmitRequest {

    private Long lessonId;
    
    @Valid
    private List<SubmitAnswerRequest> answers;
    
}
