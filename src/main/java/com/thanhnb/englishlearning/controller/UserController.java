package com.thanhnb.englishlearning.controller;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.user.UserDto;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.UserRole;
import com.thanhnb.englishlearning.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    @Operation(summary = "Lấy tất cả người dùng", description = "Lấy danh sách tất cả người dùng")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(CustomApiResponse.success(users, "Lấy thành công tất cả người dùng"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy người dùng theo ID", description = "Lấy thông tin người dùng theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<User>> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(CustomApiResponse.success(user, "Lấy người dùng thành công")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Không tìm thấy người dùng với id: " + id)));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Lấy người dùng theo username", description = "Lấy thông tin người dùng theo username")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<User>> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(CustomApiResponse.success(user, "Lấy người dùng thành công")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Không tìm thấy người dùng với username: " + username)));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Lấy người dùng theo email", description = "Lấy thông tin người dùng theo email")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<User>> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(CustomApiResponse.success(user, "Lấy email người dùng thành công")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.notFound("Không tìm thấy email người dùng với email: " + email)));
    }

    @PostMapping
    @Operation(summary = "Tạo người dùng mới", description = "Tạo một người dùng mới với thông tin đã cung cấp")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tạo người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - đầu vào không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<User>> createUser(@Valid @RequestBody UserDto userDto) {
        try {
            User createdUser = userService.createUser(userDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomApiResponse.created(createdUser, "Tạo người dùng thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomApiResponse.badRequest("Tạo người dùng thất bại: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật người dùng", description = "Cập nhật thông tin người dùng theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cập nhật người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - đầu vào không hợp lệ hoặc không tìm thấy người dùng")
    })
    public ResponseEntity<CustomApiResponse<User>> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        try {
            User updatedUser = userService.updateUser(id, userDto);
            return ResponseEntity.ok(CustomApiResponse.success(updatedUser, "Cập nhật người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CustomApiResponse.notFound("Không tìm thấy người dùng để cập nhật: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa người dùng", description = "Xóa người dùng theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Xóa người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - không tìm thấy người dùng")
    })
    public ResponseEntity<CustomApiResponse<Void>> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(CustomApiResponse.success(null, "Xóa người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CustomApiResponse.notFound("Không tìm thấy người dùng để xóa: " + e.getMessage()));
        }
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Lấy người dùng theo vai trò", description = "Lấy danh sách người dùng theo vai trò của họ")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - vai trò không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<List<User>>> getUsersByRole(@PathVariable UserRole role) {
        List<User> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(CustomApiResponse.success(users, "Lấy người dùng theo vai trò: " + role));
    }

    @PutMapping("/{id}/points")
    @Operation(summary = "Lấy điểm người dùng", description = "Lấy điểm của người dùng theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy điểm người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - ID người dùng không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<Void>> addPoints(@PathVariable Long id, @RequestParam Integer points) {
        try {
            userService.addPoints(id, points);
            return ResponseEntity.ok(CustomApiResponse.success(null, "Thêm điểm thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CustomApiResponse.notFound("Thêm điểm thất bại: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    @Operation(summary = "Lấy trạng thái hoạt động của người dùng", description = "Lấy danh sách tất cả người dùng đang hoạt động")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy người dùng hoạt động thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - tham số không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<List<User>>> getActiveUsers() {
        List<User> users = userService.getActiveUsers();
        return ResponseEntity.ok(CustomApiResponse.success(users, "Lấy người dùng hoạt động thành công"));
    }

    @GetMapping("/top-points")
    @Operation(summary = "Lấy người dùng hàng đầu theo điểm", description = "Lấy danh sách tất cả người dùng có điểm cao nhất")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - tham số không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<List<User>>> getTopUsersByPoints(@RequestParam(defaultValue = "0") Integer minPoints) {
        List<User> users = userService.getTopUsersByPoints(minPoints);
        return ResponseEntity.ok(CustomApiResponse.success(users, "Lấy người dùng hàng đầu theo điểm thành công"));
    }

    @GetMapping("/top-streak")
    @Operation(summary = "Lấy người dùng hàng đầu theo chuỗi", description = "Lấy danh sách tất cả người dùng có chuỗi cao nhất")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - tham số không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<List<User>>> getTopUsersByStreak(@RequestParam(defaultValue = "0") Integer minStreakDays) {
        List<User> users = userService.getTopUsersByStreakDays(minStreakDays);
        return ResponseEntity.ok(CustomApiResponse.success(users, "Lấy người dùng hàng đầu theo chuỗi thành công"));
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Khóa người dùng", description = "Khóa một người dùng theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Khóa người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - ID người dùng không hợp lệ"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
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
    @Operation(summary = "Mở khóa người dùng", description = "Mở khóa một người dùng theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mở khóa người dùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - ID người dùng không hợp lệ"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    public ResponseEntity<CustomApiResponse<Void>> unblockUser(@PathVariable Long id) {
        try {
            userService.unblockUser(id);
            return ResponseEntity.ok(CustomApiResponse.success(null, "Mở khóa người dùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CustomApiResponse.notFound("Không tìm thấy người dùng để mở khóa: " + e.getMessage()));
        }
    }

    @PutMapping("/{username}/last-login")
    @Operation(summary = "Cập nhật lần đăng nhập cuối cùng", description = "Cập nhật thời gian đăng nhập cuối cùng của một người dùng theo tên người dùng")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cập nhật lần đăng nhập cuối cùng thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - tên người dùng không hợp lệ"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    public ResponseEntity<CustomApiResponse<Void>> updateLastLogin(@PathVariable String username) {
        try {
            userService.updateLastLogin(username);
            return ResponseEntity.ok(CustomApiResponse.success(null, "Cập nhật lần đăng nhập cuối cùng thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CustomApiResponse.notFound("Cập nhật lần đăng nhập cuối cùng thất bại: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/streak")
    @Operation(summary = "Cập nhật số ngày streak", description = "Cập nhật số ngày streak của một người dùng theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cập nhật số ngày streak thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - ID người dùng không hợp lệ"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    public ResponseEntity<CustomApiResponse<Void>> updateStreakDays(@PathVariable Long id, @RequestParam Integer streakDays) {
        try {
            userService.updateStreakDays(id, streakDays);
            return ResponseEntity.ok(CustomApiResponse.success(null, "Cập nhật số ngày streak thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CustomApiResponse.notFound("Cập nhật số ngày streak thất bại: " + e.getMessage()));
        }
    }
}
