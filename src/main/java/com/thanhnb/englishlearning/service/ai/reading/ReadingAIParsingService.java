package com.thanhnb.englishlearning.service.ai.reading;

import com.thanhnb.englishlearning.config.AIConfig;
import com.thanhnb.englishlearning.dto.reading.ReadingLessonDTO;
import com.thanhnb.englishlearning.dto.question.QuestionDTO;
import com.thanhnb.englishlearning.dto.question.QuestionOptionDTO;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.service.ai.base.AIParsingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Parsing Service specifically for Reading Comprehension module
 * Handles PDF, DOCX, and Image files
 * Automatically generates English content + Vietnamese translation + Questions
 */
@Service
@Slf4j
public class ReadingAIParsingService extends AIParsingService<ReadingLessonDTO> {

    private final ReadingLessonRepository readingLessonRepository;

    /**
     * Constructor - inject AIConfig and Repository
     */
    public ReadingAIParsingService(AIConfig aiConfig, ReadingLessonRepository readingLessonRepository) {
        super(aiConfig);
        this.readingLessonRepository = readingLessonRepository;
    }

    @Override
    protected String getModuleName() {
        return "READING";
    }

    @Override
    protected double getTemperature() {
        return 0.4;
    }

    /**
     * Build optimized English prompt for Reading Comprehension
     * Includes both English content AND Vietnamese translation
     */
    @Override
    protected String buildPrompt() {
        return """
                You are an AI assistant specializing in English reading comprehension materials.

                TASK: Analyze the content and create a JSON output with the following structure.

                ═══════════════════════════════════════════════════════════════
                📖 ANALYSIS REQUIREMENTS
                ═══════════════════════════════════════════════════════════════

                1. **Extract Title:**
                    - Create a clear, concise title based on the main topic
                    - Max 100 characters
                    - Examples: "The Digital Paradox", "Climate Change Effects"

                2. **Extract Content (English):**
                    - Extract the English text and format as valid HTML
                    - Use <p> tags for paragraphs
                    - Use <h2>, <h3> for headings if any
                    - Use <ul>, <ol>, <li> for lists
                    - Use <strong> for bold text
                    - Remove watermarks, logos, page numbers
                    - Example: "<h2>Introduction</h2><p>This is the first paragraph.</p><p>This is the second.</p>"

                3. **Translate to Vietnamese:**
                    - Translate the ENTIRE English content to Vietnamese
                    - Format as HTML, matching the English structure (<p>, <h2>, etc.)
                    - Natural, fluent Vietnamese
                    - For technical terms: keep English in parentheses
                        Example: "<p>...điện thoại thông minh (smartphone)...</p>"
                    - Academic tone suitable for learners

                4. **Generate Questions:**

                    **A. MULTIPLE_CHOICE:**
                    - Main idea (what is the passage about?)
                    - Specific details (according to paragraph X...)
                    - Inference (what can be inferred...)
                    - Vocabulary in context (the word X means...)
                    - Author's purpose/tone

                    Format:
                    {
                        "questionText": "What is the main idea of the passage?",
                        "questionType": "MULTIPLE_CHOICE",
                        "correctAnswer": null,
                        "explanation": "The passage mainly discusses how increased connectivity leads to isolation.",
                        "points": 5,
                        "orderIndex": 1,
                        "options": [
                        {"optionText": "Technology connects people globally", "isCorrect": false, "orderIndex": 1},
                        {"optionText": "Digital life creates a paradox", "isCorrect": true, "orderIndex": 2},
                        {"optionText": "Smartphones harm mental health", "isCorrect": false, "orderIndex": 3},
                        {"optionText": "Social media is very popular", "isCorrect": false, "orderIndex": 4}
                        ]
                    }

                    **B. TRUE/FALSE:**
                    - Statement verification based on passage content
                    - Must be clearly true or false based on the text

                    Format:
                    {
                        "questionText": "The passage suggests that technology makes people more isolated. (True/False)",
                        "questionType": "TRUE_FALSE",
                        "correctAnswer": null,
                        "explanation": "True. Paragraph 2 states 'the more connected we are digitally, the more isolated we feel'.",
                        "points": 5,
                        "orderIndex": 8,
                        "options": [
                        {"optionText": "True", "isCorrect": true, "orderIndex": 1},
                        {"optionText": "False", "isCorrect": false, "orderIndex": 2}
                        ]
                    }

                    **C. FILL_BLANK:**
                    - Fill in the blank with the correct word
                    - Extract a sentence from the passage and blank out a key word
                    - Example: "The gym offers a full-service fitness _____."

                    Format:
                    {
                    "questionText": "The gym offers a full-service fitness _____.",
                    "questionType": "FILL_BLANK",
                    "correctAnswer": "membership|gym membership",
                    "explanation": "The sentence discusses gym services, so 'membership' is the correct answer.",
                    "points": 5,
                    "orderIndex": 11
                    }

                    **D. SHORT_ANSWER:**
                    - Require a complete sentence answer (not just a word)
                    - Vocabulary definition, explanation, or interpretation questions
                    - Answer should be 5-20 words

                    Format:
                    {
                    "questionText": "What does the word 'ubiquitous' in paragraph 1 mean?",
                    "questionType": "SHORT_ANSWER",
                    "correctAnswer": "present everywhere|existing everywhere|found everywhere",
                    "explanation": "Ubiquitous means present, appearing, or found everywhere.",
                    "points": 10,
                    "orderIndex": 14
                    }

                    ═══════════════════════════════════════════════════════════════
                    📐 QUESTION DESIGN GUIDELINES
                    ═══════════════════════════════════════════════════════════════

                    - Questions must test COMPREHENSION, not just memory
                    - Distractors (wrong answers) should be plausible but clearly incorrect
                    - Explanations should reference specific parts of the text
                    - Difficulty distribution: 60% easy, 30% medium, 10% hard
                    - Avoid questions answerable without reading the passage
                    - For FILL_BLANK: use words that appear in the passage
                    - For SHORT_ANSWER: accept multiple correct phrasings separated by |

                    ═══════════════════════════════════════════════════════════════
                    📦 OUTPUT JSON FORMAT
                    ═══════════════════════════════════════════════════════════════

                    {
                        "title": "The Digital Paradox",
                        "content": "<h2>Title</h2><p>English content here...</p><p>New paragraph...</p>",
                        "contentTranslation": "<h2>Tiêu đề</h2><p>Nội dung tiếng Việt...</p><p>Đoạn mới...</p>",
                        "questions": [
                        // 10-15 questions as specified above
                        ]
                    }

                    ═══════════════════════════════════════════════════════════════
                    ⚠️ CRITICAL REQUIREMENTS
                    ═══════════════════════════════════════════════════════════════

                    1. Return ONLY valid JSON, NO extra text
                    2. NO ```json``` code block wrapper
                    3. Both content and contentTranslation MUST be valid HTML text
                    4. Use <p>...</p> for paragraphs. DO NOT use \\n\\n
                    5. orderIndex starts from 1 and must be continuous
                    6. MULTIPLE_CHOICE must have exactly 1 correct answer
                    7. explanation max 200 characters
                    8. For FILL_BLANK: multiple acceptable answers separated by |
                    9. For SHORT_ANSWER: answer is 5-20 words, multiple phrasings separated by |
                    10. correctAnswer for MULTIPLE_CHOICE and TRUE_FALSE must be null (answer is in options)
                    11. If content is NOT an English reading passage, return:
                        {"error": "Not a valid English reading passage"}

                    ═══════════════════════════════════════════════════════════════
                    """;
    }

