package com.thanhnb.englishlearning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {
     // SET DEFAULT VALUES
    private String secret = "myVerySecretKeyForJWTTokenGenerationThatShouldBeLongEnough";
    private long expiration = 86400000L; // 24 hours
    
    // THÊM DEBUG
    public String getSecret() {
        System.out.println("Getting JWT Secret: " + (secret != null ? "PRESENT" : "NULL"));
        return secret;
    }
}
