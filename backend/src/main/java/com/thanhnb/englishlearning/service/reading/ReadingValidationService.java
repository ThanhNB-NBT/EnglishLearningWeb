package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service chuyên validate và fix orderIndex cho Reading module
 * Dùng khi: Import data, phát hiện lỗi, health check
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadingValidationService {

    private final ReadingLessonRepository lessonRepository;
    private final QuestionRepository questionRepository;

    // ===== LESSON VALIDATION =====

    /**
     * Validate và fix orderIndex của tất cả Reading Lessons
     */
    public Map<String, Object> validateAllLessonsOrderIndex() {
        List<ReadingLesson> lessons = lessonRepository.findAll();
        lessons.sort(Comparator.comparing(ReadingLesson::getOrderIndex));
        
        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        int fixedCount = 0;

        Set<Integer> seenIndexes = new HashSet<>();
        
        for (int i = 0; i < lessons.size(); i++) {
            ReadingLesson lesson = lessons.get(i);
            int expectedIndex = i + 1;
            int actualIndex = lesson.getOrderIndex();

            // Check gap
            if (actualIndex != expectedIndex) {
                issues.add(String.format("Lesson '%s' has orderIndex=%d, expected=%d", 
                        lesson.getTitle(), actualIndex, expectedIndex));
                lesson.setOrderIndex(expectedIndex);
                fixedCount++;
            }

            // Check duplicate
            if (seenIndexes.contains(actualIndex)) {
                issues.add(String.format("Duplicate orderIndex=%d found at lesson '%s'", 
                        actualIndex, lesson.getTitle()));
                lesson.setOrderIndex(expectedIndex);
                fixedCount++;
            }

            seenIndexes.add(expectedIndex);
        }

        if (fixedCount > 0) {
            lessonRepository.saveAll(lessons);
            log.info("Fixed {} orderIndex issues across all reading lessons", fixedCount);
        }

        result.put("totalLessons", lessons.size());
        result.put("issuesFound", issues.size());
        result.put("issuesFixed", fixedCount);
        result.put("issues", issues);

        return result;
    }

    // ===== QUESTION VALIDATION =====

    /**
     * Validate và fix orderIndex của Questions trong 1 Reading Lesson
     */
    public Map<String, Object> validateQuestionsOrderIndex(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new RuntimeException("Reading lesson không tồn tại");
        }

        List<Question> questions = questionRepository
                .findByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType.READING, lessonId);

        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        int fixedCount = 0;

        Set<Integer> seenIndexes = new HashSet<>();
        
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            int expectedIndex = i + 1;
            int actualIndex = question.getOrderIndex();

            // Check gap
            if (actualIndex != expectedIndex) {
                issues.add(String.format("Question '%s' has orderIndex=%d, expected=%d", 
                        truncateText(question.getQuestionText(), 50), actualIndex, expectedIndex));
                question.setOrderIndex(expectedIndex);
                fixedCount++;
            }

            // Check duplicate
            if (seenIndexes.contains(actualIndex)) {
                issues.add(String.format("Duplicate orderIndex=%d found at question '%s'", 
                        actualIndex, truncateText(question.getQuestionText(), 50)));
                question.setOrderIndex(expectedIndex);
                fixedCount++;
            }

            seenIndexes.add(expectedIndex);
        }

        if (fixedCount > 0) {
            questionRepository.saveAll(questions);
            log.info("Fixed {} orderIndex issues in reading lesson {}", fixedCount, lessonId);
        }

        result.put("lessonId", lessonId);
        result.put("totalQuestions", questions.size());
        result.put("issuesFound", issues.size());
        result.put("issuesFixed", fixedCount);
        result.put("issues", issues);

        return result;
    }

    /**
     * Validate và fix orderIndex của TẤT CẢ Reading Questions
     */
    public Map<String, Object> validateAllQuestionsOrderIndex() {
        List<ReadingLesson> allLessons = lessonRepository.findAll();
        
        Map<String, Object> globalResult = new HashMap<>();
        List<Map<String, Object>> lessonResults = new ArrayList<>();
        int totalIssuesFixed = 0;
        int totalIssuesFound = 0;
        int totalQuestions = 0;

        for (ReadingLesson lesson : allLessons) {
            Map<String, Object> lessonResult = validateQuestionsOrderIndex(lesson.getId());
            
            totalIssuesFixed += (int) lessonResult.get("issuesFixed");
            totalIssuesFound += (int) lessonResult.get("issuesFound");
            totalQuestions += (int) lessonResult.get("totalQuestions");
            
            if ((int) lessonResult.get("issuesFound") > 0) {
                lessonResults.add(Map.of(
                    "lessonId", lesson.getId(),
                    "lessonTitle", lesson.getTitle(),
                    "issuesFixed", lessonResult.get("issuesFixed"),
                    "issues", lessonResult.get("issues")
                ));
            }
        }

        globalResult.put("totalLessonsChecked", allLessons.size());
        globalResult.put("totalQuestions", totalQuestions);
        globalResult.put("totalIssuesFound", totalIssuesFound);
        globalResult.put("totalIssuesFixed", totalIssuesFixed);
        globalResult.put("lessonsWithIssues", lessonResults);

        log.info("Validated ALL reading questions across {} lessons, fixed {} issues", 
                allLessons.size(), totalIssuesFixed);

        return globalResult;
    }

    /**
     * Health check toàn bộ Reading module
     */
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new HashMap<>();

        // Check lessons
        Map<String, Object> lessonsCheck = validateAllLessonsOrderIndex();
        result.put("lessons", lessonsCheck);

        // Check questions
        Map<String, Object> questionsCheck = validateAllQuestionsOrderIndex();
        result.put("questions", questionsCheck);

        // Summary
        int totalIssuesFound = (int) lessonsCheck.get("issuesFound") + (int) questionsCheck.get("totalIssuesFound");
        int totalIssuesFixed = (int) lessonsCheck.get("issuesFixed") + (int) questionsCheck.get("totalIssuesFixed");

        result.put("summary", Map.of(
            "totalIssuesFound", totalIssuesFound,
            "totalIssuesFixed", totalIssuesFixed,
            "status", totalIssuesFound == 0 ? "HEALTHY" : "FIXED"
        ));

        log.info("Reading module health check completed: {} issues found and fixed", totalIssuesFixed);

        return result;
    }

    // ===== HELPER =====

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}