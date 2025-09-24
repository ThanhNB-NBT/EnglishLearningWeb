package com.thanhnb.englishlearning.entity.grammar;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

import com.thanhnb.englishlearning.entity.User;

@Entity
@Table(name = "user_grammar_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "grammar_lesson_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGrammarProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grammar_lesson_id", nullable = false)
    private GrammarLesson lesson;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "score")
    private Integer score = 0;

    @Column(name = "attempts")
    private Integer attempts = 0;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
