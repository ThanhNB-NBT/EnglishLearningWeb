package com.thanhnb.englishlearning.entity.recommendation;

import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_recommendations", indexes = {
    @Index(name = "idx_user_priority", columnList = "user_id, priority, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // --- Recommendation Info ---
    
    // NEXT_LESSON, PRACTICE_WEAK_SKILL, REVIEW_TOPIC, GENERATED_LESSON
    @Column(nullable = false, length = 50)
    private String type; 

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT") // Lý do gợi ý (AI giải thích)
    private String reasoning;

    // --- Target ---

    @Enumerated(EnumType.STRING)
    @Column(name = "target_skill", length = 20)
    private ModuleType targetSkill;

    @Column(name = "target_lesson_id")
    private Long targetLessonId;

    @Column(name = "target_topic_id")
    private Long targetTopicId;

    // --- Generated Content (JSON) ---
    // Lưu nội dung bài học AI tạo (nếu type = GENERATED_LESSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_content", columnDefinition = "jsonb")
    private String generatedContent;

    // --- Status ---

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 3;

    @Column(name = "is_shown")
    @Builder.Default
    private boolean isShown = false;

    @Column(name = "is_accepted")
    private Boolean isAccepted; // User chấp nhận gợi ý hay không

    @Column(name = "is_completed")
    @Builder.Default
    private boolean isCompleted = false;

    // --- Approval (Cho bài học AI tạo) ---

    @Column(name = "is_approved")
    private Boolean isApproved;

    @Column(name = "approved_by")
    private Long approvedBy; // ID giáo viên duyệt

    // --- Timestamps ---

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Gợi ý hết hạn sau X ngày
    
    @Column(name = "shown_at")
    private LocalDateTime shownAt;
}