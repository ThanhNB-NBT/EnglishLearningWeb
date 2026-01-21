package com.thanhnb.englishlearning.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ✅ AI Configuration - Optimized for English Learning App
 * Maps from application.properties: ai.*
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AIConfig {

    private GeminiConfig gemini = new GeminiConfig();
    private GroqConfig groq = new GroqConfig();
    private ProviderConfig provider = new ProviderConfig();

    /**
     * Gemini API Configuration
     * Best for: PDF parsing, multimodal tasks
     */
    @Data
    public static class GeminiConfig {
        private String apiKey;
        private String model = "gemini-2.5-flash"; // ✅ Updated to latest stable
        private String version = "v1beta";
        private Integer maxTokens = 32000; // ✅ Increased for better responses
        private Double temperature = 0.3; // ✅ Lower for more consistent parsing
        private Integer rateLimit = 1500;
    }

    /**
     * Groq API Configuration
     * Best for: Fast text generation, writing assessment, recommendations
     */
    @Data
    public static class GroqConfig {
        private String apiKey;
        private String model = "llama-3.3-70b-versatile"; // ✅ Best balanced model
        private Integer maxTokens = 8000; // ✅ Max available
        private Double temperature = 0.7; // ✅ Good for creative assessment
        private Integer rateLimit = 14400;
    }

    /**
     * Provider Selection for different features
     */
    @Data
    public static class ProviderConfig {
        private String pdfParser = "gemini"; // Gemini for multimodal
        private String writingAssessment = "groq"; // Groq for speed + quality
        private String recommendation = "groq"; // Groq for fast recommendations
        private String analysis = "groq"; // Groq for analysis
        private String fallback = "gemini"; // Gemini as backup
    }

    /**
     * Validation helper
     */
    public void validate() {
        if (gemini.getApiKey() == null || gemini.getApiKey().isBlank()) {
            throw new IllegalStateException("AI Gemini API Key is required");
        }
        if (groq.getApiKey() == null || groq.getApiKey().isBlank()) {
            throw new IllegalStateException("AI Groq API Key is required");
        }

        // Validate model names
        if (!isValidGroqModel(groq.getModel())) {
            throw new IllegalStateException("Invalid Groq model: " + groq.getModel());
        }
    }

    /**
     * Check if Groq model is valid
     */
    private boolean isValidGroqModel(String model) {
        return model != null && (model.equals("llama-3.3-70b-versatile") ||
                model.equals("llama-3.1-8b-instant") ||
                model.startsWith("groq/compound"));
    }
}