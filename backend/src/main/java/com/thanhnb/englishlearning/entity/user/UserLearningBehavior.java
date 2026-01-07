package com.thanhnb.englishlearning.entity.user;

import com.thanhnb.englishlearning.entity.json.SkillStats;
import com.thanhnb.englishlearning.entity.json.QuestionTypeStats;
import com.thanhnb.englishlearning.entity.json.TopicProgressStats;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "user_learning_behaviors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLearningBehavior {

    @Id
    private Long userId; // Dùng luôn ID của User làm khóa chính (OneToOne)

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // --- JSON COLUMNS (PostgreSQL JSONB) ---

    // Thống kê theo kỹ năng: Key="GRAMMAR", "READING"... Value=Stats
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skill_stats", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, SkillStats> skillStats = new HashMap<>();

    // Thống kê theo loại câu hỏi: Key="MULTIPLE_CHOICE"... Value=Stats
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_type_stats", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, QuestionTypeStats> questionTypeStats = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topic_progress", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, TopicProgressStats> topicProgress = new HashMap<>();

    // --- CALCULATED FIELDS (Để query nhanh) ---

    @Column(name = "strongest_skill", length = 50)
    private String strongestSkill;

    @Column(name = "weakest_skill", length = 50)
    private String weakestSkill;

    @Column(name = "avg_attempts_per_lesson")
    private Double avgAttemptsPerLesson;

    @Column(name = "overall_accuracy")
    private Double overallAccuracy;

    // --- TIMESTAMPS ---

    @Column(name = "last_analyzed_at")
    private LocalDateTime lastAnalyzedAt;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Helper để khởi tạo map nếu null
    @PostLoad
    private void initMaps() {
        if (skillStats == null) skillStats = new HashMap<>();
        if (questionTypeStats == null) questionTypeStats = new HashMap<>();
        if (topicProgress == null) topicProgress = new HashMap<>();
    }
}