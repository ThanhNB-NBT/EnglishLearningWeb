package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.reading.UserReadingProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ✅ Service chuyên xử lý Statistics cho Reading module
 * Tách logic từ ReadingAdminService cũ
 * Pattern: Single Responsibility Principle
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReadingStatisticsService {

    private final ReadingLessonRepository lessonRepository;
    private final UserReadingProgressRepository progressRepository;
    private final ReadingQuestionService questionService;

    // ═════════════════════════════════════════════════════════════════
    // LESSON STATISTICS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Lấy thống kê chi tiết của 1 bài đọc
     */
    public ReadingStatisticsDTO getLessonStatistics(Long lessonId) {
        log.info("[ADMIN] Getting statistics for lesson: lessonId={}", lessonId);
    
        ReadingLesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với id: " + lessonId));
    
        //Dùng query tối ưu thay vì findAll().stream()
        long totalUsers = progressRepository.count();
        long completedUsers = progressRepository.countCompletedUsersByLessonId(lessonId);
        long totalAttempts = progressRepository.getTotalAttemptsByLessonId(lessonId);
        double avgScore = progressRepository.getAverageScoreByLessonId(lessonId);
        
        int questionCount = questionService.getQuestionsByLesson(lessonId).size();
    
        ReadingStatisticsDTO stats = new ReadingStatisticsDTO();
        stats.setLessonId(lessonId);
        stats.setLessonTitle(lesson.getTitle());
        stats.setTotalUsers(totalUsers);
        stats.setCompletedUsers(completedUsers);
        stats.setCompletionRate(totalUsers > 0 ? (double) completedUsers / totalUsers * 100 : 0.0);
        stats.setTotalAttempts(totalAttempts);
        stats.setAverageScore(avgScore);
        stats.setQuestionCount(questionCount);
        stats.setIsActive(lesson.getIsActive());
    
        log.info("[ADMIN] Statistics: completedUsers={}, avgScore={:.2f}", completedUsers, avgScore);
        return stats;
    }

    // ═════════════════════════════════════════════════════════════════
    // MODULE STATISTICS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Lấy thống kê toàn bộ module Reading
     */
    public ReadingModuleStatisticsDTO getModuleStatistics() {
        log.info("[ADMIN] Getting Reading module statistics");

        long totalLessons = lessonRepository.count();
        
        long activeLessons = lessonRepository.findAll().stream()
                .filter(ReadingLesson::getIsActive)
                .count();

        long totalQuestions = lessonRepository.findAll().stream()
                .mapToInt(lesson -> questionService.getQuestionsByLesson(lesson.getId()).size())
                .sum();

        long totalUsers = progressRepository.findAll().stream()
                .map(p -> p.getUser().getId())
                .distinct()
                .count();

        long totalCompletions = progressRepository.findAll().stream()
                .filter(p -> p.getIsCompleted())
                .count();

        // Average completion rate across all lessons
        double avgCompletionRate = lessonRepository.findAll().stream()
                .mapToDouble(lesson -> {
                    long completed = progressRepository.findAll().stream()
                            .filter(p -> p.getLesson().getId().equals(lesson.getId()) && p.getIsCompleted())
                            .count();
                    long total = progressRepository.findAll().stream()
                            .filter(p -> p.getLesson().getId().equals(lesson.getId()))
                            .count();
                    return total > 0 ? (double) completed / total * 100 : 0.0;
                })
                .average()
                .orElse(0.0);

        // Build DTO
        ReadingModuleStatisticsDTO stats = new ReadingModuleStatisticsDTO();
        stats.setTotalLessons(totalLessons);
        stats.setActiveLessons(activeLessons);
        stats.setTotalQuestions(totalQuestions);
        stats.setTotalUsers(totalUsers);
        stats.setTotalCompletions(totalCompletions);
        stats.setAverageCompletionRate(avgCompletionRate);

        log.info("[ADMIN] Module stats: totalLessons={}, activeLessons={}, totalQuestions={}",
                totalLessons, activeLessons, totalQuestions);
        return stats;
    }

    // ═════════════════════════════════════════════════════════════════
    // DTO CLASSES
    // ═════════════════════════════════════════════════════════════════

    /**
     * DTO cho thống kê 1 lesson
     */
    public static class ReadingStatisticsDTO {
        private Long lessonId;
        private String lessonTitle;
        private Long totalUsers;
        private Long completedUsers;
        private Double completionRate;
        private Long totalAttempts;
        private Double averageScore;
        private Integer questionCount;
        private Boolean isActive;

        // Getters and Setters
        public Long getLessonId() {
            return lessonId;
        }

        public void setLessonId(Long lessonId) {
            this.lessonId = lessonId;
        }

        public String getLessonTitle() {
            return lessonTitle;
        }

        public void setLessonTitle(String lessonTitle) {
            this.lessonTitle = lessonTitle;
        }

        public Long getTotalUsers() {
            return totalUsers;
        }

        public void setTotalUsers(Long totalUsers) {
            this.totalUsers = totalUsers;
        }

        public Long getCompletedUsers() {
            return completedUsers;
        }

        public void setCompletedUsers(Long completedUsers) {
            this.completedUsers = completedUsers;
        }

        public Double getCompletionRate() {
            return completionRate;
        }

        public void setCompletionRate(Double completionRate) {
            this.completionRate = completionRate;
        }

        public Long getTotalAttempts() {
            return totalAttempts;
        }

        public void setTotalAttempts(Long totalAttempts) {
            this.totalAttempts = totalAttempts;
        }

        public Double getAverageScore() {
            return averageScore;
        }

        public void setAverageScore(Double averageScore) {
            this.averageScore = averageScore;
        }

        public Integer getQuestionCount() {
            return questionCount;
        }

        public void setQuestionCount(Integer questionCount) {
            this.questionCount = questionCount;
        }

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }
    }

    /**
     * DTO cho thống kê toàn module
     */
    public static class ReadingModuleStatisticsDTO {
        private Long totalLessons;
        private Long activeLessons;
        private Long totalQuestions;
        private Long totalUsers;
        private Long totalCompletions;
        private Double averageCompletionRate;

        // Getters and Setters
        public Long getTotalLessons() {
            return totalLessons;
        }

        public void setTotalLessons(Long totalLessons) {
            this.totalLessons = totalLessons;
        }

        public Long getActiveLessons() {
            return activeLessons;
        }

        public void setActiveLessons(Long activeLessons) {
            this.activeLessons = activeLessons;
        }

        public Long getTotalQuestions() {
            return totalQuestions;
        }

        public void setTotalQuestions(Long totalQuestions) {
            this.totalQuestions = totalQuestions;
        }

        public Long getTotalUsers() {
            return totalUsers;
        }

        public void setTotalUsers(Long totalUsers) {
            this.totalUsers = totalUsers;
        }

        public Long getTotalCompletions() {
            return totalCompletions;
        }

        public void setTotalCompletions(Long totalCompletions) {
            this.totalCompletions = totalCompletions;
        }

        public Double getAverageCompletionRate() {
            return averageCompletionRate;
        }

        public void setAverageCompletionRate(Double averageCompletionRate) {
            this.averageCompletionRate = averageCompletionRate;
        }
    }
}