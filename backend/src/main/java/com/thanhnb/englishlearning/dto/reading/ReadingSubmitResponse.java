package com.thanhnb.englishlearning.dto.reading;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Kết quả sau khi nộp bài đọc")
public class ReadingSubmitResponse {

        @Schema(description = "ID bài đọc", example = "1")
        private Long lessonId;

        @Schema(description = "Tiêu đề bài đọc", example = "The Digital Paradox")
        private String lessonTitle;

        @Schema(description = "Tổng số câu hỏi", example = "10")
        private Integer totalQuestions;

        @Schema(description = "Số câu trả lời đúng", example = "8")
        private Integer correctCount;

        @Schema(description = "Tổng điểm đạt được", example = "40")
        private Integer totalScore;

        @Schema(description = "Phần trăm điểm", example = "80.0")
        private Double scorePercentage;

        @Schema(description = "Đã pass (>= 80%) hay chưa", example = "true")
        private Boolean isPassed;

        @Schema(description = "Điểm thưởng nhận được (chỉ tính lần đầu complete)", example = "25")
        private Integer pointsEarned;

        @Schema(description = "Đã mở khóa bài tiếp theo chưa", example = "true")
        private Boolean hasUnlockedNext;

        @Schema(description = "ID bài đọc tiếp theo (nếu có)", example = "2")
        private Long nextLessonId;

        @Schema(description = "Chi tiết kết quả từng câu hỏi")
        private List<QuestionResultDTO> questionResults;

        @Deprecated
        @Schema(description = "⚠️ Deprecated: Dùng isPassed thay thế", example = "true")
        private Boolean completed;

        // ✅ FIX: Thêm getter method cho backward compatibility
        public Boolean isCompleted() {
                return this.isPassed != null ? this.isPassed : this.completed;
        }

        // ===== FACTORY METHODS =====

        public static ReadingSubmitResponse of(
                        Long lessonId,
                        String lessonTitle,
                        Integer totalQuestions,
                        Integer correctCount,
                        Integer totalScore,
                        Double scorePercentage,
                        Boolean isPassed,
                        Integer pointsEarned,
                        Boolean hasUnlockedNext,
                        Long nextLessonId,
                        List<QuestionResultDTO> questionResults) {

                return ReadingSubmitResponse.builder()
                                .lessonId(lessonId)
                                .lessonTitle(lessonTitle)
                                .totalQuestions(totalQuestions)
                                .correctCount(correctCount)
                                .totalScore(totalScore)
                                .scorePercentage(scorePercentage)
                                .isPassed(isPassed)
                                .pointsEarned(pointsEarned)
                                .hasUnlockedNext(hasUnlockedNext)
                                .nextLessonId(nextLessonId)
                                .questionResults(questionResults)
                                .completed(isPassed) // Backward compatibility
                                .build();
        }

        public static ReadingSubmitResponse simple(
                        Long lessonId,
                        String lessonTitle,
                        Integer totalQuestions,
                        Integer correctCount,
                        Double scorePercentage,
                        Boolean isPassed,
                        List<QuestionResultDTO> questionResults) {

                int totalScore = questionResults.stream()
                                .mapToInt(QuestionResultDTO::getPoints)
                                .sum();

                return of(lessonId, lessonTitle, totalQuestions, correctCount,
                                totalScore, scorePercentage, isPassed, 0,
                                false, null, questionResults);
        }
}