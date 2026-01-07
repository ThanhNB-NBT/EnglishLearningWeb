package com.thanhnb.englishlearning.entity.user;

import com.thanhnb.englishlearning.enums.UserRole;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"stats", "activity"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "english_level", length = 20)
    private EnglishLevel englishLevel;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ==================== RELATIONSHIPS ====================

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserStats stats;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserActivity activity;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserLearningBehavior learningBehavior;

    // ==================== HELPER METHODS ====================

    @PostPersist
    protected void initializeRelatedEntities() {
        if (this.stats == null) {
            this.stats = UserStats.builder()
                    .userId(this.id)
                    .user(this)
                    .totalPoints(0)
                    .currentStreak(0)
                    .longestStreak(0)
                    .totalLessonsCompleted(0)
                    .grammarCompleted(0)
                    .readingCompleted(0)
                    .listeningCompleted(0)
                    .totalStudyTimeMinutes(0)
                    .build();
        }

        if (this.activity == null) {
            this.activity = UserActivity.builder()
                    .userId(this.id)
                    .user(this)
                    .loginCount(0)
                    .build();
        }

        if (this.learningBehavior == null) {
            this.learningBehavior = UserLearningBehavior.builder()
                    .user(this) // userId sẽ được set tự động nhờ @MapsId
                    .skillStats(new HashMap<>())
                    .questionTypeStats(new HashMap<>())
                    .overallAccuracy(0.0)
                    .build();
        }
    }

    // Convenience methods remain unchanged
    public Integer getTotalPoints() {
        return stats != null ? stats.getTotalPoints() : 0;
    }

    public Integer getCurrentStreak() {
        return stats != null ? stats.getCurrentStreak() : 0;
    }

    public LocalDateTime getLastLoginDate() {
        return activity != null ? activity.getLastLoginDate() : null;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isTeacher() {
        return role == UserRole.TEACHER;
    }

    public boolean isStudent() {
        return role == UserRole.USER;
    }
}