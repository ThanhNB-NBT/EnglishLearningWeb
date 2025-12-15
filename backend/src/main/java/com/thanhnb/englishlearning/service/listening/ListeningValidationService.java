package com.thanhnb.englishlearning.service.listening;

import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service validation cho Listening module
 * Health checks, validate orderIndex, validate audio files
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListeningValidationService {

    private final ListeningLessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final ListeningOrderService orderService;
    private final AudioStorageService audioStorageService;

    // ═════════════════════════════════════════════════════════════════
    // LESSON ORDER VALIDATION
    // ═════════════════════════════════════════════════════════════════

    /**
     * Validate và fix orderIndex của tất cả Listening Lessons
     */
    public Map<String, Object> validateAllLessonsOrderIndex() {
        log.info("[VALIDATION] Validating all listening lessons orderIndex");

        Map<String, Object> result = new HashMap<>();

        try {
            int issuesFixed = orderService.validateAndFixAllOrderIssues();

            result.put("status", issuesFixed == 0 ? "OK" : "FIXED");
            result.put("issuesFixed", issuesFixed);
            result.put("message", issuesFixed == 0 
                    ? "OrderIndex đã đúng" 
                    : "Đã fix " + issuesFixed + " vấn đề");

            log.info("Lesson order validation completed:  {} issues fixed", issuesFixed);

        } catch (Exception e) {
            log.error("Error validating lesson order", e);
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }

        return result;
    }

    // ═════════════════════════════════════════════════════════════════
    // QUESTION ORDER VALIDATION
    // ═════════════════════════════════════════════════════════════════

    /**
     * Validate orderIndex của Questions trong 1 lesson
     */
    public Map<String, Object> validateQuestionsOrderIndex(Long lessonId) {
        log.info("[VALIDATION] Validating questions orderIndex for lesson {}", lessonId);

        Map<String, Object> result = new HashMap<>();

        try {
            if (!lessonRepository.existsById(lessonId)) {
                throw new RuntimeException("Bài nghe không tồn tại với id: " + lessonId);
            }

            List<Question> questions = questionRepository
                    .findByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType.LISTENING, lessonId);

            int issuesFixed = validateAndFixQuestionOrder(questions);

            result.put("status", issuesFixed == 0 ? "OK" : "FIXED");
            result.put("lessonId", lessonId);
            result.put("totalQuestions", questions.size());
            result.put("issuesFixed", issuesFixed);
            result.put("message", issuesFixed == 0 
                    ?  "OrderIndex đã đúng" 
                    : "Đã fix " + issuesFixed + " vấn đề");

            log.info("Question order validation for lesson {} completed: {} issues fixed", 
                    lessonId, issuesFixed);

        } catch (Exception e) {
            log.error("Error validating question order for lesson " + lessonId, e);
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Validate orderIndex của TẤT CẢ Questions trong Listening module
     */
    public Map<String, Object> validateAllQuestionsOrderIndex() {
        log.info("[VALIDATION] Validating all listening questions orderIndex");

        Map<String, Object> result = new HashMap<>();

        try {
            List<ListeningLesson> allLessons = lessonRepository.findAll();
            int totalIssuesFixed = 0;
            int lessonsChecked = 0;

            for (ListeningLesson lesson :  allLessons) {
                List<Question> questions = questionRepository
                        .findByParentTypeAndParentIdOrderByOrderIndexAsc(
                                ParentType.LISTENING, lesson.getId());

                if (! questions.isEmpty()) {
                    int fixed = validateAndFixQuestionOrder(questions);
                    totalIssuesFixed += fixed;
                    lessonsChecked++;
                }
            }

            result.put("status", totalIssuesFixed == 0 ? "OK" : "FIXED");
            result.put("lessonsChecked", lessonsChecked);
            result.put("totalIssuesFixed", totalIssuesFixed);
            result.put("message", totalIssuesFixed == 0 
                    ? "Tất cả orderIndex đã đúng" 
                    : "Đã fix " + totalIssuesFixed + " vấn đề");

            log.info("All questions order validation completed: {} issues fixed across {} lessons", 
                    totalIssuesFixed, lessonsChecked);

        } catch (Exception e) {
            log.error("Error validating all questions order", e);
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }

        return result;
    }

    // ═════════════════════════════════════════════════════════════════
    // AUDIO FILE VALIDATION
    // ═════════════════════════════════════════════════════════════════

    /**
     * Validate audio files tồn tại trên disk
     */
    public Map<String, Object> validateAudioFiles() {
        log.info("[VALIDATION] Validating audio files");

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> missingFiles = new ArrayList<>();

        try {
            List<ListeningLesson> allLessons = lessonRepository.findAll();
            int totalLessons = allLessons.size();
            int missingCount = 0;

            for (ListeningLesson lesson : allLessons) {
                String audioUrl = lesson.getAudioUrl();

                if (audioUrl == null || audioUrl.trim().isEmpty()) {
                    Map<String, Object> missing = new HashMap<>();
                    missing.put("lessonId", lesson.getId());
                    missing.put("title", lesson.getTitle());
                    missing.put("reason", "NO_AUDIO_URL");
                    missingFiles.add(missing);
                    missingCount++;
                } else if (!audioStorageService.checkFileExists(audioUrl)) {
                    Map<String, Object> missing = new HashMap<>();
                    missing.put("lessonId", lesson.getId());
                    missing.put("title", lesson.getTitle());
                    missing.put("audioUrl", audioUrl);
                    missing.put("reason", "FILE_NOT_FOUND");
                    missingFiles.add(missing);
                    missingCount++;
                }
            }

            result.put("status", missingCount == 0 ? "ALL_HEALTHY" : "MISSING_FILES");
            result.put("totalLessons", totalLessons);
            result.put("missingCount", missingCount);
            result.put("missingFiles", missingFiles);
            result.put("message", missingCount == 0 
                    ? "Tất cả audio files đều tồn tại" 
                    : "Phát hiện " + missingCount + " audio files bị thiếu");

            log.info("Audio validation completed: {}/{} files OK", 
                    totalLessons - missingCount, totalLessons);

        } catch (Exception e) {
            log.error("Error validating audio files", e);
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }

        return result;
    }

    // ═════════════════════════════════════════════════════════════════
    // HEALTH CHECK
    // ═════════════════════════════════════════════════════════════════

    /**
     * Health check toàn bộ Listening module
     */
    public Map<String, Object> healthCheck() {
        log.info("[HEALTH CHECK] Running comprehensive health check for Listening module");

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Check lesson order
            Map<String, Object> lessonOrderCheck = validateAllLessonsOrderIndex();

            // 2. Check question order
            Map<String, Object> questionOrderCheck = validateAllQuestionsOrderIndex();

            // 3. Check audio files
            Map<String, Object> audioCheck = validateAudioFiles();

            // 4. Module statistics (basic counts only)
            long totalLessons = lessonRepository.count();
            long activeLessons = lessonRepository.countByIsActiveTrue();
            long totalQuestions = questionRepository.countByParentType(ParentType.LISTENING);

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalLessons", totalLessons);
            statistics.put("activeLessons", activeLessons);
            statistics.put("inactiveLessons", totalLessons - activeLessons);
            statistics.put("totalQuestions", totalQuestions);

            // Compile results
            result.put("lessonOrder", lessonOrderCheck);
            result.put("questionOrder", questionOrderCheck);
            result.put("audioFiles", audioCheck);
            result.put("statistics", statistics);

            // Overall status
            boolean isHealthy = "OK".equals(lessonOrderCheck.get("status")) &&
                    "OK".equals(questionOrderCheck.get("status")) &&
                    "ALL_HEALTHY".equals(audioCheck.get("status"));

            Map<String, Object> summary = new HashMap<>();
            summary. put("status", isHealthy ?  "HEALTHY" : "HAS_ISSUES");
            summary.put("totalIssuesFixed", 
                    (Integer) lessonOrderCheck.getOrDefault("issuesFixed", 0) +
                    (Integer) questionOrderCheck.getOrDefault("totalIssuesFixed", 0));
            summary.put("missingAudioFiles", audioCheck.getOrDefault("missingCount", 0));

            result.put("summary", summary);

            log.info("Health check completed: {}", summary.get("status"));

        } catch (Exception e) {
            log.error("Error during health check", e);
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }

        return result;
    }

    // ═════════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Validate và fix orderIndex của questions
     */
    private int validateAndFixQuestionOrder(List<Question> questions) {
        if (questions.isEmpty()) {
            return 0;
        }

        int issuesFixed = 0;

        for (int i = 0; i < questions.size(); i++) {
            int expectedOrder = i + 1;
            Question question = questions.get(i);

            if (! question.getOrderIndex().equals(expectedOrder)) {
                question.setOrderIndex(expectedOrder);
                issuesFixed++;
            }
        }

        if (issuesFixed > 0) {
            questionRepository.saveAll(questions);
        }

        return issuesFixed;
    }
}