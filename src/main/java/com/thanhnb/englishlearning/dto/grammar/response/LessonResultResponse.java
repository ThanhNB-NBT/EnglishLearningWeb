package com.thanhnb.englishlearning.dto.grammar.response;

import com.thanhnb.englishlearning.dto.grammar.QuestionResultDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Kết quả sau khi nộp bài học")
public record LessonResultResponse(
    @Schema(description = "ID bài học", example = "1")
    Long lessonId,

    @Schema(description = "Tiêu đề bài học")
    String lessonTitle,

    @Schema(description = "Tổng số câu hỏi", example = "10")
    Integer totalQuestions,

    @Schema(description = "Số câu đúng", example = "7")
    Integer correctAnswers,

    @Schema(description = "Tổng điểm", example = "35")
    Integer score,

    @Schema(description = "Điểm thưởng nhận được", example = "10")
    Integer pointsEarned,

    @Schema(description = "Đã hoàn thành chưa", example = "true")
    Boolean isPassed,

    @Schema(description = "Đã mở khóa bài tiếp theo chưa", example = "true")
    Boolean hasUnlockedNext,

    @Schema(description = "ID bài học tiếp theo", example = "2")
    Long nextLessonId,

    @Schema(description = "Chi tiết kết quả từng câu")
    List<QuestionResultDTO> questionResults
) {}