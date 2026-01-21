package com.thanhnb.englishlearning.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AIConfigValidator {

    private final AIConfig aiConfig;

    @Bean
    public CommandLineRunner validateAIConfig() {
        return args -> {
            log.info("Validating AI Configuration...");
            
            try {
                aiConfig.validate();
                
                log.info("✅ AI Config validated successfully:");
                log.info("  - Gemini Model: {}", aiConfig.getGemini().getModel());
                log.info("  - Groq Model: {}", aiConfig.getGroq().getModel());
                log.info("  - Recommendation Provider: {}", aiConfig.getProvider().getRecommendation());
                log.info("  - Analysis Provider: {}", aiConfig.getProvider().getAnalysis());
                
            } catch (Exception e) {
                log.error("❌ AI Config validation failed: {}", e.getMessage());
                throw e;
            }
        };
    }
}