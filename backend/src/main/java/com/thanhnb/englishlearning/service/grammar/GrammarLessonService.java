package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateTaskGroupDTO;
import com.thanhnb.englishlearning.dto.question.response.TaskGroupResponseDTO;
import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.topic.TopicRepository;
import com.thanhnb.englishlearning.service.permission.TeacherPermissionService;
import com.thanhnb.englishlearning.service.question.TaskGroupService;
import com.thanhnb.englishlearning.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Grammar Lesson Service
 * Includes: CRUD + Pagination + Order Management + Status Toggle + Validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarLessonService {

    private final GrammarLessonRepository lessonRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final TeacherPermissionService teacherPermissionService;
    private final UserService userService;
    private final TaskGroupService taskGroupService;
    private final GrammarQuestionService questionService;

    // ═════════════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    public Page<GrammarLessonDTO> getLessonsByTopic(Long topicId, Pageable pageable) {
        teacherPermissionService.checkTopicPermission(topicId);
        return lessonRepository.findByTopicId(topicId, pageable)
                .map(this::toDTO);
    }

    public GrammarLessonDTO getLessonById(Long id) {
        GrammarLesson lesson = findLessonById(id);
        teacherPermissionService.checkTopicPermission(lesson.getTopic().getId());
        return toDTO(lesson);
    }

    // ═════════════════════════════════════════════════════════════════
    // CREATE
    // ═════════════════════════════════════════════════════════════════

    public GrammarLessonDTO createLesson(GrammarLessonDTO dto) {

        teacherPermissionService.checkTopicPermission(dto.getTopicId());
        // 1. Validate
        validateForCreate(dto);

        // 3. Check unique title
        if (lessonRepository.existsByTopicIdAndTitleIgnoreCase(dto.getTopicId(), dto.getTitle())) {
            throw new IllegalArgumentException("Title already exists in this topic");
        }

        // 4. Get topic
        Topic topic = topicRepository.findById(dto.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        User currentUser = userService.getCurrentUser();
        // 5. Build entity
        GrammarLesson lesson = GrammarLesson.builder()
                .topic(topic)
                .title(dto.getTitle())
                .lessonType(dto.getLessonType())
                .content(dto.getContent())
                .orderIndex(dto.getOrderIndex())
                .timeLimitSeconds(dto.getTimeLimitSeconds() != null ? dto.getTimeLimitSeconds() : 0)
                .pointsReward(dto.getPointsReward() != null ? dto.getPointsReward() : 10)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .modifiedBy(currentUser)
                .build();

        GrammarLesson saved = lessonRepository.save(lesson);
        log.info("Created grammar lesson: id={}, title={}", saved.getId(), saved.getTitle());

        return toDTO(saved);
    }

    // ═════════════════════════════════════════════════════════════════
    // UPDATE
    // ═════════════════════════════════════════════════════════════════

    public GrammarLessonDTO updateLesson(Long id, GrammarLessonDTO dto) {
        
        GrammarLesson lesson = findLessonById(id);
        teacherPermissionService.checkTopicPermission(lesson.getTopic().getId());
        // Check unique title (if changed)
        if (dto.getTitle() != null && !dto.getTitle().equals(lesson.getTitle())) {
            if (lessonRepository.existsByTopicIdAndTitleIgnoreCaseAndIdNot(
                    lesson.getTopic().getId(), dto.getTitle(), id)) {
                throw new IllegalArgumentException("Title already exists in this topic");
            }
        }

        User currentUser = userService.getCurrentUser();
        lesson.setModifiedBy(currentUser);

        // Update fields (only if provided)
        updateFields(lesson, dto);

        GrammarLesson saved = lessonRepository.save(lesson);
        log.info("Updated grammar lesson: id={}", id);

        return toDTO(saved);
    }

    // ═════════════════════════════════════════════════════════════════
    // DELETE
    // ═════════════════════════════════════════════════════════════════

    public void deleteLesson(Long id) {
        GrammarLesson lesson = findLessonById(id);
        teacherPermissionService.checkTopicPermission(lesson.getTopic().getId());
        Long topicId = lesson.getTopic().getId();
        Integer orderIndex = lesson.getOrderIndex();

        // Delete (questions cascade via QuestionCascadeDeleteListener)
        lessonRepository.delete(lesson);
        log.info("Deleted grammar lesson: id={}", id);

        // Reorder remaining lessons
        reorderAfterDelete(topicId, orderIndex);
    }

    // ═════════════════════════════════════════════════════════════════
    // STATUS TOGGLE
    // ═════════════════════════════════════════════════════════════════

    public void toggleStatus(Long id) {
        GrammarLesson lesson = findLessonById(id);
        teacherPermissionService.checkTopicPermission(lesson.getTopic().getId());

        lesson.setIsActive(!lesson.getIsActive());
        lessonRepository.save(lesson);

        log.info("Toggled lesson {} status to: {}", id, lesson.getIsActive());
    }

    // ═════════════════════════════════════════════════════════════════
    // ORDER MANAGEMENT
    // ═════════════════════════════════════════════════════════════════

    public void fixOrderIndexes(Long topicId) {
        // Lấy danh sách
        List<GrammarLesson> lessons = lessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId);
        
        // ✅ Sort lại trong memory để đảm bảo ổn định (ID tăng dần nếu trùng Order)
        lessons.sort(Comparator.comparingInt(GrammarLesson::getOrderIndex)
                .thenComparingLong(GrammarLesson::getId));

        boolean changed = false;
        for (int i = 0; i < lessons.size(); i++) {
            GrammarLesson lesson = lessons.get(i);
            int expected = i + 1;
            
            if (!Integer.valueOf(expected).equals(lesson.getOrderIndex())) {
                lesson.setOrderIndex(expected);
                changed = true;
            }
        }

        if (changed) {
            lessonRepository.saveAll(lessons);
            log.info("Fixed order indexes for {} lessons in topic {}", lessons.size(), topicId);
        }
    }

    public Integer getNextOrderIndex(Long topicId) {
        Integer max = lessonRepository.findMaxOrderIndexByTopicId(topicId);
        return (max != null ? max : 0) + 1;
    }

    // GrammarLessonService.java

