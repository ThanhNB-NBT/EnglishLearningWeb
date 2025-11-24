package com.thanhnb.englishlearning.service.ai.grammar;

import com.thanhnb.englishlearning.config.AIConfig;
import com.thanhnb.englishlearning.dto.ParseResult;
import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.service.ai.base.AIParsingService;
import com.thanhnb.englishlearning.service.question.QuestionMetadataValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * AI Parsing Service for Grammar module
 * 
 * Responsibilities:
 * - Parse PDF/DOCX/Image files using Gemini AI
 * - Convert raw text to structured Grammar lessons
 * - Validate and normalize question metadata
 * - Auto-adjust orderIndex based on existing lessons
 * 
 * @author thanhnb
 * @version 2.0 - Metadata-based architecture
 */
@Service
@Slf4j
public class GrammarAIParsingService extends AIParsingService<ParseResult> {

    private final GrammarLessonRepository lessonRepository;
    private final QuestionMetadataValidator metadataValidator;

    // Constants for default values
    private static final int DEFAULT_THEORY_POINTS = 10;
    private static final int DEFAULT_PRACTICE_POINTS = 15;
    private static final int BASE_THEORY_DURATION = 180;
    private static final int BASE_PRACTICE_DURATION = 300;
    private static final int DURATION_PER_QUESTION = 30;
    private static final int LONG_CONTENT_BONUS = 120;
    private static final int LONG_CONTENT_THRESHOLD = 5000;

