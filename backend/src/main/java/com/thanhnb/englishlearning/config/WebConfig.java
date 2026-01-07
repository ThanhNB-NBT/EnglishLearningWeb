package com.thanhnb.englishlearning.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AudioStorageProperties audioProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Lấy đường dẫn tuyệt đối của thư mục upload từ cấu hình
        // Ví dụ: /app/media/listening hoặc C:/uploads/listening
        Path uploadPath = Paths.get(audioProperties.getUploadDir()).toAbsolutePath().normalize();
        
        // 2. Tạo chuỗi location cho ResourceHandler (thêm 'file:///' và dấu '/' cuối)
        String uploadLocation = "file:///" + uploadPath.toString() + "/";

        // 3. Map URL "/media/listening/**" -> Thư mục vật lý đó
        registry.addResourceHandler("/media/listening/**")
                .addResourceLocations(uploadLocation)
                .setCachePeriod(3600); // Cache 1 giờ cho client

        log.info("✅ Static Resource Mapping Configured:");
        log.info("   URL Pattern:   /media/listening/**");
        log.info("   Physical Path: {}", uploadLocation);
        log.info("   Upload Dir:    {}", uploadPath);
    }
}