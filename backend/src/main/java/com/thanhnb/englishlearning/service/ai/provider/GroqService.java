package com.thanhnb.englishlearning.service.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.config.AIConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * ✅ FIXED: Groq AI Service with proper JSON mode handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroqService {

    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    /**
     * Generate text response
     */
    public String generate(String prompt) throws Exception {
        return generateWithRetry(prompt, false);
    }

    /**
     * Generate JSON response
     */
    public String generateJSON(String prompt) throws Exception {
        return generateWithRetry(prompt, true);
    }

    /**
     * Main generation with retry
     */
    private String generateWithRetry(String prompt, boolean jsonMode) throws Exception {
        int maxRetries = 3;
        int retryCount = 0;
        long waitTime = 2000;

        while (true) {
            try {
                return callGroqAPI(prompt, jsonMode);
                
            } catch (Exception e) {
                retryCount++;
                
                if (retryCount > maxRetries) {
                    log.error("Groq API failed after {} retries", maxRetries);
                    throw new Exception("Groq API exhausted all retries", e);
                }

                String errorMsg = e.getMessage().toLowerCase();
                if (errorMsg.contains("429") || errorMsg.contains("5")) {
                    log.warn("Groq rate limit/error, retry {}/{} after {}ms", 
                        retryCount, maxRetries, waitTime);
                    Thread.sleep(waitTime);
                    waitTime *= 2;
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * ✅ FIXED: Call Groq API with proper JSON mode
     */
    private String callGroqAPI(String prompt, boolean jsonMode) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiConfig.getGroq().getModel());
        requestBody.put("temperature", aiConfig.getGroq().getTemperature());
        requestBody.put("max_tokens", aiConfig.getGroq().getMaxTokens());

        // ✅ FIX: Add "json" keyword to prompt when using JSON mode
        String finalPrompt = prompt;
        if (jsonMode) {
            // Groq requires the word "json" in the prompt when using json_object mode
            if (!prompt.toLowerCase().contains("json")) {
                finalPrompt = "Return your response as valid JSON format.\n\n" + prompt;
            }
            requestBody.put("response_format", Map.of("type", "json_object"));
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", finalPrompt));
        requestBody.put("messages", messages);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiConfig.getGroq().getApiKey())
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        log.debug("Calling Groq API with model: {} (JSON mode: {})", 
            aiConfig.getGroq().getModel(), jsonMode);
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Groq API error (" + response.statusCode() + "): " 
                + response.body());
        }

        return extractTextFromResponse(response.body());
    }

    /**
     * Extract text from Groq response
     */
    private String extractTextFromResponse(String responseBody) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            if (content.isEmpty()) {
                throw new Exception("Empty response from Groq");
            }

            return content.trim();

        } catch (Exception e) {
            log.error("Failed to parse Groq response: {}", responseBody);
            throw new Exception("Invalid Groq response format", e);
        }
    }

    /**
     * Health check
     */
    public boolean isAvailable() {
        try {
            generate("test");
            return true;
        } catch (Exception e) {
            log.error("Groq health check failed: {}", e.getMessage());
            return false;
        }
    }
}