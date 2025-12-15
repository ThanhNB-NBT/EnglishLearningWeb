package com.thanhnb.englishlearning.service. listening;

import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com. thanhnb.englishlearning.service.question.BaseQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listening Question Service - Extends BaseQuestionService
 * Inherit tất cả CRUD operations cho questions
 * Chỉ cần implement ParentType và validation
 */
@Service
@Slf4j
@Transactional
public class ListeningQuestionService extends BaseQuestionService {

    @Autowired
    private ListeningLessonRepository lessonRepository;

    /**
     * TEMPLATE METHOD:  Chỉ định ParentType là LISTENING
     */
    @Override
    protected ParentType getParentType() {
        return ParentType.LISTENING;
    }

    /**
     * TEMPLATE METHOD: Validate lesson exists
     */
    @Override
    protected void validateLessonExists(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new RuntimeException("Bài nghe không tồn tại với id: " + lessonId);
        }
    }

    /**
     * OPTIONAL: Reorder questions after delete
     * BaseQuestionService sẽ call method này sau khi delete
     */
    @Override
    protected void reorderAfterDelete(Long lessonId, Integer deletedOrderIndex) {
        // Optional: implement custom reorder logic if needed
        // Default: BaseQuestionService already handles reindexing
        log.debug("Questions reordered for listening lesson: {}", lessonId);
    }
}