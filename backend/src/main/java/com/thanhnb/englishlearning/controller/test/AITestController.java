package com.thanhnb.englishlearning.controller.test;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.service.ai.provider.AIServiceRouter;
import com.thanhnb.englishlearning.service.ai.provider.GroqService;
import com.thanhnb.englishlearning.service.ai.provider.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * ✅ AI Service Test Controller
 * Test endpoints for Groq and Gemini AI services
 */
@RestController
@RequestMapping("/api/test/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Test", description = "Test AI services (Groq & Gemini)")
public class AITestController {

    private final GroqService groqService;
    private final GeminiService geminiService;
    private final AIServiceRouter aiRouter;

    // ==================== REQUEST DTOs ====================

    @Data
    public static class AITestRequest {
        @NotBlank(message = "Prompt is required")
        private String prompt;
    }

    // ==================== RESPONSE DTOs ====================

    @Data
    public static class AITestResponse {
        private String provider;
        private String model;
        private String response;
        private Long processingTimeMs;
    }

    @Data
    public static class HealthCheckResponse {
        private String groqStatus;
        private String geminiStatus;
        private String preferredProvider;
        private String fallbackProvider;
    }

    // ==================== ENDPOINTS ====================

    /**
     * Test Groq API
     */
    @PostMapping("/groq")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(
        summary = "Test Groq AI Service",
        description = "Send a prompt to Groq (Llama 3.1 70B) and get response"
    )
    public ResponseEntity<CustomApiResponse<AITestResponse>> testGroq(
            @Valid @RequestBody AITestRequest request) {
        
        log.info("Testing Groq with prompt: {}", request.getPrompt().substring(0, 
            Math.min(50, request.getPrompt().length())));

        try {
            long startTime = System.currentTimeMillis();
            String aiResponse = groqService.generate(request.getPrompt());
            long duration = System.currentTimeMillis() - startTime;

            AITestResponse response = new AITestResponse();
            response.setProvider("Groq");
            response.setModel("llama-3.1-70b-versatile");
            response.setResponse(aiResponse);
            response.setProcessingTimeMs(duration);

            log.info("Groq response received in {}ms", duration);
            return ResponseEntity.ok(CustomApiResponse.success(response, "Groq API test successful"));

        } catch (Exception e) {
            log.error("Groq test failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Groq API error: " + e.getMessage()));
        }
    }

    /**
     * Test Gemini API
     */
    @PostMapping("/gemini")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(
        summary = "Test Gemini AI Service",
        description = "Send a prompt to Gemini (2.0 Flash) and get response"
    )
    public ResponseEntity<CustomApiResponse<AITestResponse>> testGemini(
            @Valid @RequestBody AITestRequest request) {
        
        log.info("Testing Gemini with prompt: {}", request.getPrompt().substring(0, 
            Math.min(50, request.getPrompt().length())));

        try {
            long startTime = System.currentTimeMillis();
            String aiResponse = geminiService.generate(request.getPrompt());
            long duration = System.currentTimeMillis() - startTime;

            AITestResponse response = new AITestResponse();
            response.setProvider("Gemini");
            response.setModel("gemini-2.0-flash-exp");
            response.setResponse(aiResponse);
            response.setProcessingTimeMs(duration);

            log.info("Gemini response received in {}ms", duration);
            return ResponseEntity.ok(CustomApiResponse.success(response, "Gemini API test successful"));

        } catch (Exception e) {
            log.error("Gemini test failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Gemini API error: " + e.getMessage()));
        }
    }

    /**
     * Test AI Router (with auto-fallback)
     */
    @PostMapping("/router")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(
        summary = "Test AI Service Router",
        description = "Test router with automatic fallback. Uses config to decide primary provider."
    )
    public ResponseEntity<CustomApiResponse<AITestResponse>> testRouter(
            @Valid @RequestBody AITestRequest request) {
        
        log.info("Testing Router with prompt: {}", request.getPrompt().substring(0, 
            Math.min(50, request.getPrompt().length())));

        try {
            long startTime = System.currentTimeMillis();
            String aiResponse = aiRouter.generateForRecommendation(request.getPrompt());
            long duration = System.currentTimeMillis() - startTime;

            AITestResponse response = new AITestResponse();
            response.setProvider("Router (auto-fallback)");
            response.setModel("Based on config");
            response.setResponse(aiResponse);
            response.setProcessingTimeMs(duration);

            log.info("Router response received in {}ms", duration);
            return ResponseEntity.ok(CustomApiResponse.success(response, "Router test successful"));

        } catch (Exception e) {
            log.error("Router test failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Router error: " + e.getMessage()));
        }
    }

    /**
     * Test JSON mode
     */
    @PostMapping("/json")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(
        summary = "Test JSON generation",
        description = "Test AI JSON mode. Prompt should request structured output."
    )
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> testJSON(
            @Valid @RequestBody AITestRequest request) {
        
        log.info("Testing JSON mode with prompt");

        try {
            String jsonResponse = aiRouter.generateForRecommendation(request.getPrompt());
            
            // Try to parse as JSON to validate
            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = mapper.readValue(jsonResponse, Map.class);

            log.info("JSON test successful, parsed {} keys", parsed.size());
            return ResponseEntity.ok(CustomApiResponse.success(parsed, "JSON generation successful"));

        } catch (Exception e) {
            log.error("JSON test failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("JSON generation error: " + e.getMessage()));
        }
    }

    /**
     * Health check for AI services
     */
    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(
        summary = "Check AI services health",
        description = "Check availability of Groq and Gemini services"
    )
    public ResponseEntity<CustomApiResponse<HealthCheckResponse>> healthCheck() {
        log.info("Performing AI services health check");

        HealthCheckResponse health = new HealthCheckResponse();
        
        // Test Groq
        try {
            groqService.generate("test");
            health.setGroqStatus("✅ OK");
        } catch (Exception e) {
            health.setGroqStatus("❌ FAILED: " + e.getMessage());
        }

        // Test Gemini
        try {
            geminiService.generate("test");
            health.setGeminiStatus("✅ OK");
        } catch (Exception e) {
            health.setGeminiStatus("❌ FAILED: " + e.getMessage());
        }

        health.setPreferredProvider("Groq (for recommendations/analysis)");
        health.setFallbackProvider("Gemini");

        return ResponseEntity.ok(CustomApiResponse.success(health, "Health check completed"));
    }

    /**
     * Quick test endpoint (no auth for dev)
     */
    @GetMapping("/ping")
    @Operation(
        summary = "Quick ping test",
        description = "Simple endpoint to verify controller is working"
    )
    public ResponseEntity<CustomApiResponse<String>> ping() {
        return ResponseEntity.ok(CustomApiResponse.success(
            "AI Test Controller is running", 
            "Pong!"
        ));
    }
}