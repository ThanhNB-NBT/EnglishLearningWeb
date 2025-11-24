package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.dto.reading.ReadingLessonDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * ✅ MAIN ORCHESTRATOR - Chỉ delegate, KHÔNG chứa business logic
 * Pattern: Facade Pattern
 * Tham khảo: GrammarAdminService.java
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadingAdminService {

    // ===== DEPENDENCIES - Inject specialized services =====
    private final ReadingLessonService lessonService;
    private final ReadingQuestionService questionService;
    private final ReadingOrderService orderService;
    private final ReadingValidationService validationService;
    private final ReadingStatisticsService statisticsService;
    private final ReadingImportService importService;

    // ═════════════════════════════════════════════════════════════════
    // LESSON OPERATIONS - Delegate to ReadingLessonService
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Lấy tất cả bài đọc (bao gồm cả inactive)
     */
    public Page<ReadingLessonDTO> getAllLessons(Pageable pageable) {
        return lessonService.getAllLessonsPaginated(pageable);
    }

    /**
     * [ADMIN] Lấy chi tiết bài đọc theo ID
     */
    public ReadingLessonDTO getLessonById(Long lessonId) {
        return lessonService.getLessonDetail(lessonId);
    }

    /**
     * [ADMIN] Tạo bài đọc mới
     */
    public ReadingLessonDTO createLesson(ReadingLessonDTO dto) {
        return lessonService.createLesson(dto);
    }

    /**
     * [ADMIN] Cập nhật bài đọc
     */
    public ReadingLessonDTO updateLesson(Long lessonId, ReadingLessonDTO dto) {
        return lessonService.updateLesson(lessonId, dto);
    }

    /**
     * [ADMIN] set isActive = false
     */
    public void deactiveLesson(Long lessonId) {
        lessonService.deactiveLesson(lessonId);
    }

    /**
     * [ADMIN] Xóa bài đọc vĩnh viễn
     */
    public void deleteLesson(Long lessonId) {
        lessonService.deleteLesson(lessonId);
    }

    /**
     * [ADMIN] Activate/Deactivate lesson
     */
    public void toggleLessonStatus(Long lessonId) {
        lessonService.toggleLessonStatus(lessonId);
    }

    // ═════════════════════════════════════════════════════════════════
    // ORDER OPERATIONS - Delegate to ReadingOrderService
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Thay đổi thứ tự bài đọc
     */
    public void reorderLesson(Long lessonId, Integer newOrderIndex) {
        log.info("[ADMIN] Reordering lesson {} to position {}", lessonId, newOrderIndex);

        if (newOrderIndex == null || newOrderIndex < 1) {
            throw new RuntimeException("Vị trí mới phải lớn hơn 0");
        }

        orderService.moveLessonToPosition(lessonId, newOrderIndex);
        log.info("[ADMIN] Successfully reordered lesson {} to position {}", lessonId, newOrderIndex);
    }

    public Integer getNextLessonOrderIndex() {
        return lessonService.getNextOrderIndex();
    }

    /**
     * [ADMIN] Swap 2 lessons
     */
    public void swapLessons(Long lessonId1, Long lessonId2) {
        log.info("[ADMIN] Swapping lessons {} and {}", lessonId1, lessonId2);
        orderService.swapLessons(lessonId1, lessonId2);
        log.info("[ADMIN] Successfully swapped lessons {} and {}", lessonId1, lessonId2);
    }

    // ═════════════════════════════════════════════════════════════════
    // QUESTION OPERATIONS - Delegate to ReadingQuestionService
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Copy questions từ lesson này sang lesson khác
     */
    public void copyQuestionsToLesson(Long sourceLessonId, Long targetLessonId) {
        log.info("[ADMIN] Copying questions from lesson {} to lesson {}", sourceLessonId, targetLessonId);
        questionService.copyQuestionsToLesson(sourceLessonId, targetLessonId);
        log.info("[ADMIN] Successfully copied questions from lesson {} to lesson {}", sourceLessonId, targetLessonId);
    }

    /**
     * [ADMIN] Tạo nhiều questions cùng lúc
     */
    public List<QuestionResponseDTO> createQuestionsInBulk(Long lessonId, List<CreateQuestionDTO> createDTOs) {
        return questionService.createQuestionsInBulk(lessonId, createDTOs);
    }

    /**
     * [ADMIN] Xóa nhiều questions
     */
    public int bulkDeleteQuestions(List<Long> questionIds) {
        return questionService.bulkDeleteQuestions(questionIds);
    }

    // ═════════════════════════════════════════════════════════════════
    // STATISTICS OPERATIONS - Delegate to ReadingStatisticsService
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Lấy thống kê bài đọc
     */
    public ReadingStatisticsService.ReadingStatisticsDTO getLessonStatistics(Long lessonId) {
        return statisticsService.getLessonStatistics(lessonId);
    }

    /**
     * [ADMIN] Lấy thống kê toàn bộ module Reading
     */
    public ReadingStatisticsService.ReadingModuleStatisticsDTO getModuleStatistics() {
        return statisticsService.getModuleStatistics();
    }

    // ═════════════════════════════════════════════════════════════════
    // IMPORT OPERATIONS - Delegate to ReadingImportService
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Import lesson từ AI parsing
     */
    public ReadingLessonDTO importLessonFromFile(ReadingLessonDTO parsedLesson) {
        log.info("[ADMIN] Importing parsed lesson: {}", parsedLesson.getTitle());
        return importService.importLessonFromFile(parsedLesson);
    }

    // ═════════════════════════════════════════════════════════════════
    // VALIDATION OPERATIONS - Delegate to ReadingValidationService
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Validate và fix orderIndex của tất cả Reading Lessons
     */
    public Map<String, Object> validateAllLessonsOrderIndex() {
        return validationService.validateAllLessonsOrderIndex();
    }

    /**
     * [ADMIN] Validate và fix orderIndex của Questions trong 1 lesson
     */
    public Map<String, Object> validateQuestionsOrderIndex(Long lessonId) {
        return validationService.validateQuestionsOrderIndex(lessonId);
    }

    /**
     * [ADMIN] Validate và fix orderIndex của TẤT CẢ Reading Questions
     */
    public Map<String, Object> validateAllQuestionsOrderIndex() {
        return validationService.validateAllQuestionsOrderIndex();
    }

    /**
     * [ADMIN] Health check toàn bộ Reading module
     */
    public Map<String, Object> healthCheck() {
        return validationService.healthCheck();
    }
}