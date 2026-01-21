package com.thanhnb.englishlearning.controller;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.user.request.ChangePasswordRequest;
import com.thanhnb.englishlearning.dto.user.request.UpdateUserRequest;
import com.thanhnb.englishlearning.dto.user.response.UserActivityDto;
import com.thanhnb.englishlearning.dto.user.response.UserDashboardDto;
import com.thanhnb.englishlearning.dto.user.response.UserDetailDto;
import com.thanhnb.englishlearning.dto.user.response.UserStatsDto;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.entity.user.UserStats;
import com.thanhnb.englishlearning.exception.InvalidCredentialsException;
import com.thanhnb.englishlearning.mapper.UserMapper;
import com.thanhnb.englishlearning.service.user.StreakService.StreakInfo;
import com.thanhnb.englishlearning.service.user.UserDashboardService;
import com.thanhnb.englishlearning.service.user.StreakService;
import com.thanhnb.englishlearning.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ✅ CLEANED UP UserController
 * 
 * User Endpoints:
 * - GET /me → User profile (User + Stats + Activity)
 * - GET /me/dashboard → Complete dashboard data
 * - GET /me/stats → User statistics only
 * - GET /me/activity → User activity only
 * - GET /me/streak → Streak information
 * - PUT /me → Update profile
 * - PUT /me/change-password → Change password
 * 
 * Admin Endpoints:
 * - GET / → All users
 * - GET /{id} → User by ID
 * - GET /email/{email} → User by email
 * - DELETE /{id} → Delete user
 * - PUT /{id}/points → Add points
 * - PUT /{id}/block → Block user
 * - PUT /{id}/unblock → Unblock user
 * - PUT /{id}/streak → Update streak
 * - GET /active → Active users
 * - GET /top-points → Top users by points
 * - GET /top-streak → Top users by streak
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "User", description = "APIs liên quan đến người dùng")
public class UserController {

    private final UserService userService;
    private final StreakService streakService;
    private final UserDashboardService userDashboardService;

    // =============== USER ENDPOINTS (For authenticated users) ===============

    /**
     * ✅ Get current user with stats and activity
     * GET /api/users/me
     * 
     * Returns: UserDetailDto (User + Stats + Activity)
     */
    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin user hiện tại", description = "Lấy thông tin của user đang đăng nhập (bao gồm stats và activity)")
    public ResponseEntity<CustomApiResponse<UserDetailDto>> getCurrentUser(
            @RequestAttribute("userId") Long userId) {

        return userService.getUserById(userId)
                .map(user -> {
                    UserDetailDto dto = UserMapper.toDetailDto(user);
                    return ResponseEntity.ok(CustomApiResponse.success(dto, "Lấy thông tin thành công"));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CustomApiResponse.notFound("Không tìm thấy người dùng")));
    }

