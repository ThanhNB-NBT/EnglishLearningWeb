package com.thanhnb.englishlearning.entity.listening;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "listening_lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListeningLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    // Audio file path (e.g., /media/listening/lesson_1/audio.mp3)
    @Column(nullable = true, length = 500)
    private String audioUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String transcript;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String transcriptTranslation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty = Difficulty.BEGINNER;

    @Column(nullable = false)
    private Integer orderIndex;

    @Column(nullable = false)
    private Integer timeLimitSeconds = 600; // 10 minutes

    @Column(nullable = false)
    private Integer pointsReward = 25;

    // Replay settings
    @Column(nullable = false)
    private Boolean allowUnlimitedReplay = true;

    @Column(nullable = false)
    private Integer maxReplayCount = 3; // Only apply if allowUnlimitedReplay = false

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (difficulty == null) {
            difficulty = Difficulty.BEGINNER;
        }
        if (timeLimitSeconds == null) {
            timeLimitSeconds = 600;
        }
        if (pointsReward == null) {
            pointsReward = 25;
        }
        if (allowUnlimitedReplay == null) {
            allowUnlimitedReplay = true;
        }
        if (maxReplayCount == null) {
            maxReplayCount = 3;
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    public enum Difficulty {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED
    }

    /**
     * Check if user can replay based on settings
     */
    public boolean canReplay(int currentReplayCount) {
        if (allowUnlimitedReplay) {
            return true;
        }
        return currentReplayCount < maxReplayCount;
    }

    /**
     * Get remaining replays
     */
    public Integer getRemainingReplays(int currentReplayCount) {
        if (allowUnlimitedReplay) {
            return null; // Unlimited
        }
        return Math.max(0, maxReplayCount - currentReplayCount);
    }
}