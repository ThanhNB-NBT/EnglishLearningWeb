package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.entity.User;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.thanhnb.englishlearning.repository.UserRepository;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {

    private final UserRepository userRepository;

    // Cập nhật streak khi user có hoạt động
    public boolean updateStreakOnActivity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate today = LocalDate.now();
        LocalDate lastStreakDate = user.getLastStreakDate();

        if (lastStreakDate != null && lastStreakDate.equals(today)) {
            log.debug("User {} already has streak for today", today);
            return false;
        }

        // Lần đầu tiên hoặc streak đã reset
        if (lastStreakDate == null) {
            user.setStreakDays(1);
            user.setLastStreakDate(today);
            userRepository.save(user);
            log.info("User {} started new streak", userId);
            return true;
        }

        // Check nếu user hoạt động ngày hôm qua
        if (lastStreakDate.equals(today.minusDays(1))) {
            // Tăng Streak
            user.setStreakDays(user.getStreakDays() + 1);
            user.setLastStreakDate(today);
            userRepository.save(user);
            log.info("User {} streak increased to {}", userId, user.getStreakDays());
            return true;
        }

        // Streak bị break
        user.setStreakDays(1);
        user.setLastStreakDate(today);
        userRepository.save(user);
        log.info("User {} streak bị gián đoạn, đưa về 1", userId);
        return true;
    }

    // Kiểm tra xem user có streak của hôm nay chưa
    public boolean hasStreakToday(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate today = LocalDate.now();
        return user.getLastStreakDate() != null &&
                user.getLastStreakDate().equals(today);
    }

    // Lấy thông tin streak của user
    public StreakInfo getStreakInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate today = LocalDate.now();
        boolean hasStreakToday = user.getLastStreakDate() != null &&
                user.getLastStreakDate().equals(today);

        return StreakInfo.builder()
                .currentStreak(user.getStreakDays())
                .lastStreakDate(user.getLastStreakDate())
                .hasStreakToday(hasStreakToday)
                .build();
    }

    @Data
    @Builder
    public static class StreakInfo {
        private Integer currentStreak;
        private LocalDate lastStreakDate;
        private boolean hasStreakToday;
    }
}
