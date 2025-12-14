package com.thanhnb.englishlearning.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AudioStorageProperties audioProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get upload directory from properties
        String uploadDir = audioProperties.getUploadDir(); // "/app/media/listening"
        
        // Get parent directory: /app/media/
        File uploadDirFile = new File(uploadDir);
        File mediaRootDir = uploadDirFile.getParentFile(); // "/app/media"
        
        if (mediaRootDir == null) {
            log.error("Cannot determine media root directory from: {}", uploadDir);
            return;
        }
        
        // Ensure directory exists
        if (!mediaRootDir.exists()) {
            boolean created = mediaRootDir.mkdirs();
            log.info("Created media root directory: {} (success: {})", 
                    mediaRootDir.getAbsolutePath(), created);
        }
        
        // Build file URL: file:/app/media/
        String fileUrl = "file:" + mediaRootDir.getAbsolutePath() + "/";
        
        // Register resource handler
        // URL pattern: /media/** → file:/app/media/
        registry.addResourceHandler("/media/**")
                .addResourceLocations(fileUrl)
                .setCachePeriod(3600) // Cache 1 hour
                .resourceChain(true);
        
        log.info("Static media resources configured:");
        log.info("   URL Pattern: /media/**");
        log.info("   File Location: {}", fileUrl);
        log.info("   Directory exists: {}", mediaRootDir.exists());
        log.info("   Directory readable: {}", mediaRootDir.canRead());
        log.info("   Example mapping: /media/listening/lesson_1/audio.mp3 → {}/listening/lesson_1/audio.mp3", 
                mediaRootDir.getAbsolutePath());
    }
}