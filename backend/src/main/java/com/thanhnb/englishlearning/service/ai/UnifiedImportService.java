package com.thanhnb.englishlearning.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.dto.listening.ListeningLessonDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.reading.ReadingLessonDTO;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.service.ai.base.AIParsingService;
import com.thanhnb.englishlearning.service.ai.provider.AIServiceRouter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ✅ IMPROVED: Unified Import Service với prompts chi tiết hơn
 */
@Service
@Slf4j
public class UnifiedImportService<T> extends AIParsingService<T> {

    private String userInstruction;
    private Class<T> targetType;
    private ModuleType currentModule;

    public UnifiedImportService(AIServiceRouter aiServiceRouter, ObjectMapper objectMapper) {
        super(aiServiceRouter, objectMapper);
    }

    /**
     * Parse lesson từ file + instruction
     */
    public T parseLessonFile(MultipartFile file, String instruction, ModuleType moduleType, Class<T> targetClass)
            throws Exception {
        this.userInstruction = instruction;
        this.currentModule = moduleType;
        this.targetType = targetClass;

        log.info("📄 Parsing file: {} for module: {} with instruction: {}",
                file.getOriginalFilename(), moduleType, instruction);

        return parseFile(file, null);
    }

    /**
     * ✅ IMPROVED: Generate lesson từ instruction với prompt rõ ràng hơn
     */
    public T generateFromInstruction(String instruction, ModuleType moduleType, Class<T> targetClass)
            throws Exception {
        this.userInstruction = instruction;
        this.currentModule = moduleType;
        this.targetType = targetClass;

        log.info("🤖 Generating lesson for module: {} with instruction: {}", moduleType, instruction);

        String prompt = buildGenerationPrompt();
        log.info("📝 Generated prompt (first 800 chars):\n{}", 
            prompt.substring(0, Math.min(800, prompt.length())));
        
        log.info("🔄 Calling AI router for analysis...");
        String aiJson = aiServiceRouter.generateForAnalysis(prompt);
        
        log.info("✅ Received AI response length: {} chars", aiJson.length());
        log.info("📄 Response preview (first 1000 chars):\n{}", 
            aiJson.substring(0, Math.min(1000, aiJson.length())));

        T result = parseResponse(aiJson);
        log.info("✅ Successfully parsed to target type: {}", targetClass.getSimpleName());
        
        return result;
    }

    @Override
    protected String getModuleName() {
        return "UNIFIED_IMPORT_" + (currentModule != null ? currentModule.name() : "GENERAL");
    }

    @Override
    protected double getTemperature() {
        return 0.2;
    }

