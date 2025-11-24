package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.service.question.BaseQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ✅ REFACTORED: Reading Question Service
 * Chỉ chứa logic RIÊNG của Reading
 * Tất cả CRUD đã có sẵn từ BaseQuestionService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingQuestionService extends BaseQuestionService {

    private final ReadingLessonRepository lessonRepository;
    private final ReadingOrderService orderService;

    /**
     * ✅ Override: Chỉ định ParentType
     */
    @Override
    protected ParentType getParentType() {
        return ParentType.READING;
    }

    /**
     * ✅ Override: Validate lesson exists
     */
    @Override
    protected void validateLessonExists(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new RuntimeException("Bài đọc không tồn tại với id: " + lessonId);
        }
    }

    /**
     * ✅ Override: Reorder after delete (Reading specific)
     */
    @Override
    protected void reorderAfterDelete(Long lessonId, Integer deletedOrderIndex) {
        orderService.reorderQuestionsAfterDelete(lessonId, deletedOrderIndex);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 READING-SPECIFIC METHODS (nếu có)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Example: Get reading statistics (Reading specific)
     */
    public ReadingQuestionStats getQuestionStats(Long lessonId) {
        validateLessonExists(lessonId);

        long total = questionRepository.countByParentTypeAndParentId(
                ParentType.READING, lessonId);

        // Reading-specific stats
        return new ReadingQuestionStats(total);
    }

    /**
     * ✅ Inner class for stats
     */
    public static class ReadingQuestionStats {
        private final long totalQuestions;

        public ReadingQuestionStats(long totalQuestions) {
            this.totalQuestions = totalQuestions;
        }

        public long getTotalQuestions() {
            return totalQuestions;
        }
    }

    // Tất cả CRUD methods đều inherit từ BaseQuestionService:
    // - createQuestion(dto)
    // - updateQuestion(id, dto)
    // - deleteQuestion(id)
    // - getQuestionsByLesson(lessonId)
    // - getQuestionsByLessonPaginated(lessonId, pageable)
    // - createQuestionsInBulk(lessonId, dtos)
    // - bulkDeleteQuestions(ids)
    // - copyQuestionsToLesson(sourceId, targetId)
    // - getNextOrderIndex(lessonId)
}