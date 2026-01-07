package com.thanhnb.englishlearning.dto.common;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.service.level.LevelUpgradeService.LevelUpgradeResult;
import lombok.Builder;

import java.util.List;

@Builder
public record SubmitResultDTO(
    Integer totalQuestions,
    Integer correctCount,
    Integer totalScore,
    Double scorePercentage,
    Boolean isPassed,
    Integer pointsEarned,
    Boolean hasUnlockedNext,
    Long nextLessonId,
    List<QuestionResultDTO> results,
    LevelUpgradeResult levelUpgradeResult
) {}