    /**
     * Parse Gemini JSON response to ReadingLessonDTO
     */
    @Override
    protected ReadingLessonDTO parseResponse(String jsonResponse) throws Exception {
        try {
            ReadingLessonDTO lesson = gson.fromJson(jsonResponse, ReadingLessonDTO.class);

            if (lesson == null) {
                throw new Exception("Cannot parse JSON to ReadingLessonDTO");
            }

            // Validate required fields
            if (lesson.getTitle() == null || lesson.getTitle().isEmpty()) {
                throw new Exception("Missing title in parsed lesson");
            }

            if (lesson.getContent() == null || lesson.getContent().isEmpty()) {
                throw new Exception("Missing content in parsed lesson");
            }

            // Translation is optional - will be empty string if AI couldn't generate
            if (lesson.getContentTranslation() == null) {
                lesson.setContentTranslation("");
                log.warn("No Vietnamese translation provided by AI");
            }

            if (lesson.getQuestions() == null) {
                lesson.setQuestions(new ArrayList<>());
                log.warn("No questions generated by AI");
            }

            log.info("Parsed lesson: title='{}', content={} chars, translation={} chars, {} questions",
                    lesson.getTitle(),
                    lesson.getContent().length(),
                    lesson.getContentTranslation().length(),
                    lesson.getQuestions().size());

            return lesson;

        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            throw new Exception("Invalid JSON response from Gemini: " + e.getMessage(), e);
        }
    }

