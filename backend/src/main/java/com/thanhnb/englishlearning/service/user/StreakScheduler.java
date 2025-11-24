package com.thanhnb.englishlearning.service.user;

import java.time.LocalDate;
import java.util.List;
import com.thanhnb.englishlearning.entity.User;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.thanhnb.englishlearning.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StreakScheduler {
    private final UserRepository userRepository;

    /**
     * Chạy mỗi đêm lúc 00:01 để check và reset streak
     */
    @Scheduled(cron = "0 1 0 * * *") // 00:01 AM every day
    public void checkAndResetStreaks() {
        log.info("Starting daily streak check...");
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        
        // Lấy tất cả users có streak > 0
        List<User> activeStreakUsers = userRepository.findByStreakDaysGreaterThan(0);
        
        int resetCount = 0;
        for (User user : activeStreakUsers) {
            LocalDate lastStreakDate = user.getLastStreakDate();
            
            // Nếu lastStreakDate không phải hôm qua hoặc hôm nay
            // → Streak đã bị break
            if (lastStreakDate == null || 
                (!lastStreakDate.equals(yesterday) && !lastStreakDate.equals(today))) {
                
                log.info("Resetting streak for user {} (last streak: {})", 
                    user.getId(), lastStreakDate);
                
                user.setStreakDays(0);
                user.setLastStreakDate(null);
                userRepository.save(user);
                resetCount++;
            }
        }
        
        log.info("Daily streak check completed. Reset {} streaks", resetCount);
    }
    
}
