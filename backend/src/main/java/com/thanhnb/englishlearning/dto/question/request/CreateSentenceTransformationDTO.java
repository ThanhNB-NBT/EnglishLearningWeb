package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi viết lại câu (SENTENCE_TRANSFORMATION)")
public class CreateSentenceTransformationDTO extends CreateQuestionDTO {

    @Schema(description = "Câu gốc cần viết lại", example = "It is a pity I didn't see him.")
    @NotBlank
    private String originalSentence;

    @Schema(description = "Từ/Cụm từ gợi ý đầu câu", example = "I wish")
    private String beginningPhrase; // Có thể null nếu đề bài không cho gợi ý

    @Schema(description = "Danh sách các câu trả lời đúng (Full câu)", example = "[\"I wish I had seen him\", \"I wish that I had seen him\"]")
    @NotNull
    @Size(min = 1)
    private List<String> correctAnswers;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.SENTENCE_TRANSFORMATION;
    }
}