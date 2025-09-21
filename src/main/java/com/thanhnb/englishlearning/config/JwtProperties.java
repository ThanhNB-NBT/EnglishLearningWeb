package com.thanhnb.englishlearning.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties cho JWT
 * Map từ application.properties với prefix "jwt"
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    /**
     * Secret key để sign JWT token
     * Tối thiểu 32 characters
     * Mapping từ: jwt.secret trong application.properties
     */
    private String secret;
    
    /**
     * Thời gian expire của JWT token (milliseconds)
     * Mặc định: 86400000 = 24 giờ
     * Mapping từ: jwt.expiration trong application.properties
     */
    private long expiration = 86400000L;
}