package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.enums.LessonType;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.topic.TopicRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service xử lý import grammar lessons từ AI parsing
 * Sử dụng Topic chung và Check quyền Teacher
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarImportService {

    private final TopicRepository topicRepository;
    private final GrammarLessonRepository lessonRepository;

    /**
     * Import lessons from AI parsing service
     */
    @Transactional
    public List<GrammarLessonDTO> importLessonsFromFile(Long topicId, List<GrammarLessonDTO> parsedLessons) {
        log.info("Importing {} parsed lessons to topicId={}", parsedLessons.size(), topicId);

        // 1. Validate topic exists & Type
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic không tồn tại với id: " + topicId));

        if (topic.getModuleType() != ModuleType.GRAMMAR) {
            throw new IllegalArgumentException("Topic này không thuộc module GRAMMAR (Topic là: " + topic.getModuleType() + ")");
        }

        List<GrammarLessonDTO> savedLessons = new ArrayList<>();

        for (GrammarLessonDTO lessonDTO : parsedLessons) {
            try {
                // Validate cơ bản
                if (lessonDTO.getTitle() == null || lessonDTO.getTitle().trim().isEmpty()) {
                    log.warn("Skipping lesson with empty title");
                    continue;
                }
                if (lessonDTO.getLessonType() == null) {
                    log.warn("Skipping lesson '{}' with null lessonType", lessonDTO.getTitle());
                    continue;
                }

                // Set các giá trị mặc định nếu thiếu
                lessonDTO.setTopicId(topicId);
                
                if (lessonDTO.getPointsReward() == null || lessonDTO.getPointsReward() == 0) {
                    lessonDTO.setPointsReward(lessonDTO.getLessonType() == LessonType.THEORY ? 10 : 15);
                }
                
                // if (lessonDTO.getTimeLimitSeconds() == null || lessonDTO.getTimeLimitSeconds() == 0) {
                //     int duration = lessonDTO.getLessonType() == LessonType.THEORY ? 30 : 300;
                //     if (lessonDTO.getContent() != null && lessonDTO.getContent().length() > 5000)
                //         duration += 120;
                //     if (lessonDTO.getCreateQuestions() != null && lessonDTO.getCreateQuestions().size() > 5) // Sửa getQuestions -> getCreateQuestions cho chuẩn DTO import
                //         duration += lessonDTO.getCreateQuestions().size() * 30;
                //     lessonDTO.setTimeLimitSeconds(duration);
                // }
                
                if (lessonDTO.getIsActive() == null)
                    lessonDTO.setIsActive(true);

                // Create lesson entity
                GrammarLesson lesson = new GrammarLesson();
                lesson.setTopic(topic); // Set Topic chung
                lesson.setTitle(lessonDTO.getTitle());
                lesson.setLessonType(lessonDTO.getLessonType());
                lesson.setContent(lessonDTO.getContent());
                lesson.setOrderIndex(lessonDTO.getOrderIndex());
                lesson.setTimeLimitSeconds(lessonDTO.getTimeLimitSeconds());
                lesson.setPointsReward(lessonDTO.getPointsReward());
                lesson.setIsActive(lessonDTO.getIsActive());
                lesson.setCreatedAt(LocalDateTime.now());

                GrammarLesson savedLesson = lessonRepository.save(lesson);

                // // Tạo Questions
                // int questionCount = 0;
                // List<CreateQuestionDTO> questionsToCreate = lessonDTO.getCreateQuestions();

                // if (questionsToCreate != null && !questionsToCreate.isEmpty()) {
                //     log.info("Creating {} questions for lesson: {}", questionsToCreate.size(), savedLesson.getTitle());

                //     // Set parentId cho từng câu hỏi
                //     questionsToCreate.forEach(dto -> {
                //         dto.setParentId(savedLesson.getId());
                //         dto.setParentType(ParentType.GRAMMAR);
                //     });

                //     // Tạo questions qua service
                //     List<QuestionResponseDTO> createdQuestions = questionService.createQuestionsInBulk(
                //             savedLesson.getId(),
                //             questionsToCreate
                //     );

                //     questionCount = createdQuestions.size();
                // }

                // Build response DTO
                GrammarLessonDTO savedDTO = convertLessonToDTO(savedLesson);
                // savedDTO.setQuestionCount(questionCount);
                savedLessons.add(savedDTO);

            } catch (Exception e) {
                log.error("Error importing lesson '{}': {}",
                        lessonDTO != null ? lessonDTO.getTitle() : "unknown",
                        e.getMessage(), e);
            }
        }

        log.info("Successfully imported {}/{} lessons to topic '{}'",
                savedLessons.size(), parsedLessons.size(), topic.getName());

        return savedLessons;
    }

    private GrammarLessonDTO convertLessonToDTO(GrammarLesson lesson) {
        GrammarLessonDTO dto = new GrammarLessonDTO();
        dto.setId(lesson.getId());
        dto.setTopicId(lesson.getTopic().getId());
        dto.setTitle(lesson.getTitle());
        dto.setLessonType(lesson.getLessonType());
        dto.setContent(lesson.getContent());
        dto.setOrderIndex(lesson.getOrderIndex());
        dto.setPointsReward(lesson.getPointsReward());
        dto.setTimeLimitSeconds(lesson.getTimeLimitSeconds());
        dto.setIsActive(lesson.getIsActive());
        dto.setCreatedAt(lesson.getCreatedAt());
        dto.setTopicName(lesson.getTopic().getName());
        return dto;
    }
}