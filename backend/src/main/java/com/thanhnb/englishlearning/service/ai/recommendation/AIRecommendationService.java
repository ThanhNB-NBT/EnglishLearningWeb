package com.thanhnb.englishlearning.service.ai.recommendation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.config.AIConfig;
import com.thanhnb.englishlearning.entity.recommendation.AIRecommendation;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.entity.user.UserLearningBehavior;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.repository.recommendation.AIRecommendationRepository;
import com.thanhnb.englishlearning.repository.user.UserLearningBehaviorRepository;
import com.thanhnb.englishlearning.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIRecommendationService {

    private final UserLearningBehaviorRepository behaviorRepository;
    private final AIRecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AIConfig aiConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @lombok.Data
    private static class AIRecommendationResponse {
        private String type;
        private String title;
        private String description;
        private String reasoning;
        private String targetSkill;
        private Integer priority;
    }

    // ✅ FIX: Retry logic cho lỗi Quota (429) và Server (5xx)
    @Retryable(retryFor = { HttpClientErrorException.TooManyRequests.class,
            HttpServerErrorException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<AIRecommendation> generateRecommendations(Long userId) {
        // 1. Check Cache
        List<AIRecommendation> cached = recommendationRepository
                .findByUserIdAndExpiresAtAfterAndIsCompletedFalseOrderByPriorityDesc(userId, LocalDateTime.now());
        if (!cached.isEmpty())
            return cached;

        // 2. Get User Behavior
        UserLearningBehavior behavior = behaviorRepository.findByUserId(userId).orElse(null);
        if (behavior == null)
            return Collections.emptyList();

        User user = userRepository.findById(userId).orElseThrow();

        // 3. Call AI
        String prompt = buildPrompt(user, behavior);
        String aiJson = callGemini(prompt);

        // 4. Parse & Save
        return parseAndSaveRecommendations(user, aiJson);
    }

    // ✅ FIX: Fallback method - Trả về rỗng nếu hết quota thật sự
    @Recover
    public List<AIRecommendation> recoverFromGeminiError(Exception e, Long userId) {
        log.warn("Gemini API exhausted/failed for user {}: {}. Returning empty list.", userId, e.getMessage());
        return Collections.emptyList();
    }

    private String buildPrompt(User user, UserLearningBehavior behavior) {
        try {
            return """
                    Bạn là AI Mentor. Phân tích profile:
                    - Level: %s
                    - Skill stats: %s
                    - Weakest: %s
                    - Avg attempts: %.1f

                    Trả về JSON Array 3 gợi ý học tập (JSON thuần, không markdown):
                    [{"type": "...", "title": "...", "description": "...", "reasoning": "...", "targetSkill": "...", "priority": 1-5}]
                    """
                    .formatted(
                            user.getEnglishLevel(),
                            objectMapper.writeValueAsString(behavior.getSkillStats()),
                            behavior.getWeakestSkill(),
                            behavior.getAvgAttemptsPerLesson());
        } catch (Exception e) {
            log.error("Prompt error", e);
            return "";
        }
    }

    private String callGemini(String prompt) {
        try {
            String apiKey = aiConfig.getGemini().getApiKey();
            String model = aiConfig.getGemini().getModel();
            String url = String.format(GEMINI_URL_TEMPLATE, model, apiKey);

            Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
            return extractTextFromGeminiResponse(response);

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Gemini Rate Limit. Retrying...");
            throw e; // Ném lỗi để @Retryable bắt
        } catch (HttpServerErrorException e) {
            log.warn("Gemini Server Error. Retrying...");
            throw e;
        } catch (Exception e) {
            log.error("Gemini API Call Failed: {}", e.getMessage());
            return "[]";
        }
    }

    private List<AIRecommendation> parseAndSaveRecommendations(User user, String json) {
        try {
            if (json == null || json.equals("[]") || json.isEmpty())
                return Collections.emptyList();
            List<AIRecommendationResponse> responses = objectMapper.readValue(json, new TypeReference<>() {
            });

            List<AIRecommendation> entities = responses.stream().map(dto -> AIRecommendation.builder()
                    .user(user)
                    .type(dto.getType())
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .reasoning(dto.getReasoning())
                    .targetSkill(mapSkill(dto.getTargetSkill()))
                    .priority(dto.getPriority())
                    .isShown(false).isCompleted(false)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build()).toList();

            return recommendationRepository.saveAll(entities);
        } catch (Exception e) {
            log.error("Parse error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromGeminiResponse(Map<String, Object> response) {
        if (response != null && response.containsKey("candidates")) {
            var candidates = (List<Map<String, Object>>) response.get("candidates");
            if (!candidates.isEmpty()) {
                var content = (Map<String, Object>) candidates.get(0).get("content");
                var parts = (List<Map<String, Object>>) content.get("parts");
                if (!parts.isEmpty()) {
                    String text = (String) parts.get(0).get("text");
                    return text.replace("```json", "").replace("```", "").trim();
                }
            }
        }
        return "[]";
    }

    private ModuleType mapSkill(String skill) {
        try {
            return skill != null ? ModuleType.valueOf(skill.toUpperCase()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}