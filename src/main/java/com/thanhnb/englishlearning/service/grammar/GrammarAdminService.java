package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.dto.question.QuestionDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Main service orchestrator - Delegate to specialized services
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarAdminService {

    
    private final GrammarTopicService topicService;
    private final GrammarLessonService lessonService;
    private final GrammarQuestionService questionService;
    private final GrammarValidationService validationService;
    private final GrammarImportService importService;

    // ===== TOPIC OPERATIONS =====

    public Page<GrammarTopicDTO> getAllTopicsPaginated(Pageable pageable) {
        return topicService.getAllTopicsPaginated(pageable);
    }

    public GrammarTopicDTO createTopic(GrammarTopicDTO dto) {
        return topicService.createTopic(dto);
    }

    public GrammarTopicDTO updateTopic(Long id, GrammarTopicDTO dto) {
        return topicService.updateTopic(id, dto);
    }

    public void deleteTopic(Long id) {
        topicService.deleteTopic(id);
    }

    public Integer getNextTopicOrderIndex() {
        return topicService.getNextOrderIndex();
    }

    // ===== LESSON OPERATIONS =====

    public Page<GrammarLessonDTO> getLessonsByTopicPaginated(Long topicId, Pageable pageable) {
        return lessonService.getLessonsByTopicPaginated(topicId, pageable);
    }

    public GrammarLessonDTO getLessonDetail(Long lessonId) {
        return lessonService.getLessonDetail(lessonId);
    }

    public GrammarLessonDTO createLesson(GrammarLessonDTO dto) {
        return lessonService.createLesson(dto);
    }

    public GrammarLessonDTO updateLesson(Long id, GrammarLessonDTO dto) {
        return lessonService.updateLesson(id, dto);
    }

    public void deleteLesson(Long id, boolean cascade) {
        lessonService.deleteLesson(id, cascade);
    }

    public Integer getNextLessonOrderIndex(Long topicId) {
        return lessonService.getNextOrderIndex(topicId);
    }

    public int reorderLessons(Long topicId, Integer insertPosition, Long excludeLessonId) {
        return lessonService.reorderLessons(topicId, insertPosition, excludeLessonId);
    }

    // ===== QUESTION OPERATIONS =====

    public Page<QuestionDTO> getQuestionsByLessonPaginated(Long lessonId, Pageable pageable) {
        return questionService.getQuestionsByLessonPaginated(lessonId, pageable);
    }

    public QuestionDTO createQuestion(QuestionDTO dto) {
        return questionService.createQuestion(dto);
    }

    public QuestionDTO updateQuestion(Long id, QuestionDTO dto) {
        return questionService.updateQuestion(id, dto);
    }

    public void deleteQuestion(Long id) {
        questionService.deleteQuestion(id);
    }

    public Integer getNextQuestionOrderIndex(Long lessonId) {
        return questionService.getNextOrderIndex(lessonId);
    }

    public int bulkDeleteQuestions(List<Long> ids) {
        return questionService.bulkDeleteQuestions(ids);
    }

    // ===== BULK OPERATIONS =====

    public List<QuestionDTO> createQuestionsInBulk(Long lessonId, List<QuestionDTO> questions) {
        // Delegate to question service (will implement in next file)
        return questionService.createQuestionsInBulk(lessonId, questions);
    }

    public void copyQuestionsToLesson(Long sourceLessonId, Long targetLessonId) {
        // Delegate to question service
        questionService.copyQuestionsToLesson(sourceLessonId, targetLessonId);
    }

    // ===== VALIDATION OPERATIONS =====

    public Map<String, Object> validateAllTopicsOrderIndex() {
        return validationService.validateAllTopicsOrderIndex();
    }

    public Map<String, Object> validateLessonsOrderIndex(Long topicId) {
        return validationService.validateLessonsOrderIndex(topicId);
    }

    public Map<String, Object> validateAllLessonsOrderIndex() {
        return validationService.validateAllLessonsOrderIndex();
    }

    public Map<String, Object> validateQuestionsOrderIndex(Long lessonId) {
        return validationService.validateQuestionsOrderIndex(lessonId);
    }

    public Map<String, Object> validateAllQuestionsInTopic(Long topicId) {
        return validationService.validateAllQuestionsInTopic(topicId);
    }

    public Map<String, Object> validateAllQuestionsOrderIndex() {
        return validationService.validateAllQuestionsOrderIndex();
    }

    // ===== PDF IMPORT OPERATIONS =====

    public List<GrammarLessonDTO> importLessonsFromFile(Long topicId, List<GrammarLessonDTO> lessonDTOs) {
        return importService.importLessonsFromFile(topicId, lessonDTOs);
    }
}