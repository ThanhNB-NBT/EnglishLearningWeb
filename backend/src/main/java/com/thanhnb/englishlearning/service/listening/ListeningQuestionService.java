package com.thanhnb. englishlearning.service.listening;

import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com. thanhnb.englishlearning.service.question.BaseQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework. beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Question service cho Listening module
 * - Kế thừa tất cả CRUD operations từ BaseQuestionService
 * - Chỉ cần implement 2 methods: getParentType() và validateLessonExists()
 */
@Service
@Slf4j
@Transactional
public class ListeningQuestionService extends BaseQuestionService {

    @Autowired
    private ListeningLessonRepository listeningLessonRepository;

    @Override
    protected ParentType getParentType() {
        return ParentType. LISTENING;
    }

    @Override
    protected void validateLessonExists(Long lessonId) {
        if (!listeningLessonRepository.existsById(lessonId)) {
            throw new RuntimeException("Bài nghe không tồn tại với id: " + lessonId);
        }
    }

    @Override
    protected void reorderAfterDelete(Long lessonId, Integer deletedOrderIndex) {
        // Optional: Implement if needed
        // For now, keep default behavior (do nothing)
        log.debug("Question deleted from listening lesson {}, orderIndex was {}", 
                lessonId, deletedOrderIndex);
    }
}