package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.grammar.GrammarTopic;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.grammar.GrammarTopicRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service chuyên validate và fix orderIndex
 * Dùng khi: Import data, phát hiện lỗi, health check
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarValidationService {

    private final GrammarTopicRepository topicRepository;
    private final GrammarLessonRepository lessonRepository;
    private final QuestionRepository questionRepository;

    // ===== TOPIC VALIDATION =====

    /**
     * Validate và fix orderIndex của tất cả Topics
     */
    public Map<String, Object> validateAllTopicsOrderIndex() {
        List<GrammarTopic> topics = topicRepository.findAllByOrderIndexAsc();
        
        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        int fixedCount = 0;

        Set<Integer> seenIndexes = new HashSet<>();
        
        for (int i = 0; i < topics.size(); i++) {
            GrammarTopic topic = topics.get(i);
            int expectedIndex = i + 1;
            int actualIndex = topic.getOrderIndex();

            // Check gap
            if (actualIndex != expectedIndex) {
                issues.add(String.format("Topic '%s' has orderIndex=%d, expected=%d", 
                        topic.getName(), actualIndex, expectedIndex));
                topic.setOrderIndex(expectedIndex);
                fixedCount++;
            }

            // Check duplicate
            if (seenIndexes.contains(actualIndex)) {
                issues.add(String.format("Duplicate orderIndex=%d found at topic '%s'", 
                        actualIndex, topic.getName()));
                topic.setOrderIndex(expectedIndex);
                fixedCount++;
            }

            seenIndexes.add(expectedIndex);
        }

        if (fixedCount > 0) {
            topicRepository.saveAll(topics);
            log.info("✅ Fixed {} orderIndex issues across all topics", fixedCount);
        }

        result.put("totalTopics", topics.size());
        result.put("issuesFound", issues.size());
        result.put("issuesFixed", fixedCount);
        result.put("issues", issues);

        return result;
    }

    // ===== LESSON VALIDATION =====

    /**
     * Validate và fix orderIndex của Lessons trong 1 Topic
     */
    public Map<String, Object> validateLessonsOrderIndex(Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new RuntimeException("Topic không tồn tại");
        }

        List<GrammarLesson> lessons = lessonRepository
                .findByTopicIdOrderByOrderIndexAsc(topicId);

        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        int fixedCount = 0;

        Set<Integer> seenIndexes = new HashSet<>();
        
        for (int i = 0; i < lessons.size(); i++) {
            GrammarLesson lesson = lessons.get(i);
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
            log.info("✅ Fixed {} orderIndex issues in topic {}", fixedCount, topicId);
        }

        result.put("topicId", topicId);
        result.put("totalLessons", lessons.size());
        result.put("issuesFound", issues.size());
        result.put("issuesFixed", fixedCount);
        result.put("issues", issues);

        return result;
    }

    /**
     * Validate và fix orderIndex của tất cả Lessons
     */
    public Map<String, Object> validateAllLessonsOrderIndex() {
        List<GrammarTopic> allTopics = topicRepository.findAll();
        
        Map<String, Object> globalResult = new HashMap<>();
        List<Map<String, Object>> topicResults = new ArrayList<>();
        int totalIssuesFixed = 0;
        int totalIssuesFound = 0;
        int totalLessons = 0;

        for (GrammarTopic topic : allTopics) {
            Map<String, Object> topicResult = validateLessonsOrderIndex(topic.getId());
            
            totalIssuesFixed += (int) topicResult.get("issuesFixed");
            totalIssuesFound += (int) topicResult.get("issuesFound");
            totalLessons += (int) topicResult.get("totalLessons");
            
            if ((int) topicResult.get("issuesFound") > 0) {
                topicResults.add(Map.of(
                    "topicId", topic.getId(),
                    "topicName", topic.getName(),
                    "issuesFixed", topicResult.get("issuesFixed"),
                    "issues", topicResult.get("issues")
                ));
            }
        }

        globalResult.put("totalTopicsChecked", allTopics.size());
        globalResult.put("totalLessons", totalLessons);
        globalResult.put("totalIssuesFound", totalIssuesFound);
        globalResult.put("totalIssuesFixed", totalIssuesFixed);
        globalResult.put("topicsWithIssues", topicResults);

        log.info("✅ Validated all lessons across {} topics, fixed {} issues", 
                allTopics.size(), totalIssuesFixed);

        return globalResult;
    }

    // ===== QUESTION VALIDATION =====

    /**
     * Validate và fix orderIndex của Questions trong 1 Lesson
     */
    public Map<String, Object> validateQuestionsOrderIndex(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new RuntimeException("Lesson không tồn tại");
        }

        List<Question> questions = questionRepository
                .findByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType.GRAMMAR, lessonId);

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
            log.info("✅ Fixed {} orderIndex issues in lesson {}", fixedCount, lessonId);
        }

        result.put("lessonId", lessonId);
        result.put("totalQuestions", questions.size());
        result.put("issuesFound", issues.size());
        result.put("issuesFixed", fixedCount);
        result.put("issues", issues);

        return result;
    }

    /**
     * Validate và fix orderIndex của tất cả Questions trong 1 Topic
     */
    public Map<String, Object> validateAllQuestionsInTopic(Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new RuntimeException("Topic không tồn tại");
        }

        List<GrammarLesson> lessons = lessonRepository
                .findByTopicIdOrderByOrderIndexAsc(topicId);
        
        Map<String, Object> globalResult = new HashMap<>();
        List<Map<String, Object>> lessonResults = new ArrayList<>();
        int totalIssuesFixed = 0;
        int totalIssuesFound = 0;
        int totalQuestions = 0;

        for (GrammarLesson lesson : lessons) {
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

        globalResult.put("topicId", topicId);
        globalResult.put("totalLessonsChecked", lessons.size());
        globalResult.put("totalQuestions", totalQuestions);
        globalResult.put("totalIssuesFound", totalIssuesFound);
        globalResult.put("totalIssuesFixed", totalIssuesFixed);
        globalResult.put("lessonsWithIssues", lessonResults);

        log.info("✅ Validated all questions in topic {}, fixed {} issues", 
                topicId, totalIssuesFixed);

        return globalResult;
    }

    /**
     * Validate và fix orderIndex của TẤT CẢ Questions
     */
    public Map<String, Object> validateAllQuestionsOrderIndex() {
        List<GrammarTopic> allTopics = topicRepository.findAll();
        
        Map<String, Object> globalResult = new HashMap<>();
        List<Map<String, Object>> topicResults = new ArrayList<>();
        int totalIssuesFixed = 0;
        int totalIssuesFound = 0;
        int totalQuestions = 0;
        int totalLessons = 0;

        for (GrammarTopic topic : allTopics) {
            Map<String, Object> topicResult = validateAllQuestionsInTopic(topic.getId());
            
            totalIssuesFixed += (int) topicResult.get("totalIssuesFixed");
            totalIssuesFound += (int) topicResult.get("totalIssuesFound");
            totalQuestions += (int) topicResult.get("totalQuestions");
            totalLessons += (int) topicResult.get("totalLessonsChecked");
            
            if ((int) topicResult.get("totalIssuesFound") > 0) {
                topicResults.add(Map.of(
                    "topicId", topic.getId(),
                    "topicName", topic.getName(),
                    "issuesFixed", topicResult.get("totalIssuesFixed"),
                    "lessonsWithIssues", topicResult.get("lessonsWithIssues")
                ));
            }
        }

        globalResult.put("totalTopicsChecked", allTopics.size());
        globalResult.put("totalLessonsChecked", totalLessons);
        globalResult.put("totalQuestions", totalQuestions);
        globalResult.put("totalIssuesFound", totalIssuesFound);
        globalResult.put("totalIssuesFixed", totalIssuesFixed);
        globalResult.put("topicsWithIssues", topicResults);

        log.info("✅ Validated ALL questions across {} topics, fixed {} issues", 
                allTopics.size(), totalIssuesFixed);

        return globalResult;
    }

    // ===== HELPER =====

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}