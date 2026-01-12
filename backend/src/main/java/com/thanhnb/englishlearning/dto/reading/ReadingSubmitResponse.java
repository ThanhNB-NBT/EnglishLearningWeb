package com.thanhnb.englishlearning.dto.reading;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.service.level.LevelUpgradeService.LevelUpgradeResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadingSubmitResponse {

        @Schema(description = "ID bài đọc")
        private Long lessonId;

        @Schema(description = "Tiêu đề bài đọc")
        private String lessonTitle;

        @Schema(description = "Tổng số câu hỏi")
        private Integer totalQuestions;

        @Schema(description = "Số câu trả lời đúng")
        private Integer correctCount;

        @Schema(description = "Tổng điểm đạt được")
        private Integer totalScore;

        @Schema(description = "Phần trăm điểm")
        private Double scorePercentage;

        @Schema(description = "Có hoàn thành bài học không")
        private Boolean isCompleted;

        @Schema(description = "Điểm thưởng nhận được")
        private Integer pointsEarned;

        @Schema(description = "Có bài học tiếp theo được mở khóa không")
        private Boolean hasNextLesson;

        @Schema(description = "ID bài học tiếp theo (nếu có)")
        private Long nextLessonId;

        @Schema(description = "Chi tiết kết quả từng câu hỏi")
        private List<QuestionResultDTO> results;

        // ✅ NEW: Level upgrade information
        @Schema(description = "Có nâng cấp trình độ không")
        private Boolean levelUpgraded;

        @Schema(description = "Trình độ cũ (nếu có nâng cấp)")
        private EnglishLevel oldLevel;

        @Schema(description = "Trình độ mới (nếu có nâng cấp)")
        private EnglishLevel newLevel;

        @Schema(description = "Thông báo về tiến trình/nâng cấp")
        private String progressMessage;

        public static ReadingSubmitResponse of(
                        Long lessonId,
                        String lessonTitle,
                        int totalQuestions,
                        int correctCount,
                        int totalScore,
                        double scorePercentage,
                        boolean isCompleted,
                        int pointsEarned,
                        boolean hasNextLesson,
                        Long nextLessonId,
                        List<QuestionResultDTO> results,
                        LevelUpgradeResult levelUpgradeResult) {

                ReadingSubmitResponse response = ReadingSubmitResponse.builder()
                                .lessonId(lessonId)
                                .lessonTitle(lessonTitle)
                                .totalQuestions(totalQuestions)
                                .correctCount(correctCount)
                                .totalScore(totalScore)
                                .scorePercentage(scorePercentage)
                                .isCompleted(isCompleted)
                                .pointsEarned(pointsEarned)
                                .hasNextLesson(hasNextLesson)
                                .nextLessonId(nextLessonId)
                                .results(results)
                                .build();

                // ✅ Add level upgrade info if available
                if (levelUpgradeResult != null) {
                        response.setLevelUpgraded(levelUpgradeResult.isUpgraded());
                        response.setOldLevel(levelUpgradeResult.getOldLevel());
                        response.setNewLevel(levelUpgradeResult.getNewLevel());
                        response.setProgressMessage(levelUpgradeResult.getMessage());
                }

                return response;
        }
}