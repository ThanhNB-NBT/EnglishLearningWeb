package com.thanhnb.englishlearning.service.listening;

import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning.repository.listening.UserListeningProgressRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service chuyên xử lý thống kê cho Listening module
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ListeningStatisticsService {

        private final ListeningLessonRepository lessonRepository;
        private final QuestionRepository questionRepository;
        private final UserListeningProgressRepository progressRepository;

        /**
         * Lấy thống kê chi tiết cho 1 listening lesson
         */
        public ListeningStatisticsDTO getLessonStatistics(Long lessonId) {
                ListeningLesson lesson = lessonRepository.findById(lessonId)
                                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id:  " + lessonId));

                long questionCount = questionRepository.countByParentTypeAndParentId(
                                ParentType.LISTENING, lessonId);

                long totalAttempts = progressRepository.countByLessonId(lessonId);
                long completedCount = progressRepository.countByLessonIdAndIsCompletedTrue(lessonId);

                Double avgScore = progressRepository.findAverageScoreByLessonId(lessonId);
                Double completionRate = totalAttempts > 0
                                ? (completedCount * 100.0 / totalAttempts)
                                : 0.0;

                Map<String, Long> scoreDistribution = calculateScoreDistribution(lessonId);

                log.info("Statistics for listening lesson {}: {} questions, {} attempts, {:.2f}% completion",
                                lessonId, questionCount, totalAttempts, completionRate);

                return ListeningStatisticsDTO.builder()
                                .lessonId(lessonId)
                                .lessonTitle(lesson.getTitle())
                                .difficulty(lesson.getDifficulty().name())
                                .questionCount((int) questionCount)
                                .totalAttempts((int) totalAttempts)
                                .completedCount((int) completedCount)
                                .averageScore(round(avgScore != null ? avgScore : 0.0, 2))
                                .completionRate(round(completionRate, 2))
                                .scoreDistribution(scoreDistribution)
                                .isActive(lesson.getIsActive())
                                .build();
        }

        /**
         * Lấy thống kê toàn bộ module Listening
         */
        public ListeningModuleStatisticsDTO getModuleStatistics() {
                long totalLessons = lessonRepository.count();
                long activeLessons = lessonRepository.countByIsActiveTrue();
                long totalQuestions = questionRepository.countByParentType(ParentType.LISTENING);

                long totalAttempts = progressRepository.count();
                long totalCompleted = progressRepository.countByIsCompletedTrue();

                Double avgScore = progressRepository.findAverageScore();
                Double completionRate = totalAttempts > 0
                                ? (totalCompleted * 100.0 / totalAttempts)
                                : 0.0;

                List<ListeningLesson> allLessons = lessonRepository.findAll();

                Map<String, Long> lessonsByDifficulty = allLessons.stream()
                                .collect(Collectors.groupingBy(
                                                l -> l.getDifficulty().name(),
                                                Collectors.counting()));

                List<LessonSummaryDTO> topLessons = allLessons.stream()
                                .limit(5)
                                .map(lesson -> {
                                        long attempts = progressRepository.countByLessonId(lesson.getId());
                                        long completed = progressRepository
                                                        .countByLessonIdAndIsCompletedTrue(lesson.getId());
                                        Double lessonAvgScore = progressRepository
                                                        .findAverageScoreByLessonId(lesson.getId());

                                        return LessonSummaryDTO.builder()
                                                        .lessonId(lesson.getId())
                                                        .title(lesson.getTitle())
                                                        .difficulty(lesson.getDifficulty().name())
                                                        .attempts((int) attempts)
                                                        .completedCount((int) completed)
                                                        .averageScore(round(
                                                                        lessonAvgScore != null ? lessonAvgScore : 0.0,
                                                                        2))
                                                        .build();
                                })
                                .collect(Collectors.toList());

                log.info("Module statistics: {} lessons, {} questions, {} attempts",
                                totalLessons, totalQuestions, totalAttempts);

                return ListeningModuleStatisticsDTO.builder()
                                .totalLessons((int) totalLessons)
                                .activeLessons((int) activeLessons)
                                .totalQuestions((int) totalQuestions)
                                .totalAttempts((int) totalAttempts)
                                .totalCompleted((int) totalCompleted)
                                .averageScore(round(avgScore != null ? avgScore : 0.0, 2))
                                .completionRate(round(completionRate, 2))
                                .lessonsByDifficulty(lessonsByDifficulty)
                                .topLessons(topLessons)
                                .build();
        }

        /**
         * Calculate score distribution (0-20%, 20-40%, etc.)
         */
        private Map<String, Long> calculateScoreDistribution(Long lessonId) {
                List<BigDecimal> scores = progressRepository.findAllScoresByLessonId(lessonId);

                return scores.stream()
                                .collect(Collectors.groupingBy(
                                                score -> {
                                                        double s = score.doubleValue();
                                                        if (s < 20)
                                                                return "0-20%";
                                                        if (s < 40)
                                                                return "20-40%";
                                                        if (s < 60)
                                                                return "40-60%";
                                                        if (s < 80)
                                                                return "60-80%";
                                                        return "80-100%";
                                                },
                                                Collectors.counting()));
        }

        /**
         * Round to n decimal places
         */
        private double round(double value, int places) {
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                        return 0.0;
                }
                BigDecimal bd = BigDecimal.valueOf(value);
                bd = bd.setScale(places, RoundingMode.HALF_UP);
                return bd.doubleValue();
        }

        // ===== DTOs =====

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @lombok.Builder
        public static class ListeningStatisticsDTO {
                private Long lessonId;
                private String lessonTitle;
                private String difficulty;
                private Integer questionCount;
                private Integer totalAttempts;
                private Integer completedCount;
                private Double averageScore;
                private Double completionRate;
                private Map<String, Long> scoreDistribution;
                private Boolean isActive;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @lombok.Builder
        public static class ListeningModuleStatisticsDTO {
                private Integer totalLessons;
                private Integer activeLessons;
                private Integer totalQuestions;
                private Integer totalAttempts;
                private Integer totalCompleted;
                private Double averageScore;
                private Double completionRate;
                private Map<String, Long> lessonsByDifficulty;
                private List<LessonSummaryDTO> topLessons;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @lombok.Builder
        public static class LessonSummaryDTO {
                private Long lessonId;
                private String title;
                private String difficulty;
                private Integer attempts;
                private Integer completedCount;
                private Double averageScore;
        }
}