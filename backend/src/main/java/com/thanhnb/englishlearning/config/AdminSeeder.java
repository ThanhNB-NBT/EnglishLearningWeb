package com.thanhnb.englishlearning.config;

import com.thanhnb.englishlearning.service.user.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seed initial admin account when application starts
 * Chạy 1 lần khi khởi động, tạo admin account nếu chưa có
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder {

    private final AuthService authService;

    @Bean
    public CommandLineRunner seedAdminAccount() {
        return args -> {
            try {
                // Tạo admin account mặc định
                // QUAN TRỌNG: Đổi password này sau khi deploy production!
                authService.createAdminAccount(
                    "admin",
                    "admin@englishlearning.com",
                    "Admin@123456", // PHẢI ĐỔI PASSWORD NÀY!
                    "System Administrator"
                );
                
                log.info("=".repeat(60));
                log.info("✓ Admin account created successfully!");
                log.info("  Username: admin");
                log.info("  Email: admin@englishlearning.com");
                log.warn("  IMPORTANT: Change default password after first login!");
                log.info("=".repeat(60));
                
            } catch (Exception e) {
                if (e.getMessage().contains("đã tồn tại")) {
                    log.info("Admin account already exists, skipping seed...");
                } else {
                    log.error("Failed to seed admin account: {}", e.getMessage());
                }
            }
        };
    }
}
