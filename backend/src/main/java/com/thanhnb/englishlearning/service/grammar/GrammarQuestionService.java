package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.service.question.BaseQuestionService;
import com.thanhnb.englishlearning.enums.LessonType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrammarQuestionService extends BaseQuestionService {

    private final GrammarLessonRepository lessonRepository;
    private final GrammarOrderService orderService;

    @Override
    protected ParentType getParentType() {
        return ParentType.GRAMMAR;
    }

    @Override
    protected void validateLessonExists(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new RuntimeException("Bài học ngữ pháp không tồn tại với id: " + lessonId);
        }
    }

    @Override
    protected void reorderAfterDelete(Long lessonId, Integer deletedOrderIndex) {
        orderService.reorderQuestionsAfterDelete(lessonId, deletedOrderIndex);
    }

    public long countQuestionsByLessonType(Long topicId, LessonType lessonType) {
        return lessonRepository.findByTopicIdAndLessonTypeAndIsActiveTrueOrderByOrderIndexAsc(topicId, lessonType)
                .stream()
                .mapToLong(lesson -> questionRepository.countByParentTypeAndParentId(
                        ParentType.GRAMMAR, lesson.getId()))
                .sum();
    }
}