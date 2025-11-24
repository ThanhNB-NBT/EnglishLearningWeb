package com.thanhnb.englishlearning.dto.question.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.thanhnb.englishlearning.enums.*;
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
    @JsonSubTypes.Type(value = CreateMultipleChoiceDTO.class, name = "MULTIPLE_CHOICE"),
    @JsonSubTypes.Type(value = CreateTrueFalseDTO.class, name = "TRUE_FALSE"),
    @JsonSubTypes.Type(value = CreateTextAnswerDTO.class, name = "FILL_BLANK"),
    @JsonSubTypes.Type(value = CreateTextAnswerDTO.class, name = "SHORT_ANSWER"),
    @JsonSubTypes.Type(value = CreateTextAnswerDTO.class, name = "VERB_FORM"),
    @JsonSubTypes.Type(value = CreateTextAnswerDTO.class, name = "ERROR_CORRECTION"),
    @JsonSubTypes.Type(value = CreateMatchingDTO.class, name = "MATCHING"),
    @JsonSubTypes.Type(value = CreateSentenceBuildingDTO.class, name = "SENTENCE_BUILDING"),
    @JsonSubTypes.Type(value = CreateConversationDTO.class, name = "COMPLETE_CONVERSATION"),
    @JsonSubTypes.Type(value = CreatePronunciationsDTO.class, name = "PRONUNCIATION"),
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

    private String explanation;

    @NotNull
    @Min(value = 1, message = "Points phải => 1")
    private Integer points = 1;

    @NotNull
    @Min(value = 0, message = "Orderindex phải => 0")
    private Integer orderIndex = 0;

    public abstract QuestionType getQuestionType();
}
