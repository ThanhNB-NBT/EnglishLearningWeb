package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Main admin service orchestrator - Delegate to specialized services
 * Không chứa logic, chỉ điều phối và gọi các service con
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

    // ═══════════════════════════════════════════════════════════════════════
    // TOPIC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    public Page<GrammarTopicDTO> getAllTopicsPaginated(Pageable pageable) {
        return topicService.getAllTopicsPaginated(pageable);
    }

    public GrammarTopicDTO getTopicById(Long id) {
        return topicService.getTopicById(id);
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

    public void deactivateTopic(Long id) {
        topicService.deactivateTopic(id);
    }

    public Integer getNextTopicOrderIndex() {
        return topicService.getNextOrderIndex();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LESSON OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

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

    public void deactivateLesson(Long id) {
        lessonService.deactivateLesson(id);
    }

    public Integer getNextLessonOrderIndex(Long topicId) {
        return lessonService.getNextOrderIndex(topicId);
    }

    public int reorderLessons(Long topicId, Integer insertPosition, Long excludeLessonId) {
        return lessonService.reorderLessons(topicId, insertPosition, excludeLessonId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // QUESTION OPERATIONS (Delegate to BaseQuestionService)
    // ═══════════════════════════════════════════════════════════════════════

    public Page<QuestionResponseDTO> getQuestionsByLessonPaginated(Long lessonId, Pageable pageable) {
        return questionService.getQuestionsByLessonPaginated(lessonId, pageable);
    }

    public QuestionResponseDTO getQuestionById(Long id) {
        return questionService.getQuestionById(id);
    }

    public QuestionResponseDTO createQuestion(CreateQuestionDTO dto) {
        return questionService.createQuestion(dto);
    }

    public QuestionResponseDTO updateQuestion(Long id, QuestionResponseDTO dto) {
        return questionService.updateQuestion(id, dto);
    }

    public void deleteQuestion(Long id) {
        questionService.deleteQuestion(id);
    }

    public Integer getNextQuestionOrderIndex(Long lessonId) {
        return questionService.getNextOrderIndex(lessonId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BULK OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    public int bulkDeleteQuestions(List<Long> ids) {
        return questionService.bulkDeleteQuestions(ids);
    }

    public List<QuestionResponseDTO> createQuestionsInBulk(Long lessonId, List<CreateQuestionDTO> createDTOs) {
        return questionService.createQuestionsInBulk(lessonId, createDTOs);
    }

    public void copyQuestionsToLesson(Long sourceLessonId, Long targetLessonId) {
        questionService.copyQuestionsToLesson(sourceLessonId, targetLessonId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VALIDATION OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════════
    // IMPORT OPERATIONS (AI Parsing)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Import lessons from AI parsing service
     * @param topicId Target topic ID
     * @param lessonDTOs Parsed lessons with metadata
     * @return List of saved lessons with IDs
     */
    public List<GrammarLessonDTO> importLessonsFromFile(Long topicId, List<GrammarLessonDTO> lessonDTOs) {
        return importService.importLessonsFromFile(topicId, lessonDTOs);
    }
}