// ===== CreateListeningLessonRequest.java =====
package com.thanhnb.englishlearning.dto.listening.request;

import com.thanhnb.englishlearning.entity.listening.ListeningLesson.Difficulty;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateListeningLessonRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    @NotBlank(message = "Transcript is required")
    private String transcript;
    
    @NotBlank(message = "Transcript translation is required")
    private String transcriptTranslation;
    
    @NotNull(message = "Difficulty is required")
    private Difficulty difficulty;
    
    @NotNull(message = "Order index is required")
    @Min(value = 1, message = "Order index must be at least 1")
    private Integer orderIndex;
    
    @Min(value = 60, message = "Time limit must be at least 60 seconds")
    @Max(value = 3600, message = "Time limit must not exceed 3600 seconds")
    private Integer timeLimitSeconds = 600;
    
    @Min(value = 1, message = "Points reward must be at least 1")
    @Max(value = 100, message = "Points reward must not exceed 100")
    private Integer pointsReward = 25;
    
    private Boolean allowUnlimitedReplay = true;
    
    @Min(value = 1, message = "Max replay count must be at least 1")
    @Max(value = 10, message = "Max replay count must not exceed 10")
    private Integer maxReplayCount = 3;
    
    private Boolean isActive = true;
}