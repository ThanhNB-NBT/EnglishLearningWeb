package com.thanhnb.englishlearning.entity.reading;

import com.thanhnb.englishlearning.entity.listener.QuestionCascadeDeleteListener;
import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.entity.user.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reading_lessons")
@EntityListeners(QuestionCascadeDeleteListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"topic", "userProgresses"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReadingLesson {

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

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private String content;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private String contentTranslation;

    @Column(nullable = false)
    private Integer orderIndex;

    @Column(nullable = false)
    @Builder.Default
    private Integer timeLimitSeconds = 600;

    @Column(nullable = false)
    @Builder.Default
    private Integer pointsReward = 25;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    @UpdateTimestamp
    private LocalDateTime modifiedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private User modifiedBy;

    // ==================== RELATIONSHIPS ====================
    
    /**
     * Relation to UserProgress
     */
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserReadingProgress> userProgresses;

    // ==================== HELPER METHODS ====================

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