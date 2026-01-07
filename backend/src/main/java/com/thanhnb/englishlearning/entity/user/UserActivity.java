package com.thanhnb.englishlearning.entity.user;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user") 
@EqualsAndHashCode(onlyExplicitlyIncluded = true) 
public class UserActivity {
    
    @Id
    @EqualsAndHashCode.Include
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Version
    @Column(name = "version")
    private Long version;

    // ==================== LOGIN TRACKING ====================

    /**
     * Thời điểm login gần nhất
     * 
     * Dùng để:
     * 1. JWT Validation: Token issued before this date = INVALID
     * 2. Force logout all devices: Update field này
     * 3. Password change: Update field này để invalidate all tokens
     * 4. Block user: Update field này
     */
    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    @Column(name = "last_activity_date")
    private LocalDateTime lastActivityDate;
    
    @Column(name = "login_count", nullable = false)
    @Builder.Default
    private Integer loginCount = 0;

    // ==================== SESSION INFO ====================

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "last_user_agent", columnDefinition = "TEXT")
    private String lastUserAgent;

    // ==================== TIMESTAMPS ====================

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    // ==================== BUSINESS METHODS ====================

    /**
     * Được gọi khi:
     * - User login thành công
     * 
     * Side effects:
     * - Update lastLoginDate → Invalidate all old tokens
     * - Increment loginCount
     * - Update IP & User Agent
     */
    public void recordLogin(String ip, String userAgent) {
        this.lastLoginDate = LocalDateTime.now();
        this.lastActivityDate = LocalDateTime.now();
        this.loginCount++;
        this.lastLoginIp = ip;
        this.lastUserAgent = userAgent;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update activity timestamp
     * 
     * Có thể gọi mỗi khi user thực hiện bất kỳ action nào
     */
    public void updateActivity() {
        this.lastActivityDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Được gọi khi:
     * - Change password
     * - Logout all devices
     * - Block user
     * - Delete user
     * 
     * Cách hoạt động:
     * - Set lastLoginDate = NOW
     * - Tất cả JWT tokens issued BEFORE NOW sẽ invalid
     * - User phải login lại
     */
    public void invalidateAllTokens() {
        this.lastLoginDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Get seconds since last activity
     */
    public Long getSecondsSinceLastActivity() {
        if (lastActivityDate == null) {
            return null;
        }
        return java.time.Duration.between(lastActivityDate, LocalDateTime.now()).getSeconds();
    }

    /**
     * Get seconds since last login
     */
    public Long getSecondsSinceLastLogin() {
        if (lastLoginDate == null) {
            return null;
        }
        return java.time.Duration.between(lastLoginDate, LocalDateTime.now()).getSeconds();
    }

    /**
     * Check if user is recently active (within X seconds)
     */
    public boolean isRecentlyActive(int seconds) {
        if (lastActivityDate == null) {
            return false;
        }
        Long secondsSinceActivity = getSecondsSinceLastActivity();
        return secondsSinceActivity != null && secondsSinceActivity <= seconds;
    }
}