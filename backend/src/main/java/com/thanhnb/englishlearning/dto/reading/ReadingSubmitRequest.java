package com.thanhnb.englishlearning.dto.reading;

import lombok.Data;
import java.util.List;

import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;

@Data
public class ReadingSubmitRequest {
    
    private List<SubmitAnswerRequest> answers;
    
}
