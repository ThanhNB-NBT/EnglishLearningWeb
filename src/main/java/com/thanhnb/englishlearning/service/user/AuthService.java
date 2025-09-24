package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.dto.user.request.*;
import com.thanhnb.englishlearning.dto.user.response.*;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.OtpType;
import com.thanhnb.englishlearning.enums.UserRole;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Đăng ký user mới
     * Sử dụng RedisOtpService 
     */
    public void register(RegisterRequest request, String ipAddress) {
        // Validation
        validateRegistrationRequest(request);
        
        // Tạo user mới (chưa active)
        User user = createNewUser(request);
        userRepository.save(user);
        
        // Gửi OTP qua Redis 
        redisOtpService.generateAndSendOtp(request.getEmail(), OtpType.VERIFY_EMAIL, ipAddress);
        
        log.info("User registered successfully: {} from IP: {}", request.getUsername(), ipAddress);
    }

    /**
     * Đăng nhập user
     * Chỉ tạo JWT
     */
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // Tìm và xác thực user
        User user = authenticateUser(request);
        
        // Tạo JWT token 
        String token = jwtUtil.generateToken(user.getUsername());
        
        // Cập nhật last login time
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User logged in successfully: {} from IP: {}", user.getUsername(), ipAddress);
        
        return new AuthResponse(
            token,
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getRole().name()
        );
    }

    /**
     * Đăng xuất user
     * Sử dụng JWT blacklist 
     */
    public void logout(String token) {
        // Thêm token vào blacklist Redis
        jwtBlacklistService.blacklistToken(token);
        
        log.info("User logged out successfully");
    }

    /**
     * Đăng xuất tất cả thiết bị
     * Lưu ý: Với JWT thuần, việc này khó implement hơn database session
     * Có thể cần track active tokens của user hoặc thay đổi JWT secret
     */
    public void logoutAllSessions(Long userId) {
        // Option 1: Track active tokens của user (phức tạp)
        // Option 2: Thay đổi user's secret key làm invalid tất cả token cũ
        // Option 3: Lưu "logout_all_before" timestamp cho user
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Cập nhật "logout all before" timestamp
            // Các token tạo trước thời điểm này sẽ bị invalid
            user.setLastLoginDate(LocalDateTime.now()); // Có thể thêm field riêng
            userRepository.save(user);
            
            log.info("All sessions logged out for user ID: {}", userId);
        }
    }

    /**
     * Gửi OTP
     * Sử dụng RedisOtpService
     */
    public void sendOtp(String email, OtpType otpType, String ipAddress) {
        redisOtpService.generateAndSendOtp(email, otpType, ipAddress);
    }

    /**
     * Xác thực OTP
     * Sử dụng RedisOtpService
     */
    public boolean verifyOtp(String email, String otp, OtpType otpType) {
        return redisOtpService.verifyOtp(email, otp, otpType);
    }

    /**
     * Validate registration request
     */
    private void validateRegistrationRequest(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên tài khoản đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }
    }

    /**
     * Tạo user mới từ registration request
     */
    private User createNewUser(RegisterRequest request) {
        // Xác định role
        UserRole role = UserRole.USER; // Default
        if (request.getRole() != null) {
            try {
                role = UserRole.valueOf(request.getRole().toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role specified, using default USER: {}", request.getRole());
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setEnglishLevel(EnglishLevel.BEGINNER);
        user.setTotalPoints(0);
        user.setStreakDays(0);
        user.setIsActive(false); // Sẽ được activate sau khi verify OTP
        user.setIsVerified(false);

        return user;
    }

    /**
     * Xác thực thông tin đăng nhập
     */
    private User authenticateUser(LoginRequest request) {
        // Tìm user theo username hoặc email
        Optional<User> userOpt = userRepository.findByUsername(request.getUsernameOrEmail());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(request.getUsernameOrEmail());
        }

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Tài khoản không tồn tại");
        }

        User user = userOpt.get();

        // Kiểm tra password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác");
        }

        // Kiểm tra tài khoản đã được kích hoạt
        if (!user.getIsActive()) {
            throw new RuntimeException("Tài khoản chưa được kích hoạt. Vui lòng xác thực OTP");
        }

        return user;
    }
}