package com.thanhnb.englishlearning.entity.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTypeStats implements Serializable {
    private String typeName; // MULTIPLE_CHOICE, FILL_BLANK...
    private Double accuracy;
    private Integer correctCount;
    private Integer wrongCount;
    
    public void addResult(boolean isCorrect) {
        if (correctCount == null) correctCount = 0;
        if (wrongCount == null) wrongCount = 0;
        
        if (isCorrect) correctCount++;
        else wrongCount++;
        
        int total = correctCount + wrongCount;
        accuracy = (total > 0) ? (double) correctCount / total : 0.0;
    }
}