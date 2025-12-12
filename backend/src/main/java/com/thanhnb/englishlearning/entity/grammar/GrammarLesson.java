package com.thanhnb.englishlearning.entity.grammar;

import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.LessonType;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "grammar_lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrammarLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private GrammarTopic topic;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 50)
    private LessonType lessonType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "time_limit_seconds", nullable = false)
    private Integer timeLimitSeconds = 30;

    // Điểm thưởng khi hoàn thành bài học
    @Column(name = "points_reward")
    private Integer pointsReward = 10;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt = LocalDateTime.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserGrammarProgress> userProgresses;

    public Integer getReadingTimeRequired() {
        return lessonType == LessonType.THEORY ? timeLimitSeconds : null;
    }

    public Integer getPracticeTimeLimit() {
        return lessonType == LessonType.PRACTICE ? timeLimitSeconds : null;
    }
}