    @Override
    protected long getMaxFileSize() {
        return 10 * 1024 * 1024;
    }

    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 3000), recover = "recoverFromAIError")
    protected String buildPrompt() {
        String moduleContext = switch (currentModule) {
            case GRAMMAR -> "grammar theory and exercises";
            case READING -> "reading comprehension passage";
            case LISTENING -> "listening transcript/dialogue";
            default -> "educational content";
        };

        return String.format("""
                You are an expert EdTech content parser for English learning.

                TASK: Parse document and convert to structured JSON

                CONTEXT:
                - Module: %s (%s)
                - Instruction: "%s"
                - Target structure: %s

                OUTPUT STRUCTURE (STRICT):
                {
                  "name": "Lesson title",
                  "content": "HTML-formatted content",
                  "taskGroups": [
                    {
                      "taskName": "Task 1: Multiple Choice",
                      "instruction": "Choose the correct answer",
                      "orderIndex": 1,
                      "questions": [...]
                    }
                  ],
                  "standaloneQuestions": []
                }

                QUESTION FORMAT EXAMPLES:
                %s

                CRITICAL RULES:
                1. Return ONLY valid JSON (no markdown, no explanation)
                2. Every question MUST have ALL required fields
                3. Correct answers MUST be clearly marked
                4. Content must be clean HTML
                5. Group questions logically by type

                Parse this document now:
                """,
                currentModule,
                moduleContext,
                userInstruction != null ? userInstruction : "Extract all content",
                targetType.getSimpleName(),
                buildDetailedQuestionExamples());
    }

    /**
     * ✅ IMPROVED: Prompt chi tiết cho generation theo từng module
     */
    private String buildGenerationPrompt() {
        return switch (currentModule) {
            case GRAMMAR -> buildGrammarPrompt();
            case READING -> buildReadingPrompt();
            case LISTENING -> buildListeningPrompt();
            default -> buildGenericPrompt();
        };
    }

    /**
     * ✅ Grammar Module Prompt
     */
    private String buildGrammarPrompt() {
        return String.format("""
                You are an expert English grammar teacher creating educational content.

                ===== IMPORTANT: READ USER INSTRUCTION CAREFULLY =====
                USER INSTRUCTION: "%s"

                TASK: Create a GRAMMAR lesson BASED ON THE USER'S INSTRUCTION
                - If user provides grammar content → USE THAT EXACT CONTENT
                - If user asks to create theory → Generate clear grammar explanation with examples
                - If user specifies number of questions → CREATE EXACTLY that many questions
                - Questions MUST test the grammar point in the content
                
                MODULE TYPE: GRAMMAR (Grammar rules, theory, and practice exercises)

                ===== GRAMMAR LESSON STRUCTURE =====
                {
                  "name": "Lesson title (e.g., 'Present Perfect Tense')",
                  "content": "<h2>Grammar Rule</h2><p>Explanation...</p><h3>Examples:</h3><ul><li>Example 1</li></ul>",
                  "taskGroups": [
                    {
                      "taskName": "Task 1: Multiple Choice",
                      "instruction": "Choose the correct answer (A, B, C or D)",
                      "orderIndex": 1,
                      "questions": [...5-7 questions...]
                    },
                    {
                      "taskName": "Task 2: Fill in the Blanks",
                      "instruction": "Complete the sentences with the correct form",
                      "orderIndex": 2,
                      "questions": [...5-7 questions...]
                    }
                  ],
                  "standaloneQuestions": []
                }

                ===== GRAMMAR CONTENT REQUIREMENTS =====
                The "content" field must include:
                1. <h2>Grammar Rule Title</h2>
                2. <p>Clear explanation of the grammar point</p>
                3. <h3>Form/Structure:</h3>
                   <ul>
                     <li>Positive: Subject + verb...</li>
                     <li>Negative: Subject + don't/doesn't...</li>
                     <li>Question: Do/Does + subject...?</li>
                   </ul>
                4. <h3>Usage:</h3>
                   <p>When to use this grammar...</p>
                5. <h3>Examples:</h3>
                   <ul>
                     <li>I have lived here for 5 years.</li>
                     <li>She hasn't finished her homework yet.</li>
                   </ul>
                6. <h3>Common Mistakes:</h3>
                   <ul>
                     <li>❌ Incorrect: I live here since 2020</li>
                     <li>✅ Correct: I have lived here since 2020</li>
                   </ul>

                ===== GRAMMAR QUESTION TYPES (Mix these) =====
                1. MULTIPLE_CHOICE - Choose correct grammar form
                2. FILL_BLANK - Complete with correct form (with/without word bank)
                3. ERROR_CORRECTION - Find and correct grammar mistakes
                4. SENTENCE_TRANSFORMATION - Rewrite sentences using given structure
                5. SENTENCE_BUILDING - Arrange words to form correct sentences

                %s

                ===== GRAMMAR-SPECIFIC VALIDATION =====
                - Content must explain grammar rules clearly
                - Questions must test understanding of the grammar point
                - Include variety of question types
                - Mix difficulty levels (easy → medium → hard)
                - All questions answerable from the content

                NOW CREATE THE GRAMMAR LESSON:
                """,
                userInstruction != null ? userInstruction : "Create a complete grammar lesson",
                buildDetailedQuestionExamples());
    }

    /**
     * ✅ Reading Module Prompt
     */
    private String buildReadingPrompt() {
        return String.format("""
                You are an expert English reading comprehension teacher creating educational content.

                ===== IMPORTANT: READ USER INSTRUCTION CAREFULLY =====
                USER INSTRUCTION: "%s"

                TASK: Create a READING lesson BASED ON THE USER'S INSTRUCTION
                - If user provides reading passage → USE THAT EXACT CONTENT
                - If user asks to create passage → Generate interesting, age-appropriate text
                - If user specifies number of questions → CREATE EXACTLY that many questions
                - ALL questions MUST be answerable from the reading passage
                
                MODULE TYPE: READING (Reading comprehension with passage and questions)

                ===== READING LESSON STRUCTURE =====
                {
                  "name": "Lesson title based on passage topic (e.g., 'Exam Instructions', 'Climate Change')",
                  "content": "<h2>Title</h2><p>Paragraph 1...</p><p>Paragraph 2...</p>",
                  "taskGroups": [
                    {
                      "taskName": "Task 1: True or False",
                      "instruction": "Read the passage and decide if the statements are True or False",
                      "orderIndex": 1,
                      "questions": [...5-7 questions...]
                    },
                    {
                      "taskName": "Task 2: Multiple Choice Questions",
                      "instruction": "Choose the best answer (A, B, C or D) based on the passage",
                      "orderIndex": 2,
                      "questions": [...5-7 questions...]
                    }
                  ],
                  "standaloneQuestions": []
                }

                ===== READING CONTENT REQUIREMENTS =====
                The "content" field (the reading passage) must:
                1. Be 200-500 words (appropriate length for comprehension)
                2. Have clear paragraphs with <p> tags
                3. Include a title with <h2> tag
                4. Be well-structured and coherent
                5. Contain information that questions can test
                6. Use appropriate vocabulary level
                7. Be formatted as clean HTML

                Example structure:
                ```html
                <h2>The Benefits of Reading</h2>
                <p>Reading is one of the most important skills...</p>
                <p>Studies have shown that regular reading...</p>
                <p>In conclusion, reading regularly can...</p>
                ```

                ===== READING QUESTION TYPES (Mix these) =====
                1. TRUE_FALSE (use MULTIPLE_CHOICE with 2 options)
                   - Test factual understanding
                   - Based on explicit information in text
                
                2. MULTIPLE_CHOICE (4 options)
                   - Main idea questions
                   - Detail questions
                   - Inference questions
                   - Vocabulary in context
                
                3. FILL_BLANK
                   - Complete summary with words from passage
                   - Word bank optional
                
                4. MATCHING
                   - Match headings to paragraphs
                   - Match information to sections

                %s

                ===== READING-SPECIFIC VALIDATION =====
                - Passage must be complete and make sense
                - Every question MUST be answerable from the passage
                - Don't ask questions requiring outside knowledge
                - Mix literal and inferential questions
                - Include questions about main idea and details
                - Questions should test comprehension, not memory

                ===== QUESTION DISTRIBUTION EXAMPLE =====
                For 14 questions in 2 tasks:
                - Task 1 (True/False): 6-7 questions testing facts
                - Task 2 (Multiple Choice): 7-8 questions testing understanding

                NOW CREATE THE READING LESSON:
                """,
                userInstruction != null ? userInstruction : "Create a complete reading lesson with passage and questions",
                buildDetailedQuestionExamples());
    }

    /**
     * ✅ Listening Module Prompt
     */
    private String buildListeningPrompt() {
        return String.format("""
                You are an expert English listening teacher creating educational content.

                ===== IMPORTANT: READ USER INSTRUCTION CAREFULLY =====
                USER INSTRUCTION: "%s"

                TASK: Create a LISTENING lesson BASED ON THE USER'S INSTRUCTION
                - If user provides transcript/dialogue → USE THAT EXACT CONTENT
                - If user asks to create conversation → Generate realistic dialogue
                - If user specifies number of questions → CREATE EXACTLY that many questions
                - ALL questions MUST be answerable from the transcript
                
                MODULE TYPE: LISTENING (Dialogue/monologue with transcript and questions)

                ===== LISTENING LESSON STRUCTURE =====
                {
                  "name": "Lesson title based on topic (e.g., 'At the Restaurant', 'Weather Forecast')",
                  "content": "<h2>Transcript</h2><div class='speaker'><strong>Speaker A:</strong> Hello, how are you?</div><div class='speaker'><strong>Speaker B:</strong> I'm fine, thanks.</div>",
                  "taskGroups": [
                    {
                      "taskName": "Task 1: Listen and Choose",
                      "instruction": "Listen to the conversation and choose the correct answer",
                      "orderIndex": 1,
                      "questions": [...5-7 questions...]
                    },
                    {
                      "taskName": "Task 2: Fill in the Missing Information",
                      "instruction": "Complete the sentences based on what you hear",
                      "orderIndex": 2,
                      "questions": [...5-7 questions...]
                    }
                  ],
                  "standaloneQuestions": []
                }

                ===== LISTENING CONTENT REQUIREMENTS =====
                The "content" field (transcript) must:
                1. Be realistic, natural conversation or monologue
                2. Use proper dialogue format with speaker labels
                3. Include 150-400 words (appropriate for listening exercise)
                4. Have clear context (where, who, why)
                5. Contain specific information for questions (times, places, names, numbers)
                6. Use appropriate language level
                7. Be formatted as clean HTML with speaker identification

                Example dialogue format:
                ```html
                <h2>Conversation at a Cafe</h2>
                <div class='dialogue'>
                  <p><strong>Waiter:</strong> Good morning! What can I get for you today?</p>
                  <p><strong>Customer:</strong> I'd like a cappuccino and a croissant, please.</p>
                  <p><strong>Waiter:</strong> Sure! That'll be ready in 5 minutes.</p>
                </div>
                ```

                Example monologue format:
                ```html
                <h2>Weather Forecast</h2>
                <div class='monologue'>
                  <p>Good evening. Here's the weather forecast for tomorrow. It will be sunny in the morning with temperatures around 25 degrees Celsius. In the afternoon, expect some clouds, but it will remain dry. The temperature will rise to 28 degrees...</p>
                </div>
                ```

                ===== LISTENING QUESTION TYPES (Mix these) =====
                1. MULTIPLE_CHOICE (4 options)
                   - Who said what
                   - What happened
                   - Where/when events occur
                   - Main topic/purpose
                
                2. FILL_BLANK
                   - Complete transcript gaps
                   - Fill in specific information (numbers, names, times)
                   - With or without word bank
                
                3. TRUE_FALSE (use MULTIPLE_CHOICE with 2 options)
                   - Test factual details from dialogue
                
                4. MATCHING
                   - Match speakers to statements
                   - Match times to events

                %s

                ===== LISTENING-SPECIFIC VALIDATION =====
                - Transcript must be complete conversation/monologue
                - Include specific details (times, prices, names, places)
                - Questions test listening comprehension skills:
                  * Listening for gist (main idea)
                  * Listening for specific information
                  * Listening for detail
                  * Understanding speaker attitude/opinion
                - Mix question types that test different listening skills
                - All answers must be clearly stated in transcript

                ===== QUESTION DISTRIBUTION EXAMPLE =====
                For 12 questions in 2 tasks:
                - Task 1 (Multiple Choice): 6 questions about general understanding
                - Task 2 (Fill Blank): 6 questions about specific details

                NOTE: In actual implementation, audio file will be uploaded separately.
                The transcript here is what students will hear (and can read for support).

                NOW CREATE THE LISTENING LESSON:
                """,
                userInstruction != null ? userInstruction : "Create a complete listening lesson with transcript and questions",
                buildDetailedQuestionExamples());
    }

    /**
     * ✅ Generic Prompt (fallback)
     */
    private String buildGenericPrompt() {
        return String.format("""
                You are an expert English teacher creating educational content.

                ===== IMPORTANT: READ USER INSTRUCTION CAREFULLY =====
                USER INSTRUCTION: "%s"

                TASK: Create a complete lesson BASED ON THE USER'S INSTRUCTION ABOVE
                - If user provides content → USE THAT EXACT CONTENT, don't create new content
                - If user specifies number of questions → CREATE EXACTLY that many questions
                - If user specifies task structure → FOLLOW that structure
                - Questions MUST be based on the provided content
                
                MODULE TYPE: GENERAL (Educational content)

                ===== OUTPUT STRUCTURE (JSON ONLY, NO MARKDOWN) =====
                {
                  "name": "Appropriate lesson title based on the content",
                  "content": "The content provided by user OR well-structured HTML if user asks to generate",
                  "taskGroups": [
                    {
                      "taskName": "Task name as user specified or 'Task 1', 'Task 2'...",
                      "instruction": "Clear instruction for this task group",
                      "orderIndex": 1,
                      "questions": [
                        {
                          "questionText": "Question based on the content above",
                          "questionType": "MULTIPLE_CHOICE",
                          "points": 1,
                          "orderIndex": 1,
                          "data": {
                            "options": [
                              {"text": "Option A", "isCorrect": false, "order": 1},
                              {"text": "Option B", "isCorrect": true, "order": 2},
                              {"text": "Option C", "isCorrect": false, "order": 3},
                              {"text": "Option D", "isCorrect": false, "order": 4}
                            ]
                          }
                        }
                      ]
                    }
                  ],
                  "standaloneQuestions": []
                }

                ===== QUESTION TYPE EXAMPLES (USE EXACT FORMAT) =====
                %s

                ===== CRITICAL VALIDATION RULES =====
                1. Return ONLY JSON (no ```json, no explanations, no extra text)
                2. Every question MUST have these fields:
                   - questionText (string, not empty)
                   - questionType (one of the valid types)
                   - points (integer, default 1)
                   - orderIndex (integer, starts from 1)
                   - data (object with type-specific fields)

                3. For MULTIPLE_CHOICE questions:
                   - MUST have "options" array
                   - Each option MUST have: text, isCorrect, order
                   - EXACTLY ONE option must be isCorrect: true
                   - Minimum 2 options, typically 4 options

                4. For ERROR_CORRECTION questions:
                   - MUST have "errorText" (the incorrect word/phrase)
                   - MUST have "correction" (the correct version)

                5. For FILL_BLANK questions:
                   - MUST have "blanks" array
                   - Each blank MUST have: position, correctAnswers
                   - Can optionally have "wordBank" array

                6. For MATCHING questions:
                   - MUST have "pairs" array
                   - Each pair MUST have: left, right, order

                7. For TRUE_FALSE questions:
                   - Use MULTIPLE_CHOICE type
                   - MUST have exactly 2 options: "True" and "False"

                ===== IMPORTANT REMINDERS =====
                - Read user instruction carefully at the top
                - Create questions BASED ON the content provided
                - Follow user's requested number of questions and task structure
                - All questions must be answerable from the content
                - Don't add explanations outside the JSON

                NOW CREATE THE LESSON:
                """,
                userInstruction != null ? userInstruction : "Create a complete lesson",
                buildDetailedQuestionExamples());
    }

    /**
     * ✅ IMPROVED: Chi tiết hơn về format câu hỏi
     */
    private String buildDetailedQuestionExamples() {
        return """
                EXAMPLE 1 - MULTIPLE CHOICE (4 options):
                {
                  "questionText": "According to the poster, when do the doors close?",
                  "questionType": "MULTIPLE_CHOICE",
                  "points": 1,
                  "orderIndex": 1,
                  "data": {
                    "options": [
                      {"text": "At the exam start time", "isCorrect": false, "order": 1},
                      {"text": "5 minutes before the exam", "isCorrect": true, "order": 2},
                      {"text": "10 minutes before the exam", "isCorrect": false, "order": 3},
                      {"text": "After the exam starts", "isCorrect": false, "order": 4}
                    ]
                  }
                }

                EXAMPLE 2 - TRUE/FALSE (use MULTIPLE_CHOICE with 2 options):
                {
                  "questionText": "Students are allowed to bring books into the exam room.",
                  "questionType": "MULTIPLE_CHOICE",
                  "points": 1,
                  "orderIndex": 2,
                  "data": {
                    "options": [
                      {"text": "True", "isCorrect": false, "order": 1},
                      {"text": "False", "isCorrect": true, "order": 2}
                    ]
                  }
                }

                EXAMPLE 3 - ERROR CORRECTION:
                {
                  "questionText": "Find the error: You should use a *red* pen for the exam.",
                  "questionType": "ERROR_CORRECTION",
                  "points": 1,
                  "orderIndex": 3,
                  "data": {
                    "errorText": "red",
                    "correction": "blue or black"
                  }
                }

                EXAMPLE 4 - FILL_BLANK:
                {
                  "questionText": "Students must arrive ___ minutes before the exam.",
                  "questionType": "FILL_BLANK",
                  "points": 1,
                  "orderIndex": 4,
                  "data": {
                    "blanks": [
                      {"position": 1, "correctAnswers": ["10", "ten"], "hint": "See Poster 2"}
                    ],
                    "wordBank": ["5", "10", "15", "20"]
                  }
                }

                EXAMPLE 5 - MATCHING:
                {
                  "questionText": "Match the exam rules with the correct poster:",
                  "questionType": "MATCHING",
                  "points": 2,
                  "orderIndex": 5,
                  "data": {
                    "pairs": [
                      {"left": "Show student ID", "right": "Poster 1", "order": 1},
                      {"left": "Arrive early", "right": "Poster 2", "order": 2},
                      {"left": "Use correct pen", "right": "Poster 3", "order": 3}
                    ]
                  }
                }

                EXAMPLE 6 - SENTENCE_BUILDING:
                {
                  "questionText": "Arrange the words to make a correct sentence:",
                  "questionType": "SENTENCE_BUILDING",
                  "points": 2,
                  "orderIndex": 6,
                  "data": {
                    "words": ["phone", "your", "switched", "must", "be", "off"],
                    "correctSentence": "Your phone must be switched off"
                  }
                }

                REMEMBER:
                - Each question MUST follow this exact structure
                - isCorrect field is REQUIRED and must be boolean (true/false)
                - order field is REQUIRED and must be integer (1, 2, 3...)
                - correctAnswers must be an array, even for single answer
                """;
    }

    @Override
    protected T parseResponse(String jsonResponse) throws Exception {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            throw new Exception("AI returned empty response");
        }

        String cleaned = cleanJsonResponse(jsonResponse);
        log.debug("📋 Cleaned JSON (first 1000 chars):\n{}", 
            cleaned.substring(0, Math.min(1000, cleaned.length())));

        try {
            LessonWithTaskGroups intermediate = objectMapper.readValue(
                    cleaned,
                    LessonWithTaskGroups.class);

            validateIntermediate(intermediate);
            T result = convertToTargetType(intermediate);

            log.info("✅ Parsed successfully: {} questions in {} task groups",
                    countQuestions(intermediate),
                    intermediate.getTaskGroups() != null ? intermediate.getTaskGroups().size() : 0);

            return result;

        } catch (Exception e) {
            log.error("❌ Parse error. JSON:\n{}", cleaned);
            log.error("Error details:", e);
            throw new Exception("AI response format invalid: " + e.getMessage() + 
                "\n\nPlease check the JSON structure. First 500 chars of response:\n" + 
                cleaned.substring(0, Math.min(500, cleaned.length())));
        }
    }

    private String cleanJsonResponse(String json) {
        String cleaned = json.trim();

        // Remove markdown code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        // Remove any leading/trailing text before/after JSON
        int jsonStart = cleaned.indexOf('{');
        int jsonEnd = cleaned.lastIndexOf('}');
        
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
        }

        return cleaned.trim();
    }

    private void validateIntermediate(LessonWithTaskGroups data) throws Exception {
        if (data.getName() == null || data.getName().isBlank()) {
            throw new Exception("Missing lesson name");
        }
        if (data.getContent() == null || data.getContent().isBlank()) {
            throw new Exception("Missing lesson content");
        }

        int totalQuestions = countQuestions(data);
        if (totalQuestions == 0) {
            throw new Exception("No questions found in AI response");
        }

        // Validate each question has required fields
        if (data.getTaskGroups() != null) {
            for (var tg : data.getTaskGroups()) {
                if (tg.getQuestions() != null) {
                    for (int i = 0; i < tg.getQuestions().size(); i++) {
                        var q = tg.getQuestions().get(i);
                        validateQuestion(q, i + 1, tg.getTaskName());
                    }
                }
            }
        }

        if (data.getStandaloneQuestions() != null) {
            for (int i = 0; i < data.getStandaloneQuestions().size(); i++) {
                var q = data.getStandaloneQuestions().get(i);
                validateQuestion(q, i + 1, "Standalone");
            }
        }
    }

    private void validateQuestion(CreateQuestionDTO question, int index, String location) throws Exception {
        String prefix = location + " Question #" + index + ": ";
        
        if (question.getQuestionType() == null) {
            throw new Exception(prefix + "Missing questionType");
        }
        if (question.getData() == null) {
            throw new Exception(prefix + "Missing data object");
        }
        
        log.debug("✓ Validated {} question #{}: {}", 
            location, index, question.getQuestionType());
    }

    private int countQuestions(LessonWithTaskGroups data) {
        int count = 0;
        if (data.getTaskGroups() != null) {
            for (var tg : data.getTaskGroups()) {
                if (tg.getQuestions() != null) {
                    count += tg.getQuestions().size();
                }
            }
        }
        if (data.getStandaloneQuestions() != null) {
            count += data.getStandaloneQuestions().size();
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private T convertToTargetType(LessonWithTaskGroups intermediate) throws Exception {
        if (targetType.equals(GrammarLessonDTO.class)) {
            GrammarLessonDTO dto = new GrammarLessonDTO();
            dto.setTitle(intermediate.getName());
            dto.setContent(intermediate.getContent());

            if (intermediate.getTaskGroups() != null) {
                List<GrammarLessonDTO.TaskGroupImportData> taskGroups = intermediate.getTaskGroups().stream()
                        .map(tg -> {
                            GrammarLessonDTO.TaskGroupImportData data = new GrammarLessonDTO.TaskGroupImportData();
                            data.setTaskName(tg.getTaskName());
                            data.setInstruction(tg.getInstruction());
                            data.setOrderIndex(tg.getOrderIndex());
                            data.setQuestions(tg.getQuestions());
                            return data;
                        })
                        .collect(Collectors.toList());
                dto.setTaskGroups(taskGroups);
            }

            dto.setStandaloneQuestions(intermediate.getStandaloneQuestions());
            return (T) dto;

        } else if (targetType.equals(ReadingLessonDTO.class)) {
            ReadingLessonDTO dto = new ReadingLessonDTO();
            dto.setTitle(intermediate.getName());
            dto.setContent(intermediate.getContent());

            if (intermediate.getTaskGroups() != null) {
                List<ReadingLessonDTO.TaskGroupImportData> taskGroups = intermediate.getTaskGroups().stream()
                        .map(tg -> {
                            ReadingLessonDTO.TaskGroupImportData data = new ReadingLessonDTO.TaskGroupImportData();
                            data.setTaskName(tg.getTaskName());
                            data.setInstruction(tg.getInstruction());
                            data.setOrderIndex(tg.getOrderIndex());
                            data.setQuestions(tg.getQuestions());
                            return data;
                        })
                        .collect(Collectors.toList());
                dto.setTaskGroups(taskGroups);
            }

            dto.setStandaloneQuestions(intermediate.getStandaloneQuestions());
            return (T) dto;

        } else if (targetType.equals(ListeningLessonDTO.class)) {
            ListeningLessonDTO dto = new ListeningLessonDTO();
            dto.setTitle(intermediate.getName());
            dto.setTranscript(intermediate.getContent());

            if (intermediate.getTaskGroups() != null) {
                List<ListeningLessonDTO.TaskGroupImportData> taskGroups = intermediate.getTaskGroups().stream()
                        .map(tg -> {
                            ListeningLessonDTO.TaskGroupImportData data = new ListeningLessonDTO.TaskGroupImportData();
                            data.setTaskName(tg.getTaskName());
                            data.setInstruction(tg.getInstruction());
                            data.setOrderIndex(tg.getOrderIndex());
                            data.setQuestions(tg.getQuestions());
                            return data;
                        })
                        .collect(Collectors.toList());
                dto.setTaskGroups(taskGroups);
            }

            dto.setStandaloneQuestions(intermediate.getStandaloneQuestions());
            return (T) dto;
        }

        throw new Exception("Unsupported target type: " + targetType.getName());
    }

    public T recoverFromAIError(Exception e, String prompt) throws Exception {
        log.error("❌ AI failed after retries: {}", e.getMessage());
        throw new Exception("Cannot generate lesson. Error: " + e.getMessage() + 
            "\n\nPlease check your instruction and try again.");
    }

    @Data
    public static class LessonWithTaskGroups {
        private String name;
        private String content;
        private List<TaskGroupData> taskGroups;
        private List<CreateQuestionDTO> standaloneQuestions;
    }

    @Data
    public static class TaskGroupData {
        private String taskName;
        private String instruction;
        private Integer orderIndex;
        private List<CreateQuestionDTO> questions;
    }
}