package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.question.TaskGroupRepository;
import com.thanhnb.englishlearning.service.permission.TeacherPermissionService;
import com.thanhnb.englishlearning.service.question.BaseQuestionService;
import com.thanhnb.englishlearning.service.question.TaskGroupService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Reading Question Service
 * Handles CRUD operations for questions in reading lessons
 * WITH Teacher Access Validation
 */
@Service
@Slf4j
public class ReadingQuestionService extends BaseQuestionService {

    private final ReadingLessonRepository lessonRepository;

    public ReadingQuestionService(
            QuestionRepository questionRepository,
            TeacherPermissionService teacherPermissionService,
            ReadingLessonRepository lessonRepository,
            TaskGroupService taskGroupService,
            TaskGroupRepository taskGroupRepository) {
        super(questionRepository, teacherPermissionService, taskGroupService, taskGroupRepository);
        this.lessonRepository = lessonRepository;
    }

    @Override
    protected ParentType getParentType() {
        return ParentType.READING;
    }

    @Override
    protected void validateLessonExists(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new ResourceNotFoundException("Bài đọc không tồn tại với id: " + lessonId);
        }
    }

    /**
     * ✅ KEY METHOD: Get topicId from reading lesson
     * This enables teacher access validation
     */
    @Override
    protected Long getTopicIdFromLesson(Long lessonId) {
        ReadingLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bài đọc không tồn tại với id: " + lessonId));
        
        return lesson.getTopic().getId();
    }

    // =========================================================================
    // READING-SPECIFIC METHODS
    // =========================================================================

    /**
     * Get question statistics for a reading lesson
     */
    public ReadingQuestionStats getQuestionStats(Long lessonId) {
        validateLessonExists(lessonId);

        long total = questionRepository.countByParentTypeAndParentId(
                ParentType.READING, lessonId);

        return new ReadingQuestionStats(total);
    }

    /**
     * Get total questions count for a topic
     */
    public long countQuestionsInTopic(Long topicId) {
        return lessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId)
                .stream()
                .mapToLong(lesson -> questionRepository.countByParentTypeAndParentId(
                        ParentType.READING, lesson.getId()))
                .sum();
    }

    // =========================================================================
    // INNER CLASS
    // =========================================================================

    public static class ReadingQuestionStats {
        private final long totalQuestions;

        public ReadingQuestionStats(long totalQuestions) {
            this.totalQuestions = totalQuestions;
        }

        public long getTotalQuestions() {
            return totalQuestions;
        }
    }
}