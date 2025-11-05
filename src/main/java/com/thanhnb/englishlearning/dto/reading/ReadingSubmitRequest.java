package com.thanhnb.englishlearning.dto.reading;

import com.thanhnb.englishlearning.dto.question.SubmitAnswerRequest;

import lombok.Data;
import java.util.List;

@Data
public class ReadingSubmitRequest {
    
    private List<SubmitAnswerRequest> answers;
    
}
