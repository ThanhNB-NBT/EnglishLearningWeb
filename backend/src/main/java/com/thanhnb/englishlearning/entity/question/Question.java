package com.thanhnb.englishlearning.entity.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.dto.question.request.CreateErrorCorrectionDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateFillBlankDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateMatchingDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateMultipleChoiceDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateOpenEndedDTO;
import com.thanhnb.englishlearning.dto.question.request.CreatePronunciationsDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateSentenceBuildingDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateSentenceTransformationDTO;
import com.thanhnb.englishlearning.dto.question.request.QuestionData;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "questions", indexes = {
    @Index(name = "idx_questions_parent", columnList = "parent_id, parent_type, order_index"),
    @Index(name = "idx_questions_type", columnList = "question_type"),
    @Index(name = "idx_questions_task_group", columnList = "task_group_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"data", "taskGroup"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Question {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === Parent Information ===
    @Enumerated(EnumType.STRING)
    @Column(name = "parent_type", nullable = false, length = 50)
    private ParentType parentType;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 50)
    private QuestionType questionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_group_id")
    private TaskGroup taskGroup; // null = standalone question

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private String data;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ✅ GET: Deserialize JSON → QuestionData object
     */
    @Transient
    public QuestionData getData() {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        try {
            Class<? extends QuestionData> clazz = getDataClass();
            return objectMapper.readValue(data, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize question data for type: " + questionType, e);
        }
    }
    
    /**
     * ✅ SET: Serialize QuestionData object → JSON string
     */
    @Transient
    public void setData(QuestionData data) {
        if (data == null) {
            this.data = null;
            return;
        }
        
        try {
            this.data = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize question data", e);
        }
    }
    
    /**
     * ✅ XÁC ĐỊNH CLASS dựa vào questionType
     */
    private Class<? extends QuestionData> getDataClass() {
        return switch (questionType) {
            case MULTIPLE_CHOICE, TRUE_FALSE, COMPLETE_CONVERSATION -> 
                CreateMultipleChoiceDTO.class;
                
            case FILL_BLANK, VERB_FORM, TEXT_ANSWER -> 
                CreateFillBlankDTO.class;
                
            case ERROR_CORRECTION -> 
                CreateErrorCorrectionDTO.class;
                
            case MATCHING -> 
                CreateMatchingDTO.class;
                
            case SENTENCE_BUILDING -> 
                CreateSentenceBuildingDTO.class;
                
            case SENTENCE_TRANSFORMATION -> 
                CreateSentenceTransformationDTO.class;
                
            case PRONUNCIATION -> 
                CreatePronunciationsDTO.class;
                
            case OPEN_ENDED -> 
                CreateOpenEndedDTO.class;
                
            default -> throw new IllegalArgumentException("Unknown question type: " + questionType);
        };
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================
    
    public String getExplanation() {
        QuestionData data = getData();
        return data != null ? data.getExplanation() : null;
    }

    public <T extends QuestionData> T getTypedData(Class<T> type) {
        QuestionData data = getData();
        if (data != null && type.isInstance(data)) {
            return type.cast(data);
        }
        return null;
    }

    public boolean hasTaskGroup() {
        return taskGroup != null;
    }
    
    public boolean isStandalone() {
        return taskGroup == null;
    }
    
    // ✅ Helper để lấy thông tin task
    public String getTaskName() {
        return taskGroup != null ? taskGroup.getTaskName() : null;
    }
    
    public String getTaskInstruction() {
        return taskGroup != null ? taskGroup.getInstruction() : null;
    }
}