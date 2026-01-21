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
 * ✅ IMPROVED: Gemini Service with retry logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final AIConfig aiConfig;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public String generate(String prompt) throws Exception {
        return generateWithRetry(prompt, false);
    }

    public String generateJSON(String prompt) throws Exception {
        return generateWithRetry(prompt, true);
    }

    /**
     * ✅ NEW: Retry logic for Gemini
     */
    private String generateWithRetry(String prompt, boolean jsonMode) throws Exception {
        int maxRetries = 3;
        int retryCount = 0;
        long waitTime = 3000; // Start with 3 seconds

        while (true) {
            try {
                return callGeminiAPI(prompt, jsonMode);

            } catch (Exception e) {
                retryCount++;

                if (retryCount > maxRetries) {
                    log.error("Gemini API failed after {} retries", maxRetries);
                    throw new Exception("Gemini API exhausted all retries", e);
                }

                String errorMsg = e.getMessage().toLowerCase();

                // Retry on server errors (500, 503) or rate limit (429)
                if (errorMsg.contains("503") || errorMsg.contains("overloaded") ||
                        errorMsg.contains("429") || errorMsg.contains("500")) {

                    log.warn("Gemini server error/overload, retry {}/{} after {}ms",
                            retryCount, maxRetries, waitTime);

                    Thread.sleep(waitTime);
                    waitTime *= 2; // Exponential backoff
                } else {
                    // Non-retryable error
                    throw e;
                }
            }
        }
    }

    private String callGeminiAPI(String prompt, boolean jsonMode) throws Exception {
        String url = String.format(
                "https://generativelanguage.googleapis.com/%s/models/%s:generateContent?key=%s",
                aiConfig.getGemini().getVersion(),
                aiConfig.getGemini().getModel(),
                aiConfig.getGemini().getApiKey());

        Map<String, Object> requestBody = new HashMap<>();

        // Content
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);
        requestBody.put("contents", Collections.singletonList(content));

        // Config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", aiConfig.getGemini().getTemperature());
        generationConfig.put("maxOutputTokens", 8192);
        if (jsonMode) {
            generationConfig.put("response_mime_type", "application/json");
        }
        requestBody.put("generationConfig", generationConfig);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        log.debug("Calling Gemini API with model: {}", aiConfig.getGemini().getModel());
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Gemini API error (" + response.statusCode() + "): "
                    + response.body());
        }

        return extractTextFromResponse(response.body());
    }

    private String extractTextFromResponse(String responseBody) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("candidates")
                    .get(0).path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            text = text.trim();
            if (text.startsWith("```json"))
                text = text.substring(7);
            if (text.startsWith("```"))
                text = text.substring(3);
            if (text.endsWith("```"))
                text = text.substring(0, text.length() - 3);

            return text.trim();

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", responseBody);
            throw new Exception("Invalid Gemini response format", e);
        }
    }
}