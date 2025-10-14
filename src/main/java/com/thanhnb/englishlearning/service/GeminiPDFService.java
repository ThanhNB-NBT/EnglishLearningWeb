package com.thanhnb.englishlearning.service;

import com.thanhnb.englishlearning.config.AIConfig;
import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.dto.grammar.GrammarQuestionDTO;
import com.thanhnb.englishlearning.dto.ParseResult;
import com.thanhnb.englishlearning.enums.LessonType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiPDFService {

    private final AIConfig aiConfig;
    
    // ✅ Gson với TypeAdapter cho LocalDateTime
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, 
                (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
                    context.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class,
                (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * Parse PDF thành danh sách lessons với retry mechanism
     */
    public ParseResult parsePDF(MultipartFile file, Long topicId) throws Exception {
        log.info("🔍 Starting PDF parsing for file: {} (size: {} KB)", 
                file.getOriginalFilename(), file.getSize() / 1024);

        // Validate file
        validateFile(file);

        // Convert to base64
        String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType();

        // Build prompt
        String prompt = buildPrompt();

        // Call Gemini API with retry
        String jsonResponse = callGeminiAPIWithRetry(prompt, base64Data, mimeType, 3);

        // Parse result
        ParseResult result = parseGeminiResponse(jsonResponse);

        // Post-processing
        result = postProcessResult(result, topicId);

        log.info("✅ PDF parsing completed: {} lessons, {} total questions", 
                result.lessons.size(), 
                result.lessons.stream()
                    .filter(l -> l.getQuestions() != null)
                    .mapToInt(l -> l.getQuestions().size())
                    .sum());

        return result;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String contentType = file.getContentType();
        if (contentType == null || 
            (!contentType.equals("application/pdf") && 
             !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file PDF và DOCX");
        }

        if (file.getSize() > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("File không được vượt quá 20MB");
        }
    }

    private String buildPrompt() {
        return """
                🤖 BẠN LÀ CHUYÊN GIA PHÂN TÍCH TÀI LIỆU HỌC TIẾNG ANH.

                📋 NHIỆM VỤ: Đọc file và TỰ ĐỘNG CHIA THÀNH CÁC BÀI HỌC (LESSONS) RIÊNG BIỆT.

                ✅ QUY TẮC PHÂN CHIA:

                1️⃣ MỖI CHỦ ĐỀ NGỮ PHÁP = 1 LESSON LÀ THUYẾT (THEORY)
                   - Ví dụ: "HIỆN TẠI ĐƠN – PRESENT SIMPLE"
                   - Bao gồm: Khái niệm, cấu trúc, công thức, ví dụ, dấu hiệu nhận biết
                   - Content: Markdown format với \\n cho xuống dòng

                2️⃣ MỖI PHẦN BÀI TẬP = 1 LESSON THỰC HÀNH (PRACTICE)
                   - Ví dụ: "BÀI TẬP LUYỆN TẬP - PRESENT SIMPLE"
                   - Kết hợp "ĐỀ BÀI" + "ĐÁP ÁN" + "LỜI GIẢI" thành questions hoàn chỉnh
                   - Mỗi câu hỏi cần: questionText + correctAnswer + explanation

                3️⃣ Lọc Bỏ:
                   - Header/footer lặp lại (Hotline, Website, Fanpage, Page number)
                   - Trang bìa, mục lục
                   - Quảng cáo, watermark

                📝 FORMAT JSON CHUẨN:

                {
                  "lessons": [
                    {
                      "title": "Hiện tại đơn - Present Simple",
                      "lessonType": "THEORY",
                      "content": "# I. KHÁI NIỆM\\n\\nThì hiện tại đơn dùng để...\\n\\n## II. CẤU TRÚC\\n\\n**Khẳng định:** S + V(s/es)\\n**Phủ định:** S + don't/doesn't + V\\n**Nghi vấn:** Do/Does + S + V?",
                      "orderIndex": 1,
                      "pointsReward": 10,
                      "estimatedDuration": 180,
                      "isActive": true
                    },
                    {
                      "title": "Bài tập - Hiện tại đơn",
                      "lessonType": "PRACTICE",
                      "content": "",
                      "orderIndex": 2,
                      "pointsReward": 15,
                      "estimatedDuration": 300,
                      "isActive": true,
                      "questions": [
                        {
                          "questionText": "My father always _____ Sunday dinner. (make)",
                          "questionType": "FILL_BLANK",
                          "correctAnswer": "makes",
                          "explanation": "Dấu hiệu: 'always' (trạng từ tần suất) → Thì hiện tại đơn. Chủ ngữ 'father' (ngôi 3 số ít) → Động từ thêm 's'",
                          "points": 5,
                          "orderIndex": 1
                        }
                      ]
                    }
                  ]
                }

                🔑 CHI TIẾT QUAN TRỌNG:

                **lessonType:** CHỈ "THEORY" hoặc "PRACTICE"
                **questionType:** "FILL_BLANK", "MULTIPLE_CHOICE", "TRANSLATE", "VERB_FORM", "TRUE_FALSE"
                **pointsReward:** THEORY: 10, PRACTICE: 15
                **estimatedDuration:** THEORY: 180, PRACTICE: 300
                **correctAnswer:** Nhiều đáp án đúng: Ngăn cách bởi "|"

                ⚠️ CHỈ TRẢ VỀ JSON HỢP LỆ, KHÔNG TEXT THỪA, KHÔNG MARKDOWN WRAPPER!
                """;
    }

    private String callGeminiAPIWithRetry(String prompt, String base64Data, String mimeType, int maxRetries) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("🌐 Calling Gemini API (attempt {}/{})", attempt, maxRetries);
                return callGeminiAPI(prompt, base64Data, mimeType);
            } catch (Exception e) {
                lastException = e;
                log.warn("⚠️ Gemini API call failed (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
                
                if (attempt < maxRetries) {
                    long waitTime = (long) Math.pow(2, attempt) * 6000;
                    log.info("⏳ Retrying after {}ms...", waitTime);
                    Thread.sleep(waitTime);
                }
            }
        }

        throw new Exception("Gemini API failed after " + maxRetries + " attempts", lastException);
    }

    private String callGeminiAPI(String prompt, String base64Data, String mimeType) throws Exception {
        String url = "https://generativelanguage.googleapis.com/"
            + aiConfig.getGemini().getVersion()
            + "/models/" 
            + aiConfig.getGemini().getModel() 
            + ":generateContent?key=" 
            + aiConfig.getGemini().getApiKey();

        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mime_type", mimeType);
        inlineData.put("data", base64Data);

        Map<String, Object> filePart = new HashMap<>();
        filePart.put("inline_data", inlineData);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Arrays.asList(filePart, textPart));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", aiConfig.getGemini().getTemperature());
        generationConfig.put("maxOutputTokens", aiConfig.getGemini().getMaxTokens());
        generationConfig.put("topP", 0.95);
        generationConfig.put("topK", 40);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(content));
        requestBody.put("generationConfig", generationConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("❌ Gemini API error: {}", response.body());
            throw new Exception("Gemini API error (status " + response.statusCode() + "): " + response.body());
        }

        return extractJSONFromResponse(response.body());
    }

    private String extractJSONFromResponse(String geminiResponse) throws Exception {
        try {
            JsonObject jsonResponse = gson.fromJson(geminiResponse, JsonObject.class);

            String text = jsonResponse
                    .getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            text = text.trim();
            if (text.startsWith("```json")) {
                text = text.substring(7);
            } else if (text.startsWith("```")) {
                text = text.substring(3);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }

            return text.trim();
        } catch (Exception e) {
            log.error("❌ Failed to extract JSON from Gemini response: {}", e.getMessage());
            throw new Exception("Invalid response format from Gemini API", e);
        }
    }

    private ParseResult parseGeminiResponse(String jsonResponse) throws Exception {
        try {
            ParseResult result = gson.fromJson(jsonResponse, ParseResult.class);

            if (result == null || result.lessons == null || result.lessons.isEmpty()) {
                throw new Exception("Gemini không trả về lessons nào. Kiểm tra lại format PDF");
            }

            log.info("📊 Parsed {} lessons from Gemini response", result.lessons.size());
            return result;

        } catch (JsonSyntaxException e) {
            log.error("❌ Invalid JSON from Gemini: {}", jsonResponse);
            throw new Exception("Gemini trả về JSON không hợp lệ", e);
        }
    }

    private ParseResult postProcessResult(ParseResult result, Long topicId) {
        int currentOrderIndex = 1;

        for (GrammarLessonDTO lesson : result.lessons) {
            lesson.setTopicId(topicId);

            if (lesson.getOrderIndex() == null || lesson.getOrderIndex() == 0) {
                lesson.setOrderIndex(currentOrderIndex++);
            }

            if (lesson.getPointsReward() == null || lesson.getPointsReward() == 0) {
                lesson.setPointsReward(lesson.getLessonType() == LessonType.THEORY ? 10 : 15);
            }

            if (lesson.getEstimatedDuration() == null || lesson.getEstimatedDuration() == 0) {
                lesson.setEstimatedDuration(lesson.getLessonType() == LessonType.THEORY ? 180 : 300);
            }

            if (lesson.getIsActive() == null) {
                lesson.setIsActive(true);
            }

            if (lesson.getQuestions() != null && !lesson.getQuestions().isEmpty()) {
                int questionOrder = 1;
                for (GrammarQuestionDTO question : lesson.getQuestions()) {
                    if (question.getOrderIndex() == null || question.getOrderIndex() == 0) {
                        question.setOrderIndex(questionOrder++);
                    }
                    if (question.getPoints() == null || question.getPoints() == 0) {
                        question.setPoints(5);
                    }
                }
            }

            log.debug("✅ Processed lesson: {} (type: {}, {} questions)", 
                    lesson.getTitle(), 
                    lesson.getLessonType(), 
                    lesson.getQuestions() != null ? lesson.getQuestions().size() : 0);
        }

        return result;
    }
}