    /**
     * Post-process parsed lesson data
     */
    @Override
    protected ReadingLessonDTO postProcess(ReadingLessonDTO lesson) {
        // Clean up content
        if (lesson.getContent() != null) {
            lesson.setContent(cleanContent(lesson.getContent()));
        }

        if (lesson.getContentTranslation() != null) {
            lesson.setContentTranslation(cleanContent(lesson.getContentTranslation()));
        }

        // Process questions
        if (lesson.getQuestions() != null && !lesson.getQuestions().isEmpty()) {
            int orderIndex = 1;
            for (QuestionDTO question : lesson.getQuestions()) {
                // Ensure order index
                if (question.getOrderIndex() == null || question.getOrderIndex() == 0) {
                    question.setOrderIndex(orderIndex++);
                }

                // Ensure points
                if (question.getPoints() == null || question.getPoints() == 0) {
                    question.setPoints(question.getQuestionType() == QuestionType.FILL_BLANK ? 10 : 5);
                }

                // Process multiple choice options
                if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                    processMultipleChoiceQuestion(question);
                }

                // Validate fill blank questions
                if (question.getQuestionType() == QuestionType.FILL_BLANK) {
                    validateFillBlankQuestion(question);
                }
            }
        }

        return lesson;
    }

    /**
     * NEW: Parse file và adjust orderIndex ngay
     * Để dùng cho import vào DB
     */
    public ReadingLessonDTO parseFileForImport(MultipartFile file) throws Exception {
        log.info("[READING] Parsing file for import: {}", file.getOriginalFilename());

        // Step 1: Parse file using base class method
        ReadingLessonDTO lesson = parseFile(file, null);

        // Step 2: Get max orderIndex from DB và set
        Integer maxOrder = readingLessonRepository.findAll().stream()
                .map(l -> l.getOrderIndex())
                .max(Integer::compareTo)
                .orElse(0);

        // orderIndex sẽ là maxOrder + 1 (vì reading không có multiple lessons per file)
        lesson.setOrderIndex(maxOrder + 1);

        log.info("Assigned orderIndex: {}", lesson.getOrderIndex());

        return lesson;
    }

    /**
     * Process multiple choice question
     */
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

            // Ensure at least one correct answer
            if (!hasCorrectAnswer && !question.getOptions().isEmpty()) {
                log.warn("Question '{}' has no correct answer, setting first option as correct",
                        question.getQuestionText());
                question.getOptions().get(0).setIsCorrect(true);
            }
        }
    }

    /**
     * Validate fill blank question
     */
    private void validateFillBlankQuestion(QuestionDTO question) {
        if (question.getCorrectAnswer() == null || question.getCorrectAnswer().trim().isEmpty()) {
            log.warn("FILL_BLANK question missing correct answer: {}", question.getQuestionText());
        }
    }

    /**
     * Clean content text
     */
    private String cleanContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // Normalize line breaks
        content = content.replaceAll("\\r\\n", "\n");
        content = content.replaceAll("\\r", "\n");

        // Remove excessive whitespace
        content = content.replaceAll(" +", " ");

        // Normalize paragraph breaks
        content = content.replaceAll("\n\n\n+", "\n\n");

        // Remove leading/trailing whitespace
        content = content.trim();

        return content;
    }

    /**
     * Merge multiple reading lessons (if processing multiple files)
     */
    @Override
    protected ReadingLessonDTO mergeResults(List<ReadingLessonDTO> results) {
        if (results.size() == 1) {
            return results.get(0);
        }

        // For reading, we typically don't merge - each file = 1 lesson
        // But if needed, we can concatenate content
        ReadingLessonDTO merged = new ReadingLessonDTO();

        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder translationBuilder = new StringBuilder();
        List<QuestionDTO> allQuestions = new ArrayList<>();

        int questionOrder = 1;

        for (int i = 0; i < results.size(); i++) {
            ReadingLessonDTO lesson = results.get(i);

            // First lesson's title becomes the merged title
            if (i == 0) {
                merged.setTitle(lesson.getTitle());
            }

            // Concatenate content
            if (lesson.getContent() != null) {
                if (contentBuilder.length() > 0) {
                    contentBuilder.append("\n\n");
                }
                contentBuilder.append(lesson.getContent());
            }

            // Concatenate translation
            if (lesson.getContentTranslation() != null) {
                if (translationBuilder.length() > 0) {
                    translationBuilder.append("\n\n");
                }
                translationBuilder.append(lesson.getContentTranslation());
            }

            // Merge questions
            if (lesson.getQuestions() != null) {
                for (QuestionDTO question : lesson.getQuestions()) {
                    question.setOrderIndex(questionOrder++);
                    allQuestions.add(question);
                }
            }
        }

        merged.setContent(contentBuilder.toString());
        merged.setContentTranslation(translationBuilder.toString());
        merged.setQuestions(allQuestions);

        log.info("Merged {} lessons: total content {} chars, {} questions",
                results.size(), merged.getContent().length(), allQuestions.size());

        return merged;
    }
}