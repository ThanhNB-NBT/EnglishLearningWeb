package com.thanhnb.englishlearning.entity;

import com.thanhnb.englishlearning.enums.UserRole;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "english_level", length = 20)
    private EnglishLevel englishLevel = EnglishLevel.BEGINNER;

    @Column(name = "total_points")
    private Integer totalPoints = 0;

    @Column(name = "streak_days")
    private Integer streakDays = 0;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    @Column(name = "last_streak_date")
    private LocalDate lastStreakDate;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "is_verified")
    private Boolean isVerified = false; // Xác thực email

}
