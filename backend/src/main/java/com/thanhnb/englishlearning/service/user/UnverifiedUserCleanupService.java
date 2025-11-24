package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ✅ ENHANCED: Service tự động dọn dẹp tài khoản chưa verify
 * - Chạy scheduled job mỗi ngày lúc 2:00 AM
 * - Xóa tài khoản chưa verify quá X giờ (default 24h)
 * - Fix tài khoản có createdDate null
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnverifiedUserCleanupService {
    
    private final UserRepository userRepository;
    
    @Value("${cleanup.unverified-users.enabled:true}")
    private boolean cleanupEnabled;
    
    @Value("${cleanup.unverified-users.hours:24}")
    private int cleanupHours;
    
    /**
     * ✅ ENHANCED: Scheduled job - Chạy tự động mỗi ngày lúc 2:00 AM
     * Cron format: second minute hour day month weekday
     * 0 0 2 * * * = Mỗi ngày lúc 2:00 AM
     */
    @Scheduled(cron = "${cleanup.unverified-users.cron:0 0 2 * * *}")
    @Transactional
    public void cleanupUnverifiedAccounts() {
        if (!cleanupEnabled) {
            log.debug("Unverified user cleanup is disabled");
            return;
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(cleanupHours);
        
        try {
            log.info("🗑️ Starting scheduled unverified accounts cleanup (older than {} hours)...", cleanupHours);
            
            // ✅ FIX: Kiểm tra và fix null createdDate trước khi cleanup
            int fixedCount = fixNullCreatedDates();
            if (fixedCount > 0) {
                log.warn("Fixed {} accounts with null createdDate", fixedCount);
            }
            
            // Cleanup
            int deletedCount = userRepository.deleteUnverifiedAccountsCreatedBefore(cutoffDate);
            
            if (deletedCount > 0) {
                log.info("✅ Cleanup completed: Deleted {} unverified accounts older than {} hours", 
                        deletedCount, cleanupHours);
            } else {
                log.debug("No unverified accounts to clean up");
            }
            
        } catch (Exception e) {
            log.error("❌ Error during unverified accounts cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * ✅ ENHANCED: Manual cleanup - Có thể gọi từ admin endpoint
     * @return Số lượng tài khoản đã xóa
     */
    @Transactional
    public int cleanupNow() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(cleanupHours);
        
        log.info("🗑️ Manual cleanup triggered for unverified accounts older than {} hours", cleanupHours);
        
        try {
            // ✅ FIX: Kiểm tra và fix null createdDate trước
            int fixedCount = fixNullCreatedDates();
            if (fixedCount > 0) {
                log.warn("Fixed {} accounts with null createdDate before cleanup", fixedCount);
            }
            
            // Preview trước khi xóa
            long willDelete = userRepository.countUnverifiedAccountsOlderThan(cutoffDate);
            log.info("About to delete {} accounts...", willDelete);
            
            // Thực hiện cleanup
            int deletedCount = userRepository.deleteUnverifiedAccountsCreatedBefore(cutoffDate);
            
            log.info("✅ Manual cleanup completed: {} accounts deleted", deletedCount);
            
            return deletedCount;
            
        } catch (Exception e) {
            log.error("❌ Error during manual cleanup: {}", e.getMessage(), e);
            throw new RuntimeException("Cleanup failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * ✅ NEW: Fix tài khoản có createdDate null
     * Set createdDate = current time cho các tài khoản lỗi
     * 
     * @return Số lượng tài khoản đã fix
     */
    @Transactional
    public int fixNullCreatedDates() {
        try {
            long countNullDates = userRepository.countUnverifiedUsersWithNullCreatedAt();
            
            if (countNullDates == 0) {
                return 0;
            }
            
            log.warn("Found {} unverified accounts with null createdDate. Fixing...", countNullDates);
            
            int fixedCount = userRepository.fixNullCreatedAt(LocalDateTime.now());
            
            log.info("Fixed {} accounts with null createdDate", fixedCount);
            
            return fixedCount;
            
        } catch (Exception e) {
            log.error("Error fixing null createdDates: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Lấy số lượng tài khoản chưa verify hiện tại
     */
    public long getUnverifiedAccountCount() {
        return userRepository.countUnverifiedUsers();
    }
    
    /**
     * ✅ NEW: Lấy số lượng tài khoản sẽ bị xóa trong lần cleanup tiếp theo
     */
    public long getAccountsWillBeDeleted() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(cleanupHours);
        return userRepository.countUnverifiedAccountsOlderThan(cutoffDate);
    }
    
    /**
     * ✅ NEW: Lấy thống kê chi tiết về cleanup
     */
    public Map<String, Object> getDetailedStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUnverified = userRepository.countUnverifiedUsers();
        long nullCreatedDate = userRepository.countUnverifiedUsersWithNullCreatedAt();
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(cleanupHours);
        long willBeDeleted = userRepository.countUnverifiedAccountsOlderThan(cutoffDate);
        
        stats.put("totalUnverified", totalUnverified);
        stats.put("willBeDeleted", willBeDeleted);
        stats.put("accountsWithNullDate", nullCreatedDate);
        stats.put("cleanupHours", cleanupHours);
        stats.put("cutoffDate", cutoffDate);
        stats.put("timestamp", LocalDateTime.now());
        
        return stats;
    }
    
    /**
     * ✅ NEW: Kiểm tra xem cleanup có đang enabled không
     */
    public boolean isCleanupEnabled() {
        return cleanupEnabled;
    }
    
    /**
     * ✅ NEW: Lấy cleanup hours config
     */
    public int getCleanupHours() {
        return cleanupHours;
    }
}