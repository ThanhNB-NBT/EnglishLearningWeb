package com.thanhnb.englishlearning.entity.question;

import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.enums.ParentType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "questions", indexes = {
        @Index(name = "idx_questions_parent", columnList = "parent_id, parent_type, order_index")
})
@Data
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

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 50)
    private QuestionType questionType;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    //Các trường cho SpeakingQuestion
    @Column(name = "suggested_answer", columnDefinition = "TEXT")
    private String suggestedAnswer; //Cho câu hỏi Speaking

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds; //Giới hạn thời gian trả lời (tính bằng giây)

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    //Relationships
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuestionOption> options;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null){
            createdAt = LocalDateTime.now();
        }
    }

}
