package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.question.TaskGroupRepository;
import com.thanhnb.englishlearning.service.permission.TeacherPermissionService;
import com.thanhnb.englishlearning.service.question.BaseQuestionService;
import com.thanhnb.englishlearning.service.question.TaskGroupService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Grammar Question Service
 * Handles CRUD operations for questions in grammar lessons
 * ✅ WITH Teacher Access Validation
 */
@Service
@Slf4j
public class GrammarQuestionService extends BaseQuestionService {

    private final GrammarLessonRepository lessonRepository;

    public GrammarQuestionService(
            QuestionRepository questionRepository,
            TeacherPermissionService teacherPermissionService,
            GrammarLessonRepository lessonRepository,
            TaskGroupService taskGroupService,
            TaskGroupRepository taskGroupRepository) {
        super(questionRepository, teacherPermissionService, taskGroupService, taskGroupRepository);
        this.lessonRepository = lessonRepository;
    }

    @Override
    protected ParentType getParentType() {
        return ParentType.GRAMMAR;
    }

    @Override
    protected void validateLessonExists(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new ResourceNotFoundException("Bài học ngữ pháp không tồn tại với id: " + lessonId);
        }
    }

    /**
     * ✅ KEY METHOD: Get topicId from grammar lesson
     * This enables teacher access validation
     */
    @Override
    protected Long getTopicIdFromLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Grammar lesson not found"))
                .getTopic()
                .getId();
    }

    // =========================================================================
    // GRAMMAR-SPECIFIC METHODS
    // =========================================================================

    /**
     * Count questions by lesson type (THEORY/PRACTICE)
     */
    public long countQuestionsByLessonType(Long topicId, LessonType lessonType) {
        return lessonRepository
                .findByTopicIdAndLessonTypeAndIsActiveTrueOrderByOrderIndexAsc(topicId, lessonType)
                .stream()
                .mapToLong(lesson -> questionRepository.countByParentTypeAndParentId(
                        ParentType.GRAMMAR, lesson.getId()))
                .sum();
    }

    /**
     * Get total questions count for a topic
     */
    public long countQuestionsInTopic(Long topicId) {
        return lessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId)
                .stream()
                .mapToLong(lesson -> questionRepository.countByParentTypeAndParentId(
                        ParentType.GRAMMAR, lesson.getId()))
                .sum();
    }
}