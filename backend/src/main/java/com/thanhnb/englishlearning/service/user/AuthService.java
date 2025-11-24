package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.dto.user.request.*;
import com.thanhnb.englishlearning.dto.user.response.*;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.OtpType;
import com.thanhnb.englishlearning.enums.UserRole;
import com.thanhnb.englishlearning.exception.*;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.util.JwtUtil;
import com.thanhnb.englishlearning.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisOtpService redisOtpService;
    private final JwtBlacklistService jwtBlacklistService;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    // ==================== USER AUTHENTICATION ====================

    /**
     * Đăng ký USER mới (CHỈ CHO USER, KHÔNG CHO ADMIN)
     * User cần verify email trước khi đăng nhập
     */
    public void registerUser(RegisterRequest request, String ipAddress) {
        // Validation
        validateRegistrationRequest(request);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Tên tài khoản đã tồn tại");
        }

        // Check email - cho phép đăng ký lại nếu chưa verify
        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            if (existingUser.getIsVerified()) {
                // Email đã được verify thì không cho đăng ký lại
                throw new UserAlreadyExistsException("Email đã được sử dụng");
            } else {
                // Email chưa verify thì xóa tài khoản cũ,cho đăng ký lại
                log.info("Tìm email chưa xác thực: {}. Xóa tài khoản cũ để đăng ký lại.", request.getEmail());

                userRepository.delete(existingUser);
                log.info("Tài khoản cũ với email {} đã bị xóa. Người dùng có thể đăng ký lại với email đó.",
                        request.getEmail());
            }
        }

        // Tạo user mới với role USER (forced)
        User user = createNewUser(request, UserRole.USER);
        userRepository.save(user);

        log.info("Tài khoản mới được đăng kí: {} (ID: {})", user.getUsername(), user.getId());

        // Gửi OTP
        try {
            redisOtpService.generateAndSendOtp(request.getEmail(), OtpType.VERIFY_EMAIL, ipAddress);
            log.info("Verification OTP sent to: {} from IP: {}", request.getEmail(), ipAddress);
        } catch (Exception e) {
            // Nếu gửi email thất bại, xóa user vừa tạo để tránh tạo zombie account
            log.error("Failed to send OTP to: {}. Rolling back user creation", request.getEmail());
            userRepository.delete(user);
            throw e; // Re-throw để controller xử lý
        }

        log.info("User registered successfully: {} from IP: {}", request.getUsername(), ipAddress);
    }

    /**
     * Đăng nhập USER
     * Kiểm tra: tài khoản phải là USER role và đã verify email
     */
    public AuthResponse loginUser(LoginRequest request, String ipAddress, String userAgent) {
        // Kiểm tra account lockout
        if (loginAttemptService.isBlocked(request.getUsernameOrEmail())) {
            throw new UserBlockedException(
                    "Tài khoản tạm thời bị khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau 15 phút.");
        }

        ValidationUtil.validatePassword(request.getPassword());

        // Tìm và xác thực user
        User user = authenticateUser(request);

        // CRITICAL: Chặn ADMIN đăng nhập qua endpoint USER
        if (user.getRole() == UserRole.ADMIN) {
            log.warn("Admin attempted to login via user endpoint: {}", user.getUsername());
            throw new UnauthorizedAccessException("Vui lòng đăng nhập qua trang quản trị viên");
        }

        // Kiểm tra tài khoản đã được kích hoạt (verified email)
        if (!user.getIsActive() || !user.getIsVerified()) {
            throw new UserNotVerifiedException("Tài khoản chưa được kích hoạt. Vui lòng xác thực email");
        }

        // Reset login attempts khi đăng nhập thành công
        loginAttemptService.loginSucceeded(request.getUsernameOrEmail());

        updateStreakOnLogin(user);

        // Tạo JWT token
        String token = jwtUtil.generateToken(user.getUsername());

        // Cập nhật last login
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in successfully: {} from IP: {}", user.getUsername(), ipAddress);

        return buildAuthResponse(token, user);
    }

    /**
     * Verify email OTP cho USER
     */
    public void verifyUserEmail(String email, String otp) {
        // Validate inputs
        ValidationUtil.validateEmail(email);
        ValidationUtil.validateOtp(otp);

        boolean isValid = redisOtpService.verifyOtp(email, otp, OtpType.VERIFY_EMAIL);

        if (!isValid) {
            throw new OtpInvalidException("Xác thực OTP thất bại");
        }

        // Send welcome email (optional)
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            emailService.sendWelcomeEmail(email, userOpt.get().getUsername());
        }

        log.info("Email verified successfully for: {}", email);
    }

    /**
     * Resend OTP cho USER
     */
    public void resendUserVerificationOtp(String email, String ipAddress) {
        // Validate email
        ValidationUtil.validateEmail(email);

        // Kiểm tra email có tồn tại không
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("Email không tồn tại trong hệ thống");
        }

        User user = userOpt.get();

        // Chỉ gửi lại nếu chưa verify
        if (user.getIsVerified()) {
            throw new IllegalArgumentException("Tài khoản đã được xác thực");
        }

        redisOtpService.generateAndSendOtp(email, OtpType.VERIFY_EMAIL, ipAddress);
        log.info("Verification OTP resent to: {}", email);
    }

    // ==================== ADMIN AUTHENTICATION ====================

    /**
     * Đăng nhập ADMIN
     * KHÔNG CẦN verify email, chỉ cần username/password đúng
     */
    public AuthResponse loginAdmin(LoginRequest request, String ipAddress, String userAgent) {
        // Kiểm tra account lockout
        if (loginAttemptService.isBlocked(request.getUsernameOrEmail())) {
            throw new UserBlockedException(
                    "Tài khoản tạm thời bị khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau 15 phút.");
        }

        // Tìm và xác thực
        User admin = authenticateUser(request);

        // CRITICAL: Chỉ cho phép ADMIN role
        if (admin.getRole() != UserRole.ADMIN) {
            log.warn("Non-admin user attempted to login via admin endpoint: {}", admin.getUsername());
            loginAttemptService.loginFailed(request.getUsernameOrEmail());
            throw new UnauthorizedAccessException("Tài khoản không có quyền quản trị");
        }

        // Admin KHÔNG CẦN verify email, chỉ cần active
        if (!admin.getIsActive()) {
            throw new UserBlockedException("Tài khoản quản trị đã bị vô hiệu hóa");
        }

        // Reset login attempts
        loginAttemptService.loginSucceeded(request.getUsernameOrEmail());

        // Tạo JWT token
        String token = jwtUtil.generateToken(admin.getUsername());

        // Cập nhật last login
        admin.setLastLoginDate(LocalDateTime.now());
        userRepository.save(admin);

        log.info("Admin logged in successfully: {} from IP: {}", admin.getUsername(), ipAddress);

        return buildAuthResponse(token, admin);
    }

    /**
     * Tạo ADMIN account (chỉ dùng trong seeder hoặc admin panel)
     * KHÔNG CẦN verify email
     */
    public User createAdminAccount(String username, String email, String password, String fullName) {
        // Validation
        ValidationUtil.validateUsername(username);
        ValidationUtil.validateEmail(email);
        ValidationUtil.validatePassword(password);

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Tên tài khoản đã tồn tại");
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
        admin.setEnglishLevel(EnglishLevel.ADVANCED); // Default for admin
        admin.setTotalPoints(0);
        admin.setStreakDays(0);
        admin.setIsActive(true); // Admin active ngay
        admin.setIsVerified(true); // Admin không cần verify email

        User savedAdmin = userRepository.save(admin);
        log.info("Admin account created: {}", username);

        return savedAdmin;
    }

    // ==================== COMMON AUTHENTICATION ====================

    /**
     * Đăng xuất (chung cho cả USER và ADMIN)
     */
    public void logout(String token) {
        // Validate token format
        if (token == null || token.isEmpty()) {
            throw new InvalidTokenException("Token không hợp lệ");
        }

        // Thêm token vào blacklist Redis
        jwtBlacklistService.blacklistToken(token);
        log.info("User/Admin logged out successfully");
    }

    /**
     * Đăng xuất tất cả thiết bị
     */
    public void logoutAllSessions(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("Không tìm thấy người dùng");
        }

        User user = userOpt.get();

        // Update timestamp để invalidate tất cả token cũ
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        log.info("All sessions logged out for user ID: {}", userId);
    }

    // ==================== PASSWORD RESET (CHUNG) ====================

    /**
     * Gửi OTP reset password (cho cả USER và ADMIN)
     */
    public void sendPasswordResetOtp(String email, String ipAddress) {
        // Validate email
        ValidationUtil.validateEmail(email);

        // Kiểm tra email có tồn tại không
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("Email không tồn tại trong hệ thống");
        }

        redisOtpService.generateAndSendOtp(email, OtpType.RESET_PASSWORD, ipAddress);
        log.info("Password reset OTP sent to: {}", email);
    }

    /**
     * Verify OTP reset password
     */
    public void verifyPasswordResetOtp(String email, String otp) {
        // Validate inputs
        ValidationUtil.validateEmail(email);
        ValidationUtil.validateOtp(otp);

        boolean isValid = redisOtpService.verifyOtp(email, otp, OtpType.RESET_PASSWORD);

        if (!isValid) {
            throw new OtpInvalidException("Xác thực OTP thất bại");
        }

        log.info("Password reset OTP verified for: {}", email);
    }

    /**
     * Reset password sau khi verify OTP
     */
    public void resetPassword(String email, String newPassword) {
        // Validate inputs
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

    /**
     * Validate registration request
     */
    private void validateRegistrationRequest(RegisterRequest request) {
        // Validate username
        ValidationUtil.validateUsername(request.getUsername());

        // Validate email
        ValidationUtil.validateEmail(request.getEmail());

        // Validate password
        ValidationUtil.validatePassword(request.getPassword());

        // Check duplicates
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Tên tài khoản đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email đã tồn tại");
        }
    }

    /**
     * Tạo user mới từ registration request
     */
    private User createNewUser(RegisterRequest request, UserRole role) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(role); // Force role
        user.setEnglishLevel(EnglishLevel.BEGINNER);
        user.setTotalPoints(0);
        user.setStreakDays(0);
        user.setIsActive(false); // Sẽ được activate sau khi verify OTP
        user.setIsVerified(false);

        return user;
    }

    /**
     * Xác thực thông tin đăng nhập (username/password)
     */
    private User authenticateUser(LoginRequest request) {
        // Tìm user theo username hoặc email
        Optional<User> userOpt = userRepository.findByUsername(request.getUsernameOrEmail());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(request.getUsernameOrEmail());
        }

        if (userOpt.isEmpty()) {
            loginAttemptService.loginFailed(request.getUsernameOrEmail());
            throw new UserNotFoundException("Tài khoản không tồn tại");
        }

        User user = userOpt.get();

        // Kiểm tra password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(request.getUsernameOrEmail());
            throw new InvalidCredentialsException("Mật khẩu không chính xác");
        }

        return user;
    }

    private void updateStreakOnLogin(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastLogin = user.getLastLoginDate() != null
                ? user.getLastLoginDate().toLocalDate()
                : null;

        if (lastLogin == null) {
            // First time login
            user.setStreakDays(1);
            user.setLastStreakDate(today);
        } else if (lastLogin.equals(today)) {
            // Already logged in today - do nothing
            log.info("User {} already logged in today", user.getId());
        } else if (lastLogin.equals(today.minusDays(1))) {
            // Logged in yesterday - increase streak
            user.setStreakDays(user.getStreakDays() + 1);
            user.setLastStreakDate(today);
            log.info("User {} streak increased to {}", user.getId(), user.getStreakDays());
        } else {
            // Streak broken - reset to 1
            user.setStreakDays(1);
            user.setLastStreakDate(today);
            log.info("User {} streak reset to 1", user.getId());
        }

        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Build AuthResponse từ user entity
     */
    private AuthResponse buildAuthResponse(String token, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            // Admin response - không cần points/streak
            return new AuthResponse(
                    user.getId(),
                    token,
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole().name());
        } else {
            // User response - có points/streak
            return new AuthResponse(
                    user.getId(),
                    token,
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole().name(),
                    user.getTotalPoints(),
                    user.getStreakDays());
        }
    }
}