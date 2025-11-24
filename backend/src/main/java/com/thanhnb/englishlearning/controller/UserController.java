package com.thanhnb.englishlearning.controller;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.user.request.ChangePasswordRequest;
import com.thanhnb.englishlearning.dto.user.request.UpdateUserRequest;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.exception.InvalidCredentialsException;
import com.thanhnb.englishlearning.service.user.StreakService.StreakInfo;
import com.thanhnb.englishlearning.service.user.StreakService;
import com.thanhnb.englishlearning.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User", description = "APIs liên quan đến người dùng")
public class UserController {

    private final UserService userService;
    private final StreakService streakService;

    // =============== ADMIN ENDPOINTS =====================
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy tất cả người dùng", description = "Lấy danh sách tất cả người dùng(ADMIN)")
    public ResponseEntity<CustomApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(CustomApiResponse.success(users, "Lấy thành công tất cả người dùng"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy người dùng theo ID", description = "Lấy thông tin người dùng theo ID(ADMIN)")
    public ResponseEntity<CustomApiResponse<User>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(CustomApiResponse.success(user, "Lấy người dùng thành công")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CustomApiResponse.notFound("Không tìm thấy người dùng với id: " + id)));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy người dùng theo email", description = "Lấy thông tin người dùng theo email(ADMIN)")
    public ResponseEntity<CustomApiResponse<User>> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(CustomApiResponse.success(user, "Lấy email người dùng thành công")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CustomApiResponse.notFound("Không tìm thấy email người dùng với email: " + email)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa người dùng", description = "Xóa người dùng theo ID(ADMIN)")
    public ResponseEntity<CustomApiResponse<User>> deleteUser(@PathVariable Long id) {
        try {
            User deletedUser = userService.deleteUser(id);
            return ResponseEntity.ok(CustomApiResponse.success(deletedUser, "Xóa người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Không tìm thấy người dùng để xóa: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/points")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy điểm người dùng", description = "Lấy điểm của người dùng theo ID")
    public ResponseEntity<CustomApiResponse<Void>> addPoints(@PathVariable Long id, @RequestParam Integer points) {
        try {
            userService.addPoints(id, points);
            return ResponseEntity.ok(CustomApiResponse.success(null, "Thêm điểm thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Thêm điểm thất bại: " + e.getMessage()));
        }
    }

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

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy trạng thái hoạt động của người dùng", description = "Lấy danh sách tất cả người dùng đang hoạt động")
    public ResponseEntity<CustomApiResponse<List<User>>> getActiveUsers() {
        List<User> users = userService.getActiveUsers();
        return ResponseEntity.ok(CustomApiResponse.success(users, "Lấy người dùng hoạt động thành công"));
    }

    @GetMapping("/top-points")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy người dùng hàng đầu theo điểm", description = "Lấy danh sách tất cả người dùng có điểm cao nhất")
    public ResponseEntity<CustomApiResponse<List<User>>> getTopUsersByPoints(
            @RequestParam(defaultValue = "0") Integer minPoints) {
        List<User> users = userService.getTopUsersByPoints(minPoints);
        return ResponseEntity.ok(CustomApiResponse.success(users, "Lấy người dùng hàng đầu theo điểm thành công"));
    }

    @GetMapping("/top-streak")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy người dùng hàng đầu theo chuỗi", description = "Lấy danh sách tất cả người dùng có chuỗi cao nhất")
    public ResponseEntity<CustomApiResponse<List<User>>> getTopUsersByStreak(
            @RequestParam(defaultValue = "0") Integer minStreakDays) {
        List<User> users = userService.getTopUsersByStreakDays(minStreakDays);
        return ResponseEntity.ok(CustomApiResponse.success(users, "Lấy người dùng hàng đầu theo chuỗi thành công"));
    }

    // =============== USER ENDPOINTS =============

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin user hiện tại", description = "Lấy thông tin của user đang đăng nhập")
    public ResponseEntity<CustomApiResponse<User>> getCurrentUser(
            @RequestAttribute("userId") Long userId) {

        return userService.getUserById(userId)
                .map(user -> ResponseEntity.ok(CustomApiResponse.success(user, "Lấy thông tin thành công")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CustomApiResponse.notFound("Không tìm thấy người dùng")));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Lấy người dùng theo username", description = "Lấy thông tin người dùng theo username")
    public ResponseEntity<CustomApiResponse<User>> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(CustomApiResponse.success(user, "Lấy người dùng thành công")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CustomApiResponse.notFound("Không tìm thấy người dùng với username: " + username)));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật thông tin cá nhân", description = "User chỉ có thể cập nhật thông tin của chính mình")
    public ResponseEntity<CustomApiResponse<User>> updateCurrentUser(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateUserRequest userDto) {
        try {
            User updatedUser = userService.updateUser(userId, userDto);
            return ResponseEntity.ok(CustomApiResponse.success(updatedUser, "Cập nhật người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Không tìm thấy người dùng để cập nhật: " + e.getMessage()));
        }
    }

    @PutMapping("/me/change-password")
    @Operation(summary = "Đổi mật khẩu", description = "User chỉ có thể đổi mật khẩu của chính mình")
    public ResponseEntity<CustomApiResponse<Void>> changePassword(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {

        // Validate confirm password
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

    @GetMapping("/me/streak")
    @Operation(summary = "Lấy thông tin streak", description = "Lấy thông tin streak của user hiện tại")
    public ResponseEntity<CustomApiResponse<StreakInfo>> getStreakInfo(
            @RequestAttribute("userId") Long userId) {

        StreakInfo streakInfo = streakService.getStreakInfo(userId);
        return ResponseEntity.ok(CustomApiResponse.success(streakInfo, "Lấy thông tin streak thành công"));
    }

    @PutMapping("/{id}/last-login")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật lần đăng nhập cuối", description = "Internal use only")
    public ResponseEntity<CustomApiResponse<Void>> updateLastLogin(@PathVariable Long id) {
        try {
            userService.updateLastLogin(id);
            return ResponseEntity.ok(CustomApiResponse.success(null, "Cập nhật lần đăng nhập cuối cùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Cập nhật lần đăng nhập cuối cùng thất bại: " + e.getMessage()));
        }
    }
}
