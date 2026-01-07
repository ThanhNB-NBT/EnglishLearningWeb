package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.dto.reading.ReadingLessonDTO;
import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.reading.UserReadingProgressRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.topic.TopicRepository;
import com.thanhnb.englishlearning.service.permission.TeacherPermissionService;
import com.thanhnb.englishlearning.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadingLessonService {

    private final ReadingLessonRepository lessonRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final UserReadingProgressRepository progressRepository;
    private final TeacherPermissionService teacherPermissionService;
    private final UserService userService;

    // ═════════════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    public Page<ReadingLessonDTO> getLessonsByTopic(Long topicId, Pageable pageable) {

        return lessonRepository.findByTopicId(topicId, pageable)
                .map(this::toDTO);
    }

    public ReadingLessonDTO getLessonById(Long id) {
        ReadingLesson lesson = findLessonById(id);

        return toDTO(lesson);
    }

    // ═════════════════════════════════════════════════════════════════
    // CREATE
    // ═════════════════════════════════════════════════════════════════

    public ReadingLessonDTO createLesson(ReadingLessonDTO dto) {
        validateForCreate(dto);

        teacherPermissionService.checkTopicPermission(dto.getTopicId());
        if (lessonRepository.existsByTitleIgnoreCase(dto.getTitle())) {
            throw new IllegalArgumentException("Title already exists");
        }

        Topic topic = topicRepository.findById(dto.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        User currentUser = userService.getCurrentUser();

        ReadingLesson lesson = ReadingLesson.builder()
                .topic(topic)
                .title(dto.getTitle())
                .content(dto.getContent())
                .contentTranslation(dto.getContentTranslation())
                .orderIndex(dto.getOrderIndex())
                .timeLimitSeconds(dto.getTimeLimitSeconds() != null ? dto.getTimeLimitSeconds() : 600)
                .pointsReward(dto.getPointsReward() != null ? dto.getPointsReward() : 25)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .modifiedBy(currentUser)
                .build();

        ReadingLesson saved = lessonRepository.save(lesson);
        log.info("Created reading lesson: id={}, title={}", saved.getId(), saved.getTitle());

        return toDTO(saved);
    }

    // ═════════════════════════════════════════════════════════════════
    // UPDATE
    // ═════════════════════════════════════════════════════════════════

    public ReadingLessonDTO updateLesson(Long id, ReadingLessonDTO dto) {
        ReadingLesson lesson = findLessonById(id);
        teacherPermissionService.checkTopicPermission(lesson.getTopic().getId());

        if (dto.getTitle() != null && !dto.getTitle().equals(lesson.getTitle())) {
            if (lessonRepository.existsByTitleIgnoreCaseAndIdNot(dto.getTitle(), id)) {
                throw new IllegalArgumentException("Title already exists");
            }
        }

        User currentUser = userService.getCurrentUser();
        lesson.setModifiedBy(currentUser);

        updateFields(lesson, dto);

        ReadingLesson saved = lessonRepository.save(lesson);
        log.info("Updated reading lesson: id={}", id);

        return toDTO(saved);
    }

    // ═════════════════════════════════════════════════════════════════
    // DELETE
    // ═════════════════════════════════════════════════════════════════

    public void deleteLesson(Long id) {
        ReadingLesson lesson = findLessonById(id);
        teacherPermissionService.checkTopicPermission(lesson.getTopic().getId());
        Long topicId = lesson.getTopic().getId();
        Integer orderIndex = lesson.getOrderIndex();

        // Delete progress
        progressRepository.deleteByLessonId(id);

        // Delete lesson (questions cascade via listener)
        lessonRepository.delete(lesson);
        log.info("Deleted reading lesson: id={}", id);

        // Reorder
        reorderAfterDelete(topicId, orderIndex);
    }

    // ═════════════════════════════════════════════════════════════════
    // STATUS TOGGLE
    // ═════════════════════════════════════════════════════════════════

    public void toggleStatus(Long id) {
        ReadingLesson lesson = findLessonById(id);
        teacherPermissionService.checkTopicPermission(lesson.getTopic().getId());

        lesson.setIsActive(!lesson.getIsActive());
        lessonRepository.save(lesson);

        log.info("Toggled lesson {} status to: {}", id, lesson.getIsActive());
    }

    // ═════════════════════════════════════════════════════════════════
    // ORDER MANAGEMENT
    // ═════════════════════════════════════════════════════════════════

    public void fixOrderIndexes(Long topicId) {
        // Lấy danh sách
        List<ReadingLesson> lessons = lessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId);
        
        // ✅ Sort lại trong memory để đảm bảo ổn định (ID tăng dần nếu trùng Order)
        lessons.sort(Comparator.comparingInt(ReadingLesson::getOrderIndex)
                .thenComparingLong(ReadingLesson::getId));

        boolean changed = false;
        for (int i = 0; i < lessons.size(); i++) {
            ReadingLesson lesson = lessons.get(i);
            int expected = i + 1;
            
            if (!Integer.valueOf(expected).equals(lesson.getOrderIndex())) {
                lesson.setOrderIndex(expected);
                changed = true;
            }
        }

        if (changed) {
            lessonRepository.saveAll(lessons);
            log.info("Fixed order indexes for {} lessons in topic {}", lessons.size(), topicId);
        }
    }

    public Integer getNextOrderIndex(Long topicId) {
        Integer max = lessonRepository.findMaxOrderIndexByTopicId(topicId);
        return (max != null ? max : 0) + 1;
    }

    // ═════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════

    private ReadingLesson findLessonById(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + id));
    }

    private void validateForCreate(ReadingLessonDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }
        if (dto.getOrderIndex() == null || dto.getOrderIndex() < 1) {
            throw new IllegalArgumentException("Order index must be >= 1");
        }
    }

    private void updateFields(ReadingLesson lesson, ReadingLessonDTO dto) {
        if (dto.getTitle() != null)
            lesson.setTitle(dto.getTitle());
        if (dto.getContent() != null)
            lesson.setContent(dto.getContent());
        if (dto.getContentTranslation() != null)
            lesson.setContentTranslation(dto.getContentTranslation());
        if (dto.getOrderIndex() != null)
            lesson.setOrderIndex(dto.getOrderIndex());
        if (dto.getTimeLimitSeconds() != null)
            lesson.setTimeLimitSeconds(dto.getTimeLimitSeconds());
        if (dto.getPointsReward() != null)
            lesson.setPointsReward(dto.getPointsReward());
        if (dto.getIsActive() != null)
            lesson.setIsActive(dto.getIsActive());
    }

    private void reorderAfterDelete(Long topicId, Integer deletedPosition) {
        List<ReadingLesson> lessons = lessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId);

        lessons.stream()
                .filter(l -> l.getOrderIndex() > deletedPosition)
                .forEach(l -> l.setOrderIndex(l.getOrderIndex() - 1));

        lessonRepository.saveAll(lessons);
    }

    private ReadingLessonDTO toDTO(ReadingLesson lesson) {
        long questionCount = questionRepository.countByParentTypeAndParentId(
                ParentType.READING, lesson.getId());

        return ReadingLessonDTO.builder()
                .id(lesson.getId())
                .topicId(lesson.getTopic().getId())
                .topicName(lesson.getTopic().getName())
                .title(lesson.getTitle())
                .content(lesson.getContent())
                .contentTranslation(lesson.getContentTranslation())
                .orderIndex(lesson.getOrderIndex())
                .timeLimitSeconds(lesson.getTimeLimitSeconds())
                .pointsReward(lesson.getPointsReward())
                .isActive(lesson.getIsActive())
                .createdAt(lesson.getCreatedAt())
                .questionCount((int) questionCount)
                .modifiedAt(lesson.getModifiedAt())
                .modifiedBy(lesson.getModifiedBy() != null ? lesson.getModifiedBy().getId() : null)
                .build();
    }
}