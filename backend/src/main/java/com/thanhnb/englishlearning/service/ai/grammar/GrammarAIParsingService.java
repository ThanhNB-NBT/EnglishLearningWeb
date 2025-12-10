package com.thanhnb.englishlearning.service.ai.grammar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.config.AIConfig;
import com.thanhnb.englishlearning.dto.ParseResult;
import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.service.ai.base.AIParsingService;
import com.thanhnb.englishlearning.service.ai.base.ContentChunk;
import com.thanhnb.englishlearning.service.question.QuestionConverter;
import com.thanhnb.englishlearning.service.question.QuestionMetadataValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * ✅ OPTIMIZED Grammar AI Parsing Service
 * 
 * Key Features:
 * 1. Two-tier prompt system: Core (fixed) + Context (dynamic)
 * 2. Automatic question type detection based on content
 * 3. Flexible section parsing with user guidance
 * 4. Smart pronunciation exercise handling (MC vs Classification)
 */
@Service
@Slf4j
public class GrammarAIParsingService extends AIParsingService<ParseResult> {

    private final GrammarLessonRepository lessonRepository;
    private final QuestionMetadataValidator metadataValidator;
    private final QuestionConverter questionConverter;

    // Dynamic context from admin (can be set per request)
    private String userProvidedContext = "";

