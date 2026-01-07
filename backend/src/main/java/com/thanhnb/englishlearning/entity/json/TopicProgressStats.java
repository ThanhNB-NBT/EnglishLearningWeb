package com.thanhnb.englishlearning.entity.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicProgressStats implements Serializable {
    private Long topicId;
    private String topicName;
    private Double completionPercentage; // 0.0 - 100.0
    private Long totalLessons;
    private Integer completedLessons;
    private Double averageScore;
    private LocalDateTime lastActiveAt;

    // Helper update
    public void updateProgress(boolean isLessonPassed, double score) {
        if (completedLessons == null) completedLessons = 0;
        if (averageScore == null) averageScore = 0.0;
        
        if (isLessonPassed) {
            // Recalculate average
            double totalScore = averageScore * completedLessons;
            completedLessons++;
            averageScore = (totalScore + score) / completedLessons;
        }
        lastActiveAt = LocalDateTime.now();
    }
}