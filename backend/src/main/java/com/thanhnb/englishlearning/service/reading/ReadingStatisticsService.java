package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.reading.UserReadingProgressRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ✅ OPTIMIZED: Service chuyên xử lý Statistics cho Reading module
 * - Sử dụng query methods từ repository thay vì stream()
 * - Giảm số lần query database
 * - Cải thiện performance đáng kể
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReadingStatisticsService {

    private final ReadingLessonRepository lessonRepository;
    private final UserReadingProgressRepository progressRepository;
    private final QuestionRepository questionRepository;

    // ═════════════════════════════════════════════════════════════════
    // 📊 LESSON STATISTICS (OPTIMIZED)
    // ═════════════════════════════════════════════════════════════════

    /**
     * Lấy thống kê chi tiết của 1 bài đọc
     * ✅ OPTIMIZED: Sử dụng query methods thay vì findAll().stream()
     */
    public ReadingStatisticsDTO getLessonStatistics(Long lessonId) {
        log.info("[ADMIN] Getting statistics for lesson: lessonId={}", lessonId);
    
        ReadingLesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với id: " + lessonId));
    
        // ✅ Dùng query tối ưu thay vì findAll().stream()
        long totalUsers = progressRepository.countDistinctUsers();
        long completedUsers = progressRepository.countCompletedUsersByLessonId(lessonId);
        long totalAttempts = progressRepository.getTotalAttemptsByLessonId(lessonId);
        double avgScore = progressRepository.getAverageScoreByLessonId(lessonId);
        
        // ✅ Count questions efficiently
        long questionCount = questionRepository.countByParentTypeAndParentId(
                ParentType.READING, lessonId);
    
        ReadingStatisticsDTO stats = new ReadingStatisticsDTO();
        stats.setLessonId(lessonId);
        stats.setLessonTitle(lesson.getTitle());
        stats.setTotalUsers(totalUsers);
        stats.setCompletedUsers(completedUsers);
        stats.setCompletionRate(totalUsers > 0 ? (double) completedUsers / totalUsers * 100 : 0.0);
        stats.setTotalAttempts(totalAttempts);
        stats.setAverageScore(avgScore);
        stats.setQuestionCount((int) questionCount);
        stats.setIsActive(lesson.getIsActive());
    
        log.info("[ADMIN] Statistics: completedUsers={}, avgScore={:.2f}", completedUsers, avgScore);
        return stats;
    }

    // ═════════════════════════════════════════════════════════════════
    // 📈 MODULE STATISTICS (OPTIMIZED)
    // ═════════════════════════════════════════════════════════════════

    /**
     * Lấy thống kê toàn bộ module Reading
     * ✅ OPTIMIZED: Giảm số lần query từ N xuống còn 5 queries
     */
    public ReadingModuleStatisticsDTO getModuleStatistics() {
        log.info("[ADMIN] Getting Reading module statistics");

        // Query 1: Count lessons
        long totalLessons = lessonRepository.count();
        
        // Query 2: Count active lessons
        long activeLessons = lessonRepository.countByIsActiveTrue();

        // Query 3: Count total questions
        long totalQuestions = questionRepository.countByParentType(ParentType.READING);

        // Query 4: Count distinct users
        long totalUsers = progressRepository.countDistinctUsers();

        // Query 5: Count total completions
        long totalCompletions = progressRepository.countTotalCompletions();
        
        // Query 6: Get average score
        double moduleAverageScore = progressRepository.getModuleAverageScore();

        // Calculate average completion rate
        double avgCompletionRate = totalUsers > 0 
                ? (double) totalCompletions / totalUsers * 100 
                : 0.0;

        // Build DTO
        ReadingModuleStatisticsDTO stats = new ReadingModuleStatisticsDTO();
        stats.setTotalLessons(totalLessons);
        stats.setActiveLessons(activeLessons);
        stats.setTotalQuestions(totalQuestions);
        stats.setTotalUsers(totalUsers);
        stats.setTotalCompletions(totalCompletions);
        stats.setAverageCompletionRate(avgCompletionRate);
        stats.setModuleAverageScore(moduleAverageScore);

        log.info("[ADMIN] Module stats: totalLessons={}, activeLessons={}, totalQuestions={}, " +
                "totalUsers={}, avgScore={:.2f}",
                totalLessons, activeLessons, totalQuestions, totalUsers, moduleAverageScore);
        return stats;
    }

    // ═════════════════════════════════════════════════════════════════
    // 📊 USER-SPECIFIC STATISTICS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Lấy thống kê của một user cụ thể
     */
    public UserReadingStatisticsDTO getUserStatistics(Long userId) {
        log.info("[USER] Getting reading statistics for user: userId={}", userId);

        var allProgress = progressRepository.findByUserId(userId);
        
        long totalAttempted = allProgress.size();
        long totalCompleted = allProgress.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsCompleted()))
                .count();
        
        double avgScore = allProgress.stream()
                .mapToDouble(p -> p.getScorePercentage().doubleValue())
                .average()
                .orElse(0.0);
        
        int totalAttempts = allProgress.stream()
                .mapToInt(p -> p.getAttempts() != null ? p.getAttempts() : 0)
                .sum();

        UserReadingStatisticsDTO stats = new UserReadingStatisticsDTO();
        stats.setUserId(userId);
        stats.setTotalLessonsAttempted(totalAttempted);
        stats.setTotalLessonsCompleted(totalCompleted);
        stats.setCompletionRate(totalAttempted > 0 ? (double) totalCompleted / totalAttempted * 100 : 0.0);
        stats.setAverageScore(avgScore);
        stats.setTotalAttempts(totalAttempts);

        return stats;
    }

    // ═════════════════════════════════════════════════════════════════
    // 📋 DTO CLASSES
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
        public Long getLessonId() { return lessonId; }
        public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
        
        public String getLessonTitle() { return lessonTitle; }
        public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }
        
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
        
        public Long getCompletedUsers() { return completedUsers; }
        public void setCompletedUsers(Long completedUsers) { this.completedUsers = completedUsers; }
        
        public Double getCompletionRate() { return completionRate; }
        public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }
        
        public Long getTotalAttempts() { return totalAttempts; }
        public void setTotalAttempts(Long totalAttempts) { this.totalAttempts = totalAttempts; }
        
        public Double getAverageScore() { return averageScore; }
        public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }
        
        public Integer getQuestionCount() { return questionCount; }
        public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
        
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
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
        private Double moduleAverageScore;

        // Getters and Setters
        public Long getTotalLessons() { return totalLessons; }
        public void setTotalLessons(Long totalLessons) { this.totalLessons = totalLessons; }
        
        public Long getActiveLessons() { return activeLessons; }
        public void setActiveLessons(Long activeLessons) { this.activeLessons = activeLessons; }
        
        public Long getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(Long totalQuestions) { this.totalQuestions = totalQuestions; }
        
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
        
        public Long getTotalCompletions() { return totalCompletions; }
        public void setTotalCompletions(Long totalCompletions) { this.totalCompletions = totalCompletions; }
        
        public Double getAverageCompletionRate() { return averageCompletionRate; }
        public void setAverageCompletionRate(Double rate) { this.averageCompletionRate = rate; }
        
        public Double getModuleAverageScore() { return moduleAverageScore; }
        public void setModuleAverageScore(Double score) { this.moduleAverageScore = score; }
    }

    /**
     * DTO cho thống kê của user
     */
    public static class UserReadingStatisticsDTO {
        private Long userId;
        private Long totalLessonsAttempted;
        private Long totalLessonsCompleted;
        private Double completionRate;
        private Double averageScore;
        private Integer totalAttempts;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Long getTotalLessonsAttempted() { return totalLessonsAttempted; }
        public void setTotalLessonsAttempted(Long total) { this.totalLessonsAttempted = total; }
        
        public Long getTotalLessonsCompleted() { return totalLessonsCompleted; }
        public void setTotalLessonsCompleted(Long total) { this.totalLessonsCompleted = total; }
        
        public Double getCompletionRate() { return completionRate; }
        public void setCompletionRate(Double rate) { this.completionRate = rate; }
        
        public Double getAverageScore() { return averageScore; }
        public void setAverageScore(Double score) { this.averageScore = score; }
        
        public Integer getTotalAttempts() { return totalAttempts; }
        public void setTotalAttempts(Integer attempts) { this.totalAttempts = attempts; }
    }
}