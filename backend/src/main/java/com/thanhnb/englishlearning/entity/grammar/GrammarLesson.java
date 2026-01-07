package com.thanhnb.englishlearning.entity.grammar;

import com.thanhnb.englishlearning.entity.listener.QuestionCascadeDeleteListener;
import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.LessonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "grammar_lessons")
@EntityListeners(QuestionCascadeDeleteListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"topic", "userProgresses"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GrammarLesson {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relation to Topic
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 50)
    private LessonType lessonType; // THEORY, PRACTICE

    @Column(columnDefinition = "TEXT")
    private String content; // HTML or Markdown theory

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "time_limit_seconds")
    @Builder.Default
    private Integer timeLimitSeconds = 0; // 0 if theory lesson with no time limit

    @Column(name = "points_reward")
    @Builder.Default
    private Integer pointsReward = 10;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    @UpdateTimestamp
    private LocalDateTime modifiedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private User modifiedBy;
    
    // Relation to UserProgress
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserGrammarProgress> userProgresses;

    /**
     * Activate this lesson
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Deactivate this lesson
     */
    public void deactivate() {
        this.isActive = false;
    }
}