/**
 * ✅ CREATE LESSON WITH QUESTIONS AND TASK GROUPS
 * Used by AI Import after user reviews and confirms
 */
@Transactional
public GrammarLessonDTO createLessonWithQuestionsAndTasks(GrammarLessonDTO dto) {
    // 1. Validate basic info
    validateForCreate(dto);
    teacherPermissionService.checkTopicPermission(dto.getTopicId());
    
    // 2. Check unique title
    if (lessonRepository.existsByTopicIdAndTitleIgnoreCase(dto.getTopicId(), dto.getTitle())) {
        throw new IllegalArgumentException("Title already exists in this topic");
    }
    
    // 3. Get topic
    Topic topic = topicRepository.findById(dto.getTopicId())
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
    
    User currentUser = userService.getCurrentUser();
    
    // 4. Create lesson
    GrammarLesson lesson = GrammarLesson.builder()
            .topic(topic)
            .title(dto.getTitle())
            .lessonType(dto.getLessonType())
            .content(dto.getContent())
            .orderIndex(dto.getOrderIndex())
            .timeLimitSeconds(dto.getTimeLimitSeconds() != null ? dto.getTimeLimitSeconds() : 0)
            .pointsReward(dto.getPointsReward() != null ? dto.getPointsReward() : 10)
            .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
            .createdAt(LocalDateTime.now())
            .modifiedBy(currentUser)
            .build();
    
    lesson = lessonRepository.save(lesson);
    log.info("✅ Created lesson: id={}, title={}", lesson.getId(), lesson.getTitle());
    
    // 5. Create TaskGroups if present
    if (dto.getTaskGroups() != null && !dto.getTaskGroups().isEmpty()) {
        for (GrammarLessonDTO.TaskGroupImportData taskGroupData : dto.getTaskGroups()) {
            CreateTaskGroupDTO taskGroupDTO = CreateTaskGroupDTO.builder()
                    .taskName(taskGroupData.getTaskName())
                    .instruction(taskGroupData.getInstruction())
                    .orderIndex(taskGroupData.getOrderIndex())
                    .build();
            
            TaskGroupResponseDTO taskGroup = taskGroupService.createTaskGroup(
                ParentType.GRAMMAR,
                lesson.getId(),
                taskGroupDTO
            );
            
            log.info("✅ Created TaskGroup: id={}, name={}", taskGroup.getId(), taskGroup.getTaskName());
            
            // 6. Create questions in task group
            if (taskGroupData.getQuestions() != null && !taskGroupData.getQuestions().isEmpty()) {
                for (CreateQuestionDTO questionDTO : taskGroupData.getQuestions()) {
                    questionDTO.setTaskGroupId(taskGroup.getId());
                    questionDTO.setParentType(ParentType.GRAMMAR);
                    questionDTO.setParentId(lesson.getId());
                    
                    // Auto-assign order if missing
                    if (questionDTO.getOrderIndex() == null || questionDTO.getOrderIndex() == 0) {
                        questionDTO.setOrderIndex(
                            questionRepository.findMaxOrderIndexByLessonId(ParentType.GRAMMAR, lesson.getId())
                                .orElse(0) + 1
                        );
                    }
                    
                    questionService.createQuestion(lesson.getId(), questionDTO);
                }
                log.info("✅ Created {} questions in TaskGroup", taskGroupData.getQuestions().size());
            }
        }
    }
    
    // 7. Create standalone questions
    if (dto.getStandaloneQuestions() != null && !dto.getStandaloneQuestions().isEmpty()) {
        for (CreateQuestionDTO questionDTO : dto.getStandaloneQuestions()) {
            questionDTO.setTaskGroupId(null); // Ensure standalone
            questionDTO.setParentType(ParentType.GRAMMAR);
            questionDTO.setParentId(lesson.getId());
            
            if (questionDTO.getOrderIndex() == null || questionDTO.getOrderIndex() == 0) {
                questionDTO.setOrderIndex(
                    questionRepository.findMaxOrderIndexByLessonId(ParentType.GRAMMAR, lesson.getId())
                        .orElse(0) + 1
                );
            }
            
            questionService.createQuestion(lesson.getId(), questionDTO);
        }
        log.info("✅ Created {} standalone questions", dto.getStandaloneQuestions().size());
    }
    
    // 8. Return DTO with questions
    GrammarLessonDTO result = toDTO(lesson);
    result.setGroupedQuestions(questionService.getGroupedQuestions(lesson.getId()));
    
    return result;
}

    // ═════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════

    private GrammarLesson findLessonById(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + id));
    }

    private void validateForCreate(GrammarLessonDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (dto.getLessonType() == null) {
            throw new IllegalArgumentException("Lesson type is required");
        }
        if (dto.getOrderIndex() == null || dto.getOrderIndex() < 1) {
            throw new IllegalArgumentException("Order index must be >= 1");
        }
    }

    private void updateFields(GrammarLesson lesson, GrammarLessonDTO dto) {
        if (dto.getTitle() != null)
            lesson.setTitle(dto.getTitle());
        if (dto.getLessonType() != null)
            lesson.setLessonType(dto.getLessonType());
        if (dto.getContent() != null)
            lesson.setContent(dto.getContent());
        if (dto.getOrderIndex() != null)
            lesson.setOrderIndex(dto.getOrderIndex());
        if (dto.getTimeLimitSeconds() != null)
            lesson.setTimeLimitSeconds(dto.getTimeLimitSeconds());
        if (dto.getPointsReward() != null)
            lesson.setPointsReward(dto.getPointsReward());
        if (dto.getIsActive() != null)
            lesson.setIsActive(dto.getIsActive());
        }

    private void reorderAfterDelete(Long topicId, Integer deletedPosition) {
        int affected = lessonRepository.shiftOrderAfterDelete(topicId, deletedPosition);
        log.debug("Reordered {} lessons after deletion", affected);
    }

    private GrammarLessonDTO toDTO(GrammarLesson lesson) {
        long questionCount = questionRepository.countByParentTypeAndParentId(
                ParentType.GRAMMAR, lesson.getId());

        return GrammarLessonDTO.builder()
                .id(lesson.getId())
                .topicId(lesson.getTopic().getId())
                .topicName(lesson.getTopic().getName())
                .title(lesson.getTitle())
                .lessonType(lesson.getLessonType())
                .content(lesson.getContent())
                .orderIndex(lesson.getOrderIndex())
                .timeLimitSeconds(lesson.getTimeLimitSeconds())
                .pointsReward(lesson.getPointsReward())
                .isActive(lesson.getIsActive())
                .createdAt(lesson.getCreatedAt())
                .questionCount((int) questionCount)
                .modifiedAt(lesson.getModifiedAt())
                .modifiedBy(lesson.getModifiedBy() != null ? lesson.getModifiedBy().getId() : null)
                .build();
    }
}