    /**
     * ✅ Get complete dashboard data for home page
     * GET /api/users/me/dashboard
     * 
     * Returns: UserDashboardDto with:
     * - User info (User + Stats + Activity)
     * - Quick stats (from UserStats)
     * - Skill progress (from UserStats + lesson counts)
     * - Streak info (from UserStats)
     */
    @GetMapping("/me/dashboard")
    @Operation(summary = "Lấy dữ liệu dashboard đầy đủ", description = "Lấy tất cả dữ liệu cho trang chủ: stats, streak, skill progress...")
    public ResponseEntity<CustomApiResponse<UserDashboardDto>> getDashboard(
            @RequestAttribute("userId") Long userId) {

        try {
            UserDashboardDto dashboard = userDashboardService.getDashboardData(userId);
            return ResponseEntity.ok(CustomApiResponse.success(dashboard,
                    "Lấy dữ liệu dashboard thành công"));

        } catch (Exception e) {
            log.error("Error getting dashboard for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(CustomApiResponse.error(500, "Lỗi khi lấy dữ liệu dashboard"));
        }
    }

    /**
     * ✅ Get current user's stats only
     * GET /api/users/me/stats
     */
    @GetMapping("/me/stats")
    @Operation(summary = "Lấy thống kê của user hiện tại", description = "Lấy thống kê điểm, streak của user")
    public ResponseEntity<CustomApiResponse<UserStatsDto>> getCurrentUserStats(
            @RequestAttribute("userId") Long userId) {

        UserStats stats = userService.getUserStats(userId);
        UserStatsDto dto = UserMapper.toStatsDto(stats);
        return ResponseEntity.ok(CustomApiResponse.success(dto, "Lấy thống kê thành công"));
    }

    /**
     * ✅ Get current user's activity only
     * GET /api/users/me/activity
     */
    @GetMapping("/me/activity")
    @Operation(summary = "Lấy hoạt động của user hiện tại", description = "Lấy lịch sử đăng nhập và hoạt động")
    public ResponseEntity<CustomApiResponse<UserActivityDto>> getCurrentUserActivity(
            @RequestAttribute("userId") Long userId) {

        UserActivityDto dto = UserMapper.toActivityDto(userService.getUserActivity(userId));
        return ResponseEntity.ok(CustomApiResponse.success(dto, "Lấy hoạt động thành công"));
    }

    /**
     * ✅ Get streak information
     * GET /api/users/me/streak
     */
    @GetMapping("/me/streak")
    @Operation(summary = "Lấy thông tin streak", description = "Lấy thông tin streak của user hiện tại")
    public ResponseEntity<CustomApiResponse<StreakInfo>> getStreakInfo(
            @RequestAttribute("userId") Long userId) {

        StreakInfo streakInfo = streakService.getStreakInfo(userId);
        return ResponseEntity.ok(CustomApiResponse.success(streakInfo, "Lấy thông tin streak thành công"));
    }

    /**
     * ✅ Update user profile
     * PUT /api/users/me
     */
    @PutMapping("/me")
    @Operation(summary = "Cập nhật thông tin cá nhân", description = "User chỉ có thể cập nhật thông tin của chính mình")
    public ResponseEntity<CustomApiResponse<UserDetailDto>> updateCurrentUser(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateUserRequest userDto) {
        try {
            User updatedUser = userService.updateUser(userId, userDto);
            UserDetailDto dto = UserMapper.toDetailDto(updatedUser);
            return ResponseEntity.ok(CustomApiResponse.success(dto, "Cập nhật người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Không tìm thấy người dùng để cập nhật: " + e.getMessage()));
        }
    }

    /**
     * ✅ Change password
     * PUT /api/users/me/change-password
     */
    @PutMapping("/me/change-password")
    @Operation(summary = "Đổi mật khẩu", description = "User chỉ có thể đổi mật khẩu của chính mình")
    public ResponseEntity<CustomApiResponse<Void>> changePassword(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Mật khẩu xác nhận không khớp"));
        }

        try {
            userService.changePassword(userId, request);
            return ResponseEntity.ok(CustomApiResponse.success(null, "Đổi mật khẩu thành công"));
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound(e.getMessage()));
        }
    }

    // =============== ADMIN ENDPOINTS ===============

    /**
     * ✅ Get all users (Admin only)
     * GET /api/users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy tất cả người dùng", description = "Lấy danh sách tất cả người dùng (ADMIN)")
    public ResponseEntity<CustomApiResponse<List<UserDetailDto>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDetailDto> userDetails = users.stream()
                .map(UserMapper::toDetailDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(CustomApiResponse.success(userDetails, "Lấy thành công tất cả người dùng"));
    }

    /**
     * ✅ Get user by ID (Admin only)
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy người dùng theo ID", description = "Lấy thông tin người dùng theo ID (ADMIN)")
    public ResponseEntity<CustomApiResponse<UserDetailDto>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> {
                    UserDetailDto dto = UserMapper.toDetailDto(user);
                    return ResponseEntity.ok(CustomApiResponse.success(dto, "Lấy người dùng thành công"));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CustomApiResponse.notFound("Không tìm thấy người dùng với id: " + id)));
    }

    /**
     * ✅ Get user by email (Admin only)
     * GET /api/users/email/{email}
     */
    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy người dùng theo email", description = "Lấy thông tin người dùng theo email (ADMIN)")
    public ResponseEntity<CustomApiResponse<UserDetailDto>> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> {
                    UserDetailDto dto = UserMapper.toDetailDto(user);
                    return ResponseEntity.ok(CustomApiResponse.success(dto, "Lấy email người dùng thành công"));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CustomApiResponse.notFound("Không tìm thấy email người dùng với email: " + email)));
    }

    /**
     * ✅ Delete user (Admin only)
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa người dùng", description = "Xóa người dùng theo ID (ADMIN)")
    public ResponseEntity<CustomApiResponse<UserDetailDto>> deleteUser(@PathVariable Long id) {
        try {
            User deletedUser = userService.deleteUser(id);
            UserDetailDto dto = UserMapper.toDetailDto(deletedUser);
            return ResponseEntity.ok(CustomApiResponse.success(dto, "Xóa người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Không tìm thấy người dùng để xóa: " + e.getMessage()));
        }
    }

    /**
     * ✅ Add points to user (Admin only)
     * PUT /api/users/{id}/points?points={points}
     */
    @PutMapping("/{id}/points")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm điểm người dùng", description = "Thêm điểm cho người dùng theo ID")
    public ResponseEntity<CustomApiResponse<Void>> addPoints(
            @PathVariable Long id,
            @RequestParam Integer points) {
        try {
            userService.addPoints(id, points);
            return ResponseEntity.ok(CustomApiResponse.success(null, "Thêm điểm thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Thêm điểm thất bại: " + e.getMessage()));
        }
    }

    /**
     * ✅ Block user (Admin only)
     * PUT /api/users/{id}/block
     */
    @PutMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Khóa người dùng", description = "Khóa một người dùng theo ID")
    public ResponseEntity<CustomApiResponse<Void>> blockUser(@PathVariable Long id) {
        try {
            userService.blockUser(id);
            return ResponseEntity.ok(CustomApiResponse.success(null, "Khóa người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Không tìm thấy người dùng để khóa: " + e.getMessage()));
        }
    }

    /**
     * ✅ Unblock user (Admin only)
     * PUT /api/users/{id}/unblock
     */
    @PutMapping("/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mở khóa người dùng", description = "Mở khóa một người dùng theo ID")
    public ResponseEntity<CustomApiResponse<Void>> unblockUser(@PathVariable Long id) {
        try {
            userService.unblockUser(id);
            return ResponseEntity.ok(CustomApiResponse.success(null, "Mở khóa người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Không tìm thấy người dùng để mở khóa: " + e.getMessage()));
        }
    }

    /**
     * ✅ Update streak days (Admin only)
     * PUT /api/users/{id}/streak?streakDays={streakDays}
     */
    @PutMapping("/{id}/streak")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật số ngày streak", description = "Cập nhật số ngày streak của người dùng (Admin only)")
    public ResponseEntity<CustomApiResponse<Void>> updateStreakDays(
            @PathVariable Long id,
            @RequestParam Integer streakDays) {
        try {
            userService.updateStreakDays(id, streakDays);
            return ResponseEntity.ok(CustomApiResponse.success(null, "Cập nhật số ngày streak thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Cập nhật số ngày streak thất bại: " + e.getMessage()));
        }
    }

    /**
     * ✅ Get active users (Admin only)
     * GET /api/users/active
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy trạng thái hoạt động của người dùng", description = "Lấy danh sách tất cả người dùng đang hoạt động")
    public ResponseEntity<CustomApiResponse<List<UserDetailDto>>> getActiveUsers() {
        List<User> users = userService.getActiveUsers();
        List<UserDetailDto> userDetails = users.stream()
                .map(UserMapper::toDetailDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(CustomApiResponse.success(userDetails, "Lấy người dùng hoạt động thành công"));
    }

    /**
     * ✅ Get top users by points (Admin only)
     * GET /api/users/top-points?minPoints={minPoints}
     */
    @GetMapping("/top-points")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy người dùng hàng đầu theo điểm", description = "Lấy danh sách người dùng có điểm cao nhất")
    public ResponseEntity<CustomApiResponse<List<UserStatsDto>>> getTopUsersByPoints(
            @RequestParam(defaultValue = "0") Integer minPoints) {
        List<UserStats> userStats = userService.getTopUsersByPoints(minPoints);

        List<UserStatsDto> statsDtos = userStats.stream()
                .map(UserMapper::toStatsDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CustomApiResponse.success(statsDtos, "Lấy người dùng hàng đầu theo điểm thành công"));
    }

    /**
     * ✅ Get top users by streak (Admin only)
     * GET /api/users/top-streak?minStreakDays={minStreakDays}
     */
    @GetMapping("/top-streak")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy người dùng hàng đầu theo chuỗi", description = "Lấy danh sách người dùng có chuỗi cao nhất")
    public ResponseEntity<CustomApiResponse<List<UserStatsDto>>> getTopUsersByStreak(
            @RequestParam(defaultValue = "0") Integer minStreakDays) {
        List<UserStats> userStats = userService.getTopUsersByStreakDays(minStreakDays);

        List<UserStatsDto> statsDtos = userStats.stream()
                .map(UserMapper::toStatsDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CustomApiResponse.success(statsDtos, "Lấy người dùng hàng đầu theo chuỗi thành công"));
    }
}