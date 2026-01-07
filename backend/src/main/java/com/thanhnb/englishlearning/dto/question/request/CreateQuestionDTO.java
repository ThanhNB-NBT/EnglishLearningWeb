package com.thanhnb.englishlearning.dto.question.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.thanhnb.englishlearning.enums.QuestionType;

import io.swagger.v3.oas.annotations.media.Schema;

import com.thanhnb.englishlearning.enums.ParentType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class CreateQuestionDTO {

    // --- CỘT CỨNG ---
    private Long id;
    private ParentType parentType;
    private Long parentId;

    @Schema(description = "Nội dung câu hỏi", example = "What is the capital of Vietnam?")
    private String questionText;

    @NotNull(message = "Question type không được null")
    @Schema(description = "Loại câu hỏi", example = "MULTIPLE_CHOICE")
    private QuestionType questionType;

    @Positive(message = "Points phải là số dương")
    @Schema(description = "Điểm số", example = "2")
    private Integer points;

    @Positive(message = "Order index phải là số dương")
    @Schema(description = "Thứ tự hiển thị (tùy chọn, tự động nếu null)", example = "1")
    private Integer orderIndex;
    
    @Schema(description = "ID của TaskGroup (null = standalone question)", example = "5")
    private Long taskGroupId;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "questionType")
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
    @Valid
    @NotNull(message = "questionData không được để trống")
    private QuestionData data;
}