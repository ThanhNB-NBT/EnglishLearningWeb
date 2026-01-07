package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.dto.user.request.*;
import com.thanhnb.englishlearning.dto.user.response.*;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.entity.user.UserActivity;
import com.thanhnb.englishlearning.entity.user.UserStats;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.OtpType;
import com.thanhnb.englishlearning.enums.UserRole;
import com.thanhnb.englishlearning.exception.*;
import com.thanhnb.englishlearning.repository.user.UserActivityRepository;
import com.thanhnb.englishlearning.repository.user.UserRepository;
import com.thanhnb.englishlearning.repository.user.UserStatsRepository;
import com.thanhnb.englishlearning.util.JwtUtil;
import com.thanhnb.englishlearning.util.ValidationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserStatsRepository statsRepository;
    private final UserActivityRepository activityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisOtpService redisOtpService;
    private final JwtBlacklistService jwtBlacklistService;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    // ==================== USER AUTHENTICATION ====================

    public void registerUser(RegisterRequest request, String ipAddress) {
        validateRegistrationRequest(request);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Tên tài khoản đã tồn tại");
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            if (existingUser.getIsVerified()) {
                throw new UserAlreadyExistsException("Email đã được sử dụng");
            } else {
                log.info("Found unverified email: {}. Deleting old account for re-registration.", request.getEmail());
                userRepository.delete(existingUser);
                log.info("Old account with email {} deleted. User can re-register.", request.getEmail());
            }
        }

        User user = createNewUser(request, UserRole.USER);
        user = userRepository.save(user);

        ensureStatsAndActivityExist(user.getId());

        log.info("New account registered: {} (ID: {})", user.getUsername(), user.getId());

        try {
            redisOtpService.generateAndSendOtp(request.getEmail(), OtpType.VERIFY_EMAIL, ipAddress);
            log.info("Verification OTP sent to: {} from IP: {}", request.getEmail(), ipAddress);
        } catch (Exception e) {
            log.error("Failed to send OTP to: {}. Rolling back user creation", request.getEmail());
            userRepository.delete(user);
            throw e;
        }

        log.info("User registered successfully: {} from IP: {}", request.getUsername(), ipAddress);
    }

    /**
     * ✅ ENHANCED: Login User with Retry on Optimistic Lock
     * 
     * Retry up to 3 times with 200ms delay if optimistic lock occurs
     */
    @Retryable(retryFor = {
            ObjectOptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 1.5))
    public AuthResponse loginUser(LoginRequest request, String ipAddress, String userAgent) {
        if (loginAttemptService.isBlocked(request.getUsernameOrEmail())) {
            throw new UserBlockedException(
                    "Tài khoản tạm thời bị khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau 15 phút.");
        }

        ValidationUtil.validatePassword(request.getPassword());

        User user = authenticateUser(request);

        if (user.getRole() == UserRole.ADMIN) {
            log.warn("Admin attempted to login via user endpoint: {}", user.getUsername());
            throw new UnauthorizedAccessException("Vui lòng đăng nhập qua trang quản trị viên");
        }

        if (!user.getIsVerified()) {
            throw new UserNotVerifiedException("Tài khoản chưa được kích hoạt. Vui lòng xác thực email");
        }

        if (!user.getIsActive()) {
            throw new UserNotVerifiedException("Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên");
        }

        loginAttemptService.loginSucceeded(request.getUsernameOrEmail());

        updateStreakOnLogin(user.getId());

        try {
            recordUserLogin(user.getId(), ipAddress, userAgent);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock on login record, will retry: {}", e.getMessage());
            throw e;
        }

        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ✅ FIX: Generate token with USER role
        String token = jwtUtil.generateToken(user.getUsername(), "USER");

        log.info("User logged in successfully: {} from IP: {}", user.getUsername(), ipAddress);

        return buildAuthResponse(token, user);
    }

    public void verifyUserEmail(String email, String otp) {
        ValidationUtil.validateEmail(email);
        ValidationUtil.validateOtp(otp);

        boolean isValid = redisOtpService.verifyOtp(email, otp, OtpType.VERIFY_EMAIL);

        if (!isValid) {
            throw new OtpInvalidException("Xác thực OTP thất bại");
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            emailService.sendWelcomeEmail(email, userOpt.get().getUsername());
        }

        log.info("Email verified successfully for: {}", email);
    }

    public void resendUserVerificationOtp(String email, String ipAddress) {
        ValidationUtil.validateEmail(email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("Email không tồn tại trong hệ thống");
        }

        User user = userOpt.get();

        if (user.getIsVerified()) {
            throw new IllegalArgumentException("Tài khoản đã được xác thực");
        }

        redisOtpService.generateAndSendOtp(email, OtpType.VERIFY_EMAIL, ipAddress);
        log.info("Verification OTP resent to: {}", email);
    }

    // ==================== ADMIN AUTHENTICATION ====================

    /**
     * ✅ ENHANCED: Login Admin with Retry on Optimistic Lock
     */
    @Retryable(retryFor = {
            ObjectOptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 1.5))
    public AuthResponse loginAdmin(LoginRequest request, String ipAddress, String userAgent) {
        if (loginAttemptService.isBlocked(request.getUsernameOrEmail())) {
            throw new UserBlockedException(
                    "Tài khoản tạm thời bị khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau 15 phút.");
        }

        User admin = authenticateUser(request);

        if (admin.getRole() != UserRole.ADMIN) {
            log.warn("Non-admin user attempted to login via admin endpoint: {}", admin.getUsername());
            loginAttemptService.loginFailed(request.getUsernameOrEmail());
            throw new UnauthorizedAccessException("Tài khoản không có quyền quản trị");
        }

        if (!admin.getIsActive()) {
            throw new UserNotVerifiedException("Tài khoản admin đã bị vô hiệu hóa");
        }

        loginAttemptService.loginSucceeded(request.getUsernameOrEmail());

        try {
            recordUserLogin(admin.getId(), ipAddress, userAgent);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock on admin login record, will retry: {}", e.getMessage());
            throw e;
        }

        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ✅ FIX: Generate token with ADMIN role
        String token = jwtUtil.generateToken(admin.getUsername(), "ADMIN");

        log.info("Admin logged in successfully: {} from IP: {}", admin.getUsername(), ipAddress);

        return buildAuthResponse(token, admin);
    }

    public User createAdminAccount(String username, String email, String password, String fullName) {
        ValidationUtil.validateUsername(username);
        ValidationUtil.validateEmail(email);
        ValidationUtil.validatePassword(password);

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username đã tồn tại");
        }

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email đã tồn tại");
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setFullName(fullName);
        admin.setRole(UserRole.ADMIN);
        admin.setEnglishLevel(EnglishLevel.C2);
        admin.setIsActive(true);
        admin.setIsVerified(true);

        User savedAdmin = userRepository.save(admin);

        ensureStatsAndActivityExist(savedAdmin.getId());

        log.info("Admin account created: {}", username);

        return savedAdmin;
    }

    // ==================== COMMON AUTHENTICATION ====================

    public void logout(String token) {
        if (token == null || token.isEmpty()) {
            throw new InvalidTokenException("Token không hợp lệ");
        }

        jwtBlacklistService.blacklistToken(token);
        log.info("User/Admin logged out successfully");
    }

    /**
     * ✅ ENHANCED: Logout all with retry on optimistic lock
     */
    @Retryable(retryFor = {
            ObjectOptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 1.5))
    public void logoutAllSessions(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("Không tìm thấy người dùng");
        }

        UserActivity activity = activityRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User activity not found"));

        activity.invalidateAllTokens();
        activityRepository.save(activity);

        log.info("All sessions logged out for user ID: {}", userId);
    }

    // ==================== PASSWORD RESET (CHUNG) ====================

    public void sendPasswordResetOtp(String email, String ipAddress) {
        ValidationUtil.validateEmail(email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("Email không tồn tại trong hệ thống");
        }

        redisOtpService.generateAndSendOtp(email, OtpType.RESET_PASSWORD, ipAddress);
        log.info("Password reset OTP sent to: {}", email);
    }

    public void verifyPasswordResetOtp(String email, String otp) {
        ValidationUtil.validateEmail(email);
        ValidationUtil.validateOtp(otp);

        boolean isValid = redisOtpService.verifyOtp(email, otp, OtpType.RESET_PASSWORD);

        if (!isValid) {
            throw new OtpInvalidException("Xác thực OTP thất bại");
        }

        log.info("Password reset OTP verified for: {}", email);
    }

    public void resetPassword(String email, String newPassword) {
        ValidationUtil.validateEmail(email);
        ValidationUtil.validatePassword(newPassword);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("Email không tồn tại");
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password reset successfully for: {}", email);
    }

    // ==================== PRIVATE HELPERS ====================

    private void validateRegistrationRequest(RegisterRequest request) {
        ValidationUtil.validateUsername(request.getUsername());
        ValidationUtil.validateEmail(request.getEmail());
        ValidationUtil.validatePassword(request.getPassword());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Tên tài khoản đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email đã tồn tại");
        }
    }

    private User createNewUser(RegisterRequest request, UserRole role) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setEnglishLevel(EnglishLevel.A1);
        user.setIsActive(false);
        user.setIsVerified(false);

        return user;
    }

    private User authenticateUser(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsernameOrEmail());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(request.getUsernameOrEmail());
        }

        if (userOpt.isEmpty()) {
            loginAttemptService.loginFailed(request.getUsernameOrEmail());
            throw new UserNotFoundException("Tài khoản không tồn tại");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(request.getUsernameOrEmail());
            throw new InvalidCredentialsException("Mật khẩu không chính xác");
        }

        return user;
    }

    /**
     * ✅ NEW: Separate method for recording login
     * Isolated for better retry handling
     */
    @Retryable(retryFor = {
            ObjectOptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 1.5))
    private void recordUserLogin(Long userId, String ipAddress, String userAgent) {
        UserActivity activity = activityRepository.findById(userId)
                .orElseGet(() -> createNewActivity(userId));

        activity.recordLogin(ipAddress, userAgent);
        activityRepository.save(activity);

        log.debug("Login recorded for user ID: {}", userId);
    }

    private void updateStreakOnLogin(Long userId) {
        UserStats stats = statsRepository.findById(userId)
                .orElseGet(() -> createNewStats(userId));

        boolean updated = stats.updateStreakOnActivity();

        if (updated) {
            statsRepository.save(stats);
            log.info("User {} streak updated: {} days", userId, stats.getCurrentStreak());
        } else {
            log.debug("User {} already has streak for today", userId);
        }
    }

    @Retryable(retryFor = {
            ObjectOptimisticLockingFailureException.class
    }, maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 1.5))
    public AuthResponse loginTeacher(LoginRequest request, String ipAddress, String userAgent) {
        if (loginAttemptService.isBlocked(request.getUsernameOrEmail())) {
            throw new UserBlockedException(
                    "Tài khoản tạm thời bị khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau 15 phút.");
        }

        ValidationUtil.validatePassword(request.getPassword());

        User teacher = authenticateUser(request);

        if (teacher.getRole() != UserRole.TEACHER) {
            log.warn("Non-teacher user attempted to login via teacher endpoint: {}", teacher.getUsername());
            loginAttemptService.loginFailed(request.getUsernameOrEmail());
            throw new UnauthorizedAccessException("Tài khoản không có quyền giảng viên");
        }

        if (!teacher.getIsVerified()) {
            throw new UserNotVerifiedException("Tài khoản chưa được xác thực. Vui lòng xác thực email");
        }

        if (!teacher.getIsActive()) {
            throw new UserNotVerifiedException(
                    "Tài khoản đang chờ phê duyệt từ quản trị viên. Vui lòng liên hệ admin.");
        }

        loginAttemptService.loginSucceeded(request.getUsernameOrEmail());

        updateStreakOnLogin(teacher.getId());

        UserActivity activity = activityRepository.findById(teacher.getId())
                .orElseGet(() -> createNewActivity(teacher.getId()));

        activity.recordLogin(ipAddress, userAgent);
        activityRepository.save(activity);

        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ✅ FIX: Generate token with TEACHER role
        String token = jwtUtil.generateToken(teacher.getUsername(), "TEACHER");

        log.info("Teacher logged in successfully: {} from IP: {}", teacher.getUsername(), ipAddress);

        return buildAuthResponse(token, teacher);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            // Admin response - no stats
            return new AuthResponse(
                    user.getId(),
                    token,
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole().name());
        } else {
            // User response - with stats
            UserStats stats = statsRepository.findById(user.getId())
                    .orElseGet(() -> createNewStats(user.getId()));

            UserStatsDto statsDto = UserStatsDto.from(stats);

            return new AuthResponse(
                    user.getId(),
                    token,
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole().name(),
                    statsDto);
        }
    }

    // ==================== ENTITY CREATION HELPERS ====================

    // ✅ FIX: createTeacherAccount method in AuthService.java
    // Thay thế method này trong AuthService

    @Transactional
    public User createTeacherAccount(String username, String email, String password, String fullName) {
        // Validation
        ValidationUtil.validateUsername(username);
        ValidationUtil.validateEmail(email);
        ValidationUtil.validatePassword(password);

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username đã tồn tại");
        }

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email đã tồn tại");
        }

        // Admin sẽ phải manually approve sau
        User teacher = new User();
        teacher.setUsername(username);
        teacher.setEmail(email);
        teacher.setPassword(passwordEncoder.encode(password));
        teacher.setFullName(fullName);
        teacher.setRole(UserRole.TEACHER);
        teacher.setEnglishLevel(EnglishLevel.B2); // Default level for teachers

        // ✅ KEY FIX: Set both verified and active to true
        // Teachers created by admin are pre-approved
        teacher.setIsActive(true);
        teacher.setIsVerified(true);

        User savedTeacher = userRepository.save(teacher);

        // ✅ Ensure stats & activity exist immediately
        ensureStatsAndActivityExist(savedTeacher.getId());

        log.info("Teacher account created by admin: {} (ID: {}, auto-approved)", username, savedTeacher.getId());

        return savedTeacher;
    }

    // Helper method to ensure entities exist
    @Transactional
    private void ensureStatsAndActivityExist(Long userId) {
        // Check and create UserStats if missing
        if (!statsRepository.existsByUserId(userId)) {
            createNewStats(userId);
            log.info("Created UserStats for user ID: {}", userId);
        }

        // Check and create UserActivity if missing
        if (!activityRepository.existsByUserId(userId)) {
            createNewActivity(userId);
            log.info("Created UserActivity for user ID: {}", userId);
        }
    }

    private UserStats createNewStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

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

        return statsRepository.save(stats);
    }

    private UserActivity createNewActivity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        UserActivity activity = UserActivity.builder()
                .userId(userId)
                .user(user)
                .loginCount(0)
                .build();

        return activityRepository.save(activity);
    }
}