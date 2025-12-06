package com.thanhnb.englishlearning.service.ai.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.config.AIConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Abstract base service for AI-powered file parsing
 * REFACTORED: Switched from Gson to Jackson (ObjectMapper) to support Polymorphic Deserialization
 */
@Slf4j
public abstract class AIParsingService<T> {

    protected final AIConfig aiConfig;
    protected final ObjectMapper objectMapper; // ✅ Use Jackson ObjectMapper

    protected final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    protected AIParsingService(AIConfig aiConfig, ObjectMapper objectMapper) {
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
    }

    // ... (Các phần Abstract Methods và Public API giữ nguyên) ...
    protected abstract String buildPrompt();
    protected abstract T parseResponse(String jsonResponse) throws Exception;
    protected abstract T postProcess(T data);
    protected abstract String getModuleName();

    public T parseFile(MultipartFile file, List<Integer> pages) throws Exception {
        // ... (Logic giữ nguyên) ...
        log.info("[{}] Starting file parsing: {} (size: {} KB, pages: {})",
                getModuleName(), file.getOriginalFilename(), file.getSize() / 1024,
                pages != null ? pages.size() + " selected" : "all");

        validateFile(file);

        String mimeType = file.getContentType();
        T result;

        if (isImage(mimeType)) {
            result = parseImageFile(file);
        } else if (isPDF(mimeType) || isDOCX(mimeType)) {
            result = parseTextBasedFile(file, pages);
        } else {
            throw new Exception("Unsupported file type: " + mimeType);
        }

        result = postProcess(result);
        log.info("[{}] Parsing completed successfully", getModuleName());
        return result;
    }

    // ... (Phần parseImageFile và parseTextBasedFile giữ nguyên logic cũ, chỉ thay đổi cách gọi API nếu cần) ...
    
    protected T parseImageFile(MultipartFile file) throws Exception {
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType();
        String prompt = buildPrompt();
        String jsonResponse = callGeminiAPIWithRetry(prompt, null, base64Image, mimeType);
        return parseResponse(jsonResponse);
    }

    protected T parseTextBasedFile(MultipartFile file, List<Integer> pages) throws Exception {
        // ... (Giữ nguyên logic Chunking và Context Overlap của bạn) ...
        // Copy lại y nguyên logic parseTextBasedFile từ phiên bản trước
        String fullText = extractTextFromFile(file, pages);
        List<ContentChunk> chunks = splitIntoLogicalChunks(fullText);
        List<T> results = new ArrayList<>();
        String previousContext = "";

        for (int i = 0; i < chunks.size(); i++) {
            ContentChunk chunk = chunks.get(i);
            try {
                String contentToSend = chunk.getContent();
                if (!previousContext.isEmpty()) {
                    contentToSend = "--- CONTEXT FROM PREVIOUS PART (IGNORE FOR EXTRACTION) ---\n" 
                                  + previousContext 
                                  + "\n--- CURRENT CONTENT TO PARSE ---\n" 
                                  + chunk.getContent();
                }

                if (chunk.getContent().length() > 800) {
                    previousContext = chunk.getContent().substring(chunk.getContent().length() - 800);
                } else {
                    previousContext = chunk.getContent();
                }

                String prompt = buildPrompt();
                String jsonResponse = callGeminiAPIWithRetry(prompt, contentToSend, null, null);
                
                T chunkResult = parseResponse(jsonResponse);
                if (chunkResult != null) {
                    results.add(chunkResult);
                }
            } catch (Exception e) {
                log.error("[{}] Error processing chunk {}: {}", getModuleName(), i + 1, e.getMessage());
            }
            
            if (i < chunks.size() - 1) {
                Thread.sleep(2000);
            }
        }
        
        if (results.isEmpty()) throw new Exception("Failed to parse content");
        return mergeResults(results);
    }

    protected T mergeResults(List<T> results) {
        if (results.size() == 1) return results.get(0);
        throw new UnsupportedOperationException("mergeResults() must be implemented");
    }

    // ═══════════════════════════════════════════════════════════════
    // GEMINI API CALLS (Updated to use Jackson)
    // ═══════════════════════════════════════════════════════════════

