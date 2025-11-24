package com.thanhnb.englishlearning.entity.reading;

import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.service.common.LessonProgressService;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_reading_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
@NoArgsConstructor
@AllArgsConstructor
public class UserReadingProgress implements LessonProgressService.LessonProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private ReadingLesson lesson;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "score_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal scorePercentage = BigDecimal.ZERO;

    @Column(name = "attemps", nullable = false)
    private Integer attempts = 0;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Integer getAttempts() {
        return attempts; // Map typo field
    }

    @Override
    public void setAttempts(Integer attempts) {
        this.attempts = attempts; // Map typo field
    }

}
