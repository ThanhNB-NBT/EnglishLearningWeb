package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.dto.reading.ReadingLessonDTO;
import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ✅ Service chuyên xử lý Import cho Reading module
 * Tách logic từ ReadingAdminService cũ
 * Pattern: Single Responsibility Principle
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadingImportService {

    private final ReadingLessonRepository lessonRepository;
    private final ReadingQuestionService questionService;

    // ═════════════════════════════════════════════════════════════════
    // 📥 IMPORT FROM AI PARSING
    // ═════════════════════════════════════════════════════════════════

    /**
     * Import lesson từ AI parsing service (PDF/Image/DOCX)
     * Tương tự GrammarImportService.importLessonsFromFile()
     */
    public ReadingLessonDTO importLessonFromFile(ReadingLessonDTO parsedLesson) {
        log.info("[ADMIN] Importing parsed lesson: {}", parsedLesson.getTitle());

        // ===== VALIDATION =====
        validateParsedLesson(parsedLesson);

        // ===== GET NEXT ORDER INDEX =====
        Integer maxOrderIndex = lessonRepository.findAll().stream()
                .map(ReadingLesson::getOrderIndex)
                .max(Integer::compareTo)
                .orElse(0);

        Integer nextOrderIndex = maxOrderIndex + 1;

        // ===== CREATE LESSON ENTITY =====
        ReadingLesson lesson = new ReadingLesson();
        lesson.setTitle(parsedLesson.getTitle());
        lesson.setContent(parsedLesson.getContent());
        lesson.setContentTranslation(parsedLesson.getContentTranslation());
        lesson.setOrderIndex(nextOrderIndex);
        lesson.setPointsReward(25); // Fixed points for reading
        lesson.setIsActive(true);
        lesson.setCreatedAt(LocalDateTime.now());

        ReadingLesson savedLesson = lessonRepository.save(lesson);
        log.info("✅ Created lesson: id={}, title='{}', orderIndex={}",
                savedLesson.getId(), savedLesson.getTitle(), savedLesson.getOrderIndex());

        // ===== SAVE QUESTIONS (if exists) =====
        int questionCount = 0;
        if (parsedLesson.getQuestions() != null && !parsedLesson.getQuestions().isEmpty()) {
            log.info("📝 Creating {} questions for lesson: {}",
                    parsedLesson.getQuestions().size(), savedLesson.getTitle());

            questionService.createQuestionsInBulk(savedLesson.getId(), parsedLesson.getQuestions());
            questionCount = parsedLesson.getQuestions().size();

            log.info("✅ Created {} questions for lesson id={}", questionCount, savedLesson.getId());
        } else {
            log.warn("⚠️ No questions provided for lesson: {}", savedLesson.getTitle());
        }

        // ===== BUILD RESPONSE DTO =====
        ReadingLessonDTO result = new ReadingLessonDTO();
        result.setId(savedLesson.getId());
        result.setTitle(savedLesson.getTitle());
        result.setContent(savedLesson.getContent());
        result.setContentTranslation(savedLesson.getContentTranslation());
        result.setOrderIndex(savedLesson.getOrderIndex());
        result.setIsActive(savedLesson.getIsActive());

        log.info("🎉 Successfully imported lesson: id={}, title='{}', orderIndex={}, {} questions",
                savedLesson.getId(), savedLesson.getTitle(), savedLesson.getOrderIndex(), questionCount);

        return result;
    }

    // ═════════════════════════════════════════════════════════════════
    // 🔍 VALIDATION
    // ═════════════════════════════════════════════════════════════════

    /**
     * Validate parsed lesson từ AI
     */
    private void validateParsedLesson(ReadingLessonDTO parsedLesson) {
        if (parsedLesson == null) {
            throw new RuntimeException("Parsed lesson không được null");
        }

        // Check required fields
        if (parsedLesson.getTitle() == null || parsedLesson.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Lesson title không được để trống");
        }

        if (parsedLesson.getContent() == null || parsedLesson.getContent().trim().isEmpty()) {
            throw new RuntimeException("Lesson content không được để trống");
        }

        // Translation is optional but should have default
        if (parsedLesson.getContentTranslation() == null) {
            parsedLesson.setContentTranslation("");
            log.warn("⚠️ No Vietnamese translation provided, using empty string");
        }

        // Questions are optional
        if (parsedLesson.getQuestions() == null || parsedLesson.getQuestions().isEmpty()) {
            log.warn("⚠️ No questions provided in parsed lesson: {}", parsedLesson.getTitle());
        }

        log.info("✅ Validation passed for parsed lesson: {}", parsedLesson.getTitle());
    }

    // ═════════════════════════════════════════════════════════════════
    // 📊 IMPORT STATISTICS (Optional - for future use)
    // ═════════════════════════════════════════════════════════════════

    /**
     * Get import summary (có thể dùng cho batch import)
     */
    public ImportSummaryDTO getImportSummary(ReadingLessonDTO importedLesson) {
        ImportSummaryDTO summary = new ImportSummaryDTO();
        summary.setLessonId(importedLesson.getId());
        summary.setLessonTitle(importedLesson.getTitle());
        summary.setContentLength(importedLesson.getContent() != null ? importedLesson.getContent().length() : 0);
        summary.setTranslationLength(importedLesson.getContentTranslation() != null ? 
                importedLesson.getContentTranslation().length() : 0);
        summary.setQuestionCount(importedLesson.getQuestions() != null ? 
                importedLesson.getQuestions().size() : 0);
        summary.setOrderIndex(importedLesson.getOrderIndex());
        summary.setImportedAt(LocalDateTime.now());

        return summary;
    }

    /**
     * DTO cho import summary
     */
    public static class ImportSummaryDTO {
        private Long lessonId;
        private String lessonTitle;
        private Integer contentLength;
        private Integer translationLength;
        private Integer questionCount;
        private Integer orderIndex;
        private LocalDateTime importedAt;

        // Getters and Setters
        public Long getLessonId() {
            return lessonId;
        }

        public void setLessonId(Long lessonId) {
            this.lessonId = lessonId;
        }

        public String getLessonTitle() {
            return lessonTitle;
        }

        public void setLessonTitle(String lessonTitle) {
            this.lessonTitle = lessonTitle;
        }

        public Integer getContentLength() {
            return contentLength;
        }

        public void setContentLength(Integer contentLength) {
            this.contentLength = contentLength;
        }

        public Integer getTranslationLength() {
            return translationLength;
        }

        public void setTranslationLength(Integer translationLength) {
            this.translationLength = translationLength;
        }

        public Integer getQuestionCount() {
            return questionCount;
        }

        public void setQuestionCount(Integer questionCount) {
            this.questionCount = questionCount;
        }

        public Integer getOrderIndex() {
            return orderIndex;
        }

        public void setOrderIndex(Integer orderIndex) {
            this.orderIndex = orderIndex;
        }

        public LocalDateTime getImportedAt() {
            return importedAt;
        }

        public void setImportedAt(LocalDateTime importedAt) {
            this.importedAt = importedAt;
        }
    }
}