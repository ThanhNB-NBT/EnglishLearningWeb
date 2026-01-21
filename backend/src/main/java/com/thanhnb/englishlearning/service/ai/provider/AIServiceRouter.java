package com.thanhnb.englishlearning.service.ai.provider;

import com.thanhnb.englishlearning.config.AIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ✅ IMPROVED: AI Service Router with detailed logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceRouter {

    private final GroqService groqService;
    private final GeminiService geminiService;
    private final AIConfig aiConfig;

    public enum AIProvider {
        GROQ, GEMINI
    }

    public String generateForRecommendation(String prompt) throws Exception {
        AIProvider provider = getProvider(aiConfig.getProvider().getRecommendation());
        return generateJSON(prompt, provider);
    }

    public String generateForAnalysis(String prompt) throws Exception {
        AIProvider provider = getProvider(aiConfig.getProvider().getAnalysis());
        log.info("🎯 Router: Using {} for analysis", provider);
        return generateJSON(prompt, provider);
    }

    public String generateJSON(String prompt, AIProvider preferredProvider) throws Exception {
        log.info("📡 Starting JSON generation with preferred provider: {}", preferredProvider);
        
        if (preferredProvider == AIProvider.GROQ) {
            try {
                log.info("🚀 Attempting with Groq...");
                long startTime = System.currentTimeMillis();
                String result = groqService.generateJSON(prompt);
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ Groq succeeded in {}ms", duration);
                return result;
            } catch (Exception e) {
                log.warn("❌ Groq failed: {}", e.getMessage());
                log.info("🔄 Falling back to Gemini...");
                
                try {
                    long startTime = System.currentTimeMillis();
                    String result = geminiService.generateJSON(prompt);
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("✅ Gemini (fallback) succeeded in {}ms", duration);
                    return result;
                } catch (Exception geminiError) {
                    log.error("❌ Gemini also failed: {}", geminiError.getMessage());
                    throw geminiError;
                }
            }
        } else {
            try {
                log.info("🚀 Attempting with Gemini...");
                long startTime = System.currentTimeMillis();
                String result = geminiService.generateJSON(prompt);
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ Gemini succeeded in {}ms", duration);
                return result;
            } catch (Exception e) {
                log.warn("❌ Gemini failed: {}", e.getMessage());
                log.info("🔄 Falling back to Groq...");
                
                try {
                    long startTime = System.currentTimeMillis();
                    String result = groqService.generateJSON(prompt);
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("✅ Groq (fallback) succeeded in {}ms", duration);
                    return result;
                } catch (Exception groqError) {
                    log.error("❌ Groq also failed: {}", groqError.getMessage());
                    throw groqError;
                }
            }
        }
    }

    public String generate(String prompt, AIProvider preferredProvider) throws Exception {
        if (preferredProvider == AIProvider.GROQ) {
            try {
                return groqService.generate(prompt);
            } catch (Exception e) {
                log.warn("Groq failed, falling back to Gemini");
                return geminiService.generate(prompt);
            }
        } else {
            try {
                return geminiService.generate(prompt);
            } catch (Exception e) {
                log.warn("Gemini failed, falling back to Groq");
                return groqService.generate(prompt);
            }
        }
    }

    private AIProvider getProvider(String providerName) {
        return "groq".equalsIgnoreCase(providerName) 
            ? AIProvider.GROQ 
            : AIProvider.GEMINI;
    }
}