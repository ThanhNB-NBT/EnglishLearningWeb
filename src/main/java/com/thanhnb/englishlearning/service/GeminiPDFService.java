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

    public ParseResult parsePDF(MultipartFile file, Long topicId) throws Exception {
        log.info("📄 Starting PDF parsing for file: {} (size: {} KB)", 
                file.getOriginalFilename(), file.getSize() / 1024);

        validateFile(file);
        String base64Data = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType();
        String prompt = buildOptimizedPrompt();
        String jsonResponse = callGeminiAPIWithRetry(prompt, base64Data, mimeType, 3);
        ParseResult result = parseGeminiResponse(jsonResponse);
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

    /**
     * ✅ OPTIMIZED PROMPT - Dựa trên cấu trúc PDF thực tế
     */
    private String buildOptimizedPrompt() {
        return """
                🤖 BẠN LÀ CHUYÊN GIA PHÂN TÍCH TÀI LIỆU HỌC TIẾNG ANH
                
                📋 NHIỆM VỤ: Đọc file PDF và TỰ ĐỘNG CHIA THÀNH CÁC BÀI HỌC
                
                ✅ QUY TẮC PHÂN CHIA (DỰA TRÊN CẤU TRÚC PDF CHUẨN):
                
                📚 NHẬN DIỆN CẤU TRÚC PDF:
                - Mỗi chủ đề bắt đầu bằng TIÊU ĐỀ LỚN (VD: "HIỆN TẠI ĐƠN – PRESENT SIMPLE")
                - Phần LÝ THUYẾT bao gồm:
                  • Phần "I. KHÁI NIỆM & CẤU TRÚC"
                  • Phần "II. CHỨC NĂNG" (hoặc "II. CÁCH DÙNG")
                  • Có thể có bảng (tables) minh họa cấu trúc câu
                  • Có phần "MỞ RỘNG", "NOTE", "LƯU Ý"
                - Phần BÀI TẬP bắt đầu bằng "III. BÀI TẬP LUYỆN TẬP" hoặc "BÀI TẬP"
                  • Có đề bài (Bài tập 1, Bài tập 2,...)
                  • Có phần "LỜI GIẢI" hoặc "ĐÁP ÁN" kèm giải thích chi tiết
                
                🎯 CÁCH CHIA LESSONS:
                
                1️⃣ MỖI CHỦ ĐỀ NGỮ PHÁP = 1 LESSON LÝ THUYẾT (THEORY)
                   Ví dụ: "Hiện tại đơn - Present Simple"
                   
                   Nội dung bao gồm:
                   - Phần I: Khái niệm & Cấu trúc
                   - Phần II: Chức năng/Cách dùng
                   - Các phần mở rộng, note, ví dụ
                   
                   Content: HTML format với cấu trúc:
                   • <h2> cho các mục lớn (I, II, III)
                   • <h3> cho các mục nhỏ (1, 2, 3)
                   • <table> cho bảng cấu trúc câu
                   • <p> cho đoạn văn, giải thích
                   • <ul>/<ol> cho danh sách
                   • <strong> cho nhấn mạnh
                
                2️⃣ MỖI PHẦN BÀI TẬP = 1 LESSON THỰC HÀNH (PRACTICE)
                   Ví dụ: "Bài tập - Hiện tại đơn"
                   
                   Kết hợp ĐỀ BÀI + LỜI GIẢI thành questions:
                   - Lấy đề bài từ phần "Bài tập 1", "Bài tập 2"
                   - Lấy đáp án + giải thích từ phần "LỜI GIẢI"
                   - Mỗi câu hỏi cần: questionText + correctAnswer + explanation
                
                3️⃣ LỌC BỎ:
                   - Header/Footer lặp lại (Hotline, Website, Fanpage, số trang)
                   - Trang bìa, mục lục
                   - Quảng cáo, link YouTube cuối bài
                
                📝 VÍ DỤ CHUẨN (DỰA TRÊN PDF MẪU):
                
                {
                  "lessons": [
                    {
                      "title": "Hiện tại đơn - Present Simple",
                      "lessonType": "THEORY",
                      "content": "<h2>I. KHÁI NIỆM & CẤU TRÚC</h2><p>Thì hiện tại đơn (simple present tense): là thì dùng để diễn đạt một hành động mang tính thường xuyên (regular action), theo thói quen (habitual action) hoặc hành động lặp đi lặp lại có tính quy luật, hoặc diễn tả chân lý sự thật hiển nhiên.</p><h3>1. Câu thường</h3><table><thead><tr><th>Động từ tobe</th><th>Động từ thường</th></tr></thead><tbody><tr><td><strong>Cấu trúc:</strong><br>S + am/are/is (not) + N/Adj<br>S + am/are/is + not + N/Adj</td><td><strong>Cấu trúc:</strong><br>S + V(s/es)<br>S + do/does + not + V(ng.thể)</td></tr><tr><td><strong>Chia động từ:</strong><br>- I – am<br>- You, We, They – are<br>- He, She, It - is<br>is not = isn't<br>are not = aren't</td><td><strong>Chia động từ:</strong><br>- I, We, You, They + V(nguyên thể)<br>- He, She, It + V(s/es)<br>do not = don't<br>does not = doesn't</td></tr><tr><td><strong>Ví dụ:</strong><br>- I am a student. (Tôi là một học sinh.)<br>- He isn't a teacher. (Ông ấy không phải là một thầy giáo)</td><td><strong>Ví dụ:</strong><br>- I usually stay up late (Tôi thường xuyên thức khuya)<br>- He doesn't often go to school by bus. (Anh ấy không thường xuyên đi học bằng xe buýt)</td></tr></tbody></table><h3>2. Câu nghi vấn</h3><table><thead><tr><th>Động từ tobe</th><th>Động từ thường</th></tr></thead><tbody><tr><td colspan='2'><strong>Yes / No Question (Câu hỏi Đúng/Sai)</strong></td></tr><tr><td><strong>Cấu trúc:</strong><br>Q: Am/Are/Is (not) + S + N/Adj?<br>A: - Yes, S + am/are/is.<br>&nbsp;&nbsp;&nbsp;&nbsp;- No, S + am not/aren't/isn't.</td><td><strong>Cấu trúc:</strong><br>Q: Do/Does (not) + S + V (ng.thể)?<br>A: - Yes, S + do/does.<br>&nbsp;&nbsp;&nbsp;&nbsp;- No, S + don't/doesn't.</td></tr><tr><td><strong>Ví dụ:</strong><br>Q: Are you a student? (Bạn có phải là sinh viên không?)<br>A: Yes, I am. (Đúng vậy)<br>&nbsp;&nbsp;&nbsp;&nbsp;No, I am not. (Không phải)</td><td><strong>Ví dụ:</strong><br>Q: Does he go to school by bus? (Anh ấy có đến trường bằng xe bus không?)<br>A: Yes, he does. (Có)<br>&nbsp;&nbsp;&nbsp;&nbsp;No, he doesn't. (Không)</td></tr></tbody></table><h3>MỞ RỘNG: CÁCH THÊM S/ES</h3><ul><li>Thêm \"s\" vào đằng sau hầu hết các động từ: need-needs; work-works;…</li><li>Thêm \"es\" vào các động từ kết thúc bằng o, z, ch, sh, x, s: catch - catches; pass - passes; wash - washes; fix - fixes; go – goes, …<br><strong>💡 MsHoa tips:</strong> Xuống Sông Ông CHẳng SHợ Zì</li><li>Bỏ \"y\" và thêm \"ies\" vào sau các động từ kết thúc bởi một phụ âm + y: study - studies; copy – copies; …<br>Nhưng không biến đổi y đứng sau 1 nguyên âm: stay – stays; enjoy – enjoys</li></ul><h2>II. CHỨC NĂNG: THÌ HIỆN TẠI DÙNG ĐỂ LÀM GÌ?</h2><h3>1. Chức năng 1: Diễn đạt một thói quen hoặc hành động lặp đi lặp lại trong hiện tại</h3><p><strong>Ví dụ 1:</strong> I usually get up at 7 a.m. (Tôi thường thức dậy vào 7 giờ sáng)</p><p>Có từ tín hiệu usually, everyday chỉ những thói quen thường xảy ra → Cần điền thì hiện tại đơn</p><h3>2. Chức năng 2: Diễn tả 1 chân lý, sự thật hiển nhiên</h3><p><strong>Ví dụ 2:</strong> The earth moves around the Sun. (Trái đất quay quanh mặt trời)</p><p>Trái đất luôn luôn quay xung quanh mặt trời, đó là chân lý và sẽ không bao giờ thay đổi</p><h3>3. Chức năng 3: Áp dụng để nói về một lịch trình có sẵn, thời gian biểu cố định</h3><p><strong>Ví dụ 3:</strong> The plane takes off at 10 a.m. tomorrow. (Máy bay hạ cánh lúc 10 giờ sáng mai)</p><p>Tuy giờ cất cánh là 10 sáng mai, nhưng đây là lịch trình đã được cố định và không thay đổi</p><p><strong>NOTE:</strong> Dấu hiệu nhận biết – Thì hiện tại đơn hay xuất hiện các từ tín hiệu Always, constantly, usually, frequently, often, occasionally, sometimes, seldom, rarely, every day/week/month, ...</p>",
                      "orderIndex": 1,
                      "pointsReward": 10,
                      "estimatedDuration": 300,
                      "isActive": true
                    },
                    {
                      "title": "Bài tập - Hiện tại đơn",
                      "lessonType": "PRACTICE",
                      "content": "",
                      "orderIndex": 2,
                      "pointsReward": 15,
                      "estimatedDuration": 420,
                      "isActive": true,
                      "questions": [
                        {
                          "questionText": "My father always ________________ Sunday dinner. (make)",
                          "questionType": "FILL_BLANK",
                          "correctAnswer": "makes",
                          "explanation": "Dấu hiệu là trạng từ chỉ tần suất 'always' → Chia động từ ở thì Hiện tại đơn. Chủ ngữ 'father' (ngôi 3 số ít) → Động từ thêm 's'. (Make dinner: Làm bữa tối chứ không nói là cook dinner)",
                          "points": 5,
                          "orderIndex": 1
                        },
                        {
                          "questionText": "Ruth ________________ eggs; they ________________ her ill. (not eat; make)",
                          "questionType": "FILL_BLANK",
                          "correctAnswer": "doesn't eat|make",
                          "explanation": "Diễn đạt một thói quen ở hiện tại → Chia động từ ở thì Hiện tại đơn. Ruth (ngôi 3 số ít) → doesn't eat. They (số nhiều) → make. (Make somebody ill/sick: Làm ai đó phát bệnh hoặc kinh tởm)",
                          "points": 5,
                          "orderIndex": 2
                        }
                      ]
                    }
                  ]
                }
                
                🔑 CHI TIẾT QUAN TRỌNG:
                
                **HTML Structure cho THEORY:**
                • <h2> - Các phần lớn: I. KHÁI NIỆM, II. CHỨC NĂNG
                • <h3> - Các mục con: 1. Chức năng 1, 2. Câu nghi vấn
                • <table> - Bảng so sánh cấu trúc (PHẢI có <thead>, <tbody>, <tr>, <th>, <td>)
                • <p> - Đoạn văn giải thích
                • <ul>/<ol>/<li> - Danh sách
                • <strong> - Nhấn mạnh từ khóa
                • <br> - Xuống dòng trong cell
                
                **Xử lý bảng (TABLE):**
                • Luôn có cấu trúc: <table><thead><tr><th>...</th></tr></thead><tbody><tr><td>...</td></tr></tbody></table>
                • Dùng <br> để xuống dòng trong cell
                • Dùng &nbsp; để tạo khoảng trắng
                • Không dùng colspan/rowspan phức tạp
                
                **questionType:**
                • "FILL_BLANK" - Điền vào chỗ trống
                • "MULTIPLE_CHOICE" - Trắc nghiệm
                • "TRANSLATE" - Dịch câu
                • "VERB_FORM" - Chia động từ
                • "TRUE_FALSE" - Đúng/Sai
                
                **correctAnswer:**
                • Nếu có nhiều đáp án đúng: Dùng "|" ngăn cách (VD: "go|goes")
                • Nếu có nhiều blank: Dùng "|" theo thứ tự (VD: "doesn't eat|make")
                
                **explanation:**
                • BẮT BUỘC phải có giải thích chi tiết
                • Lấy từ phần "LỜI GIẢI" hoặc "Phân tích đáp án" trong PDF
                • Bao gồm: Dấu hiệu nhận biết + Quy tắc ngữ pháp + Ghi chú (nếu có)
                
                **pointsReward & estimatedDuration:**
                • THEORY: 10 points, 180-300 seconds (tùy độ dài)
                • PRACTICE: 15 points, 300-600 seconds (tùy số câu hỏi)
                
                ⚠️ LƯU Ý QUAN TRỌNG:
                • CHỈ TRẢ VỀ JSON HỢP LỆ, KHÔNG TEXT THỪA
                • KHÔNG DÙNG ```json``` wrapper
                • HTML PHẢI HỢP LỆ (đóng thẻ đầy đủ)
                • Bảng phải có đầy đủ <thead> và <tbody>
                • Mỗi câu hỏi PHẢI có explanation chi tiết
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
                    long waitTime = (long) Math.pow(2, attempt) * 1000;
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

            // Clean markdown wrapper
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
            
            // ✅ Validate JSON completeness
            if (!text.endsWith("}")) {
                log.warn("⚠️ JSON may be incomplete, attempting to fix...");
                text = fixIncompleteJSON(text);
            }
            
            // ✅ Log JSON for debugging
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

    private String fixIncompleteJSON(String json) {
        StringBuilder fixed = new StringBuilder(json);
        
        // Count unclosed braces and brackets
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        char prevChar = '\0';
        
        for (char c : json.toCharArray()) {
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                else if (c == '[') bracketCount++;
                else if (c == ']') bracketCount--;
            }
            prevChar = c;
        }
        
        // Close unclosed strings if needed
        if (inString) {
            fixed.append("\"");
            log.warn("⚠️ Fixed unclosed string");
        }
        
        // Close unclosed arrays
        while (bracketCount > 0) {
            fixed.append("]");
            bracketCount--;
            log.warn("⚠️ Added missing ']'");
        }
        
        // Close unclosed objects
        while (braceCount > 0) {
            fixed.append("}");
            braceCount--;
            log.warn("⚠️ Added missing '}'");
        }
        
        return fixed.toString();
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
                int duration = lesson.getLessonType() == LessonType.THEORY ? 180 : 300;
                // Adjust based on content length or question count
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

            // Clean HTML content
            if (lesson.getContent() != null && !lesson.getContent().isEmpty()) {
                lesson.setContent(cleanHtmlContent(lesson.getContent()));
            }

            // Process questions
            if (lesson.getQuestions() != null && !lesson.getQuestions().isEmpty()) {
                int questionOrder = 1;
                for (GrammarQuestionDTO question : lesson.getQuestions()) {
                    if (question.getOrderIndex() == null || question.getOrderIndex() == 0) {
                        question.setOrderIndex(questionOrder++);
                    }
                    if (question.getPoints() == null || question.getPoints() == 0) {
                        question.setPoints(5);
                    }
                    // Clean question text
                    if (question.getQuestionText() != null) {
                        question.setQuestionText(question.getQuestionText().trim());
                    }
                    // Clean answer
                    if (question.getCorrectAnswer() != null) {
                        question.setCorrectAnswer(question.getCorrectAnswer().trim());
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

    /**
     * Clean và chuẩn hóa HTML content
     */
    private String cleanHtmlContent(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        // Normalize whitespace
        html = html.replaceAll("\\s+", " ");
        
        // Remove empty paragraphs
        html = html.replaceAll("<p>\\s*</p>", "");
        
        // Remove double breaks
        html = html.replaceAll("<br>\\s*<br>", "<br>");
        
        // Fix table structure - ensure proper class
        html = html.replaceAll("<table[^>]*>", "<table class=\"tiptap-table\">");
        
        // Remove common PDF artifacts
        html = html.replaceAll("(Hotline:|Website:|Fanpage:)[^<]*", "");
        html = html.replaceAll("Trung tâm luyện thi TOEIC[^<]*", "");
        
        return html.trim();
    }
}