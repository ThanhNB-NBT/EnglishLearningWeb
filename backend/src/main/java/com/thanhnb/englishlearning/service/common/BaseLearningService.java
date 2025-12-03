package com.thanhnb.englishlearning.service.common;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.service.question.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Abstract base service cho các module học tập (Grammar, Reading, Listening, etc.)
 * Chứa logic chung: Question processing, Answer checking, Unlock logic
 */
@Slf4j
public abstract class BaseLearningService<TLesson, TProgress> {

    @Autowired
    protected QuestionService questionService;

    /**
     * TEMPLATE METHOD: Subclass override để chỉ định ParentType
     */
    protected abstract ParentType getParentType();

    // ═══════════════════════════════════════════════════════════════
    // QUESTION PROCESSING (Delegate to QuestionService)
    // ═══════════════════════════════════════════════════════════════

    /**
     * SHARED: Process answers và trả về kết quả chi tiết
     * Dùng chung cho Grammar, Reading, Listening, etc.
     * 
     * @param answers Danh sách câu trả lời của user
     * @return Danh sách kết quả chi tiết từng câu
     */
    protected List<QuestionResultDTO> processAnswers(List<SubmitAnswerRequest> answers) {
        return questionService.processAnswers(answers, getParentType());
    }

    /**
     * SHARED: Convert Question entity -> QuestionResponseDTO
     * Tự động shuffle options cho MULTIPLE_CHOICE
     * 
     * @param question Question entity
     * @param shuffleOptions Có shuffle options không
     */
    protected QuestionResponseDTO convertQuestionToDTO(Question question, boolean shuffleOptions) {
        return questionService.convertToDTO(question, shuffleOptions);
    }

    /**
     * SHARED: Convert Question entity -> QuestionResponseDTO (with shuffle)
     */
    protected QuestionResponseDTO convertQuestionToDTO(Question question) {
        return questionService.convertToDTO(question, true);
    }

    /**
     * SHARED: Convert list of questions to DTOs
     */
    protected List<QuestionResponseDTO> convertQuestionsToDTOs(List<Question> questions, boolean shuffleOptions) {
        return questionService.convertToDTOs(questions, shuffleOptions);
    }

    /**
     * SHARED: Load questions by parent
     */
    protected List<Question> loadQuestionsByParent(Long parentId) {
        return questionService.loadQuestionsByParent(getParentType(), parentId);
    }

    /**
     * SHARED: Count questions by parent
     */
    protected long countQuestionsByParent(Long parentId) {
        return questionService.countQuestionsByParent(getParentType(), parentId);
    }

    /**
     * SHARED: Validate answer count
     */
    protected void validateAnswerCount(List<SubmitAnswerRequest> answers, Long parentId) {
        questionService.validateAnswerCount(answers, getParentType(), parentId);
    }

    // ═══════════════════════════════════════════════════════════════
    // SCORING UTILITIES
    // ═══════════════════════════════════════════════════════════════

    /**
     * SHARED: Calculate total score from results
     */
    protected int calculateTotalScore(List<QuestionResultDTO> results) {
        return questionService.calculateTotalScore(results);
    }

    /**
     * SHARED: Calculate correct count from results
     */
    protected int calculateCorrectCount(List<QuestionResultDTO> results) {
        return questionService.calculateCorrectCount(results);
    }

    /**
     * SHARED: Calculate score percentage
     */
    protected double calculateScorePercentage(int correctCount, int totalQuestions) {
        return questionService.calculateScorePercentage(correctCount, totalQuestions);
    }

    /**
     * SHARED: Calculate score percentage from results
     */
    protected double calculateScorePercentage(List<QuestionResultDTO> results) {
        return questionService.calculateScorePercentage(results);
    }

    /**
     * SHARED: Check if passed (default 80%)
     */
    protected boolean isPassed(double scorePercentage) {
        return isPassed(scorePercentage, 80.0);
    }

