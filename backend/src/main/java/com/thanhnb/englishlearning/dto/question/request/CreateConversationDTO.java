package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi hội thoại (CONVERSATION)")
public class CreateConversationDTO extends CreateQuestionDTO {

    private String hint;
    @NotBlank
    private String conversationContext;

    @NotNull
    @Size(min = 2)
    private List<String> options;

    @NotBlank
    private String correctAnswer;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.COMPLETE_CONVERSATION;
    }
}
