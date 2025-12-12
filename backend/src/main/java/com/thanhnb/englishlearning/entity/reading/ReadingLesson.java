package com.thanhnb.englishlearning.entity.reading;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private String content;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private String contentTranslation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty = Difficulty.BEGINNER;

    @Column(nullable = false)
    private Integer orderIndex;

    @Column(nullable = false)
    private Integer timeLimitSeconds = 600;

    @Column(nullable = false)
    private Integer pointsReward = 25;

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
    }

    public enum Difficulty {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED
    }
    
}