    public GrammarAIParsingService(
            AIConfig aiConfig,
            ObjectMapper objectMapper,
            GrammarLessonRepository lessonRepository,
            QuestionMetadataValidator metadataValidator,
            QuestionConverter questionConverter) {
        super(aiConfig, objectMapper);
        this.lessonRepository = lessonRepository;
        this.metadataValidator = metadataValidator;
        this.questionConverter = questionConverter;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CORE PROMPT (Fixed - Defines all question types)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    protected String buildPrompt() {
        return buildCorePrompt() + "\n\n" + buildDynamicContext();
    }

    private String buildCorePrompt() {
        return """
                You are an English textbook parser. Extract lessons following STRICT rules.

                ═══════════════════════════════════════════════════════════════
                🛡️ ANTI-HALLUCINATION RULES (CRITICAL)
                ═══════════════════════════════════════════════════════════════
                1. **Insufficient Content:** If the text provided is just a header, a page number, or a fragment less than 2 sentences -> **RETURN EMPTY LESSONS ARRAY** `{"lessons": []}`.
                2. **Do Not Invent:** Do NOT generate questions or theory that are not explicitly in the text.
                3. **Context Only:** If the text only contains "CONTEXT FROM PREVIOUS PART" and nothing else meaningful -> **RETURN EMPTY**.

                ═══════════════════════════════════════════════════════════════════════════
                🎯 CORE RULES
                ═══════════════════════════════════════════════════════════════════════════
                1. IGNORE "--- CONTEXT FROM PREVIOUS PART ---"
                2. ONLY parse "--- CURRENT CONTENT TO PARSE ---"
                3. Theory → ONE lesson (lessonType: "THEORY")
                4. Each Exercise → SEPARATE lesson (lessonType: "PRACTICE")
                5. NEVER put questions in theory lesson content
                6. AUTO-DETECT question type based on content structure

                ═══════════════════════════════════════════════════════════════════════════
                📦 OUTPUT FORMAT
                ═══════════════════════════════════════════════════════════════════════════
                {
                  "lessons": [
                    {
                      "title": "Pronunciation - Theory",
                      "lessonType": "THEORY",
                      "content": "<h3>I. Phiên âm...</h3><p>Theory content...</p>"
                    },
                    {
                      "title": "Pronunciation - Exercise 1",
                      "lessonType": "PRACTICE",
                      "createQuestions": [...]
                    }
                  ]
                }

                ═══════════════════════════════════════════════════════════════════════════
                ❓ QUESTION TYPES - AUTO DETECTION RULES
                ═══════════════════════════════════════════════════════════════════════════

                🔍 DETECTION LOGIC:

                IF question has format "A. word1 B. word2 C. word3 D. word4"
                AND asks "choose the word with different pronunciation"
                → USE: MULTIPLE_CHOICE (NOT PRONUNCIATION type!)

                IF question asks "classify/group words by pronunciation"
                AND provides categories like "/e/", "/iː/"
                → USE: PRONUNCIATION (classification type)

                IF question has "True/False" or "Right/Wrong"
                → USE: TRUE_FALSE

                IF question has blanks like "I ___(1)___ to school"
                → USE: FILL_BLANK

                IF question asks "rewrite/transform" with beginning phrase
                → USE: SENTENCE_TRANSFORMATION

                IF question asks "arrange/rearrange words"
                → USE: SENTENCE_BUILDING

                IF question asks "match A with B"
                → USE: MATCHING

                IF question asks "find and correct the error"
                → USE: ERROR_CORRECTION

                ═══════════════════════════════════════════════════════════════════════════
                📝 QUESTION TYPE DEFINITIONS
                ═══════════════════════════════════════════════════════════════════════════

                1️⃣ MULTIPLE_CHOICE (Standard A,B,C,D - Most common for pronunciation exercises)
                {
                  "questionText": "Choose the word whose underlined part is pronounced differently",
                  "questionType": "MULTIPLE_CHOICE",
                  "points": 5,
                  "orderIndex": 1,
                  "options": [
                    {"text": "head", "isCorrect": false, "order": 1},
                    {"text": "please", "isCorrect": true, "order": 2},
                    {"text": "heavy", "isCorrect": false, "order": 3},
                    {"text": "measure", "isCorrect": false, "order": 4}
                  ]
                }

                2️⃣ PRONUNCIATION (Classification/Grouping - Rare, only when explicitly grouping)
                {
                  "questionText": "Classify these words by their vowel sound",
                  "questionType": "PRONUNCIATION",
                  "points": 5,
                  "orderIndex": 1,
                  "words": ["head", "please", "heavy", "measure"],
                  "categories": ["/e/", "/iː/"],
                  "classifications": [
                    {"word": "head", "category": "/e/"},
                    {"word": "please", "category": "/iː/"},
                    {"word": "heavy", "category": "/e/"},
                    {"word": "measure", "category": "/e/"}
                  ]
                }

                3️⃣ TRUE_FALSE
                {
                  "questionText": "The sky is blue.",
                  "questionType": "TRUE_FALSE",
                  "points": 5,
                  "orderIndex": 1,
                  "options": [
                    {"text": "True", "isCorrect": true, "order": 1},
                    {"text": "False", "isCorrect": false, "order": 2}
                  ]
                }

                4️⃣ FILL_BLANK
                {
                  "questionText": "I ___(1)___ to school yesterday.",
                  "questionType": "FILL_BLANK",
                  "points": 5,
                  "orderIndex": 1,
                  "blanks": [
                    {"position": 1, "correctAnswers": ["went", "walked"]}
                  ]
                }

                5️⃣ SENTENCE_TRANSFORMATION
                {
                  "questionText": "Rewrite: It is a pity I didn't see him.",
                  "questionType": "SENTENCE_TRANSFORMATION",
                  "points": 5,
                  "orderIndex": 1,
                  "originalSentence": "It is a pity I didn't see him.",
                  "beginningPhrase": "I wish",
                  "correctAnswers": ["I wish I had seen him"]
                }

                6️⃣ SENTENCE_BUILDING
                {
                  "questionText": "Arrange: I / go / will / home",
                  "questionType": "SENTENCE_BUILDING",
                  "points": 5,
                  "orderIndex": 1,
                  "words": ["I", "go", "will", "home"],
                  "correctSentence": "I will go home"
                }

                7️⃣ MATCHING
                {
                  "questionText": "Match words with meanings",
                  "questionType": "MATCHING",
                  "points": 5,
                  "orderIndex": 1,
                  "pairs": [
                    {"left": "happy", "right": "vui vẻ", "order": 1},
                    {"left": "sad", "right": "buồn", "order": 2}
                  ]
                }

                8️⃣ ERROR_CORRECTION
                {
                  "questionText": "He go to school every day.",
                  "questionType": "ERROR_CORRECTION",
                  "points": 5,
                  "orderIndex": 1,
                  "errorText": "go",
                  "correction": "goes"
                }

                ═══════════════════════════════════════════════════════════════════════════
                📚 EXAMPLE: PRONUNCIATION EXERCISE (MULTIPLE CHOICE)
                ═══════════════════════════════════════════════════════════════════════════

                INPUT TEXT:
                ```
                Exercise 1:
                1. A. head B. please C. heavy D. measure
                2. A. note B. gloves C. some D. other

                Đáp án:
                1.B  2.A
                ```

                ✅ CORRECT OUTPUT (Use MULTIPLE_CHOICE, not PRONUNCIATION):
                {
                  "lessons": [
                    {
                      "title": "Pronunciation - Exercise 1",
                      "lessonType": "PRACTICE",
                      "createQuestions": [
                        {
                          "questionText": "Choose the word whose underlined part is pronounced differently: A. head B. please C. heavy D. measure",
                          "questionType": "MULTIPLE_CHOICE",
                          "points": 5,
                          "orderIndex": 1,
                          "options": [
                            {"text": "head", "isCorrect": false, "order": 1},
                            {"text": "please", "isCorrect": true, "order": 2},
                            {"text": "heavy", "isCorrect": false, "order": 3},
                            {"text": "measure", "isCorrect": false, "order": 4}
                          ]
                        },
                        {
                          "questionText": "Choose the word whose underlined part is pronounced differently: A. note B. gloves C. some D. other",
                          "questionType": "MULTIPLE_CHOICE",
                          "points": 5,
                          "orderIndex": 2,
                          "options": [
                            {"text": "note", "isCorrect": true, "order": 1},
                            {"text": "gloves", "isCorrect": false, "order": 2},
                            {"text": "some", "isCorrect": false, "order": 3},
                            {"text": "other", "isCorrect": false, "order": 4}
                          ]
                        }
                      ]
                    }
                  ]
                }

                ═══════════════════════════════════════════════════════════════════════════
                ⚠️ CRITICAL VALIDATION CHECKLIST
                ═══════════════════════════════════════════════════════════════════════════
                ✓ Pronunciation "choose different word" exercises → MULTIPLE_CHOICE
                ✓ Each question has ALL required fields per DTO
                ✓ Answer keys are integrated (don't output separately)
                ✓ orderIndex starts from 1 in each lesson
                ✓ All "order" fields start from 1
                """;
    }

    private String buildDynamicContext() {
        if (userProvidedContext == null || userProvidedContext.trim().isEmpty()) {
            return """
                    ═══════════════════════════════════════════════════════════════════════════
                    📋 PARSING INSTRUCTIONS (Default)
                    ═══════════════════════════════════════════════════════════════════════════
                    Parse ALL content in the document following the core rules above.
                    """;
        }

        return """
                ═══════════════════════════════════════════════════════════════════════════
                📋 PARSING INSTRUCTIONS (User Provided)
                ═══════════════════════════════════════════════════════════════════════════
                """ + userProvidedContext + """

                Follow these specific instructions while adhering to all core rules above.
                """;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API - Enhanced with context support
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Parse file with optional user context for better accuracy
     * 
     * @param file           PDF/DOCX file to parse
     * @param topicId        Topic ID to assign lessons to
     * @param pages          Specific pages to parse (null = all pages)
     * @param parsingContext User guidance (e.g., "Only parse sections I, II and
     *                       exercises. Skip section III")
     */
    public ParseResult parseFileWithContext(
            MultipartFile file,
            Long topicId,
            List<Integer> pages,
            String parsingContext) throws Exception {

        // Set dynamic context
        this.userProvidedContext = parsingContext;

        log.info("📄 Parsing: {} (topicId: {}, pages: {}, has context: {})",
                file.getOriginalFilename(),
                topicId,
                pages != null ? pages.size() : "all",
                parsingContext != null && !parsingContext.trim().isEmpty());

        if (parsingContext != null && !parsingContext.trim().isEmpty()) {
            log.info("📋 User context: {}", parsingContext);
        }

        ParseResult result = parseFile(file, pages);

        if (topicId != null) {
            adjustOrderForTopic(result, topicId);
        }

        // Clear context after use
        this.userProvidedContext = "";

        return result;
    }

    // Backward compatibility
    public ParseResult parseFileWithTopicId(MultipartFile file, Long topicId, List<Integer> pages)
            throws Exception {
        return parseFileWithContext(file, topicId, pages, null);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PARSING WITH ENHANCED VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    protected ParseResult parseResponse(String jsonResponse) throws Exception {
        try {
            log.debug("🔍 Parsing AI response (length: {} chars)", jsonResponse.length());

            String preview = jsonResponse.substring(0, Math.min(500, jsonResponse.length()));
            log.debug("📄 Response preview: {}", preview);

            ParseResult result = objectMapper.readValue(jsonResponse, ParseResult.class);

            if (result == null) {
                throw new Exception("Parsed result is null");
            }

            if (result.lessons == null) {
                result.lessons = new ArrayList<>();
            }

            log.info("✅ Parsed {} lessons", result.lessons.size());

            for (int i = 0; i < result.lessons.size(); i++) {
                GrammarLessonDTO lesson = result.lessons.get(i);
                int qCount = lesson.getCreateQuestions() != null ? lesson.getCreateQuestions().size() : 0;
                log.info("  📚 Lesson {}: '{}' ({}) - {} questions",
                        i + 1, lesson.getTitle(), lesson.getLessonType(), qCount);

                // Log question types for verification
                if (qCount > 0) {
                    Map<String, Integer> typeCount = new HashMap<>();
                    lesson.getCreateQuestions()
                            .forEach(q -> typeCount.merge(q.getQuestionType().toString(), 1, (oldVal, newVal) -> oldVal + newVal));
                    log.debug("    Question types: {}", typeCount);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("❌ JSON Parsing Error: {}", e.getMessage());
            log.error("📄 Raw response (first 1000 chars): {}",
                    jsonResponse.substring(0, Math.min(1000, jsonResponse.length())));
            throw new Exception("Failed to parse AI response: " + e.getMessage(), e);
        }
    }

    @Override
    protected ParseResult postProcess(ParseResult result) {
        if (result.lessons == null) {
            log.warn("⚠️ No lessons to post-process");
            return result;
        }

        log.info("🔧 Post-processing {} lessons", result.lessons.size());

        Iterator<GrammarLessonDTO> iterator = result.lessons.iterator();
        int lessonIndex = 0;

        while (iterator.hasNext()) {
            GrammarLessonDTO lesson = iterator.next();
            lessonIndex++;

            // --- LOGIC LỌC PHANTOM LESSON ---
            boolean hasQuestions = lesson.getCreateQuestions() != null && !lesson.getCreateQuestions().isEmpty();
            boolean hasContent = lesson.getContent() != null && lesson.getContent().length() > 50; // Nội dung HTML > 50
                                                                                                   // ký tự

            // Nếu bài học KHÔNG có câu hỏi VÀ (không có nội dung hoặc nội dung quá
            // ngắn/rác)
            // -> XÓA NGAY
            if (!hasQuestions && !hasContent) {
                log.warn("🗑️ Removed phantom lesson: '{}' (No questions, content too short)", lesson.getTitle());
                iterator.remove();
                continue;
            }

            // Nếu bài học có tiêu đề mặc định kiểu "Section 1" mà không có nội dung gì ->
            // XÓA
            if (lesson.getTitle().toLowerCase().startsWith("section") && !hasQuestions && !hasContent) {
                iterator.remove();
                continue;
            }

            if (lesson.getTitle() == null || lesson.getTitle().trim().isEmpty()) {
                log.warn("⚠️ Removing lesson #{} - missing title", lessonIndex);
                iterator.remove();
                continue;
            }

            log.debug("📖 Processing: '{}'", lesson.getTitle());

            // Set defaults
            if (lesson.getLessonType() == null) {
                lesson.setLessonType(LessonType.THEORY);
            }
            if (lesson.getIsActive() == null) {
                lesson.setIsActive(true);
            }
            if (lesson.getPointsReward() == null) {
                lesson.setPointsReward(10);
            }
            if (lesson.getTimeLimitSeconds() == null) {
                lesson.setTimeLimitSeconds(300);
            }

            // Validate questions
            if (lesson.getCreateQuestions() != null && !lesson.getCreateQuestions().isEmpty()) {
                processQuestions(lesson);
            }
        }

        log.info("✅ Post-processing complete. Final: {} lessons", result.lessons.size());
        return result;
    }

    private void processQuestions(GrammarLessonDTO lesson) {
        List<CreateQuestionDTO> questions = lesson.getCreateQuestions();
        log.debug("  🔍 Validating {} questions", questions.size());

        Iterator<CreateQuestionDTO> qIterator = questions.iterator();
        int validCount = 0;
        int order = 1;

        while (qIterator.hasNext()) {
            CreateQuestionDTO question = qIterator.next();

            try {
                if (question.getOrderIndex() == null || question.getOrderIndex() == 0) {
                    question.setOrderIndex(order);
                }

                Map<String, Object> metadata = questionConverter.buildMetadata(question);

                if (metadata != null) {
                    metadataValidator.sanitizeMetadata(question.getQuestionType(), metadata);
                    metadataValidator.validate(question.getQuestionType(), metadata);
                }

                validCount++;
                order++;

                log.debug("    ✓ Q{}: {} ({})",
                        question.getOrderIndex(),
                        truncate(question.getQuestionText(), 40),
                        question.getQuestionType());

            } catch (Exception e) {
                log.warn("    ✗ Invalid question: {} - {}",
                        truncate(question.getQuestionText(), 40),
                        e.getMessage());
                qIterator.remove();
            }
        }

        // Auto-set PRACTICE if has questions
        if (validCount > 0) {
            lesson.setLessonType(LessonType.PRACTICE);
        }

        log.debug("  ✓ {} valid questions", validCount);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MERGING (Conservative approach)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    protected ParseResult mergeResults(List<ParseResult> results) {
        log.info("🔗 Merging {} chunks", results.size());

        ParseResult merged = new ParseResult();
        merged.lessons = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            ParseResult result = results.get(i);

            if (result == null || result.lessons == null) {
                continue;
            }

            log.debug("  📦 Chunk {} has {} lessons", i + 1, result.lessons.size());

            for (GrammarLessonDTO lesson : result.lessons) {
                if (merged.lessons.isEmpty()) {
                    merged.lessons.add(lesson);
                    continue;
                }

                GrammarLessonDTO lastLesson = merged.lessons.get(merged.lessons.size() - 1);

                // Only merge if EXACT same title + type
                if (shouldMerge(lastLesson, lesson)) {
                    mergeLessons(lastLesson, lesson);
                    log.debug("    🔗 Merged into: '{}'", lastLesson.getTitle());
                } else {
                    merged.lessons.add(lesson);
                    log.debug("    ➕ Added: '{}'", lesson.getTitle());
                }
            }
        }

        // Re-index
        for (int i = 0; i < merged.lessons.size(); i++) {
            merged.lessons.get(i).setOrderIndex(i + 1);
        }

        log.info("✅ Merge complete. Total: {} lessons", merged.lessons.size());
        return merged;
    }

    private boolean shouldMerge(GrammarLessonDTO l1, GrammarLessonDTO l2) {
        return l1.getTitle().equals(l2.getTitle()) &&
                l1.getLessonType() == l2.getLessonType();
    }

    private void mergeLessons(GrammarLessonDTO target, GrammarLessonDTO source) {
        if (source.getContent() != null && !source.getContent().trim().isEmpty()) {
            String existing = target.getContent() != null ? target.getContent() : "";
            target.setContent(existing + "\n" + source.getContent());
        }

        if (source.getCreateQuestions() != null && !source.getCreateQuestions().isEmpty()) {
            if (target.getCreateQuestions() == null) {
                target.setCreateQuestions(new ArrayList<>());
            }
            target.getCreateQuestions().addAll(source.getCreateQuestions());

            for (int i = 0; i < target.getCreateQuestions().size(); i++) {
                target.getCreateQuestions().get(i).setOrderIndex(i + 1);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITIES & OVERRIDES
    // ═══════════════════════════════════════════════════════════════════════════

    private void adjustOrderForTopic(ParseResult result, Long topicId) {
        if (result.lessons == null || result.lessons.isEmpty())
            return;

        Integer maxOrder = lessonRepository.findMaxOrderIndexByTopicId(topicId);
        int baseOrder = (maxOrder != null ? maxOrder : 0);

        for (GrammarLessonDTO lesson : result.lessons) {
            lesson.setTopicId(topicId);
            lesson.setOrderIndex(++baseOrder);
        }
    }

    @Override
    protected String getModuleName() {
        return "GRAMMAR";
    }

    @Override
    protected double getTemperature() {
        return 0.2; // Low temperature for consistent parsing
    }

    @Override
    protected long getMaxFileSize() {
        return 20 * 1024 * 1024;
    }

    @Override
    protected boolean isImage(String m) {
        return m != null && m.startsWith("image/");
    }

    @Override
    protected boolean isPDF(String m) {
        return "application/pdf".equals(m);
    }

    @Override
    protected boolean isDOCX(String m) {
        return m != null && m.contains("wordprocessingml");
    }

    @Override
    protected String extractTextFromFile(MultipartFile file, List<Integer> pages) throws Exception {
        String mimeType = file.getContentType();

        if (isPDF(mimeType)) {
            return extractTextFromPDF(file, pages);
        } else if (isDOCX(mimeType)) {
            return extractTextFromDOCX(file);
        }

        throw new Exception("Unsupported file type: " + mimeType);
    }

    @Override
    protected List<ContentChunk> splitIntoLogicalChunks(String text) {
        List<ContentChunk> chunks = new ArrayList<>();

        // Regex cũ của bạn (Giữ nguyên vì nó tốt)
        String regex = "(?m)^(?=(CHUYÊN ĐỀ|UNIT|CHAPTER|PART|PHẦN|TEST|PRACTICE TEST|Exercise|Bài tập)\\s+\\d+|[IVX]+\\.\\s)";

        String[] sections = text.split(regex);

        if (sections.length > 1) {
            for (int i = 0; i < sections.length; i++) {
                String section = sections[i].trim();

                // --- CẢI TIẾN BỘ LỌC RÁC ---
                // 1. Bỏ qua nếu quá ngắn (Tăng từ 50 lên 200 ký tự)
                // Các bài học thật sự thường dài hơn 200 ký tự. Header/Số trang thường ngắn
                // hơn.
                if (section.length() < 150) {
                    log.info("⚠️ Skipping chunk {} (Too short, likely header/footer): {}...", i, truncate(section, 20));
                    continue;
                }

                // 2. Bỏ qua nếu chỉ toàn số hoặc ký tự đặc biệt (Rác do PDF lỗi)
                if (section.matches("^[0-9\\s\\.,\\-\\|]+$")) {
                    continue;
                }

                String title = extractSectionTitle(section, i + 1);
                chunks.add(new ContentChunk(title, section));
            }
        } else {
            // Fallback: Nếu không tìm thấy header, vẫn check độ dài
            if (text.length() > 150) {
                chunks = splitByCharacterCount(text, 6000);
            }
        }

        return chunks;
    }

    // Helper để log gọn
    private String truncate(String s, int len) {
        return s.length() > len ? s.substring(0, len) : s;
    }

    @Override
    protected void validateFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("File is empty");
        }

        if (file.getSize() > getMaxFileSize()) {
            throw new Exception("File too large");
        }

        String mimeType = file.getContentType();
        if (!isImage(mimeType) && !isPDF(mimeType) && !isDOCX(mimeType)) {
            throw new Exception("Unsupported file type: " + mimeType);
        }
    }
}