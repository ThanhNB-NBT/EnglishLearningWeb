package com.thanhnb.englishlearning.entity.question;

import com.thanhnb.englishlearning.enums.ParentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_groups", indexes = {
    @Index(name = "idx_task_groups_parent", columnList = "parent_id, parent_type, order_index")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "parent_type", nullable = false, length = 50)
    private ParentType parentType;

    @Column(name = "parent_id", nullable = false)
    private Long parentId; // lessonId

    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName; // "Task 1", "Part A", "Section 1"

    @Column(name = "instruction", columnDefinition = "TEXT")
    private String instruction; // "Choose the correct answer A, B, C or D"

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex; // Thứ tự hiển thị task

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Quan hệ 1-nhiều với Question
    @OneToMany(mappedBy = "taskGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    // Helper methods
    public int getQuestionCount() {
        return questions != null ? questions.size() : 0;
    }

    public int getTotalPoints() {
        return questions != null 
            ? questions.stream().mapToInt(Question::getPoints).sum() 
            : 0;
    }

    public void addQuestion(Question question) {
        questions.add(question);
        question.setTaskGroup(this);
    }

    public void removeQuestion(Question question) {
        questions.remove(question);
        question.setTaskGroup(null);
    }
}