package com.thanhnb.englishlearning.dto.question.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO trả về chi tiết câu hỏi")
public class QuestionResponseDTO {
    
    @Schema(description = "ID câu hỏi", example = "101")
    private Long id;

    @Schema(description = "Loại parent (GRAMMAR, READING...)", example = "GRAMMAR")
    private ParentType parentType;
    
    @Schema(description = "ID của parent (lesson_id, topic_id)", example = "1")
    private Long parentId;
    
    @Schema(description = "Nội dung câu hỏi", example = "She ___ to school every day.")
    private String questionText;

    @Schema(description = "Loại câu hỏi", example = "MULTIPLE_CHOICE")
    private QuestionType questionType;

    @Schema(description = "Giải thích đáp án")
    private String explanation;
    
    @Schema(description = "Điểm số", example = "5")
    private Integer points;
    
    @Schema(description = "Thứ tự câu hỏi", example = "1")
    private Integer orderIndex;

    @Schema(description = "Ngày tạo")
    private LocalDateTime createdAt;
    
    @Schema(description = "Dữ liệu đặc thù của câu hỏi (JSON)")
    private Map<String, Object> metadata;
}