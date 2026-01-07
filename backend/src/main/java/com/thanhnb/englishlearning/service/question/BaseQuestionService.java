package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.dto.question.response.TaskGroupedQuestionsDTO;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.entity.question.TaskGroup;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.question.TaskGroupRepository;
import com.thanhnb.englishlearning.service.permission.TeacherPermissionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public abstract class BaseQuestionService {

    protected final QuestionRepository questionRepository;
    protected final TeacherPermissionService teacherPermissionService;
    protected final TaskGroupService taskGroupService;
    protected final TaskGroupRepository taskGroupRepository;

    protected BaseQuestionService(
            QuestionRepository questionRepository,
            TeacherPermissionService teacherPermissionService,
            TaskGroupService taskGroupService,
            TaskGroupRepository taskGroupRepository) {
        this.questionRepository = questionRepository;
        this.teacherPermissionService = teacherPermissionService;
        this.taskGroupService = taskGroupService;
        this.taskGroupRepository = taskGroupRepository;
    }

    // Các class con phải implement
    protected abstract ParentType getParentType();

    protected abstract void validateLessonExists(Long lessonId);

    protected abstract Long getTopicIdFromLesson(Long lessonId);

    // =========================================================================
    // CRUD OPERATIONS
    // =========================================================================

    @Transactional
    public QuestionResponseDTO createQuestion(Long lessonId, @Valid CreateQuestionDTO dto) {
        validateLessonExists(lessonId);
        Long topicId = getTopicIdFromLesson(lessonId);
        teacherPermissionService.checkTopicPermission(topicId);

        if (dto.getOrderIndex() == null || dto.getOrderIndex() == 0) {
            dto.setOrderIndex(getNextOrderIndex(lessonId));
        }

        // Update TaskGroup (nếu có)
        TaskGroup taskGroup = null;
        if (dto.getTaskGroupId() != null) {
            taskGroup = taskGroupService.findTaskGroupById(dto.getTaskGroupId());

            // Validate taskGroup thuộc đúng lesson
            if (!taskGroup.getParentType().equals(getParentType())
                    || !taskGroup.getParentId().equals(lessonId)) {
                throw new IllegalArgumentException(
                        "TaskGroup không thuộc lesson này");
            }
        }

        Question question = Question.builder()
                .parentType(getParentType())
                .parentId(lessonId)
                .questionType(dto.getQuestionType())
                .questionText(dto.getQuestionText())
                .points(dto.getPoints() != null ? dto.getPoints() : 1)
                .orderIndex(dto.getOrderIndex())
                .taskGroup(taskGroup)
                .build();

        question.setData(dto.getData());

        Question saved = questionRepository.save(question);
        log.info("Created {} question id={}", getParentType(), saved.getId());
        return toResponseDTO(saved);
    }

    @Transactional
    public QuestionResponseDTO updateQuestion(Long id, @Valid CreateQuestionDTO dto) {
        Question question = findQuestionById(id);
        Long topicId = getTopicIdFromLesson(question.getParentId());
        teacherPermissionService.checkTopicPermission(topicId);

        // Update cột cứng
        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());
        question.setPoints(dto.getPoints());

        if (dto.getOrderIndex() != null) {
            question.setOrderIndex(dto.getOrderIndex());
        }

        // Update TaskGroup
        if (dto.getTaskGroupId() != null) {
            TaskGroup taskGroup = taskGroupService.findTaskGroupById(dto.getTaskGroupId());

            if (!taskGroup.getParentType().equals(getParentType())
                    || !taskGroup.getParentId().equals(question.getParentId())) {
                throw new IllegalArgumentException(
                        "TaskGroup không thuộc lesson này");
            }

            question.setTaskGroup(taskGroup);
        } else {
            question.setTaskGroup(null); // Chuyển thành standalone
        }

        // Update JSON Data
        question.setData(dto.getData());

        Question saved = questionRepository.save(question);

        if (saved.getTaskGroup() != null) {
            saved.getTaskGroup().getTaskName();
            saved.getTaskGroup().getInstruction();
        }

        return toResponseDTO(saved);
    }

    public void deleteQuestion(Long id) {
        Question question = findQuestionById(id);
        Long topicId = getTopicIdFromLesson(question.getParentId());
        teacherPermissionService.checkTopicPermission(topicId);
        questionRepository.delete(question);
    }

    public QuestionResponseDTO getQuestionById(Long id) {
        return toResponseDTO(findQuestionById(id));
    }

    public List<QuestionResponseDTO> getQuestionsByLessonId(Long lessonId) {
        validateLessonExists(lessonId);
        return questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(getParentType(), lessonId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // TASK GROUPING

    public TaskGroupedQuestionsDTO getGroupedQuestions(Long lessonId) {
        validateLessonExists(lessonId);

        // Lấy tất cả TaskGroups (có eager load questions qua @OneToMany)
        List<TaskGroup> taskGroups = taskGroupRepository
                .findByParentTypeAndParentIdWithQuestions(getParentType(), lessonId);

        if (taskGroups.isEmpty()) {
            // Không có task → trả về tất cả questions dạng standalone
            List<QuestionResponseDTO> allQuestions = questionRepository
                    .findByParentTypeAndParentIdOrderByOrderIndexAsc(getParentType(), lessonId)
                    .stream()
                    .map(this::toResponseDTO)
                    .toList();

            return TaskGroupedQuestionsDTO.builder()
                    .hasTaskStructure(false)
                    .standaloneQuestions(allQuestions)
                    .build();
        }

        List<TaskGroupedQuestionsDTO.TaskGroup> tasks = taskGroups.stream()
                .map(tg -> TaskGroupedQuestionsDTO.TaskGroup.builder()
                        .taskGroupId(tg.getId())
                        .taskName(tg.getTaskName())
                        .taskInstruction(tg.getInstruction())
                        .taskOrder(tg.getOrderIndex())
                        .questions(tg.getQuestions().stream()
                                .map(this::toResponseDTO)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        List<QuestionResponseDTO> standalone = questionRepository
                .findByParentTypeAndParentIdAndTaskGroupIsNullOrderByOrderIndexAsc(getParentType(), lessonId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return TaskGroupedQuestionsDTO.builder()
                .hasTaskStructure(true)
                .tasks(tasks)
                .standaloneQuestions(standalone.isEmpty() ? null : standalone)
                .build();
    }

    public Map<String, Object> getTaskStats(Long lessonId) {
        validateLessonExists(lessonId);

        List<TaskGroup> taskGroups = taskGroupRepository
                .findByParentTypeAndParentIdOrderByOrderIndexAsc(getParentType(), lessonId);

        List<Map<String, Object>> taskStats = taskGroups.stream()
                .map(tg -> {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("taskGroupId", tg.getId());
                    stat.put("taskName", tg.getTaskName());
                    stat.put("instruction", tg.getInstruction());
                    stat.put("orderIndex", tg.getOrderIndex());
                    stat.put("questionCount", tg.getQuestionCount());
                    stat.put("totalPoints", tg.getTotalPoints());
                    return stat;
                })
                .collect(Collectors.toList());

        long standaloneCount = questionRepository
                .countByParentTypeAndParentIdAndTaskGroupIsNull(getParentType(), lessonId);

        return Map.of(
                "hasTaskStructure", !taskStats.isEmpty(),
                "taskCount", taskStats.size(),
                "taskStats", taskStats,
                "standaloneQuestionCount", standaloneCount);
    }

    // =========================================================================
    // BULK & UTILS
    // =========================================================================

    @Transactional
    public List<QuestionResponseDTO> createQuestionsInBulk(Long lessonId, @Valid List<CreateQuestionDTO> dtos) {
        validateLessonExists(lessonId);
        Long topicId = getTopicIdFromLesson(lessonId);
        teacherPermissionService.checkTopicPermission(topicId);

        Integer nextOrder = getNextOrderIndex(lessonId);

        List<Question> questions = IntStream.range(0, dtos.size())
                .mapToObj(i -> {
                    CreateQuestionDTO dto = dtos.get(i);
                    int order = (dto.getOrderIndex() != null && dto.getOrderIndex() > 0)
                            ? dto.getOrderIndex()
                            : nextOrder + i;

                    TaskGroup taskGroup = null;
                    if (dto.getTaskGroupId() != null) {
                        taskGroup = taskGroupService.findTaskGroupById(dto.getTaskGroupId());
                    }

                    Question question = Question.builder()
                            .parentType(getParentType())
                            .parentId(lessonId)
                            .questionType(dto.getQuestionType())
                            .questionText(dto.getQuestionText())
                            .points(dto.getPoints() != null ? dto.getPoints() : 1)
                            .orderIndex(order)
                            .taskGroup(taskGroup)
                            .build();

                    question.setData(dto.getData());
                    return question;
                })
                .toList();

        List<Question> saved = questionRepository.saveAll(questions);
        return saved.stream().map(this::toResponseDTO).toList();
    }

    @Transactional
    public void bulkDeleteQuestions(List<Long> ids) {
        if (ids == null || ids.isEmpty())
            return;

        List<Question> questions = questionRepository.findAllById(ids);
        if (questions.isEmpty())
            return;

        Question first = questions.get(0);
        Long topicId = getTopicIdFromLesson(first.getParentId());
        teacherPermissionService.checkTopicPermission(topicId);

        questionRepository.deleteAll(questions);
        log.info("Bulk deleted {} questions", questions.size());
    }

    @Transactional
    public void fixOrderIndexes(Long lessonId) {
        validateLessonExists(lessonId);
        Long topicId = getTopicIdFromLesson(lessonId);
        teacherPermissionService.checkTopicPermission(topicId);

        List<Question> questions = questionRepository
                .findByParentTypeAndParentIdOrderByOrderIndexAsc(getParentType(), lessonId);

        questions.sort(Comparator.comparingInt(Question::getOrderIndex)
                .thenComparingLong(Question::getId));

        int updatedCount = 0;
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            int expected = i + 1;
            if (q.getOrderIndex() == null || q.getOrderIndex() != expected) {
                questionRepository.updateOrderIndex(q.getId(), expected);
                updatedCount++;
            }
        }
        if (updatedCount > 0) {
            log.info("Fixed order for {} questions in lesson {}", updatedCount, lessonId);
        }
    }

    public Integer getNextOrderIndex(Long lessonId) {
        return questionRepository.findMaxOrderIndexByLessonId(getParentType(), lessonId)
                .orElse(0) + 1;
    }

    protected Question findQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
    }

    protected QuestionResponseDTO toResponseDTO(Question question) {
        Long taskGroupId = null;
        String taskGroupName = null;
        String taskInstruction = null;

        if (question.getTaskGroup() != null) {
            TaskGroup tg = question.getTaskGroup();
            taskGroupId = tg.getId();

            try {
                taskGroupName = tg.getTaskName();
                taskInstruction = tg.getInstruction();
            } catch (Exception e) {
                log.debug("Could not load TaskGroup details");
            }
        }
        return QuestionResponseDTO.builder()
                .id(question.getId())
                .parentType(question.getParentType())
                .parentId(question.getParentId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .points(question.getPoints())
                .orderIndex(question.getOrderIndex())
                .taskGroupId(taskGroupId)
                .taskGroupName(taskGroupName)
                .taskInstruction(taskInstruction)
                .createdAt(question.getCreatedAt())
                .data(question.getData())
                .build();
    }
}