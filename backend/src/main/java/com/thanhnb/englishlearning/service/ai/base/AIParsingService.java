package com.thanhnb.englishlearning.service.ai.base;

import com.thanhnb.englishlearning.config.AIConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Abstract base service for AI-powered file parsing
 * Provides common functionality for Grammar, Reading, Listening modules
 */
@Slf4j
public abstract class AIParsingService<T> {

    protected final AIConfig aiConfig;

    protected final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> context
                            .serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime
                            .parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    protected final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * Constructor - only requires AIConfig
     */
    protected AIParsingService(AIConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    // ═══════════════════════════════════════════════════════════════
    // ABSTRACT METHODS - Must be implemented by subclasses
    // ═══════════════════════════════════════════════════════════════

    /**
     * Build AI prompt specific to the module
     */
    protected abstract String buildPrompt();

    /**
     * Parse Gemini JSON response to module-specific DTO
     */
    protected abstract T parseResponse(String jsonResponse) throws Exception;

    /**
     * Post-process parsed data
     */
    protected abstract T postProcess(T data);

    /**
     * Get module name for logging
     */
    protected abstract String getModuleName();

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════

    /**
     * Parse file (PDF, DOCX, or Image)
     * Main entry point for all modules
     */
    public T parseFile(MultipartFile file, List<Integer> pages) throws Exception {
        log.info("[{}] Starting file parsing: {} (size: {} KB, pages: {})",
                getModuleName(), file.getOriginalFilename(), file.getSize() / 1024,
                pages != null ? pages.size() + " selected" : "all");

        validateFile(file);

        String mimeType = file.getContentType();
        T result;

        if (isImage(mimeType)) {
            // Process image directly
            result = parseImageFile(file);
        } else if (isPDF(mimeType) || isDOCX(mimeType)) {
            // Process text-based files with smart chunking
            result = parseTextBasedFile(file, pages);
        } else {
            throw new Exception("Unsupported file type: " + mimeType);
        }

        // Post-process
        result = postProcess(result);

        log.info("[{}] Parsing completed successfully", getModuleName());
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // FILE PARSING LOGIC
    // ═══════════════════════════════════════════════════════════════

    /**
     * Parse image file
     */
    protected T parseImageFile(MultipartFile file) throws Exception {
        log.info("[{}] Processing image file", getModuleName());

        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType();

        String prompt = buildPrompt();
        String jsonResponse = callGeminiAPIWithImage(prompt, base64Image, mimeType);

        return parseResponse(jsonResponse);
    }

    /**
     * Parse text-based file (PDF/DOCX) with smart chunking
     */
    /**
     * Parse text-based file (PDF/DOCX) with smart chunking
     */
    protected T parseTextBasedFile(MultipartFile file, List<Integer> pages) throws Exception {
        log.info("[{}] Processing text-based file with smart chunking", getModuleName());

        // Extract text
        String fullText = extractTextFromFile(file, pages);
        log.info("[{}] Extracted text: {} characters", getModuleName(), fullText.length());

        // Split into chunks
        List<ContentChunk> chunks = splitIntoLogicalChunks(fullText);
        log.info("[{}] Split into {} chunks", getModuleName(), chunks.size());

        // Process each chunk
        List<T> results = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ContentChunk chunk = chunks.get(i);
            log.info("[{}] Processing chunk {}/{}: {} ({} chars)",
                    getModuleName(), i + 1, chunks.size(), chunk.getTitle(), chunk.getContent().length());

            try {
                String prompt = buildPrompt();
                String jsonResponse = callGeminiAPIWithText(prompt, chunk.getContent());
                T chunkResult = parseResponse(jsonResponse);
                results.add(chunkResult);

            } catch (Exception e) {
                log.error("[{}] Error processing chunk {}: {}", getModuleName(), i + 1, e.getMessage());
            }

            // ===== SỬA LỖI Ở ĐÂY =====
            // Di chuyển Thread.sleep ra ngoài khối try...catch
            // Luôn luôn nghỉ 2 giây (hoặc hơn) trước khi xử lý chunk tiếp theo (nếu có)
            // để tránh lỗi Rate Limit 429
            if (i < chunks.size() - 1) {
                log.info("[{}] Pausing 3 seconds before next chunk to avoid rate limit...", getModuleName());
                Thread.sleep(3000); // Tăng lên 3 giây cho an toàn
            }
        }

        if (results.isEmpty()) {
            throw new Exception("Failed to parse any content from file");
        }

        // Merge results
        return mergeResults(results);
    }

    /**
     * Merge multiple parsing results (must be implemented if using chunking)
     */
    protected T mergeResults(List<T> results) {
        if (results.size() == 1) {
            return results.get(0);
        }
        throw new UnsupportedOperationException(
                "mergeResults() must be implemented for multi-chunk processing");
    }

    // ═══════════════════════════════════════════════════════════════
    // TEXT EXTRACTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Extract text from file (PDF or DOCX)
     */
    protected String extractTextFromFile(MultipartFile file, List<Integer> pages) throws Exception {
        String mimeType = file.getContentType();

        if (isPDF(mimeType)) {
            return extractTextFromPDF(file, pages);
        } else if (isDOCX(mimeType)) {
            return extractTextFromDOCX(file);
        } else {
            throw new Exception("Unsupported file type for text extraction: " + mimeType);
        }
    }

    /**
     * Extract text from PDF
     */
    protected String extractTextFromPDF(MultipartFile file, List<Integer> pages) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument
                .load(file.getInputStream())) {

            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();

            if (pages != null && !pages.isEmpty()) {
                // Extract selected pages
                StringBuilder allText = new StringBuilder();
                for (Integer pageNum : pages) {
                    if (pageNum < 1 || pageNum > document.getNumberOfPages()) {
                        log.warn("Skipping invalid page number: {}", pageNum);
                        continue;
                    }
                    stripper.setStartPage(pageNum);
                    stripper.setEndPage(pageNum);
                    String pageText = stripper.getText(document);
                    allText.append("\n\n========== PAGE ").append(pageNum).append(" ==========\n\n");
                    allText.append(pageText);
                }
                return allText.toString();
            } else {
                // Extract all pages
                return stripper.getText(document);
            }
        }
    }

    /**
     * Extract text from DOCX
     */
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

    // ═══════════════════════════════════════════════════════════════
    // SMART CHUNKING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Split text into logical chunks
     * Priority: Exercise → Heading → Page → Character count
     */
    protected List<ContentChunk> splitIntoLogicalChunks(String text) {
        List<ContentChunk> chunks = new ArrayList<>();

        // Try different splitting strategies
        String[] sections = text.split("(?i)(?=Exercise\\s+\\d+:|Bài tập\\s+\\d+:|EXERCISE\\s+\\d+|BÀI TẬP\\s+\\d+)");

        if (sections.length > 1) {
            log.info("Found {} Exercise sections", sections.length);
        } else {
            sections = text.split("(?i)(?=^(?:I{1,3}|IV|V|VI{1,3}|[1-9]\\d{0,2})\\s*\\.)");
            if (sections.length > 1) {
                log.info("Found {} heading sections", sections.length);
            } else {
                sections = text.split("(?i)(?==+ PAGE \\d+ =+)");
                if (sections.length > 1) {
                    log.info("Found {} page sections", sections.length);
                } else {
                    log.info("No logical boundaries found, using character split");
                    return splitByCharacterCount(text, 8000);
                }
            }
        }

        // Process sections
        for (int i = 0; i < sections.length; i++) {
            String section = sections[i].trim();
            if (section.isEmpty() || section.length() < 100) {
                continue;
            }

            String title = extractSectionTitle(section, i + 1);

            if (section.length() > 10000) {
                log.info("Section '{}' too large ({} chars), splitting further",
                        title, section.length());
                List<ContentChunk> subChunks = splitByCharacterCount(section, 8000);
                for (int j = 0; j < subChunks.size(); j++) {
                    subChunks.get(j).setTitle(title + " - Part " + (j + 1));
                }
                chunks.addAll(subChunks);
            } else {
                chunks.add(new ContentChunk(title, section));
            }
        }

        return chunks;
    }

    /**
     * Extract section title intelligently
     */
    protected String extractSectionTitle(String section, int fallbackNumber) {
        String[] lines = section.split("\n", 3);
        String firstLine = lines[0].trim();

        if (firstLine.length() > 5 && firstLine.length() < 150) {
            if (firstLine.matches("(?i).*(Exercise|Bài tập|Chapter|Chương).*")) {
                return firstLine;
            }
            if (firstLine.matches("^(?:I{1,3}|IV|V|VI{1,3}|[1-9]\\d{0,2})\\..*")) {
                return firstLine;
            }
        }

        if (firstLine.length() > 80) {
            return firstLine.substring(0, 77) + "...";
        }

        return "Section " + fallbackNumber;
    }

    /**
     * Split by character count (fallback)
     */
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

    // ═══════════════════════════════════════════════════════════════
    // GEMINI API CALLS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Call Gemini API with image
     */
    protected String callGeminiAPIWithImage(String prompt, String base64Image, String mimeType) throws Exception {
        List<Map<String, Object>> parts = new ArrayList<>();

        Map<String, Object> promptPart = new HashMap<>();
        promptPart.put("text", prompt);
        parts.add(promptPart);

        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mime_type", mimeType);
        inlineData.put("data", base64Image);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("inline_data", inlineData);
        parts.add(imagePart);

        return callGeminiAPI(parts);
    }

    /**
     * Call Gemini API with text
     */
    protected String callGeminiAPIWithText(String prompt, String content) throws Exception {
        List<Map<String, Object>> parts = new ArrayList<>();

        Map<String, Object> promptPart = new HashMap<>();
        promptPart.put("text", prompt);
        parts.add(promptPart);

        Map<String, Object> contentPart = new HashMap<>();
        contentPart.put("text", "\n\n--- CONTENT TO PARSE ---\n\n" + content);
        parts.add(contentPart);

        return callGeminiAPI(parts);
    }

    /**
     * Common Gemini API call
     */
    protected String callGeminiAPI(List<Map<String, Object>> parts) throws Exception {
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
        generationConfig.put("topP", 0.95);
        generationConfig.put("topK", 40);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(content));
        requestBody.put("generationConfig", generationConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Gemini API error: {}", response.body());
            throw new Exception("Gemini API error (status " + response.statusCode() + "): " + response.body());
        }

        return extractJSONFromResponse(response.body());
    }

    /**
     * Extract JSON from Gemini response
     */
    protected String extractJSONFromResponse(String geminiResponse) throws Exception {
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
            log.error("Failed to extract JSON from Gemini response: {}", e.getMessage());
            throw new Exception("Invalid response format from Gemini API", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get temperature for AI (can be overridden)
     */
    protected double getTemperature() {
        return aiConfig.getGemini().getTemperature();
    }

    /**
     * Validate file
     */
    protected void validateFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Cannot determine file type");
        }

        boolean isValid = isPDF(contentType) || isDOCX(contentType) || isImage(contentType);
        if (!isValid) {
            throw new IllegalArgumentException("Only PDF, DOCX, JPG, PNG, WEBP are supported");
        }

        long maxSize = getMaxFileSize();
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds %dMB", maxSize / (1024 * 1024)));
        }
    }

    /**
     * Get max file size (can be overridden)
     */
    protected long getMaxFileSize() {
        return 20 * 1024 * 1024; // 20MB
    }

    /**
     * Check if mime type is image
     */
    protected boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Check if mime type is PDF
     */
    protected boolean isPDF(String mimeType) {
        return "application/pdf".equals(mimeType);
    }

    /**
     * Check if mime type is DOCX
     */
    protected boolean isDOCX(String mimeType) {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType);
    }
}