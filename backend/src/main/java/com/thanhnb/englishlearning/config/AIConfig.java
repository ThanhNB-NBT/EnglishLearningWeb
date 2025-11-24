package com.thanhnb.englishlearning.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ✅ Configuration class cho AI API Keys
 * Đọc từ application.properties và validate
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AIConfig {
    
    private GeminiConfig gemini;
    private GroqConfig groq;
    private ProviderConfig provider;
    
    @Data
    public static class GeminiConfig {
        private String apiKey;
        private String model = "gemini-2.0-flash";
        private String version = "v1beta";
        private Integer maxTokens = 16000;
        private Double temperature = 0.4;
    }
    
    @Data
    public static class GroqConfig {
        private String apiKey;
        private String model = "llama-3.1-8b-instant";
        private Integer maxTokens = 4096;
    }
    
    @Data
    public static class ProviderConfig {
        private String pdfParser = "gemini";
        private String writingAssessment = "groq";
    }
}