    public GrammarAIParsingService(
            AIConfig aiConfig,
            GrammarLessonRepository lessonRepository,
            QuestionMetadataValidator metadataValidator) {
        super(aiConfig);
        this.lessonRepository = lessonRepository;
        this.metadataValidator = metadataValidator;
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected String getModuleName() {
        return "GRAMMAR";
    }

    @Override
    protected double getTemperature() {
        return 0.5; // Lower temperature for more consistent output
    }

    @Override
    protected String buildPrompt() {
        return """
                You are an AI assistant specializing in English grammar learning materials.

                TASK: Analyze the content and create JSON with grammar lessons and exercises.

                ═══════════════════════════════════════════════════════════════
                📋 LESSON CLASSIFICATION RULES
                ═══════════════════════════════════════════════════════════════

                **THEORY (Grammar explanation):**
                - Explains concepts, definitions, grammar structures
                - Classification tables, examples with translations
                - Rules, usage, comparison charts
                - Title keywords: "Part", "Chapter", "Concept", "Structure", "Grammar"

                **PRACTICE (Exercises):**
                - Questions, fill-in-the-blanks, multiple choice
                - Error correction, sentence transformation
                - Title keywords: "Exercise", "Practice", "Drill", "Test"

                ═══════════════════════════════════════════════════════════════
                🎵 AUDIO SUPPORT (Optional)
                ═══════════════════════════════════════════════════════════════

                **For Phonetics THEORY lessons, add metadata:**
                {
                  "title": "Unit 1 - Phonetics: /æ/ vs /e/",
                  "lessonType": "THEORY",
                  "content": "<h2>Pronunciation...</h2>",
                  "metadata": {
                    "audioUrl": "https://storage.../unit1_phonetics.mp3",
                    "audioDuration": 120,
                    "audioType": "PHONETICS"
                  },
                  "orderIndex": 1
                }

                **For regular lessons (no audio):**
                {
                  "title": "Present Simple Tense",
                  "lessonType": "THEORY",
                  "content": "<h2>Grammar...</h2>",
                  "metadata": null,
                  "orderIndex": 1
                }

                NOTE: metadata is OPTIONAL, only add if lesson has audio/video

                ═══════════════════════════════════════════════════════════════
                🔀 HOW TO SPLIT LESSONS
                ═══════════════════════════════════════════════════════════════

                **Principles:**
                1. Each GRAMMAR TOPIC = 1 THEORY + 1 PRACTICE (if exercises exist)
                2. If theory only → Create 1 THEORY lesson
                3. If multiple exercise types → Split into separate PRACTICE lessons
                4. Keep related content together

                ═══════════════════════════════════════════════════════════════
                📄 CONTENT FORMAT (THEORY ONLY)
                ═══════════════════════════════════════════════════════════════

                **HTML Structure:**
                <h2>MAIN HEADING</h2>
                <h3>Sub-heading</h3>
                <p>Explanation paragraph...</p>

                <table class="tiptap-table">
                  <thead>
                    <tr><th>Form</th><th>Example</th><th>Usage</th></tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td><strong>S + V(s/es)</strong></td>
                      <td>She works here.</td>
                      <td>Daily routine</td>
                    </tr>
                  </tbody>
                </table>

                <ul>
                  <li><strong>Affirmative:</strong> S + V(s/es)</li>
                  <li><strong>Negative:</strong> S + don't/doesn't + V</li>
                </ul>

                **Requirements:**
                - MUST include class="tiptap-table"
                - Use <br> for line breaks inside cells
                - Close all tags properly

                ═══════════════════════════════════════════════════════════════
                ❓ QUESTION FORMATS (PRACTICE ONLY)
                ═══════════════════════════════════════════════════════════════

                **1. MULTIPLE_CHOICE:**
                {
                  "questionText": "She ___ to school every day.",
                  "questionType": "MULTIPLE_CHOICE",
                  "explanation": "Present simple with 3rd person",
                  "points": 5,
                  "orderIndex": 1,
                  "metadata": {
                    "hint": "Think about 3rd person singular",
                    "options": [
                      {"text": "go", "isCorrect": false, "order": 1},
                      {"text": "goes", "isCorrect": true, "order": 2},
                      {"text": "going", "isCorrect": false, "order": 3},
                      {"text": "went", "isCorrect": false, "order": 4}
                    ]
                  }
                }

                **2. FILL_BLANK:**
                {
                  "questionText": "She (be) ___ young when she started.",
                  "questionType": "FILL_BLANK",
                  "explanation": "Simple past tense",
                  "points": 5,
                  "orderIndex": 1,
                  "metadata": {
                    "hint": "Past tense of 'be' with singular subject",
                    "answer": "was",
                    "caseSensitive": false
                  }
                }

                **3. SHORT_ANSWER:**
                {
                  "questionText": "What is the past tense of 'go'?",
                  "questionType": "SHORT_ANSWER",
                  "explanation": "Irregular verb",
                  "points": 5,
                  "orderIndex": 1,
                  "metadata": {
                    "hint": "It's an irregular verb",
                    "answer": "went",
                    "caseSensitive": false
                  }
                }

                **4. VERB_FORM:**
                {
                  "questionText": "Complete with correct form: I (study) ___ English now.",
                  "questionType": "VERB_FORM",
                  "explanation": "Present continuous for current action",
                  "points": 5,
                  "orderIndex": 1,
                  "metadata": {
                    "hint": "Use present continuous",
                    "answer": "am studying",
                    "caseSensitive": false
                  }
                }

                **5. ERROR_CORRECTION:**
                {
                  "questionText": "Find and correct: She don't like coffee.",
                  "questionType": "ERROR_CORRECTION",
                  "explanation": "Subject-verb agreement",
                  "points": 5,
                  "orderIndex": 1,
                  "metadata": {
                    "hint": "Check the auxiliary verb",
                    "answer": "doesn't",
                    "caseSensitive": false
                  }
                }

                **6. MATCHING:**
                {
                  "questionText": "Match the words with their meanings:",
                  "questionType": "MATCHING",
                  "explanation": "Vocabulary matching",
                  "points": 10,
                  "orderIndex": 1,
                  "metadata": {
                    "hint": "Think about common usage",
                    "pairs": [
                      {"left": "happy", "right": "vui vẻ", "order": 1},
                      {"left": "sad", "right": "buồn", "order": 2},
                      {"left": "angry", "right": "tức giận", "order": 3}
                    ]
                  }
                }

                **7. SENTENCE_BUILDING:**
                {
                  "questionText": "Arrange words to make a correct sentence:",
                  "questionType": "SENTENCE_BUILDING",
                  "explanation": "Word order in present simple",
                  "points": 10,
                  "orderIndex": 1,
                  "metadata": {
                    "hint": "Subject + Verb + Object",
                    "words": ["school", "goes", "She", "to", "every", "day"],
                    "correctSentence": "She goes to school every day"
                  }
                }

                **8. COMPLETE_CONVERSATION:**
                {
                  "questionText": "Complete the conversation:",
                  "questionType": "COMPLETE_CONVERSATION",
                  "explanation": "Appropriate response",
                  "points": 10,
                  "orderIndex": 1,
                  "metadata": {
                    "hint": "Think about polite responses",
                    "conversationContext": [
                      "A: How are you today?",
                      "B: ___"
                    ],
                    "options": ["I'm fine, thank you", "Yes, I do", "No, I'm not"],
                    "correctAnswer": "I'm fine, thank you"
                  }
                }

                **9. PRONUNCIATION:**
                {
                  "questionText": "Classify words by vowel sound:",
                  "questionType": "PRONUNCIATION",
                  "explanation": "Distinguishing /æ/ and /e/",
                  "points": 10,
                  "orderIndex": 1,
                  "metadata": {
                    "hint": "Listen to the vowel sound",
                    "words": ["cat", "bed", "hat", "pen"],
                    "audioUrl": "https://storage.../pronunciation.mp3",
                    "categories": ["/æ/", "/e/"],
                    "correctClassifications": [
                      {"word": "cat", "category": "/æ/"},
                      {"word": "bed", "category": "/e/"}
                    ]
                  }
                }

                **10. READING_COMPREHENSION:**
                {
                  "questionText": "Read and complete the blanks:",
                  "questionType": "READING_COMPREHENSION",
                  "explanation": "Reading comprehension",
                  "points": 15,
                  "orderIndex": 1,
                  "metadata": {
                    "hint": "Read the whole passage first",
                    "passage": "My family (1)___ four people. We (2)___ in a small house.",
                    "blanks": [
                      {
                        "position": 1,
                        "options": ["have", "has", "having"],
                        "correctAnswer": "has"
                      },
                      {
                        "position": 2,
                        "options": ["live", "lives", "living"],
                        "correctAnswer": "live"
                      }
                    ]
                  }
                }

                **11. OPEN_ENDED:**
                {
                  "questionText": "Write about your daily routine (50-100 words):",
                  "questionType": "OPEN_ENDED",
                  "explanation": "Free writing practice",
                  "points": 20,
                  "orderIndex": 1,
                  "metadata": {
                    "hint": "Use present simple tense",
                    "suggestedAnswer": "I wake up at 6 AM...",
                    "timeLimitSeconds": "600",
                    "minWords": 50,
                    "maxWords": 100
                  }
                }

                **12. TRUE_FALSE:**
                {
                  "questionText": "She go to school every day.",
                  "questionType": "TRUE_FALSE",
                  "explanation": "Present simple with 3rd person",
                  "points": 5,
                  "orderIndex": 1,
                  "metadata": {
                    "hint": "Think about 3rd person singular",
                    "options": [
                      {"text": "true", "isCorrect": false, "order": 1},
                      {"text": "false", "isCorrect": true, "order": 2},
                    ]
                  }
                }

                ═══════════════════════════════════════════════════════════════
                📦 OUTPUT JSON FORMAT
                ═══════════════════════════════════════════════════════════════

                {
                  "lessons": [
                    {
                      "title": "Present Simple Tense",
                      "lessonType": "THEORY",
                      "content": "<h2>Present Simple</h2>...",
                      "metadata": null,
                      "orderIndex": 1,
                      "pointsReward": 10,
                      "estimatedDuration": 300,
                      "isActive": true,
                      "questions": null
                    },
                    {
                      "title": "Exercise: Present Simple",
                      "lessonType": "PRACTICE",
                      "content": null,
                      "metadata": null,
                      "orderIndex": 2,
                      "pointsReward": 15,
                      "estimatedDuration": 600,
                      "isActive": true,
                      "questions": [...]
                    }
                  ]
                }

                ═══════════════════════════════════════════════════════════════
                ⚠️ CRITICAL REQUIREMENTS
                ═══════════════════════════════════════════════════════════════

                1. Return ONLY valid JSON, NO extra text or markdown
                2. NO ```json``` wrapper
                3. orderIndex starts from 1 (will be adjusted later)
                4. HTML must be valid and properly closed
                5. MULTIPLE_CHOICE must have exactly 1 correct answer
                6. MATCHING must have at least 2 pairs
                7. All metadata fields must match exact structure
                8. explanation max 200 chars
                9. pointsReward: THEORY = 10, PRACTICE = 15
                10. estimatedDuration: THEORY = 180-300s, PRACTICE = 300-600s
                11. lessonType must be "THEORY" or "PRACTICE" (case-sensitive)
                12. caseSensitive defaults to false

                ═══════════════════════════════════════════════════════════════
                """;
    }

    // ═══════════════════════════════════════════════════════════════
    // PARSING & VALIDATION
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected ParseResult parseResponse(String jsonResponse) throws Exception {
        try {
            ParseResult result = gson.fromJson(jsonResponse, ParseResult.class);

            if (result == null) {
                throw new Exception("Failed to parse JSON response");
            }

            if (result.getError() != null) {
                throw new Exception("AI returned error: " + result.getError());
            }

            if (result.lessons == null || result.lessons.isEmpty()) {
                throw new Exception("No lessons found in AI response");
            }

            log.info("✅ Parsed {} lessons from AI", result.lessons.size());
            return result;

        } catch (Exception e) {
            log.error("❌ Error parsing Gemini response: {}", e.getMessage());
            throw new Exception("Invalid JSON from Gemini: " + e.getMessage(), e);
        }
    }

    @Override
    protected ParseResult postProcess(ParseResult result) {
        if (result.lessons == null || result.lessons.isEmpty()) {
            log.warn("⚠️ Empty result, skipping post-process");
            return result;
        }

        int theoryCount = 0;
        int practiceCount = 0;
        int totalQuestions = 0;

        for (GrammarLessonDTO lesson : result.lessons) {
            // Process lesson
            processLesson(lesson);

            // Count statistics
            if (lesson.getLessonType() == LessonType.THEORY) {
                theoryCount++;
            } else {
                practiceCount++;
            }

            // Process questions if exist
            if (lesson.getQuestions() != null && !lesson.getQuestions().isEmpty()) {
                totalQuestions += lesson.getQuestions().size();
                processQuestions(lesson);
            }
        }

        // Add summary metadata
        ParseResult.ParseMetadata metadata = new ParseResult.ParseMetadata();
        metadata.setTotalTheoryLessons(theoryCount);
        metadata.setTotalPracticeLessons(practiceCount);
        metadata.setTotalQuestions(totalQuestions);
        result.setMetadata(metadata);

        log.info("✅ Post-process complete: {} theory, {} practice, {} questions",
                theoryCount, practiceCount, totalQuestions);

        return result;
    }

    @Override
    protected ParseResult mergeResults(List<ParseResult> results) {
        if (results.size() == 1) {
            return results.get(0);
        }

        ParseResult merged = new ParseResult();
        merged.lessons = new ArrayList<>();

        // Merge all lessons
        for (ParseResult result : results) {
            if (result != null && result.lessons != null) {
                merged.lessons.addAll(result.lessons);
            }
        }

        // Sort by orderIndex
        merged.lessons.sort(Comparator.comparingInt(
                l -> l.getOrderIndex() != null ? l.getOrderIndex() : Integer.MAX_VALUE));

        // Re-index sequentially
        for (int i = 0; i < merged.lessons.size(); i++) {
            merged.lessons.get(i).setOrderIndex(i + 1);
        }

        log.info("✅ Merged {} chunks into {} lessons", results.size(), merged.lessons.size());
        return merged;
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API - Enhanced with topicId support
    // ═══════════════════════════════════════════════════════════════

    /**
     * Parse file and auto-adjust orderIndex based on existing lessons in topic
     * 
     * @param file    File to parse (PDF/DOCX/Image)
     * @param topicId Target topic ID
     * @param pages   Page numbers (null = all pages)
     * @return Parsed result with adjusted orderIndex
     */
    public ParseResult parseFileWithTopicId(MultipartFile file, Long topicId, List<Integer> pages)
            throws Exception {

        log.info("📄 Parsing file for topic {}: {}", topicId, file.getOriginalFilename());

        // Parse file using base class method
        ParseResult result = parseFile(file, pages);

        // Adjust orderIndex based on existing lessons
        if (topicId != null) {
            adjustOrderIndexForTopic(result, topicId);
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Process single lesson: validate and set defaults
     */
    private void processLesson(GrammarLessonDTO lesson) {
        // Infer lesson type if missing
        if (lesson.getLessonType() == null) {
            lesson.setLessonType(inferLessonType(lesson));
        }

        // Set default points
        if (lesson.getPointsReward() == null || lesson.getPointsReward() == 0) {
            lesson.setPointsReward(getDefaultPoints(lesson.getLessonType()));
        }

        // Calculate estimated duration
        if (lesson.getEstimatedDuration() == null || lesson.getEstimatedDuration() == 0) {
            lesson.setEstimatedDuration(calculateDuration(lesson));
        }

        // Set active by default
        if (lesson.getIsActive() == null) {
            lesson.setIsActive(true);
        }

        // Clean HTML content for theory lessons
        if (lesson.getLessonType() == LessonType.THEORY && lesson.getContent() != null) {
            lesson.setContent(cleanHtmlContent(lesson.getContent()));
        }

        // Validate lesson metadata if exists
        if (lesson.getMetadata() != null) {
            validateLessonMetadata(lesson.getMetadata(), lesson.getTitle());
        }
    }

    /**
     * Infer lesson type from content
     */
    private LessonType inferLessonType(GrammarLessonDTO lesson) {
        // Has content → likely THEORY
        if (lesson.getContent() != null && !lesson.getContent().trim().isEmpty()) {
            return LessonType.THEORY;
        }

        // Has questions → PRACTICE
        if (lesson.getQuestions() != null && !lesson.getQuestions().isEmpty()) {
            return LessonType.PRACTICE;
        }

        // Check title keywords
        String title = lesson.getTitle() != null ? lesson.getTitle().toLowerCase() : "";
        if (title.contains("exercise") || title.contains("practice") || title.contains("drill")) {
            return LessonType.PRACTICE;
        }

        // Default to THEORY
        return LessonType.THEORY;
    }

    /**
     * Get default points based on lesson type
     */
    private int getDefaultPoints(LessonType type) {
        return type == LessonType.THEORY ? DEFAULT_THEORY_POINTS : DEFAULT_PRACTICE_POINTS;
    }

    /**
     * Calculate estimated duration based on content and questions
     */
    private int calculateDuration(GrammarLessonDTO lesson) {
        int baseDuration = lesson.getLessonType() == LessonType.THEORY
                ? BASE_THEORY_DURATION
                : BASE_PRACTICE_DURATION;

        // Add time for long content
        if (lesson.getContent() != null && lesson.getContent().length() > LONG_CONTENT_THRESHOLD) {
            baseDuration += LONG_CONTENT_BONUS;
        }

        // Add time for questions
        if (lesson.getQuestions() != null && lesson.getQuestions().size() > 5) {
            baseDuration += lesson.getQuestions().size() * DURATION_PER_QUESTION;
        }

        return baseDuration;
    }

    /**
     * Validate lesson-level metadata (audio support)
     */
    private void validateLessonMetadata(Map<String, Object> metadata, String lessonTitle) {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }

        // Validate audioUrl if exists
        if (metadata.containsKey("audioUrl")) {
            Object audioUrlObj = metadata.get("audioUrl");

            if (!(audioUrlObj instanceof String)) {
                log.warn("⚠️ Lesson '{}': audioUrl must be String", lessonTitle);
                return;
            }

            String audioUrl = (String) audioUrlObj;
            if (!audioUrl.startsWith("http://") && !audioUrl.startsWith("https://")) {
                log.warn("⚠️ Lesson '{}': Invalid audioUrl format: {}", lessonTitle, audioUrl);
            } else {
                log.debug("✅ Lesson '{}': Valid audioUrl", lessonTitle);
            }
        }

        // Validate audioDuration if exists
        if (metadata.containsKey("audioDuration")) {
            Object durationObj = metadata.get("audioDuration");
            if (!(durationObj instanceof Number)) {
                log.warn("⚠️ Lesson '{}': audioDuration must be Number", lessonTitle);
            }
        }
    }

    /**
     * Process all questions in a lesson
     */
    private void processQuestions(GrammarLessonDTO lesson) {
        int orderIndex = 1;

        for (QuestionResponseDTO question : lesson.getQuestions()) {
            // Auto-assign orderIndex if missing
            if (question.getOrderIndex() == null || question.getOrderIndex() == 0) {
                question.setOrderIndex(orderIndex++);
            }

            // Normalize question text
            if (question.getQuestionText() != null) {
                question.setQuestionText(normalizeQuestionText(question.getQuestionText()));
            }

            // Validate metadata structure
            validateQuestionMetadata(question, lesson.getTitle());
        }
    }

    /**
     * Validate question metadata using centralized validator
     */
    private void validateQuestionMetadata(QuestionResponseDTO question, String lessonTitle) {
        if (question.getMetadata() == null) {
            log.warn("⚠️ Lesson '{}': Question '{}' has null metadata",
                    lessonTitle, truncate(question.getQuestionText(), 50));
            return;
        }

        try {
            metadataValidator.validate(question.getQuestionType(), question.getMetadata());
            log.debug("✅ Validated metadata for question: {}", question.getQuestionType());
        } catch (Exception e) {
            log.error("❌ Lesson '{}': Invalid metadata for question '{}': {}",
                    lessonTitle, truncate(question.getQuestionText(), 50), e.getMessage());
        }
    }

    /**
     * Normalize question text: clean up blanks and special characters
     */
    private String normalizeQuestionText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text
                // Normalize multiple dots to triple underscore
                .replaceAll("\\.{4,}", "___")
                // Normalize multiple underscores
                .replaceAll("_{2,}", "___")
                // Remove superscript numbers
                .replaceAll("[¹²³⁴⁵⁶⁷⁸⁹⁰]", "")
                // Normalize whitespace
                .replaceAll("\\s+", " ")
                // Fix spacing around blanks
                .replaceAll("\\s+___", " ___")
                .replaceAll("___\\s+", "___ ")
                .trim();
    }

    /**
     * Clean HTML content: remove ads, normalize whitespace
     */
    private String cleanHtmlContent(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        return html
                // Normalize whitespace
                .replaceAll("\\s+", " ")
                // Remove empty paragraphs
                .replaceAll("<p>\\s*</p>", "")
                // Remove double line breaks
                .replaceAll("<br>\\s*<br>", "<br>")
                // Ensure tables have proper class
                .replaceAll("<table[^>]*>", "<table class=\"tiptap-table\">")
                // Remove advertisement content
                .replaceAll("(Hotline:|Website:|Fanpage:)[^<]*", "")
                .replaceAll("Trung tâm[^<]*", "")
                .trim();
    }

    /**
     * Adjust orderIndex based on existing lessons in topic
     */
    private void adjustOrderIndexForTopic(ParseResult result, Long topicId) {
        if (result.lessons == null || result.lessons.isEmpty()) {
            log.warn("⚠️ No lessons to adjust orderIndex");
            return;
        }

        // Get max orderIndex from database
        Integer maxOrder = lessonRepository.findMaxOrderIndexByTopicId(topicId);
        int baseOrder = (maxOrder != null ? maxOrder : 0);

        log.info("📊 Topic {}: Current max orderIndex = {}, starting from {}",
                topicId, maxOrder, baseOrder + 1);

        Set<Integer> usedIndexes = new HashSet<>();

        for (GrammarLessonDTO lesson : result.lessons) {
            lesson.setTopicId(topicId);

            // Calculate new orderIndex
            Integer newIndex = lesson.getOrderIndex();
            if (newIndex == null || newIndex == 0) {
                newIndex = ++baseOrder;
            } else {
                newIndex = baseOrder + newIndex;
            }

            // Ensure uniqueness
            while (usedIndexes.contains(newIndex)) {
                newIndex++;
            }

            usedIndexes.add(newIndex);
            lesson.setOrderIndex(newIndex);

            log.debug("  → Lesson '{}': orderIndex = {}",
                    truncate(lesson.getTitle(), 40), newIndex);
        }

        int minIndex = result.lessons.get(0).getOrderIndex();
        int maxIndex = result.lessons.get(result.lessons.size() - 1).getOrderIndex();

        log.info("✅ Adjusted orderIndex range: {} - {}", minIndex, maxIndex);
    }

    /**
     * Truncate text for logging
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}