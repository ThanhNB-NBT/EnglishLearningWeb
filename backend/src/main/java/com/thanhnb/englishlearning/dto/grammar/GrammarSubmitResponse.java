package com.thanhnb.englishlearning.dto.grammar;

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

/**
 * ✅ Response sau khi nộp bài Grammar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Grammar Submit Response - Kết quả nộp bài ngữ pháp")
public class GrammarSubmitResponse {

    // ═══════════════════════════════════════════════════════════
    // BASIC INFO
    // ═══════════════════════════════════════════════════════════
    
    @Schema(description = "ID bài học", example = "1")
    private Long lessonId;
    
    @Schema(description = "Tiêu đề bài học", example = "Present Simple Tense")
    private String lessonTitle;

    // ═══════════════════════════════════════════════════════════
    // SCORE INFO
    // ═══════════════════════════════════════════════════════════
    
    @Schema(description = "Tổng số câu hỏi", example = "10")
    private Integer totalQuestions;
    
    @Schema(description = "Số câu trả lời đúng", example = "8")
    private Integer correctCount;
    
    @Schema(description = "Tổng điểm đạt được", example = "80")
    private Integer totalScore;
    
    @Schema(description = "Phần trăm điểm", example = "80.0")
    private Double scorePercentage;
    
    @Schema(description = "Có hoàn thành bài học không", example = "true")
    private Boolean isPassed;

    // ═══════════════════════════════════════════════════════════
    // REWARDS & UNLOCK
    // ═══════════════════════════════════════════════════════════
    
    @Schema(description = "Điểm thưởng nhận được", example = "50")
    private Integer pointsEarned;
    
    @Schema(description = "Có bài học tiếp theo được mở khóa không", example = "true")
    private Boolean hasUnlockedNext;
    
    @Schema(description = "ID bài học tiếp theo (nếu có)", example = "2")
    private Long nextLessonId;

    // ═══════════════════════════════════════════════════════════
    // DETAILED RESULTS
    // ═══════════════════════════════════════════════════════════
    
    @Schema(description = "Chi tiết kết quả từng câu hỏi")
    private List<QuestionResultDTO> results;

    // ═══════════════════════════════════════════════════════════
    // LEVEL UPGRADE INFO
    // ═══════════════════════════════════════════════════════════
    
    @Schema(description = "Có nâng cấp trình độ không", example = "true")
    private Boolean levelUpgraded;
    
    @Schema(description = "Trình độ cũ (nếu có nâng cấp)", example = "A1")
    private EnglishLevel oldLevel;
    
    @Schema(description = "Trình độ mới (nếu có nâng cấp)", example = "A2")
    private EnglishLevel newLevel;
    
    @Schema(description = "Thông báo về tiến trình/nâng cấp", 
            example = "🎉 Chúc mừng! Bạn đã được nâng cấp từ A1 lên A2!")
    private String progressMessage;

    // ═══════════════════════════════════════════════════════════
    // FACTORY METHOD
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Create response from submission result
     */
    public static GrammarSubmitResponse of(
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
            List<QuestionResultDTO> results,
            LevelUpgradeResult levelUpgradeResult) {

        GrammarSubmitResponse response = GrammarSubmitResponse.builder()
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
                .results(results)
                .build();

        // Add level upgrade info if available
        if (levelUpgradeResult != null) {
            response.setLevelUpgraded(levelUpgradeResult.isUpgraded());
            response.setOldLevel(levelUpgradeResult.getOldLevel());
            response.setNewLevel(levelUpgradeResult.getNewLevel());
            response.setProgressMessage(levelUpgradeResult.getMessage());
        }

        return response;
    }
}