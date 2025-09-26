package com.thanhnb.englishlearning.controller;

import com.thanhnb.englishlearning.dto.user.request.*;
import com.thanhnb.englishlearning.dto.user.response.*;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.enums.OtpType;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.service.user.AuthService;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication, registration, and account management")
public class AuthController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthController.class);

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
    @Operation(summary = "Get authentication endpoints", description = "Retrieve list of all available authentication API endpoints")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Endpoints retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<CustomApiResponse<Object>> getAuthEndpoints() {
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

        return ResponseEntity.ok(CustomApiResponse.success(Map.of(
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
    @Operation(summary = "Register new user", description = "Create a new user account and send verification OTP to email")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - invalid input or user already exists")
    })
    public ResponseEntity<CustomApiResponse<String>> register(
        @Valid @RequestBody RegisterRequest request, 
        @Parameter(description = "HTTP request object for IP detection") HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        try {
            authService.register(request, ipAddress);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomApiResponse.created(
                    "Tài khoản đã được tạo thành công! Mã OTP đã được gửi đến email " + request.getEmail(),
                    "Đăng ký thành công"
                ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomApiResponse.badRequest("Đăng ký thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid credentials")
    })
    public ResponseEntity<CustomApiResponse<AuthResponse>> login(
        @Valid @RequestBody LoginRequest request, 
        @Parameter(description = "HTTP request object for IP and User-Agent detection") HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        try {
            AuthResponse authResponse = authService.login(request, ipAddress, userAgent);
            return ResponseEntity.ok(CustomApiResponse.success(authResponse, "Đăng nhập thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CustomApiResponse.error(401, "Đăng nhập thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with OTP", description = "Verify user's email address using OTP code")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid OTP or OTP expired")
    })
    public ResponseEntity<CustomApiResponse<String>> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            boolean isValid = authService.verifyOtp(request.getEmail(), request.getOtp(), OtpType.VERIFY_EMAIL);
            
            if (isValid) {
                return ResponseEntity.ok(CustomApiResponse.success(
                    "Xác thực email thành công! Tài khoản đã được kích hoạt và bạn có thể đăng nhập.",
                    "Xác thực thành công"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest("Mã OTP không hợp lệ hoặc đã hết hạn"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomApiResponse.badRequest("Xác thực thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/resend-verify-email")
    @Operation(summary = "Resend verification email", description = "Resend OTP code for email verification")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
        @ApiResponse(responseCode = "400", description = "Failed to send OTP")
    })
    public ResponseEntity<CustomApiResponse<String>> resendVerifyEmail(
        @Valid @RequestBody OtpRequest request, 
        @Parameter(description = "HTTP request object for IP detection") HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        try {
            authService.sendOtp(request.getEmail(), OtpType.VERIFY_EMAIL, ipAddress);
            return ResponseEntity.ok(CustomApiResponse.success(
                "Mã OTP mới đã được gửi đến email: " + request.getEmail(),
                "Gửi lại OTP thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomApiResponse.badRequest("Gửi lại OTP thất bại: " + e.getMessage()));
        } finally {
            logger.info("Received email: {}, type: {}", request.getEmail(), request.getEmail().getClass().getName());
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send OTP code for password reset")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
        @ApiResponse(responseCode = "400", description = "Failed to send OTP")
    })
    public ResponseEntity<CustomApiResponse<String>> forgotPassword(
        @Valid @RequestBody OtpRequest request, 
        @Parameter(description = "HTTP request object for IP detection") HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        try {
            authService.sendOtp(request.getEmail(), OtpType.RESET_PASSWORD, ipAddress);
            return ResponseEntity.ok(CustomApiResponse.success(
                "Mã OTP reset password đã được gửi đến email: " + request.getEmail(),
                "Gửi OTP reset password thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomApiResponse.badRequest("Gửi OTP thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-reset-password")
    @Operation(summary = "Verify reset password OTP", description = "Verify OTP code for password reset process")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid OTP or OTP expired")
    })
    public ResponseEntity<CustomApiResponse<String>> verifyResetPassword(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            boolean isValid = authService.verifyOtp(request.getEmail(), request.getOtp(), OtpType.RESET_PASSWORD);
            
            if (isValid) {
                return ResponseEntity.ok(CustomApiResponse.success(
                    "Xác thực OTP thành công! Bạn có thể đặt lại mật khẩu mới.",
                    "Xác thực OTP reset password thành công"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest("Mã OTP không hợp lệ hoặc đã hết hạn"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomApiResponse.badRequest("Xác thực thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidate JWT token and logout user from current device")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "400", description = "Invalid token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CustomApiResponse<String>> logout(
            @Parameter(description = "Bearer token in Authorization header", required = true) 
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authService.logout(token);
                
                return ResponseEntity.ok(CustomApiResponse.success(
                    "Đăng xuất thành công",
                    "Logout successful"
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Token không hợp lệ"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomApiResponse.error(500, "Logout thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices", description = "Invalidate all JWT tokens for the user across all devices")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout from all devices successful"),
        @ApiResponse(responseCode = "400", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CustomApiResponse<String>> logoutAll(
            @Parameter(description = "Authentication object containing user details") Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isPresent()) {
                authService.logoutAllSessions(userOpt.get().getId());
                return ResponseEntity.ok(CustomApiResponse.success(
                    "Đã đăng xuất tất cả thiết bị",
                    "Logout all sessions successful"
                ));
            }
            
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("User không tồn tại"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomApiResponse.error(500, "Logout all thất bại: " + e.getMessage()));
        }
    }
}