package com.thanhnb.englishlearning.service.ai.grammar;

import com.thanhnb.englishlearning.config.AIConfig;
import com.thanhnb.englishlearning.dto.ParseResult;
import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.dto.question.QuestionDTO;
import com.thanhnb.englishlearning.dto.question.QuestionOptionDTO;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.service.ai.base.AIParsingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI Parsing Service for Grammar module (NEW ARCHITECTURE)
 * Replaces old GeminiPDFService with clean OOP design
 */
@Service
@Slf4j
public class GrammarAIParsingService extends AIParsingService<ParseResult> {

    private final GrammarLessonRepository grammarLessonRepository;

    public GrammarAIParsingService(AIConfig aiConfig, GrammarLessonRepository grammarLessonRepository) {
        super(aiConfig);
        this.grammarLessonRepository = grammarLessonRepository;
    }

    @Override
    protected String getModuleName() {
        return "GRAMMAR";
    }

    @Override
    protected double getTemperature() {
        return 0.5;
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
                📐 HOW TO SPLIT LESSONS
                ═══════════════════════════════════════════════════════════════
                
                **Principles:**
                1. Each GRAMMAR TOPIC = 1 THEORY + 1 PRACTICE (if exercises exist)
                2. If theory only → Create 1 THEORY lesson
                3. If multiple exercise types → Split into separate PRACTICE lessons
                4. Keep related content together
                
                ═══════════════════════════════════════════════════════════════
                📝 CONTENT FORMAT (THEORY ONLY)
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
                
                **1. FILL_BLANK:**
                {
                  "questionText": "She (be) ___ young.",
                  "questionType": "FILL_BLANK",
                  "correctAnswer": "was|is",
                  "explanation": "Simple past or present",
                  "points": 5,
                  "orderIndex": 1
                }
                
                **2. MULTIPLE_CHOICE:**
                {
                  "questionText": "She ___ to school every day.",
                  "questionType": "MULTIPLE_CHOICE",
                  "correctAnswer": null,
                  "explanation": "Present simple with 3rd person",
                  "points": 5,
                  "orderIndex": 1,
                  "options": [
                    {"optionText": "go", "isCorrect": false, "orderIndex": 1},
                    {"optionText": "goes", "isCorrect": true, "orderIndex": 2},
                    {"optionText": "going", "isCorrect": false, "orderIndex": 3},
                    {"optionText": "went", "isCorrect": false, "orderIndex": 4}
                  ]
                }
                
                **3. TRANSLATE:**
                {
                  "questionText": "Translate: Tôi học tiếng Anh mỗi ngày.",
                  "questionType": "TRANSLATE",
                  "correctAnswer": "I study English every day|I learn English every day",
                  "explanation": "Simple present for habit",
                  "points": 10,
                  "orderIndex": 1
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
                
                1. Return ONLY valid JSON, NO extra text
                2. NO ```json``` wrapper
                3. orderIndex starts from 1 (relative, will be adjusted later)
                4. HTML must be valid
                5. MULTIPLE_CHOICE must have exactly 1 correct answer
                6. explanation max 150 chars
                7. pointsReward: THEORY = 10, PRACTICE = 15
                8. estimatedDuration: THEORY = 180-300s, PRACTICE = 300-600s
                9. lessonType must be "THEORY" or "PRACTICE" (case-sensitive)
                
                ═══════════════════════════════════════════════════════════════
                """;
    }

    @Override
    protected ParseResult parseResponse(String jsonResponse) throws Exception {
        try {
            ParseResult result = gson.fromJson(jsonResponse, ParseResult.class);

            if (result == null) {
                throw new Exception("Cannot parse JSON to ParseResult");
            }

            if (result.getError() != null) {
                throw new Exception("AI returned error: " + result.getError());
            }

            if (result.lessons == null || result.lessons.isEmpty()) {
                throw new Exception("No lessons found in parsed result");
            }

            log.info("Parsed {} grammar lessons from AI", result.lessons.size());
            return result;

        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            throw new Exception("Invalid JSON from Gemini: " + e.getMessage(), e);
        }
    }

    @Override
    protected ParseResult postProcess(ParseResult result) {
        if (result.lessons == null || result.lessons.isEmpty()) {
            return result;
        }

        int theoryCount = 0;
        int practiceCount = 0;
        int totalQuestions = 0;

        for (GrammarLessonDTO lesson : result.lessons) {
            // Ensure lesson type
            if (lesson.getLessonType() == null) {
                lesson.setLessonType(
                    (lesson.getContent() != null && !lesson.getContent().isEmpty()) 
                        ? LessonType.THEORY 
                        : LessonType.PRACTICE
                );
            }

            // Count
            if (lesson.getLessonType() == LessonType.THEORY) {
                theoryCount++;
            } else {
                practiceCount++;
            }

            // Ensure defaults
            if (lesson.getPointsReward() == null || lesson.getPointsReward() == 0) {
                lesson.setPointsReward(
                    lesson.getLessonType() == LessonType.THEORY ? 10 : 15
                );
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

            // Clean HTML content
            if (lesson.getLessonType() == LessonType.THEORY && lesson.getContent() != null) {
                lesson.setContent(cleanHtmlContent(lesson.getContent()));
            }

            // Process questions
            if (lesson.getQuestions() != null && !lesson.getQuestions().isEmpty()) {
                totalQuestions += lesson.getQuestions().size();
                processQuestions(lesson);
            }
        }

        // Add metadata
        ParseResult.ParseMetadata metadata = new ParseResult.ParseMetadata();
        metadata.setTotalTheoryLessons(theoryCount);
        metadata.setTotalPracticeLessons(practiceCount);
        metadata.setTotalQuestions(totalQuestions);
        result.setMetadata(metadata);

        log.info("Post-process complete: {} theory, {} practice, {} questions",
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

        for (ParseResult result : results) {
            if (result != null && result.lessons != null) {
                merged.lessons.addAll(result.lessons);
            }
        }

        // Sort and re-index
        merged.lessons.sort((a, b) -> {
            int orderA = a.getOrderIndex() != null ? a.getOrderIndex() : 999;
            int orderB = b.getOrderIndex() != null ? b.getOrderIndex() : 999;
            return Integer.compare(orderA, orderB);
        });

        for (int i = 0; i < merged.lessons.size(); i++) {
            merged.lessons.get(i).setOrderIndex(i + 1);
        }

        log.info("Merged {} batches into {} lessons", results.size(), merged.lessons.size());
        return merged;
    }

    /**
     * Parse file WITH topicId để adjust orderIndex ngay
     * Override method từ base class để add topicId parameter
     */
    public ParseResult parseFileWithTopicId(org.springframework.web.multipart.MultipartFile file, 
                                            Long topicId, 
                                            List<Integer> pages) throws Exception {
        // Call base class method
        ParseResult result = parseFile(file, pages);
        
        // Adjust orderIndex based on DB
        if (topicId != null) {
            adjustOrderIndexForTopic(result, topicId);
        }
        
        return result;
    }

    /**
     * Adjust orderIndex dựa trên max orderIndex trong DB
     * Similar to GeminiPDFService.postProcessResult()
     */
    private void adjustOrderIndexForTopic(ParseResult result, Long topicId) {
        if (result.lessons == null || result.lessons.isEmpty()) {
            return;
        }

        Integer maxOrder = grammarLessonRepository.findMaxOrderIndexByTopicId(topicId);
        int baseOrder = (maxOrder != null ? maxOrder : 0);

        log.info("Adjusting orderIndex for topic {}, baseOrder={}", topicId, baseOrder);

        Set<Integer> seen = new HashSet<>();

        for (GrammarLessonDTO lesson : result.lessons) {
            lesson.setTopicId(topicId);

            Integer idx = lesson.getOrderIndex();
            if (idx == null || idx == 0) {
                idx = ++baseOrder;
            } else {
                idx = baseOrder + idx;
            }

            while (seen.contains(idx)) {
                idx++;
            }

            seen.add(idx);
            lesson.setOrderIndex(idx);
        }

        log.info("OrderIndex adjusted: range {}-{}", 
                result.lessons.get(0).getOrderIndex(), 
                result.lessons.get(result.lessons.size() - 1).getOrderIndex());
    }

    // ===== HELPER METHODS =====

    private void processQuestions(GrammarLessonDTO lesson) {
        int orderIndex = 1;
        
        for (QuestionDTO question : lesson.getQuestions()) {
            if (question.getOrderIndex() == null || question.getOrderIndex() == 0) {
                question.setOrderIndex(orderIndex++);
            }

            if (question.getPoints() == null || question.getPoints() == 0) {
                question.setPoints(question.getQuestionType() == QuestionType.TRANSLATE ? 10 : 5);
            }

            if (question.getQuestionText() != null) {
                question.setQuestionText(normalizeQuestionText(question.getQuestionText()));
            }

            if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                processMultipleChoiceQuestion(question);
            }

            if (question.getQuestionType() == QuestionType.FILL_BLANK) {
                validateFillBlankQuestion(question, lesson.getTitle());
            }
        }
    }

    private void processMultipleChoiceQuestion(QuestionDTO question) {
        if (question.getOptions() != null) {
            int optionOrder = 1;
            boolean hasCorrectAnswer = false;

            for (QuestionOptionDTO option : question.getOptions()) {
                if (option.getOrderIndex() == null || option.getOrderIndex() == 0) {
                    option.setOrderIndex(optionOrder++);
                }
                if (Boolean.TRUE.equals(option.getIsCorrect())) {
                    hasCorrectAnswer = true;
                }
            }

            if (!hasCorrectAnswer && !question.getOptions().isEmpty()) {
                log.warn("Question '{}' has no correct answer, setting first as correct",
                        question.getQuestionText());
                question.getOptions().get(0).setIsCorrect(true);
            }
        }
    }

    private void validateFillBlankQuestion(QuestionDTO question, String lessonTitle) {
        String text = question.getQuestionText();

        if (!text.contains("___") && !text.contains("(") && !text.contains("[blank]")) {
            log.warn("FILL_BLANK question missing blank marker in lesson '{}': {}",
                    lessonTitle, text);
        }

        if (question.getCorrectAnswer() == null || question.getCorrectAnswer().trim().isEmpty()) {
            log.warn("FILL_BLANK question missing correct answer in lesson '{}': {}",
                    lessonTitle, text);
        }
    }

    private String normalizeQuestionText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        text = text.replaceAll("\\.{4,}", "___");
        text = text.replaceAll("_{2,}", "___");
        text = text.replaceAll("[¹²³⁴⁵⁶⁷⁸⁹⁰]", "");
        text = text.replaceAll("\\s+", " ");
        text = text.replaceAll("\\s+___", " ___");
        text = text.replaceAll("___\\s+", "___ ");

        return text.trim();
    }

    private String cleanHtmlContent(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        html = html.replaceAll("\\s+", " ");
        html = html.replaceAll("<p>\\s*</p>", "");
        html = html.replaceAll("<br>\\s*<br>", "<br>");
        html = html.replaceAll("<table[^>]*>", "<table class=\"tiptap-table\">");
        html = html.replaceAll("(Hotline:|Website:|Fanpage:)[^<]*", "");
        html = html.replaceAll("Trung tâm[^<]*", "");

        return html.trim();
    }
}