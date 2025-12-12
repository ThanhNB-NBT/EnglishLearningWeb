package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.grammar.GrammarTopicRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarOrderService {

    private final GrammarTopicRepository topicRepository;
    private final GrammarLessonRepository lessonRepository;
    private final QuestionRepository questionRepository;

    public void reorderTopicsAfterDelete(Integer deletedPosition) {
        int affected = topicRepository.shiftOrderAfterDelete(deletedPosition);
        log.info("Reordered {} topics after deletion at position {}", affected, deletedPosition);
    }

    public void reorderLessonsAfterDelete(Long topicId, Integer deletedPosition) {
        int affected = lessonRepository.shiftOrderAfterDelete(topicId, deletedPosition);
        log.info("Reordered {} lessons after deletion at position {}", affected, deletedPosition);
    }

    public void reorderQuestionsAfterDelete(Long lessonId, Integer deletedPosition) {
        int affected = questionRepository.shiftOrderAfterDelete(ParentType.GRAMMAR, lessonId, deletedPosition);
        log.info("Reordered {} questions after deletion at position {}", affected, deletedPosition);
    }
    
    public int reorderLessons(Long topicId, Integer insertPosition, Long excludeLessonId) {
        if (!topicRepository.existsById(topicId)) {
            throw new IllegalArgumentException("Topic không tồn tại với id: " + topicId);
        }

        if (insertPosition == null || insertPosition < 1) {
            throw new IllegalArgumentException("Vị trí chèn phải lớn hơn 0");
        }

        List<GrammarLesson> allLessons = lessonRepository
                .findByTopicIdOrderByOrderIndexAsc(topicId);

        if (excludeLessonId != null) {
            allLessons = allLessons.stream()
                    .filter(lesson -> !lesson.getId().equals(excludeLessonId))
                    .collect(Collectors.toList());
        }

        List<GrammarLesson> affectedLessons = allLessons.stream()
                .filter(lesson -> lesson.getOrderIndex() >= insertPosition)
                .collect(Collectors.toList());

        if (affectedLessons.isEmpty()) {
            log.info("No lessons need reordering at position {}", insertPosition);
            return 0;
        }

        for (GrammarLesson lesson : affectedLessons) {
            lesson.setOrderIndex(lesson.getOrderIndex() + 1);
        }

        lessonRepository.saveAll(affectedLessons);
        log.info("Reordered {} lessons starting from position {}", affectedLessons.size(), insertPosition);

        return affectedLessons.size();
    }
}