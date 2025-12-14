package com.thanhnb.englishlearning.dto.listening.response;

import com.thanhnb.englishlearning.entity.listening.ListeningLesson.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListeningLessonListResponse {
    
    private Long id;
    private String title;
    private Difficulty difficulty;
    private Integer orderIndex;
    private Integer pointsReward;
    private Boolean isActive;
    
    // Progress info
    private Boolean isCompleted;
    private BigDecimal scorePercentage;
    private Integer attempts;
    private Boolean isUnlocked;
    
    // Question count
    private Long questionCount;
}