package com.thanhnb.englishlearning.service.common;

import com.thanhnb.englishlearning.dto.question.QuestionDTO;
import com.thanhnb.englishlearning.dto.question.QuestionOptionDTO;
import com.thanhnb.englishlearning.dto.question.QuestionResultDTO;
import com.thanhnb.englishlearning.dto.question.SubmitAnswerRequest;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.entity.question.QuestionOption;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.question.QuestionOptionRepository;
import com.thanhnb.englishlearning.service.question.AnswerCheckingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ✅ Abstract base service cho các module học tập (Grammar, Reading, Listening, etc.)
 * Chứa logic chung: Question processing, Answer checking, Unlock logic
 */
@Slf4j
public abstract class BaseLearningService<TLesson, TProgress> {

    @Autowired
    protected QuestionRepository questionRepository;

    @Autowired
    protected QuestionOptionRepository questionOptionRepository;

    @Autowired
    protected AnswerCheckingService answerCheckingService;

    /**
     * ✅ TEMPLATE METHOD: Subclass override để chỉ định ParentType
     */
    protected abstract ParentType getParentType();

    /**
     * ✅ SHARED: Process answers và trả về kết quả chi tiết
     * Dùng chung cho Grammar, Reading, Listening, etc.
     */
    protected List<QuestionResultDTO> processAnswers(List<SubmitAnswerRequest> answers) {
        ParentType parentType = getParentType();
        
        return answers.stream().map(answerRequest -> {
            Question question = questionRepository.findByIdWithOptions(answerRequest.getQuestionId())
                    .orElseThrow(() -> new RuntimeException(
                            "Question không tồn tại với id: " + answerRequest.getQuestionId()));

            // Validate parentType
            if (question.getParentType() != parentType) {
                throw new RuntimeException(
                        String.format("Question này không thuộc %s module", parentType));
            }

            boolean isCorrect = answerCheckingService.checkAnswer(question, answerRequest);
            int points = isCorrect ? question.getPoints() : 0;
            String hint = isCorrect ? null : answerCheckingService.generateHint(question, answerRequest);

            return new QuestionResultDTO(
                    question.getId(),
                    question.getQuestionText(),
                    answerRequest.getAnswer(),
                    question.getCorrectAnswer(),
                    
                    isCorrect,
                    question.getExplanation(),
                    points,
                    hint
            );
        }).collect(Collectors.toList());
    }

    /**
     * ✅ SHARED: Convert Question entity -> QuestionDTO
     * Tự động shuffle options cho MULTIPLE_CHOICE
     */
    protected QuestionDTO convertQuestionToDTO(Question question) {
        List<QuestionOption> options = questionOptionRepository
                .findByQuestionIdOrderByOrderIndexAsc(question.getId());

        List<QuestionOptionDTO> optionDTOs = options.stream()
                .map(option -> new QuestionOptionDTO(
                        option.getId(),
                        question.getId(),
                        option.getOptionText(),
                        option.getIsCorrect(),
                        option.getOrderIndex()))
                .collect(Collectors.toList());

        // Shuffle options cho MULTIPLE_CHOICE để tránh gian lận
        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE && !optionDTOs.isEmpty()) {
            Collections.shuffle(optionDTOs);
            for (int i = 0; i < optionDTOs.size(); i++) {
                optionDTOs.get(i).setOrderIndex(i + 1);
            }
        }

        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setLessonId(question.getParentId());
        dto.setQuestionText(question.getQuestionText());
        dto.setQuestionType(question.getQuestionType());
        dto.setCorrectAnswer(question.getCorrectAnswer());
        dto.setExplanation(question.getExplanation());
        dto.setPoints(question.getPoints());
        dto.setOrderIndex(question.getOrderIndex());
        dto.setCreatedAt(question.getCreatedAt());
        dto.setOptions(optionDTOs.isEmpty() ? null : optionDTOs);
        dto.setShowCorrectAnswer(true);

        return dto;
    }

    /**
     * ✅ SHARED: Check lesson unlock status (sequential unlock)
     * 
     * Rules:
     * 1. First lesson (orderIndex = 1) always unlocked
     * 2. Next lesson unlocked only when previous lesson completed
     * 
     * @param lesson Current lesson to check
     * @param allLessons All lessons sorted by orderIndex
     * @param userId User ID
     * @param isProgressCompleted Lambda to check if progress completed
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
            log.debug("✅ Lesson orderIndex={} is first -> UNLOCKED", orderIndex);
            return true;
        }

        // Rule 2: Find previous lesson
        L previousLesson = allLessons.stream()
                .filter(l -> orderIndexGetter.getOrderIndex(l).equals(orderIndex - 1))
                .findFirst()
                .orElse(null);

        if (previousLesson == null) {
            log.warn("⚠️ No previous lesson found for orderIndex={} -> UNLOCKED (fallback)", orderIndex);
            return true;
        }

        // Check if previous lesson completed
        Long previousLessonId = lessonIdGetter.getLessonId(previousLesson);
        boolean isPreviousCompleted = progressChecker.isCompleted(userId, previousLessonId);

        if (isPreviousCompleted) {
            log.debug("✅ Previous lesson id={} completed -> UNLOCKED", previousLessonId);
        } else {
            log.debug("🔒 Previous lesson id={} NOT completed -> LOCKED", previousLessonId);
        }

        return isPreviousCompleted;
    }

    // ===== FUNCTIONAL INTERFACES để tránh reflection =====

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

    /**
     * ✅ SHARED: Calculate score percentage
     */
    protected double calculateScorePercentage(int correctCount, int totalQuestions) {
        return totalQuestions > 0 ? (double) correctCount / totalQuestions * 100 : 0;
    }

    /**
     * ✅ SHARED: Check if passed (default 80%)
     */
    protected boolean isPassed(double scorePercentage) {
        return isPassed(scorePercentage, 80.0);
    }

    /**
     * ✅ SHARED: Check if passed with custom threshold
     */
    protected boolean isPassed(double scorePercentage, double threshold) {
        return scorePercentage >= threshold;
    }

    /**
     * ✅ SHARED: Find next lesson in sequence
     */
    protected <L> L findNextLesson(L currentLesson, List<L> allLessons, 
            LessonOrderIndexGetter<L> orderIndexGetter) {
        Integer currentOrderIndex = orderIndexGetter.getOrderIndex(currentLesson);
        
        return allLessons.stream()
                .filter(l -> orderIndexGetter.getOrderIndex(l).equals(currentOrderIndex + 1))
                .findFirst()
                .orElse(null);
    }
}