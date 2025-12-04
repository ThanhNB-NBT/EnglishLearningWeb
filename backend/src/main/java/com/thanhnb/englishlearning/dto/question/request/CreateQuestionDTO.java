package com.thanhnb.englishlearning.dto.question.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.thanhnb.englishlearning.enums.QuestionType;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "questionType",
    visible = true
)
@JsonSubTypes({
    // 1. Nhóm Lựa chọn (Xử lý cả True/False, Conversation)
    @JsonSubTypes.Type(value = CreateMultipleChoiceDTO.class, name = "MULTIPLE_CHOICE"),
    @JsonSubTypes.Type(value = CreateMultipleChoiceDTO.class, name = "TRUE_FALSE"),
    @JsonSubTypes.Type(value = CreateMultipleChoiceDTO.class, name = "COMPLETE_CONVERSATION"),
    
    // 2. Nhóm Điền từ (Xử lý cả Verb Form, Text Answer)
    @JsonSubTypes.Type(value = CreateFillBlankDTO.class, name = "FILL_BLANK"),
    @JsonSubTypes.Type(value = CreateFillBlankDTO.class, name = "VERB_FORM"),
    @JsonSubTypes.Type(value = CreateFillBlankDTO.class, name = "TEXT_ANSWER"),
    
    // 3. Các nhóm đặc thù
    @JsonSubTypes.Type(value = CreateErrorCorrectionDTO.class, name = "ERROR_CORRECTION"),
    @JsonSubTypes.Type(value = CreateMatchingDTO.class, name = "MATCHING"),
    @JsonSubTypes.Type(value = CreateSentenceBuildingDTO.class, name = "SENTENCE_BUILDING"),
    @JsonSubTypes.Type(value = CreateSentenceTransformationDTO.class, name = "SENTENCE_TRANSFORMATION"),
    @JsonSubTypes.Type(value = CreatePronunciationsDTO.class, name = "PRONUNCIATION"),
    @JsonSubTypes.Type(value = CreateOpenEndedDTO.class, name = "OPEN_ENDED")
})
public abstract class CreateQuestionDTO {
    @NotNull private com.thanhnb.englishlearning.enums.ParentType parentType;
    @NotNull private Long parentId;
    @NotBlank private String questionText;
    private String explanation;
    @NotNull @Min(1) private Integer points = 1;
    @NotNull @Min(0) private Integer orderIndex = 0;
    @JsonProperty("questionType") private QuestionType questionType;
    public abstract QuestionType getQuestionType();
}