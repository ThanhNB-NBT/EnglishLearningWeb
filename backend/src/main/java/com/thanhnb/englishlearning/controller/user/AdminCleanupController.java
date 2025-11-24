package com.thanhnb.englishlearning.controller.user;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.service.user.UnverifiedUserCleanupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/cleanup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Cleanup", description = "APIs for managing unverified user cleanup")
public class AdminCleanupController {
    
    private final UnverifiedUserCleanupService cleanupService;
    private final UserRepository userRepository;
    
    @Value("${cleanup.unverified-users.hours:24}")
    private int cleanupHours;
    
    /**
     * Lấy thống kê tài khoản chưa verify
     */
    @GetMapping("/stats")
    @Operation(summary = "Get unverified accounts statistics")
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> getUnverifiedStats() {
        try {
            long totalUnverified = userRepository.countUnverifiedUsers();
            LocalDateTime cutoffDate = LocalDateTime.now().minusHours(cleanupHours);
            long oldUnverified = userRepository.countUnverifiedAccountsOlderThan(cutoffDate);
            
            // Consistent response structure
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUnverifiedAccounts", totalUnverified);
            stats.put("accountsOlderThanCutoff", oldUnverified);
            stats.put("cutoffHours", cleanupHours);
            stats.put("nextCleanupWillDelete", oldUnverified);
            stats.put("timestamp", LocalDateTime.now());
            
            log.debug("Stats fetched: total={}, old={}, hours={}", 
                    totalUnverified, oldUnverified, cleanupHours);
            
            return ResponseEntity.ok(CustomApiResponse.success(stats, "Lấy thống kê thành công"));
            
        } catch (Exception e) {
            log.error("Error getting unverified stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(CustomApiResponse.error(500, "Lỗi khi lấy thống kê: " + e.getMessage()));
        }
    }
    
    /**
     * Lấy danh sách tài khoản chưa verify (có pagination)
     */
    @GetMapping("/unverified-users")
    @Operation(summary = "Get list of unverified accounts")
    public ResponseEntity<CustomApiResponse<List<Map<String, Object>>>> getUnverifiedUsers(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<User> unverifiedUsers = userRepository.findUnverifiedUsers();
            
            List<Map<String, Object>> userList = unverifiedUsers.stream()
                    .limit(limit)
                    .map(user -> {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("id", user.getId());
                        userInfo.put("username", user.getUsername());
                        userInfo.put("email", user.getEmail());
                        
                        LocalDateTime createdAt = user.getCreatedAt();
                        if (createdAt != null) {
                            userInfo.put("createdAt", createdAt);
                            
                            // Calculate hours old
                            long hoursOld = Duration.between(createdAt, LocalDateTime.now()).toHours();
                            userInfo.put("hoursOld", hoursOld);
                            
                            // Add helper fields
                            userInfo.put("willBeDeleted", hoursOld >= cleanupHours);
                            userInfo.put("daysOld", hoursOld / 24);
                        } else {
                            // Fallback for null createdAt
                            userInfo.put("createdAt", null);
                            userInfo.put("hoursOld", 0);
                            userInfo.put("willBeDeleted", false);
                            userInfo.put("daysOld", 0);
                            
                            log.warn("User {} has null createdAt", user.getId());
                        }
                        
                        return userInfo;
                    })
                    .collect(Collectors.toList());
            
            log.debug("Fetched {} unverified users (limit: {})", userList.size(), limit);
            
            return ResponseEntity.ok(CustomApiResponse.success(userList, "Lấy danh sách thành công"));
            
        } catch (Exception e) {
            log.error("Error getting unverified users: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(CustomApiResponse.error(500, "Lỗi khi lấy danh sách: " + e.getMessage()));
        }
    }
    
    /**
     * Chạy cleanup thủ công (manual trigger)
     */
    @PostMapping("/run-now")
    @Operation(summary = "Run cleanup manually", description = "Trigger cleanup job immediately")
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> runCleanupNow() {
        try {
            log.info("🗑️ Admin triggered manual cleanup");
            
            // Preview trước khi cleanup
            LocalDateTime cutoffDate = LocalDateTime.now().minusHours(cleanupHours);
            long willDelete = userRepository.countUnverifiedAccountsOlderThan(cutoffDate);
            
            if (willDelete == 0) {
                log.info("No accounts to cleanup");
                Map<String, Object> result = new HashMap<>();
                result.put("deletedCount", 0);
                result.put("message", "Không có tài khoản nào cần xóa");
                result.put("timestamp", LocalDateTime.now());
                
                return ResponseEntity.ok(CustomApiResponse.success(
                        result, 
                        "Không có tài khoản nào cần cleanup"));
            }
            
            // Thực hiện cleanup
            int deletedCount = cleanupService.cleanupNow();
            
            // Consistent response structure
            Map<String, Object> result = new HashMap<>();
            result.put("deletedCount", deletedCount);
            result.put("cutoffHours", cleanupHours);
            result.put("cutoffDate", cutoffDate);
            result.put("timestamp", LocalDateTime.now());
            
            log.info("Cleanup completed: {} accounts deleted", deletedCount);
            
            return ResponseEntity.ok(CustomApiResponse.success(
                    result, 
                    "Cleanup hoàn tất: Đã xóa " + deletedCount + " tài khoản"));
            
        } catch (Exception e) {
            log.error("Error running manual cleanup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(CustomApiResponse.error(500, "Lỗi khi chạy cleanup: " + e.getMessage()));
        }
    }
    
    /**
     * Preview - Xem sẽ xóa bao nhiêu tài khoản nếu chạy cleanup ngay
     */
    @GetMapping("/preview")
    @Operation(summary = "Preview cleanup", description = "See how many accounts will be deleted")
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> previewCleanup() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusHours(cleanupHours);
            long willBeDeleted = userRepository.countUnverifiedAccountsOlderThan(cutoffDate);
            
            // Consistent response structure với timestamp
            Map<String, Object> preview = new HashMap<>();
            preview.put("accountsWillBeDeleted", willBeDeleted);
            preview.put("cutoffDate", cutoffDate);
            preview.put("cutoffHours", cleanupHours);
            preview.put("timestamp", LocalDateTime.now());
            
            log.debug("Preview cleanup: {} accounts will be deleted", willBeDeleted);
            
            return ResponseEntity.ok(CustomApiResponse.success(
                    preview, 
                    "Preview cleanup: " + willBeDeleted + " tài khoản sẽ bị xóa"));
            
        } catch (Exception e) {
            log.error("Error previewing cleanup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(CustomApiResponse.error(500, "Lỗi preview: " + e.getMessage()));
        }
    }
    
    /**
     * Get cleanup configuration
     */
    @GetMapping("/config")
    @Operation(summary = "Get cleanup configuration")
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> getCleanupConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cleanupHours", cleanupHours);
            config.put("cleanupCron", "0 0 2 * * *"); // 2:00 AM daily
            config.put("description", "Tự động xóa tài khoản chưa verify sau " + cleanupHours + " giờ");
            
            return ResponseEntity.ok(CustomApiResponse.success(config, "Lấy cấu hình thành công"));
            
        } catch (Exception e) {
            log.error("Error getting cleanup config: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(CustomApiResponse.error(500, "Lỗi lấy cấu hình: " + e.getMessage()));
        }
    }
}