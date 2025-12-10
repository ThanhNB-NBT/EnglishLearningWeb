package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.dto.user.request.ChangePasswordRequest;
import com.thanhnb.englishlearning.dto.user.request.UpdateUserRequest;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.exception.InvalidCredentialsException;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.util.ValidationUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

    /**
     * ✅ ENHANCED: Delete user and invalidate all tokens
     */
    public User deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        // ✅ Invalidate all user tokens before deleting
        // Update lastLoginDate to make all existing tokens invalid
        try {
            user.setLastLoginDate(LocalDateTime.now());
            userRepository.save(user);
            log.info("Invalidated all tokens for deleted user: {}", user.getUsername());
        } catch (Exception e) {
            log.warn("Failed to invalidate tokens for user: {}", user.getUsername());
        }
        
        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());
        return user;
    }

    /**
     * ✅ ENHANCED: Change password and invalidate all existing tokens
     */
    public void changePassword(Long userId, ChangePasswordRequest changePasswordRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getNewPassword())) {
            throw new InvalidCredentialsException("Mật khẩu xác nhận không khớp");
        }

        // Verify old password
        if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Mật khẩu cũ không chính xác");
        }

        if (changePasswordRequest.getOldPassword().equals(changePasswordRequest.getNewPassword())) {
            throw new InvalidCredentialsException("Mật khẩu mới phải khác mật khẩu cũ");
        }

        ValidationUtil.validatePassword(changePasswordRequest.getNewPassword());

        // Update new password
        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        
        // ✅ CRITICAL: Invalidate all existing tokens by updating lastLoginDate
        // This forces user to login again with new password
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed successfully for user ID: {} (all tokens invalidated)", userId);
    }

    /**
     * ✅ ENHANCED: Block user and force logout from all devices
     */
    public void blockUser(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        // Set inactive
        user.setIsActive(false);
        
        // ✅ CRITICAL: Force logout by updating lastLoginDate
        // This will invalidate all existing JWT tokens
        // Next API call will fail with "Tài khoản đã bị khóa"
        user.setLastLoginDate(LocalDateTime.now());
        
        userRepository.save(user);
        
        log.info("User {} has been BLOCKED and all sessions INVALIDATED", user.getUsername());
    }

    /**
     * ✅ ENHANCED: Unblock user
     */
    public void unblockUser(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        
        user.setIsActive(true);
        userRepository.save(user);
        
        log.info("User {} has been UNBLOCKED", user.getUsername());
    }

    public void updateLastLogin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
    }

    public void addPoints(Long userId, int points) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        user.setTotalPoints(user.getTotalPoints() + points);
        userRepository.save(user);
    }

    public void updateStreakDays(Long userId, int streakDays) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        user.setStreakDays(streakDays);
        userRepository.save(user);
    }

    public List<User> getTopUsersByPoints(int minPoints) {
        return userRepository.findTopUsersByPoints(minPoints);
    }

    public List<User> getTopUsersByStreakDays(int minStreakDays) {
        return userRepository.findTopUsersByStreak(minStreakDays);
    }

    public List<User> getActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }
}