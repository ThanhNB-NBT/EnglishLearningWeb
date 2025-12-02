package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.ParentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "DTO để TẠO MỚI câu hỏi nói (OPEN_ENDED)")
public class CreateSpeakingDTO {
    
    // --- Các trường chung ---
    @NotNull
    private ParentType parentType; // Sẽ luôn là SPEAKING
    
    @NotNull
    private Long parentId; // topic_id
    
    @NotBlank
    @Schema(example = "Tell me about your hobbies.")
    private String questionText;
    
    private String explanation; // Có thể là giải thích về điểm ngữ pháp
    
    @NotNull @Min(1)
    private Integer points = 10;
    
    @NotNull @Min(1)
    private Integer orderIndex;

    // --- Trường đặc thù ---
    @Schema(description = "Câu trả lời mẫu (cho AI so sánh)", example = "I enjoy reading books and playing sports...")
    private String suggestedAnswer;
    
    @Schema(description = "Thời gian trả lời tối đa (giây)", example = "60")
    private Integer timeLimitSeconds;
}