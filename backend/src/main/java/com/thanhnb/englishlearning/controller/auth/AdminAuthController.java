package com.thanhnb.englishlearning.controller.auth;

import com.thanhnb.englishlearning.dto.user.request.*;
import com.thanhnb.englishlearning.dto.user.response.*;
import com.thanhnb.englishlearning.service.user.AuthService;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Authentication", description = "APIs for admin authentication and account management")
public class AdminAuthController {

    private final AuthService authService;

    /**
     * Đăng nhập ADMIN
     * POST /api/auth/admin/login
     * KHÔNG CẦN verify email
     */
    @PostMapping("/login")
    @Operation(summary = "Admin login", description = "Authenticate admin and return JWT token")
    public ResponseEntity<CustomApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse authResponse = authService.loginAdmin(request, ipAddress, userAgent);

        return ResponseEntity.ok(CustomApiResponse.success(
                authResponse,
                "Đăng nhập thành công!"));
    }

    /**
     * Tạo ADMIN account mới
     * POST /api/auth/admin/create
     * CHỈ superadmin hoặc system có thể gọi
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')") // Chỉ admin hiện tại mới tạo được admin mới
    @Operation(summary = "Create new admin account", description = "Create a new admin account, accessible only by existing admins")
    public ResponseEntity<?> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        authService.createAdminAccount(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFullName());

        return ResponseEntity.ok(CustomApiResponse.success(
                "Tạo tài khoản quản trị thành công!",
                "Tạo tài khoản thành công"));
    }

    /**
     * Send password reset OTP (dùng chung với user)
     * POST /api/auth/admin/forgot-password
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
     * POST /api/auth/admin/verify-reset-password
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
     * POST /api/auth/admin/reset-password
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset admin password using verified OTP")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getNewPassword());

        return ResponseEntity.ok(CustomApiResponse.success(
                "Đặt lại mật khẩu thành công!",
                "Đặt lại mật khẩu thành công"));
    }

    /**
     * Logout
     * POST /api/auth/admin/logout
     */
    @PostMapping("/logout")
    @Operation(summary = "Admin logout", description = "Logout admin by invalidating the JWT token")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);

        return ResponseEntity.ok(CustomApiResponse.success(
                "Đăng xuất thành công!",
                "Đăng xuất thành công"));
    }

    /**
     * Logout all devices
     * POST /api/auth/admin/logout-all
     */
    @PostMapping("/logout-all")
    @Operation(summary = "Admin logout from all devices", description = "Logout admin from all devices by invalidating all JWT tokens")
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
