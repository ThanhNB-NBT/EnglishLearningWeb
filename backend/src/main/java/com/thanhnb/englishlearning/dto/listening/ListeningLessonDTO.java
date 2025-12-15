package com.thanhnb.englishlearning.dto.listening;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListeningLessonDTO {

    private Long id;
    private String title;
    private String audioUrl;
    private String transcript;
    private String transcriptTranslation;
    private String difficulty;
    private Integer timeLimitSeconds;
    private Integer orderIndex;
    private Integer pointsReward;
    private Boolean allowUnlimitedReplay;
    private Integer maxReplayCount;
    private Boolean isActive;
    private LocalDateTime createdAt;
    
    // Fields cho Client/List View
    private Integer questionCount;
    private Boolean isCompleted;
    private Double scorePercentage;
    private Integer attempts;
    private Boolean isUnlocked;
    private Boolean isAccessible;

    /**
     * Factory method cho dạng tóm tắt (List view)
     */
    public static ListeningLessonDTO summary(
            Long id,
            String title,
            ListeningLesson.Difficulty difficulty,
            Integer orderIndex,
            Integer pointsReward,
            Boolean isActive,
            Integer questionCount) {
        
        return ListeningLessonDTO.builder()
                .id(id)
                .title(title)
                .difficulty(difficulty != null ? difficulty.name() : "INTERMEDIATE")
                .orderIndex(orderIndex)
                .pointsReward(pointsReward)
                .isActive(isActive)
                .questionCount(questionCount)
                .build();
    }

    /**
     * Factory method cho dạng đầy đủ (Detail/Admin view)
     */
    public static ListeningLessonDTO full(
            Long id,
            String title,
            String audioUrl,
            String transcript,
            String transcriptTranslation,
            ListeningLesson.Difficulty difficulty,
            Integer timeLimitSeconds,
            Integer orderIndex,
            Integer pointsReward,
            Boolean allowUnlimitedReplay,
            Integer maxReplayCount,
            Boolean isActive,
            LocalDateTime createdAt) {
        
        return ListeningLessonDTO.builder()
                .id(id)
                .title(title)
                .audioUrl(audioUrl)
                .transcript(transcript)
                .transcriptTranslation(transcriptTranslation)
                .difficulty(difficulty != null ? difficulty.name() : "INTERMEDIATE")
                .timeLimitSeconds(timeLimitSeconds)
                .orderIndex(orderIndex)
                .pointsReward(pointsReward)
                .allowUnlimitedReplay(allowUnlimitedReplay)
                .maxReplayCount(maxReplayCount)
                .isActive(isActive)
                .createdAt(createdAt)
                .build();
    }
    
    // Setter tiện ích cho danh sách câu hỏi
    public void withQuestions(List<QuestionResponseDTO> questions) {
        // Nếu bạn muốn nhúng questions vào DTO này thay vì dùng LessonDetailResponse riêng
        // Cần thêm field List<QuestionResponseDTO> questions vào class này
    }
    
    // Setter tiện ích cho progress
    public void withProgress(Boolean isCompleted, Double scorePercentage, Integer attempts, LocalDateTime completedAt) {
        this.isCompleted = isCompleted;
        this.scorePercentage = scorePercentage;
        this.attempts = attempts;
        // this.completedAt = completedAt; // Nếu cần field này
    }
}