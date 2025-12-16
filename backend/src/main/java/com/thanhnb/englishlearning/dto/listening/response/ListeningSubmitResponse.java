package com.thanhnb.englishlearning.dto.listening.response;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ListeningSubmitResponse {
        private Long lessonId;
        private Boolean isPassed;
        private double scorePercentage;
        private Integer totalScore;
        private int correctCount;
        private int totalQuestions;
        private int pointsEarned;

        // Hỗ trợ hiển thị kết quả chi tiết
        private List<QuestionResultDTO> results;

        // Điều hướng
        private boolean hasNextLesson;
        private Long nextLessonId;

        public static ListeningSubmitResponse of(
                        Long lessonId,
                        Integer totalQuestions,
                        Integer correctCount,
                        Integer totalScore,
                        Double scorePercentage,
                        Boolean isPassed,
                        Integer pointsEarned,
                        Long nextLessonId,
                        List<QuestionResultDTO> results) {

                return ListeningSubmitResponse.builder()
                                .lessonId(lessonId)
                                .totalQuestions(totalQuestions)
                                .correctCount(correctCount)
                                .totalScore(totalScore)
                                .scorePercentage(scorePercentage)
                                .isPassed(isPassed)
                                .pointsEarned(pointsEarned)
                                .nextLessonId(nextLessonId)
                                .results(results)
                                .build();
        }

        public static ListeningSubmitResponse simple(
                        Long lessonId,
                        Integer totalQuestions,
                        Integer correctCount,
                        Double scorePercentage,
                        Boolean isPassed,
                        List<QuestionResultDTO> results) {

                int totalScore = results.stream()
                                .mapToInt(QuestionResultDTO::getPoints)
                                .sum();

                return of(lessonId, totalQuestions, correctCount,
                                totalScore, scorePercentage, isPassed, 0,
                                null, results);
        }
}