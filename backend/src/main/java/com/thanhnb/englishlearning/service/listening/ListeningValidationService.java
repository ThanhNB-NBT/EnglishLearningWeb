package com.thanhnb.englishlearning.service.listening;

import com.thanhnb. englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.entity. question.Question;
import com.thanhnb.englishlearning. enums.ParentType;
import com.thanhnb.englishlearning. repository.listening.ListeningLessonRepository;
import com. thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok. extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation. Transactional;

import java. nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Service chuyên validate và fix orderIndex + audio files cho Listening module
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListeningValidationService {

    private final ListeningLessonRepository lessonRepository;
    private final QuestionRepository questionRepository;

    // ===== LESSON VALIDATION =====

    /**
     * Validate và fix orderIndex của tất cả Listening Lessons
     */
    public Map<String, Object> validateAllLessonsOrderIndex() {
        List<ListeningLesson> lessons = lessonRepository.findAll();
        lessons.sort(Comparator.comparing(ListeningLesson::getOrderIndex));
        
        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        int fixedCount = 0;

        Set<Integer> seenIndexes = new HashSet<>();
        
        for (int i = 0; i < lessons.size(); i++) {
            ListeningLesson lesson = lessons.get(i);
            int expectedIndex = i + 1;
            int actualIndex = lesson.getOrderIndex();

            // Check gap
            if (actualIndex != expectedIndex) {
                issues. add(String.format("Lesson '%s' has orderIndex=%d, expected=%d", 
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
            log.info("Fixed {} orderIndex issues across all listening lessons", fixedCount);
        }

        result.put("totalLessons", lessons.size());
        result.put("issuesFound", issues.size());
        result.put("issuesFixed", fixedCount);
        result.put("issues", issues);

        return result;
    }

    // ===== QUESTION VALIDATION =====

    /**
     * Validate và fix orderIndex của Questions trong 1 Listening Lesson
     */
    public Map<String, Object> validateQuestionsOrderIndex(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new RuntimeException("Listening lesson không tồn tại");
        }

        List<Question> questions = questionRepository
                .findByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType.LISTENING, lessonId);

        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        int fixedCount = 0;

        Set<Integer> seenIndexes = new HashSet<>();
        
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions. get(i);
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
            log.info("Fixed {} orderIndex issues in listening lesson {}", fixedCount, lessonId);
        }

        result.put("lessonId", lessonId);
        result.put("totalQuestions", questions.size());
        result.put("issuesFound", issues.size());
        result.put("issuesFixed", fixedCount);
        result.put("issues", issues);

        return result;
    }

    /**
     * Validate và fix orderIndex của TẤT CẢ Listening Questions
     */
    public Map<String, Object> validateAllQuestionsOrderIndex() {
        List<ListeningLesson> allLessons = lessonRepository. findAll();
        
        Map<String, Object> globalResult = new HashMap<>();
        List<Map<String, Object>> lessonResults = new ArrayList<>();
        int totalIssuesFixed = 0;
        int totalIssuesFound = 0;
        int totalQuestions = 0;

        for (ListeningLesson lesson :  allLessons) {
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

        log.info("Validated ALL listening questions across {} lessons, fixed {} issues", 
                allLessons. size(), totalIssuesFixed);

        return globalResult;
    }

    // ===== AUDIO FILE VALIDATION =====

    /**
     * Validate tất cả audio files tồn tại
     */
    public Map<String, Object> validateAudioFiles() {
        List<ListeningLesson> allLessons = lessonRepository.findAll();
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> missingFiles = new ArrayList<>();
        int totalLessons = allLessons.size();
        int missingCount = 0;

        for (ListeningLesson lesson : allLessons) {
            String audioUrl = lesson.getAudioUrl();
            
            if (audioUrl == null || audioUrl.isEmpty()) {
                missingFiles.add(Map.of(
                    "lessonId", lesson.getId(),
                    "lessonTitle", lesson.getTitle(),
                    "issue", "Audio URL is null or empty"
                ));
                missingCount++;
                continue;
            }

            // Convert URL to file system path
            // /media/listening/lesson_1/audio.mp3 → /app/media/listening/lesson_1/audio.mp3
            String relativePath = audioUrl.replaceFirst("^/media/", "");
            Path filePath = Paths.get("/app/media").resolve(relativePath);

            if (!Files.exists(filePath)) {
                missingFiles.add(Map.of(
                    "lessonId", lesson.getId(),
                    "lessonTitle", lesson.getTitle(),
                    "audioUrl", audioUrl,
                    "filePath", filePath.toString(),
                    "issue", "File does not exist on disk"
                ));
                missingCount++;
            }
        }

        result. put("totalLessons", totalLessons);
        result.put("missingCount", missingCount);
        result.put("healthyCount", totalLessons - missingCount);
        result.put("missingFiles", missingFiles);
        result.put("status", missingCount == 0 ? "ALL_HEALTHY" : "ISSUES_FOUND");

        log.info("Audio validation:  {}/{} lessons have valid audio files", 
                totalLessons - missingCount, totalLessons);

        return result;
    }

    // ===== HEALTH CHECK =====

    /**
     * Health check toàn bộ Listening module
     */
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new HashMap<>();

        // Check lessons
        Map<String, Object> lessonsCheck = validateAllLessonsOrderIndex();
        result.put("lessons", lessonsCheck);

        // Check questions
        Map<String, Object> questionsCheck = validateAllQuestionsOrderIndex();
        result.put("questions", questionsCheck);

        // Check audio files
        Map<String, Object> audioCheck = validateAudioFiles();
        result.put("audioFiles", audioCheck);

        // Summary
        int totalIssuesFound = (int) lessonsCheck.get("issuesFound") 
                + (int) questionsCheck.get("totalIssuesFound")
                + (int) audioCheck.get("missingCount");
        
        int totalIssuesFixed = (int) lessonsCheck.get("issuesFixed") 
                + (int) questionsCheck.get("totalIssuesFixed");

        result.put("summary", Map.of(
            "totalIssuesFound", totalIssuesFound,
            "totalIssuesFixed", totalIssuesFixed,
            "audioIssues", audioCheck.get("missingCount"),
            "status", totalIssuesFound == 0 ? "HEALTHY" : "ISSUES_FOUND"
        ));

        log.info("Listening module health check completed:  {} issues found, {} fixed", 
                totalIssuesFound, totalIssuesFixed);

        return result;
    }

    // ===== HELPER =====

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}