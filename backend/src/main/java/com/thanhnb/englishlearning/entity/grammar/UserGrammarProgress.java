package com.thanhnb.englishlearning.entity.grammar;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.service.common.LessonProgressService;

@Entity
@Table(name = "user_grammar_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "lesson"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserGrammarProgress implements LessonProgressService.LessonProgress {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private GrammarLesson lesson;

    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "score_percentage")
    @Builder.Default
    private Double scorePercentage = 0.0;

    @Column(name = "reading_time")
    @Builder.Default
    private Integer readingTime = 0;

    @Column(name = "has_scrolled_to_end")
    @Builder.Default
    private Boolean hasScrolledToEnd = false;

    @Column(name = "attempts")
    @Builder.Default
    private Integer attempts = 0;

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
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}