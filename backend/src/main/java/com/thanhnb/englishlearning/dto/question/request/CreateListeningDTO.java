package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * DTO cho Listening Comprehension
 * Tương tự Reading nhưng có thêm audioUrl
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO cho Listening Comprehension")
public class CreateListeningDTO extends CreateQuestionDTO {

    @NotBlank(message = "Audio URL không được để trống")
    @Schema(description = "URL file âm thanh", example = "https://cdn.example.com/audio/lesson1.mp3")
    private String audioUrl;
    
    @Schema(description = "Transcript (Script nghe)", 
            example = "A: Hi, how are you? B: I'm fine, thanks...")
    private String transcript;
    
    @Schema(description = "Có hiển thị transcript khi làm bài không", example = "false")
    private Boolean showTranscript;
    
    @Schema(description = "Thời lượng audio (giây)", example = "120")
    private Integer audioDuration;
    
    @NotNull
    @Size(min = 1, message = "Cần ít nhất 1 câu hỏi")
    @Valid
    private List<SubQuestionDTO> subQuestions;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.LISTENING_COMPREHENSION;
    }

    /**
     * Sub-question cho Listening (giống Reading)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubQuestionDTO {
        @NotNull @Min(1)
        private Integer order;
        
        @NotBlank
        private String questionText;
        
        @NotNull
        private SubQuestionType type;
        
        @NotNull @Min(1)
        private Integer points;
        
        // Fields tùy theo type (giống Reading)
        private List<String> options;
        private String correctAnswer;
        private Boolean correctBoolean;
        private List<String> acceptedAnswers;
        private Boolean caseSensitive;
        private String explanation;
    }
    
    public enum SubQuestionType {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        SHORT_ANSWER,
        FILL_BLANK,
        MATCHING
    }
}