    protected String callGeminiAPIWithRetry(String prompt, String textContent, String base64Image, String mimeType) throws Exception {
        int maxRetries = 3;
        int retryCount = 0;
        long waitTime = 2000;

        while (true) {
            try {
                List<Map<String, Object>> parts = new ArrayList<>();
                Map<String, Object> promptPart = new HashMap<>();
                promptPart.put("text", prompt);
                parts.add(promptPart);

                if (textContent != null) {
                    Map<String, Object> contentPart = new HashMap<>();
                    contentPart.put("text", textContent);
                    parts.add(contentPart);
                }

                if (base64Image != null && mimeType != null) {
                    Map<String, Object> inlineData = new HashMap<>();
                    inlineData.put("mime_type", mimeType);
                    inlineData.put("data", base64Image);
                    Map<String, Object> imagePart = new HashMap<>();
                    imagePart.put("inline_data", inlineData);
                    parts.add(imagePart);
                }

                return callGeminiAPI(parts);

            } catch (Exception e) {
                retryCount++;
                if (retryCount > maxRetries) throw e;
                if (e.getMessage().contains("429") || e.getMessage().contains("500")) {
                    Thread.sleep(waitTime);
                    waitTime *= 2;
                } else {
                    throw e;
                }
            }
        }
    }

    private String callGeminiAPI(List<Map<String, Object>> parts) throws Exception {
        String url = "https://generativelanguage.googleapis.com/"
                + aiConfig.getGemini().getVersion()
                + "/models/"
                + aiConfig.getGemini().getModel()
                + ":generateContent?key="
                + aiConfig.getGemini().getApiKey();

        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", getTemperature());
        generationConfig.put("maxOutputTokens", 8192);
        generationConfig.put("response_mime_type", "application/json"); // ✅ Force JSON mode

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(content));
        requestBody.put("generationConfig", generationConfig);

        // ✅ Use Jackson to serialize request
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Gemini API error (" + response.statusCode() + "): " + response.body());
        }

        return extractJSONFromResponse(response.body());
    }

    protected String extractJSONFromResponse(String geminiResponse) throws Exception {
        try {
            // ✅ Use Jackson to parse response
            JsonNode rootNode = objectMapper.readTree(geminiResponse);
            
            String text = rootNode.path("candidates")
                    .get(0).path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            // Cleanup JSON string
            text = text.trim();
            if (text.startsWith("```json")) text = text.substring(7);
            if (text.startsWith("```")) text = text.substring(3);
            if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
            
            return text.trim();

        } catch (Exception e) {
            log.error("Failed to extract JSON. Raw response: {}", geminiResponse);
            throw new Exception("Invalid response format from Gemini API", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TEXT EXTRACTION & CHUNKING (KEEPING ORIGINAL LOGIC)
    // ═══════════════════════════════════════════════════════════════

    protected String extractTextFromPDF(MultipartFile file, List<Integer> pages) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument
                .load(file.getInputStream())) {

            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();

            if (pages != null && !pages.isEmpty()) {
                StringBuilder allText = new StringBuilder();
                for (Integer pageNum : pages) {
                    if (pageNum < 1 || pageNum > document.getNumberOfPages()) {
                        continue;
                    }
                    stripper.setStartPage(pageNum);
                    stripper.setEndPage(pageNum);
                    allText.append("\n\n========== PAGE ").append(pageNum).append(" ==========\n\n");
                    allText.append(stripper.getText(document));
                }
                return allText.toString();
            } else {
                return stripper.getText(document);
            }
        }
    }

    protected String extractTextFromDOCX(MultipartFile file) throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(
                file.getInputStream())) {

            StringBuilder text = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            return text.toString();
        }
    }

    protected String extractSectionTitle(String section, int fallbackNumber) {
        String[] lines = section.split("\n", 3);
        String firstLine = lines[0].trim();

        if (firstLine.length() > 5 && firstLine.length() < 150) {
            return firstLine;
        }
        return "Section " + fallbackNumber;
    }

    protected List<ContentChunk> splitByCharacterCount(String text, int maxChars) {
        List<ContentChunk> chunks = new ArrayList<>();
        int start = 0;
        int chunkNum = 1;

        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());

            if (end < text.length()) {
                int lastNewline = text.lastIndexOf("\n\n", end);
                if (lastNewline > start + (maxChars / 2)) {
                    end = lastNewline;
                }
            }

            String chunk = text.substring(start, end).trim();
            chunks.add(new ContentChunk("Chunk " + chunkNum, chunk));

            start = end;
            chunkNum++;
        }
        return chunks;
    }
    
    protected abstract double getTemperature();
    protected abstract long getMaxFileSize();
    protected abstract boolean isImage(String mimeType);
    protected abstract boolean isPDF(String mimeType);
    protected abstract boolean isDOCX(String mimeType);
    protected abstract String extractTextFromFile(MultipartFile file, List<Integer> pages) throws Exception;
    protected abstract List<ContentChunk> splitIntoLogicalChunks(String text);
    protected abstract void validateFile(MultipartFile file) throws Exception;
}