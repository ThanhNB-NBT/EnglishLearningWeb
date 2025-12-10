package com.thanhnb.englishlearning.entity.question;

import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.enums.ParentType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "questions", indexes = {
        @Index(name = "idx_questions_parent", columnList = "parent_id, parent_type, order_index")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "parent_type", nullable = false, length = 50)
    private ParentType parentType; //GRAMMAR, SPEAKING, READING

    @Column(name = "parent_id", nullable = false)
    private Long parentId; //lesson_id hoặc topic_id

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 50)
    private QuestionType questionType;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (points == null) points = 1;
        if (orderIndex == null) orderIndex = 0;
    }

}
