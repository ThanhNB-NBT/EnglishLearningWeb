package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.dto.user.request.ChangePasswordRequest;
import com.thanhnb.englishlearning.dto.user.request.UpdateUserRequest;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.entity.user.UserActivity;
import com.thanhnb.englishlearning.entity.user.UserStats;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.exception.InvalidCredentialsException;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.user.UserActivityRepository;
import com.thanhnb.englishlearning.repository.user.UserRepository;
import com.thanhnb.englishlearning.repository.user.UserStatsRepository;
import com.thanhnb.englishlearning.util.ValidationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserStatsRepository statsRepository;
    private final UserActivityRepository activityRepository;
    private final PasswordEncoder passwordEncoder;

    // ==================== BASIC CRUD ====================

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User updateUser(Long id, UpdateUserRequest updateUserRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        user.setFullName(updateUserRequest.getFullName());
        return userRepository.save(user);
    }

    // ==================== AUTHENTICATION & SECURITY ====================

    /**
     * ✅ ENHANCED: Delete user with retry on optimistic lock
     */
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 200, multiplier = 1.5)
    )
    public User deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        try {
            UserActivity activity = activityRepository.findById(id).orElse(null);
            if (activity != null) {
                activity.invalidateAllTokens();
                activityRepository.save(activity);
                log.info("Invalidated all tokens for deleted user: {}", user.getUsername());
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate tokens for user: {}", user.getUsername(), e);
        }
        
        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());
        return user;
    }

    /**
     * ✅ ENHANCED: Change password with retry on optimistic lock
     */
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 200, multiplier = 1.5)
    )
    public void changePassword(Long userId, ChangePasswordRequest changePasswordRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new InvalidCredentialsException("Mật khẩu xác nhận không khớp");
        }

        if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Mật khẩu cũ không chính xác");
        }

        if (changePasswordRequest.getOldPassword().equals(changePasswordRequest.getNewPassword())) {
            throw new InvalidCredentialsException("Mật khẩu mới phải khác mật khẩu cũ");
        }

        ValidationUtil.validatePassword(changePasswordRequest.getNewPassword());

        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
        
        UserActivity activity = activityRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User activity not found"));
        
        activity.invalidateAllTokens();
        activityRepository.save(activity);

        log.info("Password changed successfully for user ID: {} (all tokens invalidated)", userId);
    }

    /**
     * ✅ ENHANCED: Block user with retry on optimistic lock
     */
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 200, multiplier = 1.5)
    )
    public void blockUser(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        user.setIsActive(false);
        userRepository.save(user);
        
        UserActivity activity = activityRepository.findById(id).orElse(null);
        if (activity != null) {
            activity.invalidateAllTokens();
            activityRepository.save(activity);
        }
        
        log.info("User {} has been BLOCKED and all sessions INVALIDATED", user.getUsername());
    }

    public void unblockUser(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        user.setIsActive(true);
        userRepository.save(user);
        
        log.info("User {} has been UNBLOCKED", user.getUsername());
    }

    /**
     * ✅ ENHANCED: Update last login with retry on optimistic lock
     */
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 1.5)
    )
    public void updateLastLogin(Long id, String ip, String userAgent) {
        UserActivity activity = activityRepository.findById(id)
                .orElseGet(() -> createNewActivity(id));
        
        activity.recordLogin(ip != null ? ip : "unknown", userAgent != null ? userAgent : "unknown");
        activityRepository.save(activity);
        
        log.debug("Last login updated for user ID: {}", id);
    }

    @Deprecated
    public void updateLastLogin(Long id) {
        updateLastLogin(id, null, null);
    }

    // ==================== GAMIFICATION ====================

    /**
     * ✅ ENHANCED: Add points with retry on optimistic lock
     */
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 1.5)
    )
    public void addPoints(Long userId, int points) {
        UserStats stats = statsRepository.findById(userId)
                .orElseGet(() -> createNewStats(userId));
        
        stats.addPoints(points);
        statsRepository.save(stats);
        
        log.debug("Added {} points to user ID: {} (Total: {})", 
                points, userId, stats.getTotalPoints());
    }

    /**
     * ✅ ENHANCED: Update streak with retry on optimistic lock
     */
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 1.5)
    )
    public void updateStreakDays(Long userId, int streakDays) {
        UserStats stats = statsRepository.findById(userId)
                .orElseGet(() -> createNewStats(userId));
        
        stats.setCurrentStreak(streakDays);
        statsRepository.save(stats);
        
        log.debug("Updated streak to {} days for user ID: {}", streakDays, userId);
    }

    /**
     * ✅ ENHANCED: Increment lesson with retry on optimistic lock
     */
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 1.5)
    )
    public void incrementLessonCompleted(Long userId, ParentType parentType) {
        UserStats stats = statsRepository.findById(userId)
                .orElseGet(() -> createNewStats(userId));
        
        stats.incrementLessons(parentType);
        statsRepository.save(stats);
        
        log.debug("Incremented {} lesson for user ID: {}", parentType, userId);
    }

    /**
     * ✅ ENHANCED: Add study time with retry on optimistic lock
     */
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 1.5)
    )
    public void addStudyTime(Long userId, int minutes) {
        UserStats stats = statsRepository.findById(userId)
                .orElseGet(() -> createNewStats(userId));
        
        stats.addStudyTime(minutes);
        statsRepository.save(stats);
        
        log.debug("Added {} minutes study time for user ID: {}", minutes, userId);
    }

    // ==================== QUERIES ====================

    public List<UserStats> getTopUsersByPoints(int minPoints) {
        return statsRepository.findTopByPoints(minPoints, 
                org.springframework.data.domain.PageRequest.of(0, 10));
    }

    public List<UserStats> getTopUsersByStreakDays(int minStreakDays) {
        return statsRepository.findTopByStreak(minStreakDays,
                org.springframework.data.domain.PageRequest.of(0, 10));
    }

    public List<User> getActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    // ==================== HELPER METHODS ====================

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private UserStats createNewStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserStats stats = UserStats.builder()
                .userId(userId)
                .user(user)
                .totalPoints(0)
                .currentStreak(0)
                .longestStreak(0)
                .totalLessonsCompleted(0)
                .grammarCompleted(0)
                .readingCompleted(0)
                .listeningCompleted(0)
                .totalStudyTimeMinutes(0)
                .build();
        
        log.warn("Creating missing stats for user ID: {}", userId);
        return statsRepository.save(stats);
    }

    private UserActivity createNewActivity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserActivity activity = UserActivity.builder()
                .userId(userId)
                .user(user)
                .loginCount(0)
                .build();
        
        log.warn("Creating missing activity for user ID: {}", userId);
        return activityRepository.save(activity);
    }

    public UserStats getUserStats(Long userId) {
        return statsRepository.findById(userId)
                .orElseGet(() -> createNewStats(userId));
    }

    public UserActivity getUserActivity(Long userId) {
        return activityRepository.findById(userId)
                .orElseGet(() -> createNewActivity(userId));
    }
}