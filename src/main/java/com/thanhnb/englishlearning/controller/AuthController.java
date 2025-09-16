package com.thanhnb.englishlearning.controller;

import com.thanhnb.englishlearning.dto.*;
import com.thanhnb.englishlearning.enums.OtpType;
import com.thanhnb.englishlearning.service.AuthService;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    // Lấy địa chỉ IP từ request
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    @GetMapping("/endpoints")
    public ResponseEntity<ApiResponse<Object>> getAuthEndpoints() {
        var endpoints = Arrays.asList(
            Map.of("method", "POST", "url", "/api/auth/register", "description", "📝 Đăng ký tài khoản mới"),
            Map.of("method", "POST", "url", "/api/auth/login", "description", "🔐 Đăng nhập"),
            Map.of("method", "POST", "url", "/api/auth/verify-email", "description", "✅ Xác thực email"),
            Map.of("method", "POST", "url", "/api/auth/resend-verify-email", "description", "🔄 Gửi lại OTP xác thực email"),
            Map.of("method", "POST", "url", "/api/auth/forgot-password", "description", "🔒 Quên mật khẩu"),
            Map.of("method", "POST", "url", "/api/auth/verify-reset-password", "description", "🔑 Xác thực OTP reset password"),
            Map.of("method", "POST", "url", "/api/auth/logout", "description", "🚪 Đăng xuất"),
            Map.of("method", "POST", "url", "/api/auth/logout-all", "description", "🚪 Đăng xuất tất cả thiết bị"),
            Map.of("method", "GET", "url", "/api/auth/endpoints", "description", "📋 Xem danh sách endpoints")
        );

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "controller", "AuthController v2.0 - Separate OTP Table",
            "baseUrl", "http://localhost:8980/api/auth",
            "totalEndpoints", endpoints.size(),
            "endpoints", endpoints,
            "features", Arrays.asList(
                "✅ Đăng ký với OTP verification",
                "✅ Đăng nhập với JWT token",
                "✅ Forgot password với OTP",
                "✅ Giới hạn 5 OTP/giờ",
                "✅ Tối đa 3 lần thử sai",
                "✅ Auto cleanup expired OTP"
            )
        ), "Danh sách Authentication API endpoints"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
        @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        try {
            authService.register(request, ipAddress);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                    "Tài khoản đã được tạo thành công! Mã OTP đã được gửi đến email " + request.getEmail(),
                    "Đăng ký thành công"
                ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.badRequest("Đăng ký thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
        @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        try {
            AuthResponse authResponse = authService.login(request, ipAddress, userAgent);
            return ResponseEntity.ok(ApiResponse.success(authResponse, "Đăng nhập thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "Đăng nhập thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            boolean isValid = authService.verifyOtp(request.getEmail(), request.getOtp(), OtpType.VERIFY_EMAIL);
            
            if (isValid) {
                return ResponseEntity.ok(ApiResponse.success(
                    "Xác thực email thành công! Tài khoản đã được kích hoạt và bạn có thể đăng nhập.",
                    "Xác thực thành công"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest("Mã OTP không hợp lệ hoặc đã hết hạn"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.badRequest("Xác thực thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/resend-verify-email")
    public ResponseEntity<ApiResponse<String>> resendVerifyEmail(
        @Valid @RequestBody OtpRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        try {
            authService.sendOtp(request.getEmail(), OtpType.VERIFY_EMAIL, ipAddress);
            return ResponseEntity.ok(ApiResponse.success(
                "Mã OTP mới đã được gửi đến email: " + request.getEmail(),
                "Gửi lại OTP thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.badRequest("Gửi lại OTP thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
        @Valid @RequestBody OtpRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        try {
            authService.sendOtp(request.getEmail(), OtpType.RESET_PASSWORD, ipAddress);
            return ResponseEntity.ok(ApiResponse.success(
                "Mã OTP reset password đã được gửi đến email: " + request.getEmail(),
                "Gửi OTP reset password thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.badRequest("Gửi OTP thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-reset-password")
    public ResponseEntity<ApiResponse<String>> verifyResetPassword(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            boolean isValid = authService.verifyOtp(request.getEmail(), request.getOtp(), OtpType.RESET_PASSWORD);
            
            if (isValid) {
                return ResponseEntity.ok(ApiResponse.success(
                    "Xác thực OTP thành công! Bạn có thể đặt lại mật khẩu mới.",
                    "Xác thực OTP reset password thành công"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest("Mã OTP không hợp lệ hoặc đã hết hạn"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.badRequest("Xác thực thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authService.logout(token);
                
                return ResponseEntity.ok(ApiResponse.success(
                    "Đăng xuất thành công",
                    "Logout successful"
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Token không hợp lệ"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Logout thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<String>> logoutAll(Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isPresent()) {
                authService.logoutAllSessions(userOpt.get().getId());
                return ResponseEntity.ok(ApiResponse.success(
                    "Đã đăng xuất tất cả thiết bị",
                    "Logout all sessions successful"
                ));
            }
            
            return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest("User không tồn tại"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Logout all thất bại: " + e.getMessage()));
        }
    }
}

