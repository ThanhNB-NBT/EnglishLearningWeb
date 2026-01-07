package com.thanhnb.englishlearning.entity.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.thanhnb.englishlearning.enums.ParentType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
@EqualsAndHashCode(onlyExplicitlyIncluded = true) 
public class UserStats {

    @Id
    @EqualsAndHashCode.Include
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // ==================== POINTS ====================

    @Column(name = "total_points", nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    // ==================== STREAK ====================

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private Integer longestStreak = 0;

    @Column(name = "last_streak_date")
    private LocalDate lastStreakDate;

    // ==================== COMPLETION ====================

    @Column(name = "total_lessons_completed", nullable = false)
    @Builder.Default
    private Integer totalLessonsCompleted = 0;

    @Column(name = "total_study_time_minutes", nullable = false)
    @Builder.Default
    private Integer totalStudyTimeMinutes = 0;

    @Column(name = "grammar_completed", nullable = false)
    @Builder.Default
    private Integer grammarCompleted = 0;

    @Column(name = "reading_completed", nullable = false)
    @Builder.Default
    private Integer readingCompleted = 0;

    @Column(name = "listening_completed", nullable = false)
    @Builder.Default
    private Integer listeningCompleted = 0;

    // ==================== TIMESTAMPS ====================

    @Column(name = "last_updated")
    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }

    // ==================== BUSINESS METHODS ====================

    /**
     * Update streak on activity
     * 
     * @return true if streak was updated, false if already has streak today
     */
    public boolean updateStreakOnActivity() {
        LocalDate today = LocalDate.now();
        
        // Already studied today
        if (lastStreakDate != null && lastStreakDate.equals(today)) {
            return false;
        }
        
        // First time or streak was reset
        if (lastStreakDate == null) {
            currentStreak = 1;
            lastStreakDate = today;
            updateLongestStreak();
            lastUpdated = LocalDateTime.now();
            return true;
        }
        
        // Consecutive day
        if (lastStreakDate.equals(today.minusDays(1))) {
            currentStreak++;
            lastStreakDate = today;
            updateLongestStreak();
            lastUpdated = LocalDateTime.now();
            return true;
        }
        
        // Streak broken
        currentStreak = 1;
        lastStreakDate = today;
        lastUpdated = LocalDateTime.now();
        return true;
    }

    /**
     * Update streak (kept for compatibility)
     */
    public void updateStreak() {
        updateStreakOnActivity();
    }

    /**
     * Reset streak (used by scheduler)
     */
    public void resetStreak() {
        this.currentStreak = 0;
        this.lastStreakDate = null;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Check if has streak today
     */
    public boolean hasStreakToday() {
        return lastStreakDate != null && lastStreakDate.equals(LocalDate.now());
    }

    /**
     * Update longest streak if current > longest
     */
    private void updateLongestStreak() {
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }
    }

    /**
     * Add points
     */
    public void addPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Points cannot be negative");
        }
        this.totalPoints += points;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Deduct points (if needed)
     */
    public void deductPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Points cannot be negative");
        }
        this.totalPoints = Math.max(0, this.totalPoints - points);
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Increment lesson completed
     */
    public void incrementLessons(ParentType type) {
        if (type == null) return;

        if (this.totalLessonsCompleted == null) this.totalLessonsCompleted = 0;
        this.totalLessonsCompleted++;
        
        switch (type) {
            case GRAMMAR -> {
                if (this.grammarCompleted == null) this.grammarCompleted = 0;
                this.grammarCompleted++;
            }
            case READING -> {
                if (this.readingCompleted == null) this.readingCompleted = 0;
                this.readingCompleted++;
            }
            case LISTENING -> {
                if (this.listeningCompleted == null) this.listeningCompleted = 0;
                this.listeningCompleted++;
            }
            
            default -> {
                // Các loại bài học khác (Speaking, Writing...) tạm thời chỉ tính vào totalLessonsCompleted
            }
        }
        
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Add study time
     */
    public void addStudyTime(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("Study time cannot be negative");
        }
        this.totalStudyTimeMinutes += minutes;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Get average session time
     */
    public Integer getAverageSessionMinutes() {
        if (totalLessonsCompleted == 0) {
            return 0;
        }
        return totalStudyTimeMinutes / totalLessonsCompleted;
    }
}