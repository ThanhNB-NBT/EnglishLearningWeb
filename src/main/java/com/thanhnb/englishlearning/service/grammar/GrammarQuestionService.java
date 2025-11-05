package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.service.question.BaseQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ✅ REFACTORED: Grammar Question Service
 * Chỉ chứa logic RIÊNG của Grammar
 * Tất cả CRUD đã có sẵn từ BaseQuestionService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrammarQuestionService extends BaseQuestionService {

    private final GrammarLessonRepository lessonRepository;
    private final GrammarOrderService orderService;

    /**
     * ✅ Override: Chỉ định ParentType
     */
    @Override
    protected ParentType getParentType() {
        return ParentType.GRAMMAR;
    }

    /**
     * ✅ Override: Validate lesson exists
     */
    @Override
    protected void validateLessonExists(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new RuntimeException("Bài học ngữ pháp không tồn tại với id: " + lessonId);
        }
    }

    /**
     * ✅ Override: Reorder after delete (Grammar specific)
     */
    @Override
    protected void reorderAfterDelete(Long lessonId, Integer deletedOrderIndex) {
        orderService.reorderQuestionsAfterDelete(lessonId, deletedOrderIndex);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 GRAMMAR-SPECIFIC METHODS (nếu có)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Example: Get questions by lesson type (Grammar specific)
     */
    public long countQuestionsByLessonType(Long topicId, com.thanhnb.englishlearning.enums.LessonType lessonType) {
        // Grammar-specific logic
        return lessonRepository.findByTopicIdAndLessonTypeAndIsActiveTrueOrderByOrderIndexAsc(topicId, lessonType)
                .stream()
                .mapToLong(lesson -> questionRepository.countByParentTypeAndParentId(
                        ParentType.GRAMMAR, lesson.getId()))
                .sum();
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