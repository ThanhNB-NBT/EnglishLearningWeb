package com.thanhnb.englishlearning.dto.question.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.thanhnb.englishlearning.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "questionType",
    visible = true
)
@JsonSubTypes({
    // 1. Trắc nghiệm
    @JsonSubTypes.Type(value = CreateMultipleChoiceDTO.class, name = "MULTIPLE_CHOICE"),
    
    // 2. Đúng/Sai
    @JsonSubTypes.Type(value = CreateTrueFalseDTO.class, name = "TRUE_FALSE"),
    
    // 3. Nhóm điền từ
    @JsonSubTypes.Type(value = CreateFillBlankDTO.class, name = "FILL_BLANK"),
    
    @JsonSubTypes.Type(value = CreateFillBlankDTO.class, name = "VERB_FORM"),
    
    // 4. Tìm lỗi sai
    @JsonSubTypes.Type(value = CreateErrorCorrectionDTO.class, name = "ERROR_CORRECTION"),
    
    // 5. Tự luận
    @JsonSubTypes.Type(value = CreateTextAnswerDTO.class, name = "TEXT_ANSWER"),
    @JsonSubTypes.Type(value = CreateTextAnswerDTO.class, name = "SHORT_ANSWER"),
    
    // 6. Nối cặp
    @JsonSubTypes.Type(value = CreateMatchingDTO.class, name = "MATCHING"),

    // 7. Sắp xếp câu
    @JsonSubTypes.Type(value = CreateSentenceBuildingDTO.class, name = "SENTENCE_BUILDING"),

    // 8. Hội thoại
    @JsonSubTypes.Type(value = CreateConversationDTO.class, name = "COMPLETE_CONVERSATION"),

    // 9. Phát âm
    @JsonSubTypes.Type(value = CreatePronunciationsDTO.class, name = "PRONUNCIATION"),

    // 10. Đọc hiểu & Tự luận mở
    @JsonSubTypes.Type(value = CreateReadingComprehensionDTO.class, name = "READING_COMPREHENSION"),
    @JsonSubTypes.Type(value = CreateOpenEndedDTO.class, name = "OPEN_ENDED")
})
public abstract class CreateQuestionDTO {
    
    @NotNull(message = "ParentType không được null")
    private ParentType parentType;

    @NotNull(message = "ParentId không được null")
    private Long parentId;

    @NotBlank(message = "QuestionText không được để trống")
    private String questionText;

    @Schema(description = "Giải thích đáp án")
    private String explanation;

    @NotNull
    @Min(value = 1, message = "Points phải => 1")
    private Integer points = 1;

    @NotNull
    @Min(value = 0, message = "Orderindex phải => 0")
    private Integer orderIndex = 0;

    public abstract QuestionType getQuestionType();
}