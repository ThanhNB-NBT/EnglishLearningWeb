package com.thanhnb.englishlearning.service;

import com.thanhnb.englishlearning.config.AIConfig;
import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.dto.grammar.GrammarQuestionDTO;
import com.thanhnb.englishlearning.dto.ParseResult;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
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
    private final GrammarLessonRepository grammarLessonRepository;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> context
                            .serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime
                            .parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * ✅ BACKWARD COMPATIBILITY
     */
    public ParseResult parsePDF(MultipartFile file, Long topicId) throws Exception {
        return parseFile(file, topicId, null);
    }

    /**
     * ✅ HYBRID APPROACH - Main entry point
     */
    public ParseResult parseFile(MultipartFile file, Long topicId, List<Integer> pages) throws Exception {
        log.info("📄 Starting file parsing: {} (size: {} KB, pages: {})",
                file.getOriginalFilename(), file.getSize() / 1024,
                pages != null ? pages.size() + " selected" : "all");

        validateFile(file);

        String mimeType = file.getContentType();
        ParseResult finalResult;

        if (isImage(mimeType)) {
            // Image - process as base64 (single call)
            String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
            log.info("📸 Processing image file");
            String prompt = buildOptimizedPrompt();
            String jsonResponse = callGeminiAPIWithRetry(prompt, base64Data, mimeType, 3);
            finalResult = parseGeminiResponse(jsonResponse);

        } else if (isPDF(mimeType) || isDOCX(mimeType)) {
            // ✅ HYBRID: Always use smart chunking for text-based files
            finalResult = parseFileWithSmartChunking(file, topicId, pages);

        } else {
            throw new Exception("Unsupported file type: " + mimeType);
        }

        log.info("✅ Parsing completed: {} lessons, {} total questions",
                finalResult.lessons.size(),
                finalResult.lessons.stream()
                        .filter(l -> l.getQuestions() != null)
                        .mapToInt(l -> l.getQuestions().size())
                        .sum());

        return finalResult;
    }

    /**
     * ✅ SMART CHUNKING - Main logic
     */
    private ParseResult parseFileWithSmartChunking(MultipartFile file, Long topicId, List<Integer> pages)
            throws Exception {

        log.info("🧠 Starting SMART CHUNKING strategy");

        // 1. Extract full text
        String fullText;
        String mimeType = file.getContentType();

        if (isPDF(mimeType)) {
            if (pages != null && !pages.isEmpty()) {
                fullText = extractTextFromSelectedPages(file, pages);
            } else {
                fullText = extractAllTextFromPDF(file);
            }
        } else if (isDOCX(mimeType)) {
            fullText = extractTextFromDOCX(file);
        } else {
            throw new Exception("Unsupported file type for chunking: " + mimeType);
        }

        log.info("📄 Total text length: {} characters", fullText.length());

        // 2. Split into logical chunks
        List<ContentChunk> chunks = splitIntoLogicalChunks(fullText);

        log.info("📦 Split into {} logical chunks", chunks.size());

        // 3. Process each chunk with retry
        List<ParseResult> allResults = new ArrayList<>();
        int chunkNum = 0;

        for (ContentChunk chunk : chunks) {
            chunkNum++;
            log.info("🔄 Processing chunk {}/{}: {} ({} chars)",
                    chunkNum, chunks.size(), chunk.title, chunk.content.length());

            try {
                String prompt = buildOptimizedPrompt();
                String jsonResponse = callGeminiAPIWithRetry(prompt, chunk.content, "text/plain", 2);
                ParseResult chunkResult = parseGeminiResponse(jsonResponse);

                if (chunkResult != null && chunkResult.lessons != null && !chunkResult.lessons.isEmpty()) {
                    allResults.add(chunkResult);
                    log.info("✅ Chunk {} parsed: {} lessons", chunkNum, chunkResult.lessons.size());
                } else {
                    log.warn("⚠️ Chunk {} returned no lessons", chunkNum);
                }

                // Delay between chunks to avoid rate limit
                if (chunkNum < chunks.size()) {
                    log.info("⏳ Waiting 2 seconds before next chunk...");
                    Thread.sleep(2000);
                }

            } catch (Exception e) {
                log.error("❌ Error processing chunk {}: {}", chunkNum, e.getMessage());
                // Continue with next chunk instead of failing completely
            }
        }

        if (allResults.isEmpty()) {
            throw new Exception("Failed to parse any lessons from file");
        }

        // 4. Merge results
        ParseResult finalResult = mergeParseResults(allResults);

        // 5. Post-process
        finalResult = postProcessResult(finalResult, topicId);

        log.info("✅ Smart chunking completed: {} lessons, {} total questions",
                finalResult.lessons.size(),
                finalResult.lessons.stream()
                        .filter(l -> l.getQuestions() != null)
                        .mapToInt(l -> l.getQuestions().size())
                        .sum());

        return finalResult;
    }

    /**
     * ✅ Split text into logical chunks (PRIORITY: Exercise → Heading → Page → Chars)
     */
    private List<ContentChunk> splitIntoLogicalChunks(String text) {
        List<ContentChunk> chunks = new ArrayList<>();

        // ✅ PRIORITY 1: Split by Exercise patterns
        String[] sections = text.split("(?i)(?=Exercise\\s+\\d+:|Bài tập\\s+\\d+:|EXERCISE\\s+\\d+|BÀI TẬP\\s+\\d+)");

        if (sections.length > 1) {
            log.info("🎯 Found {} Exercise sections", sections.length);
        } else {
            // ✅ PRIORITY 2: Split by heading patterns (I., II., 1., 2.)
            sections = text.split("(?i)(?=^(?:I{1,3}|IV|V|VI{1,3}|[1-9]\\d{0,2})\\s*\\.)");

            if (sections.length > 1) {
                log.info("🎯 Found {} heading sections", sections.length);
            } else {
                // ✅ PRIORITY 3: Split by PAGE markers
                sections = text.split("(?i)(?==+ PAGE \\d+ =+)");

                if (sections.length > 1) {
                    log.info("🎯 Found {} page sections", sections.length);
                } else {
                    // ✅ LAST FALLBACK: Split by character count
                    log.info("🎯 No logical boundaries found, using character split");
                    return splitByCharacterCount(text, 8000);
                }
            }
        }

        // Process sections
        for (int i = 0; i < sections.length; i++) {
            String section = sections[i].trim();

            if (section.isEmpty() || section.length() < 100) {
                log.debug("⏭️ Skipping too short section: {} chars", section.length());
                continue;
            }

            // Extract title
            String title = extractSectionTitle(section, i + 1);

            // Check if section is too large (> 10k chars)
            if (section.length() > 10000) {
                log.info("⚠️ Section '{}' too large ({} chars), splitting further",
                        title, section.length());
                List<ContentChunk> subChunks = splitByCharacterCount(section, 8000);
                // Add prefix to sub-chunk titles
                for (int j = 0; j < subChunks.size(); j++) {
                    subChunks.get(j).title = title + " - Part " + (j + 1);
                }
                chunks.addAll(subChunks);
            } else {
                chunks.add(new ContentChunk(title, section));
            }
        }

        log.info("📊 Logical chunking result:");
        for (int i = 0; i < chunks.size(); i++) {
            log.info("  Chunk {}: {} ({} chars)",
                    i + 1, chunks.get(i).title, chunks.get(i).content.length());
        }

        return chunks;
    }

    /**
     * ✅ Extract section title intelligently
     */
    private String extractSectionTitle(String section, int fallbackNumber) {
        String[] lines = section.split("\n", 3);
        String firstLine = lines[0].trim();

        // Check if first line looks like a title
        if (firstLine.length() > 5 && firstLine.length() < 150) {
            // Check if it contains Exercise/Bài tập keywords
            if (firstLine.matches("(?i).*(Exercise|Bài tập|Chapter|Chương).*")) {
                return firstLine;
            }
            // Check if it starts with numbering
            if (firstLine.matches("^(?:I{1,3}|IV|V|VI{1,3}|[1-9]\\d{0,2})\\..*")) {
                return firstLine;
            }
        }

        // Fallback: Use first 80 chars
        if (firstLine.length() > 80) {
            return firstLine.substring(0, 77) + "...";
        }

        // Last fallback: Generic name
        return "Section " + fallbackNumber;
    }

    /**
     * ✅ Fallback: Split by character count (try to split at paragraph boundaries)
     */
    private List<ContentChunk> splitByCharacterCount(String text, int maxChars) {
        List<ContentChunk> chunks = new ArrayList<>();
        int start = 0;
        int chunkNum = 1;

        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());

            // Try to find paragraph boundary
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

    /**
     * ✅ Merge multiple ParseResults
     */
    private ParseResult mergeParseResults(List<ParseResult> results) {
        ParseResult merged = new ParseResult();
        merged.lessons = new ArrayList<>();

        for (ParseResult result : results) {
            if (result != null && result.lessons != null) {
                merged.lessons.addAll(result.lessons);
            }
        }

        // Sort by orderIndex from Gemini
        merged.lessons.sort((a, b) -> {
            int orderA = a.getOrderIndex() != null ? a.getOrderIndex() : 999;
            int orderB = b.getOrderIndex() != null ? b.getOrderIndex() : 999;
            return Integer.compare(orderA, orderB);
        });

        // Re-index to ensure continuity (1, 2, 3, ...)
        for (int i = 0; i < merged.lessons.size(); i++) {
            merged.lessons.get(i).setOrderIndex(i + 1);
        }

        log.info("✅ Merged {} batches into {} total lessons", results.size(), merged.lessons.size());

        return merged;
    }

    /**
     * ✅ Extract text from selected PDF pages
     */
    private String extractTextFromSelectedPages(MultipartFile file, List<Integer> pages) throws Exception {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder allText = new StringBuilder();

            log.info("📄 Extracting text from {} selected pages", pages.size());

            for (Integer pageNum : pages) {
                if (pageNum < 1 || pageNum > document.getNumberOfPages()) {
                    log.warn("⚠️ Skipping invalid page number: {}", pageNum);
                    continue;
                }

                try {
                    stripper.setStartPage(pageNum);
                    stripper.setEndPage(pageNum);

                    String pageText = stripper.getText(document);

                    allText.append("\n\n========== PAGE ").append(pageNum).append(" ==========\n\n");
                    allText.append(pageText);

                    log.debug("✅ Page {}: {} characters", pageNum, pageText.length());
                } catch (Exception e) {
                    log.error("❌ Error extracting page {}: {}", pageNum, e.getMessage());
                }
            }

            if (allText.length() == 0) {
                throw new Exception("No text extracted from selected pages");
            }

            log.info("✅ Total extracted: {} characters from {} pages", allText.length(), pages.size());
            return allText.toString();
        }
    }

    /**
     * ✅ Extract all text from PDF
     */
    private String extractAllTextFromPDF(MultipartFile file) throws Exception {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * ✅ Extract text from DOCX
     */
    private String extractTextFromDOCX(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {

            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            return text.toString();
        }
    }

    /**
     * ✅ File type helpers
     */
    private boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    private boolean isPDF(String mimeType) {
        return "application/pdf".equals(mimeType);
    }

    private boolean isDOCX(String mimeType) {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType);
    }

    /**
     * ✅ Validate file
     */
    private void validateFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        String contentType = file.getContentType();
        log.info("📄 File validation passed: {} ({})", file.getOriginalFilename(), contentType);

        if (contentType == null) {
            throw new IllegalArgumentException("Không xác định được loại file");
        }

        boolean isValidType = isPDF(contentType) ||
                isDOCX(contentType) ||
                isImage(contentType);

        if (!isValidType) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file PDF, DOCX, JPG, PNG, WEBP");
        }

        if (file.getSize() > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("File không được vượt quá 20MB");
        }
    }

    /**
     * ✅ OPTIMIZED PROMPT
     */
    private String buildOptimizedPrompt() {
        return """
                Bạn là trợ lý phân tích tài liệu học tiếng Anh. Nhiệm vụ: Đọc nội dung và tạo JSON theo format sau.
                
                ═══════════════════════════════════════════════════════════════
                📋 QUY TẮC PHÂN LOẠI LESSON
                ═══════════════════════════════════════════════════════════════
                
                **THEORY (Lý thuyết):**
                - Giải thích khái niệm, định nghĩa, cấu trúc ngữ pháp
                - Bảng phân loại, ví dụ minh họa
                - Tiêu đề chứa: "Phần", "Chương", "Khái niệm", "Cấu trúc"
                
                **PRACTICE (Bài tập):**
                - Câu hỏi, đề bài, chỗ trống cần điền
                - Lựa chọn A/B/C/D
                - Tiêu đề chứa: "Bài tập", "Exercise", "Practice", "Đáp án"
                
                ═══════════════════════════════════════════════════════════════
                📐 CÁCH CHIA LESSONS
                ═══════════════════════════════════════════════════════════════
                
                **Nguyên tắc:**
                1. Mỗi CHỦ ĐỀ = 1 THEORY + 1 PRACTICE (nếu có bài tập)
                2. Nếu chỉ có lý thuyết → Tạo 1 THEORY lesson
                3. Nếu nhiều dạng bài tập → Tách thành nhiều PRACTICE lessons
                
                ═══════════════════════════════════════════════════════════════
                📝 FORMAT CONTENT (CHỈ CHO THEORY)
                ═══════════════════════════════════════════════════════════════
                
                **HTML Structure:**
                <h2>TIÊU ĐỀ LỚN</h2>
                <h3>Tiêu đề con</h3>
                <p>Đoạn văn giải thích...</p>
                
                <table class="tiptap-table">
                  <thead><tr><th>Cột 1</th><th>Cột 2</th></tr></thead>
                  <tbody>
                    <tr>
                      <td><strong>Tiêu đề</strong><br>Nội dung 1</td>
                      <td><strong>Ví dụ:</strong><br>- Item 1</td>
                    </tr>
                  </tbody>
                </table>
                
                <ul><li>Danh sách không thứ tự</li></ul>
                <ol><li>Danh sách có thứ tự</li></ol>
                
                **Lưu ý:**
                - PHẢI có class="tiptap-table"
                - Dùng <br> để xuống dòng trong cell
                - Đóng thẻ đầy đủ
                
                ═══════════════════════════════════════════════════════════════
                ❓ FORMAT QUESTIONS (CHO PRACTICE)
                ═══════════════════════════════════════════════════════════════
                
                **1. FILL_BLANK** (Điền vào chỗ trống):
                
                Dấu hiệu: Có ___, (...), [blank], hoặc số đánh dấu ¹, ², ³
                
                Xử lý: Mỗi chỗ trống = 1 question riêng biệt
                
                Input: "She (be)¹ ...... young. Her father (marry)² ......."
                
                Output:
                [
                  {
                    "questionText": "She (be) ___ young.",
                    "questionType": "FILL_BLANK",
                    "correctAnswer": "was",
                    "explanation": "Quá khứ đơn: be → was",
                    "points": 5,
                    "orderIndex": 1
                  },
                  {
                    "questionText": "Her father (marry) ___ again.",
                    "questionType": "FILL_BLANK",
                    "correctAnswer": "married",
                    "explanation": "Quá khứ đơn: marry → married",
                    "points": 5,
                    "orderIndex": 2
                  }
                ]
                
                **2. MULTIPLE_CHOICE** (Trắc nghiệm):
                
                ⚠️ QUAN TRỌNG: optionText CHỈ chứa NỘI DUNG, KHÔNG có A/B/C/D
                
                Input: "1. A. enough B. young C. country D. mountain"
                
                Output:
                {
                  "questionText": "Chọn từ có phần gạch chân phát âm khác: A. enough B. young C. country D. mountain",
                  "questionType": "MULTIPLE_CHOICE",
                  "correctAnswer": null,
                  "explanation": "mountain phát âm /aʊ/, các từ khác /ʌ/",
                  "points": 5,
                  "orderIndex": 1,
                  "options": [
                    {"optionText": "enough", "isCorrect": false, "orderIndex": 1},
                    {"optionText": "young", "isCorrect": false, "orderIndex": 2},
                    {"optionText": "country", "isCorrect": false, "orderIndex": 3},
                    {"optionText": "mountain", "isCorrect": true, "orderIndex": 4}
                  ]
                }
                
                **3. TRANSLATE** (Dịch câu):
                {
                  "questionText": "Dịch sang tiếng Anh: Tôi thích học tiếng Anh",
                  "questionType": "TRANSLATE",
                  "correctAnswer": "I like learning English",
                  "explanation": "like + V-ing: thích làm gì",
                  "points": 10,
                  "orderIndex": 1
                }
                
                **Xử lý đáp án & giải thích:**
                - Tìm phần "Đáp án", "Lời giải", "Answer Key"
                - Nếu CÓ → Lấy nhưng RÚT GỌN (tối đa 150 ký tự)
                - Nếu KHÔNG có → Tự sinh giải thích NGẮN GỌN (1-2 câu)
                
                ═══════════════════════════════════════════════════════════════
                🗑️ LỌC BỎ NOISE
                ═══════════════════════════════════════════════════════════════
                
                Bỏ qua: Header/Footer lặp lại, số trang, logo, quảng cáo, link
                
                ═══════════════════════════════════════════════════════════════
                📦 OUTPUT JSON FORMAT
                ═══════════════════════════════════════════════════════════════
                
                {
                  "lessons": [
                    {
                      "title": "Tên bài học",
                      "lessonType": "THEORY" | "PRACTICE",
                      "content": "HTML (CHỈ cho THEORY)",
                      "orderIndex": 1,
                      "pointsReward": 10,
                      "estimatedDuration": 300,
                      "isActive": true,
                      "questions": [...]
                    }
                  ]
                }
                
                ═══════════════════════════════════════════════════════════════
                ⚠️ LƯU Ý QUAN TRỌNG
                ═══════════════════════════════════════════════════════════════
                
                1. CHỈ TRẢ VỀ JSON HỢP LỆ, KHÔNG TEXT THỪA
                2. KHÔNG DÙNG ```json``` wrapper
                3. orderIndex BẮT ĐẦU TỪ 1 và LIÊN TỤC
                4. HTML PHẢI hợp lệ
                5. MULTIPLE_CHOICE PHẢI có ít nhất 1 đáp án đúng
                6. explanation KHÔNG quá 150 ký tự
                7. pointsReward: THEORY = 10, PRACTICE = 15
                8. estimatedDuration: THEORY = 180s, PRACTICE = 300-600s
                
                ═══════════════════════════════════════════════════════════════
                """;
    }

    /**
     * ✅ Call Gemini API with retry
     */
    private String callGeminiAPIWithRetry(String prompt, String contentData, String mimeType, int maxRetries)
            throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("🌐 Calling Gemini API (attempt {}/{})", attempt, maxRetries);
                return callGeminiAPI(prompt, contentData, mimeType);
            } catch (Exception e) {
                lastException = e;
                log.warn("⚠️ Gemini API call failed (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    long waitTime = (long) Math.pow(2, attempt) * 1000;
                    log.info("⏳ Retrying after {}ms...", waitTime);
                    Thread.sleep(waitTime);
                }
            }
        }

        throw new Exception("Gemini API failed after " + maxRetries + " attempts", lastException);
    }

    /**
     * ✅ Call Gemini API (with maxOutputTokens = 8192)
     */
    private String callGeminiAPI(String prompt, String contentData, String mimeType) throws Exception {
        String url = "https://generativelanguage.googleapis.com/"
                + aiConfig.getGemini().getVersion()
                + "/models/"
                + aiConfig.getGemini().getModel()
                + ":generateContent?key="
                + aiConfig.getGemini().getApiKey();

        // Build parts
        List<Map<String, Object>> parts = new ArrayList<>();

        Map<String, Object> promptPart = new HashMap<>();
        promptPart.put("text", prompt);
        parts.add(promptPart);

        if (isImage(mimeType)) {
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", contentData);

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("inline_data", inlineData);
            parts.add(imagePart);

            log.debug("📤 Sending as image: {} ({} KB)", mimeType, contentData.length() / 1024);
        } else {
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", "\n\n--- CONTENT TO PARSE ---\n\n" + contentData);
            parts.add(textPart);

            log.debug("📤 Sending as text: {} characters", contentData.length());
        }

        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);

        // ✅ HYBRID: maxOutputTokens = 8192
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", aiConfig.getGemini().getTemperature());
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

        log.debug("📤 Request size: {} bytes", gson.toJson(requestBody).length());

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("❌ Gemini API error: {}", response.body());
            throw new Exception("Gemini API error (status " + response.statusCode() + "): " + response.body());
        }

        log.debug("✅ Response size: {} bytes", response.body().length());

        return extractJSONFromResponse(response.body());
    }

    /**
     * ✅ Extract JSON from Gemini response
     */
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

            text = text.trim();

            if (!text.endsWith("}")) {
                log.warn("⚠️ JSON may be incomplete, attempting to fix...");
                text = fixIncompleteJSON(text);
            }

            log.debug("📄 Extracted JSON length: {} characters", text.length());
            if (text.length() < 1000) {
                log.debug("📄 JSON content: {}", text);
            } else {
                log.debug("📄 JSON preview (first 500 chars): {}", text.substring(0, 500));
                log.debug("📄 JSON preview (last 200 chars): {}", text.substring(text.length() - 200));
            }

            return text;
        } catch (Exception e) {
            log.error("❌ Failed to extract JSON from Gemini response: {}", e.getMessage());
            throw new Exception("Invalid response format from Gemini API", e);
        }
    }

    /**
     * ✅ Fix incomplete JSON (auto-complete brackets and remove incomplete questions)
     */
    private String fixIncompleteJSON(String json) {
        StringBuilder fixed = new StringBuilder(json);

        // ✅ Check if JSON ends abruptly in the middle of a question
        if (json.contains("\"questionText\":") && !json.trim().endsWith("}")) {
            log.warn("⚠️ Detected incomplete question, attempting to fix...");

            // Find last complete question
            int lastCompleteQuestion = json.lastIndexOf("},\n        {");
            if (lastCompleteQuestion == -1) {
                lastCompleteQuestion = json.lastIndexOf("}]");
            }

            if (lastCompleteQuestion > 0) {
                // Cut off incomplete question
                fixed = new StringBuilder(json.substring(0, lastCompleteQuestion + 1));
                log.info("✂️ Truncated incomplete question at position {}", lastCompleteQuestion);
            }
        }

        // ✅ Original logic for fixing brackets
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        char prevChar = '\0';

        for (char c : fixed.toString().toCharArray()) {
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{')
                    braceCount++;
                else if (c == '}')
                    braceCount--;
                else if (c == '[')
                    bracketCount++;
                else if (c == ']')
                    bracketCount--;
            }
            prevChar = c;
        }

        if (inString) {
            fixed.append("\"");
            log.warn("⚠️ Fixed unclosed string");
        }

        while (bracketCount > 0) {
            fixed.append("]");
            bracketCount--;
            log.warn("⚠️ Added missing ']'");
        }

        while (braceCount > 0) {
            fixed.append("}");
            braceCount--;
            log.warn("⚠️ Added missing '}'");
        }

        String result = fixed.toString();

        // ✅ Validate basic JSON structure
        if (result.contains("\"lessons\"") && result.contains("[")) {
            log.info("✅ JSON structure appears valid after fix");
        } else {
            log.error("❌ JSON structure still invalid after fix");
        }

        return result;
    }

    /**
     * ✅ Parse Gemini response with validation
     */
    private ParseResult parseGeminiResponse(String jsonResponse) throws Exception {
        try {
            // ✅ Validate JSON structure first
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                throw new Exception("Gemini trả về response rỗng");
            }

            // ✅ Check if it's valid JSON
            if (!jsonResponse.trim().startsWith("{")) {
                log.error("❌ Response không phải JSON. First 200 chars: {}",
                        jsonResponse.substring(0, Math.min(200, jsonResponse.length())));
                throw new Exception("Gemini không trả về JSON. Có thể nội dung quá phức tạp hoặc API bị lỗi");
            }

            ParseResult result = gson.fromJson(jsonResponse, ParseResult.class);

            if (result == null) {
                throw new Exception("Không thể parse JSON thành ParseResult");
            }

            if (result.lessons == null || result.lessons.isEmpty()) {
                log.warn("⚠️ Gemini trả về JSON nhưng không có lessons");
                log.debug("📄 Full response: {}", jsonResponse);
                throw new Exception("Gemini không trả về lessons nào. Có thể nội dung không phù hợp hoặc quá ngắn");
            }

            log.info("✅ Parsed {} lessons from Gemini response", result.lessons.size());
            return result;

        } catch (JsonSyntaxException e) {
            log.error("❌ Invalid JSON syntax from Gemini");
            log.debug("📄 Response preview (first 500 chars): {}",
                    jsonResponse.substring(0, Math.min(500, jsonResponse.length())));
            log.debug("📄 Response preview (last 200 chars): {}",
                    jsonResponse.substring(Math.max(0, jsonResponse.length() - 200)));
            throw new Exception("Gemini trả về JSON không hợp lệ: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ Post-process result with validation
     */
    private ParseResult postProcessResult(ParseResult result, Long topicId) {
        Integer maxOrder = grammarLessonRepository.findMaxOrderIndexByTopicId(topicId);
        int baseOrder = (maxOrder != null ? maxOrder : 0);
        int seq = baseOrder;
    
        // Tránh trùng/thiếu sau khi dịch
        Set<Integer> seen = new HashSet<>();
    
        for (GrammarLessonDTO lesson : result.lessons) {
            lesson.setTopicId(topicId);
    
            Integer idx = lesson.getOrderIndex();
            if (idx == null || idx == 0) {
                idx = ++seq;              // gán tiếp nối
            } else {
                idx = baseOrder + idx;    // dịch theo base
            }
    
            // đảm bảo không trùng nếu dữ liệu đầu vào có lặp
            while (seen.contains(idx)) {
                idx++;
            }
            seen.add(idx);
            lesson.setOrderIndex(idx);
    
            if (lesson.getPointsReward() == null || lesson.getPointsReward() == 0) {
                lesson.setPointsReward(lesson.getLessonType() == LessonType.THEORY ? 10 : 15);
            }
    
            if (lesson.getEstimatedDuration() == null || lesson.getEstimatedDuration() == 0) {
                int duration = lesson.getLessonType() == LessonType.THEORY ? 180 : 300;
                if (lesson.getContent() != null && lesson.getContent().length() > 5000) {
                    duration += 120;
                }
                if (lesson.getQuestions() != null && lesson.getQuestions().size() > 5) {
                    duration += lesson.getQuestions().size() * 30;
                }
                lesson.setEstimatedDuration(duration);
            }
    
            if (lesson.getIsActive() == null) {
                lesson.setIsActive(true);
            }
    
            if (lesson.getContent() != null && !lesson.getContent().isEmpty()) {
                lesson.setContent(cleanHtmlContent(lesson.getContent()));
            }
    
            if (lesson.getQuestions() != null && !lesson.getQuestions().isEmpty()) {
                int questionOrder = 1;
                for (GrammarQuestionDTO question : lesson.getQuestions()) {
                    if (question.getQuestionText() != null) {
                        String normalized = normalizeQuestionText(question.getQuestionText());
                        question.setQuestionText(normalized);
                    }
                    if (question.getCorrectAnswer() != null) {
                        question.setCorrectAnswer(question.getCorrectAnswer().trim());
                    }
                    if (question.getOrderIndex() == null || question.getOrderIndex() == 0) {
                        question.setOrderIndex(questionOrder++);
                    }
                    if (question.getPoints() == null || question.getPoints() == 0) {
                        question.setPoints(5);
                    }
                    if (QuestionType.FILL_BLANK.equals(question.getQuestionType())) {
                        validateFillBlankQuestion(question, lesson.getTitle());
                    }
                }
            }
    
            log.debug("✅ Processed lesson: {} (type: {}, orderIndex: {}, {} questions)",
                    lesson.getTitle(),
                    lesson.getLessonType(),
                    lesson.getOrderIndex(),
                    lesson.getQuestions() != null ? lesson.getQuestions().size() : 0);
        }
    
        return result;
    }

    /**
     * ✅ Normalize question text (convert markers to ___)
     */
    private String normalizeQuestionText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Replace common markers with ___
        text = text.replaceAll("\\.{4,}", "___");  // ........ → ___
        text = text.replaceAll("_{2,}", "___");          // __ or ___ → ___
        text = text.replaceAll("[¹²³⁴⁵⁶⁷⁸⁹⁰]", ""); // Remove superscript numbers

        // Clean up whitespace
        text = text.replaceAll("\\s+", " ");
        text = text.replaceAll("\\s+___", " ___");
        text = text.replaceAll("___\\s+", "___ ");

        return text.trim();
    }

    /**
     * ✅ Validate FILL_BLANK questions
     */
    private void validateFillBlankQuestion(GrammarQuestionDTO question, String lessonTitle) {
        String text = question.getQuestionText();

        // Check if has blank marker
        if (!text.contains("___") && !text.contains("(") && !text.contains("[blank]")) {
            log.warn("⚠️ FILL_BLANK question missing blank marker in lesson '{}': {}",
                    lessonTitle, text);
        }

        // Check if has correct answer
        if (question.getCorrectAnswer() == null || question.getCorrectAnswer().trim().isEmpty()) {
            log.warn("⚠️ FILL_BLANK question missing correct answer in lesson '{}': {}",
                    lessonTitle, text);
        }

        // Validate multiple blanks format
        String correctAnswer = question.getCorrectAnswer();
        if (correctAnswer != null) {
            // Count blanks in question
            long blankCount = text.chars().filter(ch -> ch == '_').count() / 3; // ___ = 3 underscores

            // Count answers (separated by |)
            long answerCount = correctAnswer.contains("|")
                    ? correctAnswer.split("\\|").length
                    : 1;

            if (blankCount > 1 && blankCount != answerCount) {
                log.warn("⚠️ Blank count ({}) != answer count ({}) in lesson '{}': {}",
                        blankCount, answerCount, lessonTitle, text);
            }
        }
    }

    /**
     * ✅ Clean HTML content
     */
    private String cleanHtmlContent(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        html = html.replaceAll("\\s+", " ");
        html = html.replaceAll("<p>\\s*</p>", "");
        html = html.replaceAll("<br>\\s*<br>", "<br>");
        html = html.replaceAll("<table[^>]*>", "<table class=\"tiptap-table\">");
        html = html.replaceAll("(Hotline:|Website:|Fanpage:)[^<]*", "");
        html = html.replaceAll("Trung tâm luyện thi TOEIC[^<]*", "");

        return html.trim();
    }

    /**
     * ✅ Helper class for content chunks
     */
    private static class ContentChunk {
        String title;
        String content;

        ContentChunk(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}