    /**
     * SHARED: Check if passed with custom threshold
     */
    protected boolean isPassed(double scorePercentage, double threshold) {
        return scorePercentage >= threshold;
    }

    // ═══════════════════════════════════════════════════════════════
    // LESSON UNLOCK LOGIC (Sequential)
    // ═══════════════════════════════════════════════════════════════

    /**
     * SHARED: Check lesson unlock status (sequential unlock)
     * 
     * Rules:
     * 1. First lesson (orderIndex = 1) always unlocked
     * 2. Next lesson unlocked only when previous lesson completed
     * 
     * @param lesson Current lesson to check
     * @param allLessons All lessons sorted by orderIndex
     * @param userId User ID
     * @param orderIndexGetter Lambda to get orderIndex from lesson
     * @param lessonIdGetter Lambda to get lessonId from lesson
     * @param progressChecker Lambda to check if progress completed
     */
    protected <L> boolean isLessonUnlocked(
            L lesson,
            List<L> allLessons,
            Long userId,
            LessonOrderIndexGetter<L> orderIndexGetter,
            LessonIdGetter<L> lessonIdGetter,
            ProgressCompletedChecker progressChecker) {

        Integer orderIndex = orderIndexGetter.getOrderIndex(lesson);

        // Rule 1: First lesson always unlocked
        if (orderIndex == 1) {
            log.debug("Lesson orderIndex={} is first -> UNLOCKED", orderIndex);
            return true;
        }

        // Rule 2: Find previous lesson
        L previousLesson = allLessons.stream()
                .filter(l -> orderIndexGetter.getOrderIndex(l).equals(orderIndex - 1))
                .findFirst()
                .orElse(null);

        if (previousLesson == null) {
            log.warn("No previous lesson found for orderIndex={} -> UNLOCKED (fallback)", orderIndex);
            return true;
        }

        // Check if previous lesson completed
        Long previousLessonId = lessonIdGetter.getLessonId(previousLesson);
        boolean isPreviousCompleted = progressChecker.isCompleted(userId, previousLessonId);

        if (isPreviousCompleted) {
            log.debug("Previous lesson id={} completed -> UNLOCKED", previousLessonId);
        } else {
            log.debug("Previous lesson id={} NOT completed -> LOCKED", previousLessonId);
        }

        return isPreviousCompleted;
    }

    /**
     * SHARED: Find next lesson in sequence
     */
    protected <L> L findNextLesson(L currentLesson, List<L> allLessons, 
            LessonOrderIndexGetter<L> orderIndexGetter) {
        Integer currentOrderIndex = orderIndexGetter.getOrderIndex(currentLesson);
        
        return allLessons.stream()
                .filter(l -> orderIndexGetter.getOrderIndex(l).equals(currentOrderIndex + 1))
                .findFirst()
                .orElse(null);
    }

    /**
     * SHARED: Find previous lesson in sequence
     */
    protected <L> L findPreviousLesson(L currentLesson, List<L> allLessons,
            LessonOrderIndexGetter<L> orderIndexGetter) {
        Integer currentOrderIndex = orderIndexGetter.getOrderIndex(currentLesson);
        
        return allLessons.stream()
                .filter(l -> orderIndexGetter.getOrderIndex(l).equals(currentOrderIndex - 1))
                .findFirst()
                .orElse(null);
    }

    // ═══════════════════════════════════════════════════════════════
    // FUNCTIONAL INTERFACES (để tránh reflection)
    // ═══════════════════════════════════════════════════════════════

    @FunctionalInterface
    protected interface LessonOrderIndexGetter<L> {
        Integer getOrderIndex(L lesson);
    }

    @FunctionalInterface
    protected interface LessonIdGetter<L> {
        Long getLessonId(L lesson);
    }

    @FunctionalInterface
    protected interface ProgressCompletedChecker {
        boolean isCompleted(Long userId, Long lessonId);
    }
}