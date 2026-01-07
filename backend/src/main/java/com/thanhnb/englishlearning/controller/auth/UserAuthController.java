package com.thanhnb.englishlearning.controller.auth;

import com.thanhnb.englishlearning.dto.user.request.*;
import com.thanhnb.englishlearning.dto.user.response.*;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.service.user.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
@RequestMapping("/api/auth/user")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication, registration, and account management")
public class UserAuthController {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthController.class);

    private final AuthService authService;

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

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account and send verification OTP to email")
    public ResponseEntity<CustomApiResponse<String>> register(
            @Valid @RequestBody RegisterRequest request,
            @Parameter(description = "HTTP request object for IP detection") HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        try {
            authService.registerUser(request, ipAddress);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CustomApiResponse.created(
                            "Tài khoản đã được tạo thành công! Mã OTP đã được gửi đến email " + request.getEmail(),
                            "Đăng ký thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest("Đăng ký thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public ResponseEntity<CustomApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @Parameter(description = "HTTP request object for IP and User-Agent detection") HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        try {
            AuthResponse authResponse = authService.loginUser(request, ipAddress, userAgent);
            return ResponseEntity.ok(CustomApiResponse.success(authResponse, "Đăng nhập thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CustomApiResponse.error(401, "Đăng nhập thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with OTP", description = "Verify user's email address using OTP code")
    public ResponseEntity<CustomApiResponse<String>> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            authService.verifyUserEmail(request.getEmail(), request.getOtp());

            return ResponseEntity.ok(CustomApiResponse.success(
                    "Xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ.",
                    "Xác thực thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest("Xác thực thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/resend-verify-email")
    @Operation(summary = "Resend verification email", description = "Resend OTP code for email verification")
    public ResponseEntity<CustomApiResponse<String>> resendVerifyEmail(
            @Valid @RequestBody OtpRequest request,
            @Parameter(description = "HTTP request object for IP detection") HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        try {
            authService.resendUserVerificationOtp(request.getEmail(), ipAddress);
            return ResponseEntity.ok(CustomApiResponse.success(
                    "Mã OTP mới đã được gửi đến email: " + request.getEmail(),
                    "Gửi lại OTP thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest("Gửi lại OTP thất bại: " + e.getMessage()));
        } finally {
            logger.info("Received email: {}, type: {}", request.getEmail(), request.getEmail().getClass().getName());
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send OTP code for password reset")
    public ResponseEntity<CustomApiResponse<String>> forgotPassword(
            @Valid @RequestBody OtpRequest request,
            @Parameter(description = "HTTP request object for IP detection") HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        try {
            authService.sendPasswordResetOtp(request.getEmail(), ipAddress);
            return ResponseEntity.ok(CustomApiResponse.success(
                    "Mã OTP reset password đã được gửi đến email: " + request.getEmail(),
                    "Gửi OTP reset password thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest("Gửi OTP thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-reset-password")
    @Operation(summary = "Verify reset password OTP", description = "Verify OTP code for password reset process")
    public ResponseEntity<CustomApiResponse<String>> verifyResetPassword(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            authService.verifyPasswordResetOtp(request.getEmail(), request.getOtp());

            return ResponseEntity.ok(CustomApiResponse.success(
                    "Xác thực OTP thành công! Bạn có thể đặt lại mật khẩu mới.",
                    "Xác thực OTP reset password thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest("Xác thực thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<CustomApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getNewPassword());

        return ResponseEntity.ok(CustomApiResponse.success(
                "Mật khẩu đã được đặt lại thành công! Bạn có thể đăng nhập với mật khẩu mới.",
                "Reset password thành công"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidate JWT token and logout user from current device")
    public ResponseEntity<CustomApiResponse<String>> logout(
            @Parameter(description = "Bearer token in Authorization header", required = true) @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            authService.logout(token);

            logger.info("User logged out successfully, token blacklisted");

            return ResponseEntity.ok(CustomApiResponse.success(
                    "Đăng xuất thành công",
                    "Logout successful"));
        } catch (Exception e) {
            logger.error("Logout failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomApiResponse.error(500, "Logout thất bại: " + e.getMessage()));
        }
    }

    /**
     * Logout from all devices
     * Invalidate tất cả JWT tokens của user
     */
    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices", description = "Invalidate all JWT tokens for the user across all devices")
    public ResponseEntity<CustomApiResponse<String>> logoutAll( @RequestAttribute("userId") Long userId) {

        try {

            authService.logoutAllSessions(userId);

            return ResponseEntity.ok(CustomApiResponse.success(
                    "Đã đăng xuất tất cả thiết bị",
                    "Logout all sessions successful"));

        } catch (Exception e) {
            logger.error("Logout all failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomApiResponse.error(500, "Logout all thất bại: " + e.getMessage()));
        }
    }
}