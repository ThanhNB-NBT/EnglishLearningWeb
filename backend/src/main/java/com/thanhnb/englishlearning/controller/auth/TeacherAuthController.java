// src/controller/user/TeacherAuthController.java - FIXED CREATE ENDPOINT

package com.thanhnb.englishlearning.controller.auth;

import com.thanhnb.englishlearning.dto.user.request.*;
import com.thanhnb.englishlearning.dto.user.response.*;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.service.user.AuthService;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth/teacher")
@RequiredArgsConstructor
@Tag(name = "Teacher Authentication", description = "APIs for teacher authentication and account management")
public class TeacherAuthController {

    private final AuthService authService;

    /**
     * Đăng nhập teacher
     * POST /api/auth/teacher/login
     */
    @PostMapping("/login")
    @Operation(summary = "Teacher login", description = "Authenticate teacher and return JWT token")
    public ResponseEntity<CustomApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse authResponse = authService.loginTeacher(request, ipAddress, userAgent);

        return ResponseEntity.ok(CustomApiResponse.success(
                authResponse,
                "Đăng nhập thành công!"));
    }

    /**
     * ✅ FIX: Create teacher account - Return proper response
     * POST /api/auth/teacher/create
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new teacher account", description = "Create a new teacher account (Admin only)")
    public ResponseEntity<CustomApiResponse<Object>> createTeacher(@Valid @RequestBody RegisterRequest request) {
        try {
            // ✅ FIX: Call service and get created user
            User createdTeacher = authService.createTeacherAccount(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getFullName());

            // ✅ FIX: Return success response with teacher data (no token needed)
            return ResponseEntity.ok(CustomApiResponse.success(
                    Map.of(
                        "id", createdTeacher.getId(),
                        "username", createdTeacher.getUsername(),
                        "email", createdTeacher.getEmail(),
                        "fullName", createdTeacher.getFullName(),
                        "role", createdTeacher.getRole().name(),
                        "isActive", createdTeacher.getIsActive(),
                        "isVerified", createdTeacher.getIsVerified()
                    ),
                    "Tạo tài khoản Teacher thành công!"));
        } catch (Exception e) {
            // ✅ FIX: Return error response
            return ResponseEntity.badRequest().body(
                    CustomApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * Send password reset OTP
     * POST /api/auth/teacher/forgot-password
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset OTP", description = "Send OTP code for password reset")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody OtpRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIP(httpRequest);
        authService.sendPasswordResetOtp(request.getEmail(), ipAddress);

        return ResponseEntity.ok(CustomApiResponse.success(
                "Mã OTP đặt lại mật khẩu đã được gửi đến email!",
                "Gửi OTP thành công"));
    }

    /**
     * Verify password reset OTP
     * POST /api/auth/teacher/verify-reset-password
     */
    @PostMapping("/verify-reset-password")
    @Operation(summary = "Verify password reset OTP", description = "Verify OTP code for password reset process")
    public ResponseEntity<?> verifyResetPassword(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyPasswordResetOtp(request.getEmail(), request.getOtp());

        return ResponseEntity.ok(CustomApiResponse.success(
                "Xác thực OTP thành công!",
                "Xác thực OTP thành công"));
    }

    /**
     * Reset password
     * POST /api/auth/teacher/reset-password
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset teacher password using verified OTP")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getNewPassword());

        return ResponseEntity.ok(CustomApiResponse.success(
                "Đặt lại mật khẩu thành công!",
                "Đặt lại mật khẩu thành công"));
    }

    /**
     * Logout
     * POST /api/auth/teacher/logout
     */
    @PostMapping("/logout")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Teacher logout", description = "Logout teacher by invalidating the JWT token")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);

        return ResponseEntity.ok(CustomApiResponse.success(
                "Đăng xuất thành công!",
                "Đăng xuất thành công"));
    }

    /**
     * Logout all devices
     * POST /api/auth/teacher/logout-all
     */
    @PostMapping("/logout-all")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Teacher logout from all devices", description = "Logout teacher from all devices")
    public ResponseEntity<?> logoutAll(@RequestAttribute("userId") Long userId) {
        authService.logoutAllSessions(userId);

        return ResponseEntity.ok(CustomApiResponse.success(
                "Đã đăng xuất tất cả thiết bị!",
                "Đã đăng xuất tất cả thiết bị"));
    }

    // Helper method
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}