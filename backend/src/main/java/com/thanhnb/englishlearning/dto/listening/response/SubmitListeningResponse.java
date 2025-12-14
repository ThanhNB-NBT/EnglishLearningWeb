package com.thanhnb.englishlearning.dto.listening.response;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitListeningResponse {
    
    private Boolean isPassed;
    private BigDecimal scorePercentage;
    private Integer correctCount;
    private Integer totalQuestions;
    private Integer totalScore;
    private Integer attempts;
    
    // Rewards
    private Boolean isFirstCompletion;
    private Integer pointsEarned;
    
    // Results
    private List<QuestionResultDTO> results;
    
    // Navigation
    private Boolean hasNextLesson;
    private Long nextLessonId;
}