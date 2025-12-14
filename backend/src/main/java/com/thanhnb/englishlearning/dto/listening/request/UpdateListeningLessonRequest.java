package com.thanhnb.englishlearning.dto.listening.request;

import com.thanhnb.englishlearning.entity.listening.ListeningLesson.Difficulty;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateListeningLessonRequest {
    
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    private String transcript;
    
    private String transcriptTranslation;
    
    private Difficulty difficulty;
    
    @Min(value = 1, message = "Order index must be at least 1")
    private Integer orderIndex;
    
    @Min(value = 60, message = "Time limit must be at least 60 seconds")
    @Max(value = 3600, message = "Time limit must not exceed 3600 seconds")
    private Integer timeLimitSeconds;
    
    @Min(value = 1, message = "Points reward must be at least 1")
    @Max(value = 100, message = "Points reward must not exceed 100")
    private Integer pointsReward;
    
    private Boolean allowUnlimitedReplay;
    
    @Min(value = 1, message = "Max replay count must be at least 1")
    @Max(value = 10, message = "Max replay count must not exceed 10")
    private Integer maxReplayCount;
    
    private Boolean isActive;
}