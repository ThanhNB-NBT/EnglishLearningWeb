package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.grammar.GrammarTopic;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.grammar.GrammarTopicRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service xử lý import grammar lessons từ AI parsing
 * Sử dụng metadata-based architecture (JSONB)
 * Reuse QuestionConversionService để convert
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarImportService {

    private final GrammarTopicRepository topicRepository;
    private final GrammarLessonRepository lessonRepository;
    private final GrammarQuestionService questionService;

    /**
     * Import lessons from AI parsing service
     *
     * @param topicId       Target topic ID
     * @param parsedLessons Lessons từ AI parsing (chứa QuestionResponseDTO)
     * @return List of saved lessons with IDs and question counts
     */
    @Transactional
    public List<GrammarLessonDTO> importLessonsFromFile(Long topicId, List<GrammarLessonDTO> parsedLessons) {
        log.info("Importing {} parsed lessons to topicId={}", parsedLessons.size(), topicId);

        // Validate topic exists
        GrammarTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại với id: " + topicId));

        List<GrammarLessonDTO> savedLessons = new ArrayList<>();

        for (GrammarLessonDTO lessonDTO : parsedLessons) {
            try {
                // ===== VALIDATE VÀ TẠO LESSON (Logic này vẫn đúng) =====
                if (lessonDTO.getTitle() == null || lessonDTO.getTitle().trim().isEmpty()) {
                    log.warn("Skipping lesson with empty title");
                    continue;
                }
                if (lessonDTO.getLessonType() == null) {
                    log.warn("Skipping lesson '{}' with null lessonType", lessonDTO.getTitle());
                    continue;
                }

                lessonDTO.setTopicId(topicId);
                // (Các logic set default cho points, duration, isActive... vẫn giữ nguyên)
                if (lessonDTO.getPointsReward() == null || lessonDTO.getPointsReward() == 0) {
                    lessonDTO.setPointsReward(lessonDTO.getLessonType() == LessonType.THEORY ? 10 : 15);
                }
                if (lessonDTO.getTimeLimitSeconds() == null || lessonDTO.getTimeLimitSeconds() == 0) {
                    int duration = lessonDTO.getLessonType() == LessonType.THEORY ? 30 : 300;
                    if (lessonDTO.getContent() != null && lessonDTO.getContent().length() > 5000)
                        duration += 120;
                    if (lessonDTO.getQuestions() != null && lessonDTO.getQuestions().size() > 5)
                        duration += lessonDTO.getQuestions().size() * 30;
                    lessonDTO.setTimeLimitSeconds(duration);
                }
                if (lessonDTO.getIsActive() == null)
                    lessonDTO.setIsActive(true);

                // Create lesson entity
                GrammarLesson lesson = new GrammarLesson();
                lesson.setTopic(topic);
                lesson.setTitle(lessonDTO.getTitle());
                lesson.setLessonType(lessonDTO.getLessonType());
                lesson.setContent(lessonDTO.getContent());
                lesson.setOrderIndex(lessonDTO.getOrderIndex());
                lesson.setTimeLimitSeconds(lessonDTO.getTimeLimitSeconds());
                lesson.setPointsReward(lessonDTO.getPointsReward());
                lesson.setIsActive(lessonDTO.getIsActive());
                lesson.setCreatedAt(LocalDateTime.now());
                lesson.setMetadata(lessonDTO.getMetadata());

                GrammarLesson savedLesson = lessonRepository.save(lesson);

                // ===== SỬA ĐỔI: TẠO QUESTIONS =====
                int questionCount = 0;
                List<CreateQuestionDTO> questionsToCreate = lessonDTO.getCreateQuestions();

                if (questionsToCreate != null && !questionsToCreate.isEmpty()) {
                    log.info("Creating {} questions for lesson: {}",
                            questionsToCreate.size(), savedLesson.getTitle());

                    // Set parentId và parentType cho mỗi question
                    questionsToCreate.forEach(dto -> {
                        dto.setParentId(savedLesson.getId());
                        dto.setParentType(ParentType.GRAMMAR);
                        // Validation tự động qua @Valid trong createDTO
                    });

                    // Tạo questions qua service
                    List<QuestionResponseDTO> createdQuestions = questionService.createQuestionsInBulk(
                            savedLesson.getId(),
                            questionsToCreate
                    );

                    questionCount = createdQuestions.size();
                    log.info("Created {} questions for lesson id={}", questionCount, savedLesson.getId());
                } else {
                    log.debug("No questions for lesson: {} ({})",
                            savedLesson.getTitle(), savedLesson.getLessonType());
                }

                // Build response DTO
                GrammarLessonDTO savedDTO = convertLessonToDTO(savedLesson);
                savedDTO.setQuestionCount(questionCount);
                savedLessons.add(savedDTO);

                log.info("Imported lesson: {} (type: {}, orderIndex: {}, {} questions)",
                        savedLesson.getTitle(),
                        savedLesson.getLessonType(),
                        savedLesson.getOrderIndex(),
                        questionCount);

            } catch (Exception e) {
                log.error("Error importing lesson '{}': {}",
                        lessonDTO != null ? lessonDTO.getTitle() : "unknown",
                        e.getMessage(), e);
            }
        }

        log.info("Successfully imported {}/{} lessons to topic '{}' (ID: {})",
                savedLessons.size(), parsedLessons.size(), topic.getName(), topicId);

        return savedLessons;
    }

    private GrammarLessonDTO convertLessonToDTO(GrammarLesson lesson) {
        return GrammarLessonDTO.full(
                lesson.getId(),
                lesson.getTopic().getId(),
                lesson.getTitle(),
                lesson.getLessonType(),
                lesson.getContent(),
                lesson.getOrderIndex(),
                lesson.getPointsReward(),
                lesson.getTimeLimitSeconds(),
                lesson.getIsActive(),
                lesson.getCreatedAt(),
                lesson.getMetadata(),
                lesson.getTopic().getName());
    }
}