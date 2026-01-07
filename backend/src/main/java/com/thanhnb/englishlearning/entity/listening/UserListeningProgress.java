package com.thanhnb.englishlearning.entity.listening;

import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.service.common.LessonProgressService;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_listening_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "lesson"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserListeningProgress implements LessonProgressService.LessonProgress {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private ListeningLesson lesson;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "score_percentage")
    @Builder.Default
    private Double scorePercentage = 0.0;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    // Listening-specific tracking
    @Column(name = "play_count", nullable = false)
    @Builder.Default
    private Integer playCount = 0; // Number of times user clicked play

    @Column(name = "has_viewed_transcript", nullable = false)
    @Builder.Default
    private Boolean hasViewedTranscript = false;

    @Column(name = "transcript_viewed_at")
    private LocalDateTime transcriptViewedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (playCount == null) {
            playCount = 0;
        }
        if (hasViewedTranscript == null) {
            hasViewedTranscript = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Increment play count
     */
    public void incrementPlayCount() {
        this.playCount = (this.playCount != null ? this.playCount : 0) + 1;
    }

    /**
     * Mark transcript as viewed
     */
    public void markTranscriptViewed() {
        if (!this.hasViewedTranscript) {
            this.hasViewedTranscript = true;
            this.transcriptViewedAt = LocalDateTime.now();
        }
    }

    /**
     * Check if can view transcript (after 2 plays)
     */
    public boolean canViewTranscript() {
        return this.playCount >= 2;
    }
}