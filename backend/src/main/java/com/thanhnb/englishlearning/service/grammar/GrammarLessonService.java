package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.grammar.GrammarTopic;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.grammar.GrammarTopicRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarLessonService {

    private final GrammarLessonRepository lessonRepository;
    private final GrammarTopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final GrammarOrderService orderService;

    public Page<GrammarLessonDTO> getLessonsByTopicPaginated(Long topicId, Pageable pageable) {
        if (!topicRepository.existsById(topicId)) {
            throw new RuntimeException("Topic không tồn tại với id: " + topicId);
        }

        return lessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId, pageable)
                .map(lesson -> {
                    GrammarLessonDTO dto = convertToDTO(lesson);
                    long questionCount = questionRepository.countByParentTypeAndParentId(
                            ParentType.GRAMMAR, lesson.getId());
                    dto.setQuestionCount((int) questionCount);
                    return dto;
                });
    }

    public GrammarLessonDTO getLessonDetail(Long lessonId) {
        GrammarLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại với id: " + lessonId));

        GrammarLessonDTO dto = convertToDTO(lesson);
        
        long questionCount = questionRepository.countByParentTypeAndParentId(
                ParentType.GRAMMAR, lessonId);
        dto.setQuestionCount((int) questionCount);

        return dto;
    }

    public GrammarLessonDTO createLesson(GrammarLessonDTO dto) {
        GrammarTopic topic = topicRepository.findById(dto.getTopicId())
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại với id: " + dto.getTopicId()));

        if (lessonRepository.existsByTopicIdAndTitleIgnoreCase(dto.getTopicId(), dto.getTitle())) {
            throw new RuntimeException("Tiêu đề bài học đã tồn tại trong topic này");
        }

        if (dto.getEstimatedDuration() != null && dto.getEstimatedDuration() < 0) {
            throw new RuntimeException("Thời gian ước lượng phải lớn hơn hoặc bằng 0");
        }

        if (dto.getPointsReward() != null && dto.getPointsReward() <= 0) {
            throw new RuntimeException("Điểm thưởng phải lớn hơn 0");
        }

        GrammarLesson lesson = new GrammarLesson();
        lesson.setTopic(topic);
        lesson.setTitle(dto.getTitle());
        lesson.setLessonType(dto.getLessonType());
        lesson.setContent(dto.getContent());
        lesson.setOrderIndex(dto.getOrderIndex());
        lesson.setEstimatedDuration(dto.getEstimatedDuration() != null ? dto.getEstimatedDuration() : 30);
        lesson.setPointsReward(dto.getPointsReward() != null ? dto.getPointsReward() : 10);
        lesson.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        lesson.setCreatedAt(LocalDateTime.now());
        lesson.setMetadata(dto.getMetadata());

        GrammarLesson savedLesson = lessonRepository.save(lesson);
        log.info("Created new Grammar Lesson: {}", savedLesson.getTitle());

        return convertToDTO(savedLesson);
    }

    public GrammarLessonDTO updateLesson(Long id, GrammarLessonDTO dto) {
        GrammarLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại với id: " + id));

        if (lessonRepository.existsByTopicIdAndTitleIgnoreCaseAndIdNot(
                lesson.getTopic().getId(), dto.getTitle(), id)) {
            throw new RuntimeException("Tiêu đề bài học đã tồn tại trong topic này");
        }

        if (dto.getEstimatedDuration() != null && dto.getEstimatedDuration() < 0) {
            throw new RuntimeException("Thời gian ước lượng phải lớn hơn hoặc bằng 0");
        }

        if (dto.getPointsReward() != null && dto.getPointsReward() <= 0) {
            throw new RuntimeException("Điểm thưởng phải lớn hơn 0");
        }

        lesson.setTitle(dto.getTitle());
        lesson.setLessonType(dto.getLessonType());
        lesson.setContent(dto.getContent());
        lesson.setOrderIndex(dto.getOrderIndex());
        
        if (dto.getEstimatedDuration() != null) {
            lesson.setEstimatedDuration(dto.getEstimatedDuration());
        }
        if (dto.getPointsReward() != null) {
            lesson.setPointsReward(dto.getPointsReward());
        }
        if (dto.getIsActive() != null) {
            lesson.setIsActive(dto.getIsActive());
        }

        GrammarLesson savedLesson = lessonRepository.save(lesson);
        log.info("Updated Grammar Lesson: {}", savedLesson.getTitle());

        return convertToDTO(savedLesson);
    }

    public void deleteLesson(Long id, boolean cascade) {
        GrammarLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại với id: " + id));

        boolean shouldCascade = cascade || lesson.getLessonType().equals(LessonType.PRACTICE);

        if (shouldCascade) {
            List<Question> questions = questionRepository
                    .findByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType.GRAMMAR, id);
            
            if (!questions.isEmpty()) {
                questionRepository.deleteAllByIdInBatch(
                        questions.stream().map(Question::getId).toList());
                
                log.info("Deleted {} questions for lesson {}", 
                        questions.size(), lesson.getTitle());
            }
        } else {
            long count = questionRepository.countByParentTypeAndParentId(ParentType.GRAMMAR, id);
            if (count > 0) {
                throw new RuntimeException("Không thể xóa bài học vì có " + count + " câu hỏi thuộc bài học này");
            }
        }

        Long topicId = lesson.getTopic().getId();
        Integer deletedOrderIndex = lesson.getOrderIndex();

        lessonRepository.delete(lesson);
        log.info("Deleted Grammar Lesson: {} (orderIndex: {})", lesson.getTitle(), deletedOrderIndex);

        orderService.reorderLessonsAfterDelete(topicId, deletedOrderIndex);
    }

    public void deactivateLesson(Long id) {
        GrammarLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại với id: " + id));

        lesson.setIsActive(false);
        lessonRepository.save(lesson);
        log.info("Deactivated Grammar Lesson: {}", lesson.getTitle());
    }

    public Integer getNextOrderIndex(Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new RuntimeException("Topic không tồn tại với id: " + topicId);
        }

        Integer maxOrder = lessonRepository.findMaxOrderIndexByTopicId(topicId);
        if (maxOrder != null) {
            log.info("Max lesson orderIndex in topic {}: {}", topicId, maxOrder);
            return maxOrder + 1;
        }

        log.info("No lessons found in topic {}, returning 1", topicId);
        return 1;
    }

    public int reorderLessons(Long topicId, Integer insertPosition, Long excludeLessonId) {
        return orderService.reorderLessons(topicId, insertPosition, excludeLessonId);
    }

    private GrammarLessonDTO convertToDTO(GrammarLesson lesson) {
        return GrammarLessonDTO.full(
                lesson.getId(),
                lesson.getTopic().getId(),
                lesson.getTitle(),
                lesson.getLessonType(),
                lesson.getContent(),
                lesson.getOrderIndex(),
                lesson.getPointsReward(),
                lesson.getEstimatedDuration(),
                lesson.getIsActive(),
                lesson.getCreatedAt(),
                lesson.getMetadata(),
                lesson.getTopic().getName()
        );
    }
}