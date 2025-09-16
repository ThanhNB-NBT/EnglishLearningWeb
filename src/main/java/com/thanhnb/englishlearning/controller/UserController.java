package com.thanhnb.englishlearning.controller;

import com.thanhnb.englishlearning.dto.ApiResponse;
import com.thanhnb.englishlearning.dto.UserDto;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.UserRole;
import com.thanhnb.englishlearning.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    
    private final UserService userService;

    @GetMapping("/endpoints")
    public ResponseEntity<ApiResponse<Object>> getEndpoints() {
        List<Map<String, String>> endpoints = Arrays.asList(
            Map.of("method", "GET", "url", "/api/users", "description", "Lấy tất cả người dùng"),
            Map.of("method", "GET", "url", "/api/users/{id}", "description", "Lấy người dùng theo ID"),
            Map.of("method", "GET", "url", "/api/users/username/{username}", "description", "Lấy người dùng theo username"),
            Map.of("method", "GET", "url", "/api/users/email/{email}", "description", "Lấy người dùng theo email"),
            Map.of("method", "POST", "url", "/api/users", "description", "Tạo người dùng mới"),
            Map.of("method", "PUT", "url", "/api/users/{id}", "description", "Cập nhật người dùng"),
            Map.of("method", "DELETE", "url", "/api/users/{id}", "description", "Xóa người dùng"),
            Map.of("method", "GET", "url", "/api/users/role/{role}", "description", "Lấy người dùng theo vai trò"),
            Map.of("method", "PUT", "url", "/api/users/{id}/points?points=50", "description", "Thêm điểm cho người dùng"),
            Map.of("method", "GET", "url", "/api/users/active", "description", "Lấy người dùng đang hoạt động"),
            Map.of("method", "GET", "url", "/api/users/top-points?minPoints=0", "description", "Top người dùng theo điểm"),
            Map.of("method", "GET", "url", "/api/users/top-streak?minStreakDays=0", "description", "Top người dùng theo streak"),
            Map.of("method", "PUT", "url", "/api/users/{id}/block", "description", "Khóa người dùng"),
            Map.of("method", "PUT", "url", "/api/users/{id}/unblock", "description", "Mở khóa người dùng"),
            Map.of("method", "PUT", "url", "/api/users/{username}/last-login", "description", "Cập nhật lần đăng nhập cuối"),
            Map.of("method", "PUT", "url", "/api/users/{id}/streak?streakDays=7", "description", "Cập nhật streak days"),
            Map.of("method", "GET", "url", "/api/users/endpoints", "description", "Xem danh sách endpoints")
        );
        
        return ResponseEntity.ok(ApiResponse.success(Map.of("endpoints", endpoints), "Danh sách User API endpoints"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users, "Lấy thành công tất cả người dùng"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user, "Lấy người dùng thành công")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Không tìm thấy người dùng với id: " + id)));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse<User>> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user, "Lấy người dùng thành công")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Không tìm thấy người dùng với username: " + username)));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<User>> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user, "Lấy email người dùng thành công")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Không tìm thấy email người dùng với email: " + email)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody UserDto userDto) {
        try {
            User createdUser = userService.createUser(userDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(createdUser, "Tạo người dùng thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.badRequest("Tạo người dùng thất bại: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        try {
            User updatedUser = userService.updateUser(id, userDto);
            return ResponseEntity.ok(ApiResponse.success(updatedUser, "Cập nhật người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Không tìm thấy người dùng để cập nhật: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Không tìm thấy người dùng để xóa: " + e.getMessage()));
        }
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<List<User>>> getUsersByRole(@PathVariable UserRole role) {
        List<User> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(ApiResponse.success(users, "Lấy người dùng theo vai trò: " + role));
    }

    @PutMapping("/{id}/points")
    public ResponseEntity<ApiResponse<Void>> addPoints(@PathVariable Long id, @RequestParam Integer points) {
        try {
            userService.addPoints(id, points);
            return ResponseEntity.ok(ApiResponse.success(null, "Thêm điểm thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Thêm điểm thất bại: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<User>>> getActiveUsers() {
        List<User> users = userService.getActiveUsers();
        return ResponseEntity.ok(ApiResponse.success(users, "Lấy người dùng hoạt động thành công"));
    }

    @GetMapping("/top-points")
    public ResponseEntity<ApiResponse<List<User>>> getTopUsersByPoints(@RequestParam(defaultValue = "0") Integer minPoints) {
        List<User> users = userService.getTopUsersByPoints(minPoints);
        return ResponseEntity.ok(ApiResponse.success(users, "Lấy người dùng hàng đầu theo điểm thành công"));
    }

    @GetMapping("/top-streak")
    public ResponseEntity<ApiResponse<List<User>>> getTopUsersByStreak(@RequestParam(defaultValue = "0") Integer minStreakDays) {
        List<User> users = userService.getTopUsersByStreakDays(minStreakDays);
        return ResponseEntity.ok(ApiResponse.success(users, "Lấy người dùng hàng đầu theo chuỗi thành công"));
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable Long id) {
        try {
            userService.blockUser(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Khóa người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Không tìm thấy người dùng để khóa: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/unblock")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable Long id) {
        try {
            userService.unblockUser(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Mở khóa người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Không tìm thấy người dùng để mở khóa: " + e.getMessage()));
        }
    }

    @PutMapping("/{username}/last-login")
    public ResponseEntity<ApiResponse<Void>> updateLastLogin(@PathVariable String username) {
        try {
            userService.updateLastLogin(username);
            return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật lần đăng nhập cuối cùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Cập nhật lần đăng nhập cuối cùng thất bại: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/streak")
    public ResponseEntity<ApiResponse<Void>> updateStreakDays(@PathVariable Long id, @RequestParam Integer streakDays) {
        try {
            userService.updateStreakDays(id, streakDays);
            return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật số ngày streak thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound("Cập nhật số ngày streak thất bại: " + e.getMessage()));
        }
    }
}
