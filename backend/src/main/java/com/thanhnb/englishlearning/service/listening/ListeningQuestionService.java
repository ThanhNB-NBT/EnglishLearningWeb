package com.thanhnb.englishlearning.service.listening;

import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.question.TaskGroupRepository;
import com.thanhnb.englishlearning.service.permission.TeacherPermissionService;
import com.thanhnb.englishlearning.service.question.BaseQuestionService;
import com.thanhnb.englishlearning.service.question.TaskGroupService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Listening Question Service
 * Handles CRUD operations for questions in listening lessons
 * ✅ WITH Teacher Access Validation
 */
@Service
@Slf4j
public class ListeningQuestionService extends BaseQuestionService {

    private final ListeningLessonRepository lessonRepository;

    public ListeningQuestionService(
            QuestionRepository questionRepository,
            TeacherPermissionService teacherPermissionService,
            ListeningLessonRepository lessonRepository,
            TaskGroupService taskGroupService,
            TaskGroupRepository taskGroupRepository) {
        super(questionRepository, teacherPermissionService, taskGroupService, taskGroupRepository);
        this.lessonRepository = lessonRepository;
    }

    @Override
    protected ParentType getParentType() {
        return ParentType.LISTENING;
    }

    @Override
    protected void validateLessonExists(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new ResourceNotFoundException("Bài nghe không tồn tại với id: " + lessonId);
        }
    }

    

    /**
     * ✅ KEY METHOD: Get topicId from listening lesson
     * This enables teacher access validation
     */
    @Override
    protected Long getTopicIdFromLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Listening lesson not found"))
                .getTopic()
                .getId();
    }

    // =========================================================================
    // LISTENING-SPECIFIC METHODS
    // =========================================================================

    /**
     * Get question statistics for a listening lesson
     */
    public ListeningQuestionStats getQuestionStats(Long lessonId) {
        validateLessonExists(lessonId);

        long total = questionRepository.countByParentTypeAndParentId(
                ParentType.LISTENING, lessonId);

        return new ListeningQuestionStats(total);
    }

    /**
     * Get total questions count for a topic
     */
    public long countQuestionsInTopic(Long topicId) {
        return lessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId)
                .stream()
                .mapToLong(lesson -> questionRepository.countByParentTypeAndParentId(
                        ParentType.LISTENING, lesson.getId()))
                .sum();
    }

    // =========================================================================
    // INNER CLASS
    // =========================================================================

    public static class ListeningQuestionStats {
        private final long totalQuestions;

        public ListeningQuestionStats(long totalQuestions) {
            this.totalQuestions = totalQuestions;
        }

        public long getTotalQuestions() {
            return totalQuestions;
        }
    }
}