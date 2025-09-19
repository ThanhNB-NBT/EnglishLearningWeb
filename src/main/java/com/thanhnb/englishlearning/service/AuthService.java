package com.thanhnb.englishlearning.service;

import com.thanhnb.englishlearning.dto.AuthResponse;
import com.thanhnb.englishlearning.dto.LoginRequest;
import com.thanhnb.englishlearning.dto.RegisterRequest;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.entity.UserSession;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.OtpType;
import com.thanhnb.englishlearning.enums.UserRole;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.repository.UserSessionRepository;
import com.thanhnb.englishlearning.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    public void register(RegisterRequest request, String ipAddress) {
        // Kiểm tra username và email đã tồn tại
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên tài khoản đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // Xác định role
        UserRole role = UserRole.USER; // Mặc định
        if (request.getRole() != null) {
            try {
                role = UserRole.valueOf(request.getRole().toString().toUpperCase()); // Chuyển String thành enum
            } catch (IllegalArgumentException e) {
                log.warn("Role không hợp lệ, sử dụng mặc định USER: {}", request.getRole());
            }
        }

        // Tạo user mới
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setEnglishLevel(EnglishLevel.BEGINNER);
        user.setTotalPoints(0);
        user.setStreakDays(0);
        user.setIsActive(false);
        user.setIsVerified(false);

        userRepository.save(user);

        // Gửi OTP xác thực email
        otpService.generateAndSendOtp(request.getEmail(), OtpType.VERIFY_EMAIL, ipAddress);
    }

    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
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

        // Cập nhật last login
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        // Tạo JWT token
        String token = jwtUtil.generateToken(user.getUsername());

        return new AuthResponse(
            token,
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getRole().name()
        );
    }

    public void logout(String token) {
        String tokenHash = jwtUtil.hashToken(token);
        userSessionRepository.deactivateSession(tokenHash);
        log.info("User logged out");
    }

    public void logoutAllSessions(Long userId) {
        userSessionRepository.deactivateAllUserSessions(userId);
        log.info("All sessions logged out for user ID: {}", userId);
    }

    public void sendOtp(String email, OtpType otpType, String ipAddress) {
        otpService.generateAndSendOtp(email, otpType, ipAddress);
    }

    public boolean verifyOtp(String email, String otp, OtpType otpType) {
        return otpService.verifyOtp(email, otp, otpType);
    }

    private void createUserSession(Long userId, String token, String ipAddress, String userAgent) {
        String tokenHash = jwtUtil.hashToken(token);
        LocalDateTime expiresAt = jwtUtil.getExpirationDateFromToken(token)
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();

        UserSession session = new UserSession(userId, tokenHash, ipAddress, expiresAt);
        session.setUserAgent(userAgent);
        userSessionRepository.save(session);
        log.info("Created user session for user ID: {}, from IP: {}", userId, ipAddress);
    }

    public boolean isSessionActive(String token) {
        String tokenHash = jwtUtil.hashToken(token);
        Optional<UserSession> sessionOpt = userSessionRepository.findByTokenHashAndIsActiveTrue(tokenHash);
        if (sessionOpt.isPresent()) {
            // Cập nhật last activity
            userSessionRepository.updateLastActivity(tokenHash, LocalDateTime.now());
            return true;
        }
        return false;
    }
}
