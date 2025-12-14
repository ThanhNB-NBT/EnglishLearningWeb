package com.thanhnb.englishlearning.service. listening;

import com.thanhnb.englishlearning.dto. listening.ListeningLessonDTO;
import com.thanhnb.englishlearning.dto.listening.request.CreateListeningLessonRequest;
import com.thanhnb. englishlearning.dto.listening.request.UpdateListeningLessonRequest;
import com. thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto. question.response.QuestionResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework. web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * ✅ MAIN ORCHESTRATOR - Chỉ delegate, KHÔNG chứa business logic
 * Pattern:  Facade Pattern
 * Tương tự:  ReadingAdminService. java
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListeningAdminService {

    // ===== DEPENDENCIES - Inject specialized services =====
    private final ListeningLessonService lessonService;
    private final ListeningQuestionService questionService;
    private final ListeningOrderService orderService;
    private final ListeningValidationService validationService;
    private final ListeningStatisticsService statisticsService;

    // ═════════════════════════════════════════════════════════════════
    // LESSON OPERATIONS - Delegate to ListeningLessonService
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Lấy tất cả bài nghe (bao gồm cả inactive)
     */
    public Page<ListeningLessonDTO> getAllLessons(Pageable pageable) {
        return lessonService.getAllLessonsPaginated(pageable);
    }

    /**
     * [ADMIN] Lấy chi tiết bài nghe theo ID
     */
    public ListeningLessonDTO getLessonById(Long lessonId) {
        return lessonService.getLessonDetail(lessonId);
    }

    /**
     * [ADMIN] Tạo bài nghe mới với audio
     */
    public ListeningLessonDTO createLesson(CreateListeningLessonRequest request, MultipartFile audioFile) 
            throws IOException {
        return lessonService.createLesson(request, audioFile);
    }

    /**
     * [ADMIN] Cập nhật bài nghe
     */
    public ListeningLessonDTO updateLesson(Long lessonId, UpdateListeningLessonRequest request, 
            MultipartFile audioFile) throws IOException {
        return lessonService. updateLesson(lessonId, request, audioFile);
    }

    /**
     * [ADMIN] Set isActive = false
     */
    public void deactivateLesson(Long lessonId) {
        lessonService. deactivateLesson(lessonId);
    }

    /**
     * [ADMIN] Xóa bài nghe vĩnh viễn (bao gồm audio file)
     */
    public void deleteLesson(Long lessonId) {
        lessonService. deleteLesson(lessonId);
    }

    /**
     * [ADMIN] Activate/Deactivate lesson
     */
    public void toggleLessonStatus(Long lessonId) {
        lessonService.toggleLessonStatus(lessonId);
    }

    // ═════════════════════════════════════════════════════════════════
    // ORDER OPERATIONS - Delegate to ListeningOrderService
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Thay đổi thứ tự bài nghe
     */
    public void reorderLesson(Long lessonId, Integer newOrderIndex) {
        log.info("[ADMIN] Reordering lesson {} to position {}", lessonId, newOrderIndex);

        if (newOrderIndex == null || newOrderIndex < 1) {
            throw new RuntimeException("Vị trí mới phải lớn hơn 0");
        }

        orderService.moveLessonToPosition(lessonId, newOrderIndex);
        log.info("[ADMIN] Successfully reordered lesson {} to position {}", lessonId, newOrderIndex);
    }

    /**
     * [ADMIN] Lấy orderIndex tiếp theo
     */
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
    // QUESTION OPERATIONS - Delegate to ListeningQuestionService
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Copy questions từ lesson này sang lesson khác
     */
    public void copyQuestionsToLesson(Long sourceLessonId, Long targetLessonId) {
        log.info("[ADMIN] Copying questions from lesson {} to lesson {}", sourceLessonId, targetLessonId);
        questionService.copyQuestionsToLesson(sourceLessonId, targetLessonId);
        log.info("[ADMIN] Successfully copied questions from lesson {} to lesson {}", 
                sourceLessonId, targetLessonId);
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
    // STATISTICS OPERATIONS - Delegate to ListeningStatisticsService
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Lấy thống kê bài nghe
     */
    public ListeningStatisticsService. ListeningStatisticsDTO getLessonStatistics(Long lessonId) {
        return statisticsService.getLessonStatistics(lessonId);
    }

    /**
     * [ADMIN] Lấy thống kê toàn bộ module Listening
     */
    public ListeningStatisticsService.ListeningModuleStatisticsDTO getModuleStatistics() {
        return statisticsService.getModuleStatistics();
    }

    // ═════════════════════════════════════════════════════════════════
    // VALIDATION OPERATIONS - Delegate to ListeningValidationService
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Validate và fix orderIndex của tất cả Listening Lessons
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
     * [ADMIN] Validate và fix orderIndex của TẤT CẢ Listening Questions
     */
    public Map<String, Object> validateAllQuestionsOrderIndex() {
        return validationService.validateAllQuestionsOrderIndex();
    }

    /**
     * [ADMIN] Health check toàn bộ Listening module
     */
    public Map<String, Object> healthCheck() {
        return validationService.healthCheck();
    }

    /**
     * [ADMIN] Validate audio files tồn tại
     */
    public Map<String, Object> validateAudioFiles() {
        return validationService.validateAudioFiles();
    }
}