package com.thanhnb.englishlearning.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for audio storage
 * Maps app.audio.* properties from application.yml/properties
 */
@Configuration
@ConfigurationProperties(prefix = "app.audio")
@Data
public class AudioStorageProperties {
    
    /**
     * Directory for uploading audio files
     * Default: /app/media/listening
     */
    private String uploadDir = "/app/media/listening";
    
    /**
     * Maximum file size in bytes
     * Default: 52428800 (50MB)
     */
    private Long maxFileSize = 52428800L;
}