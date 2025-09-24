package com.thanhnb.englishlearning.dto.grammar.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import com.thanhnb.englishlearning.dto.grammar.QuestionResultDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonResultResponse {

    @Schema(description = "ID của bài học", example = "1")
    private Long lessonId;

    @Schema(description = "Tiêu đề của bài học", example = "Thì hiện tại đơn")
    private String lessonTitle;

    @Schema(description = "Tổng số câu hỏi trong bài học", example = "10")
    private Integer totalQuestions;

    @Schema(description = "Số câu trả lời đúng", example = "7")
    private Integer correctAnswers;

    @Schema(description = "Tổng điểm đạt được", example = "70")
    private Integer score;

    @Schema(description = "Điểm thưởng cho việc hoàn thành bài học", example = "100")
    private Integer pointsEarned;

    @Schema(description = "Trạng thái hoàn thành bài học", example = "true")
    private Boolean isCompleted;

    @Schema(description = "Trạng thái mở khóa bài học tiếp theo", example = "true")
    private Boolean hasUnlockedNext;

    @Schema(description = "ID của bài học tiếp theo, nếu đã mở khóa", example = "2")
    private Long nextLessonId;

    @Schema(description = "Kết quả chi tiết cho từng câu hỏi")
    private List<QuestionResultDTO> questionResults;
}
