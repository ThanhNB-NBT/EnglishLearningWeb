package com.thanhnb.englishlearning.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AudioStorageProperties audioProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            // ✅ Get absolute path from configured upload dir
            Path uploadPath = Paths.get(audioProperties.getUploadDir()).toAbsolutePath().normalize();

            log.info("🔍 RAW CONFIG: {}", audioProperties.getUploadDir());
            log.info("🔍 RESOLVED TO: {}", uploadPath);
            log.info("🔍 EXISTS? {}", Files.exists(uploadPath));

            // ✅ Get parent directory (media root)
            Path mediaRoot = uploadPath.getParent();

            if (mediaRoot == null) {
                log.error("❌ Cannot determine media root directory from: {}", uploadPath);
                mediaRoot = uploadPath; // Fallback
            }

            // ✅ FIX 2: Build correct file:// URL
            // Windows: file:///C:/app/media/
            // Linux: file:///app/media/
            String mediaLocation = mediaRoot.toUri().toString();

            // Ensure trailing slash
            if (!mediaLocation.endsWith("/")) {
                mediaLocation += "/";
            }

            // ✅ FIX 3: Map /media/** (not /media/listening/**)
            // This allows /media/listening/..., /media/reading/..., etc.
            registry.addResourceHandler("/media/**")
                    .addResourceLocations(mediaLocation)
                    .setCachePeriod(3600)
                    .resourceChain(true);

            // ✅ List actual files in directory
            if (Files.exists(uploadPath)) {
                log.info("📁 Files in {}:", uploadPath);
                try (var stream = Files.list(uploadPath)) {
                    stream.limit(5).forEach(p -> log.info("   - {}", p.getFileName()));
                } catch (Exception e) {
                    log.error("Failed to list files", e);
                }
            } else {
                log.error("❌ Upload directory does NOT exist: {}", uploadPath);
            }

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("✅ Static Resource Handler Configured");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("   URL Pattern:        /media/**");
            log.info("   Physical Location:  {}", mediaLocation);
            log.info("   Upload Directory:   {}", uploadPath);
            log.info("   Media Root:         {}", mediaRoot);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("   Example URLs:");
            log.info("   → /media/listening/lesson_1/audio.mp3");
            log.info("   → Maps to: {}",
                    new File(mediaRoot.toFile(), "listening/lesson_1/audio.mp3").getAbsolutePath());
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("❌ Failed to configure static resource handler", e);
            throw new RuntimeException("Static resource configuration failed", e);
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // ✅ CRITICAL: CORS for audio streaming
        registry.addMapping("/media/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000", "http://localhost:8980")
                .allowedMethods("GET", "HEAD", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Accept-Ranges", "Content-Range", "Content-Length", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}