package com.thanhnb.englishlearning.dto.listening.response;

import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson.Difficulty;
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
public class ListeningLessonDetailResponse {
    
    // Lesson info
    private Long id;
    private String title;
    private String audioUrl;
    private Difficulty difficulty;
    private Integer timeLimitSeconds;
    private Integer pointsReward;
    
    // Replay settings
    private Boolean allowUnlimitedReplay;
    private Integer maxReplayCount;
    private Integer remainingReplays; // null if unlimited
    
    // Transcript (only if unlocked)
    private String transcript;
    private String transcriptTranslation;
    private Boolean transcriptUnlocked;
    
    // Progress info
    private Boolean isCompleted;
    private BigDecimal scorePercentage;
    private Integer attempts;
    private Integer playCount;
    private Boolean hasViewedTranscript;
    
    // Questions
    private List<QuestionResponseDTO> questions;
    
    // Navigation
    private Boolean hasNext;
    private Boolean hasPrevious;
}