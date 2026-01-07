package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.dto.question.request.CreateTaskGroupDTO;
import com.thanhnb.englishlearning.dto.question.response.TaskGroupResponseDTO;
import com.thanhnb.englishlearning.entity.question.TaskGroup;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.question.TaskGroupRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TaskGroupService {

    private final TaskGroupRepository taskGroupRepository;

    /**
     * Tạo TaskGroup mới
     */
    @Transactional
    public TaskGroupResponseDTO createTaskGroup(
            ParentType parentType,
            Long lessonId,
            @Valid CreateTaskGroupDTO dto) {
        // Check trùng tên trong cùng lesson
        if (taskGroupRepository.existsByParentTypeAndParentIdAndTaskName(
                parentType, lessonId, dto.getTaskName())) {
            throw new IllegalArgumentException(
                    "Task name '" + dto.getTaskName() + "' đã tồn tại trong lesson này");
        }

        // Auto-generate orderIndex nếu null
        Integer orderIndex = dto.getOrderIndex();
        if (orderIndex == null || orderIndex == 0) {
            orderIndex = getNextOrderIndex(parentType, lessonId);
        }

        TaskGroup taskGroup = TaskGroup.builder()
                .parentType(parentType)
                .parentId(lessonId)
                .taskName(dto.getTaskName())
                .instruction(dto.getInstruction())
                .orderIndex(orderIndex)
                .build();

        TaskGroup saved = taskGroupRepository.save(taskGroup);
        log.info("Created TaskGroup id={} for {} lesson={}",
                saved.getId(), parentType, lessonId);

        return toResponseDTO(saved);
    }

    /**
     * Cập nhật TaskGroup
     */
    @Transactional
    public TaskGroupResponseDTO updateTaskGroup(Long id, @Valid CreateTaskGroupDTO dto) {
        TaskGroup taskGroup = findTaskGroupById(id);

        // Check trùng tên (nếu đổi tên)
        if (!taskGroup.getTaskName().equals(dto.getTaskName())) {
            if (taskGroupRepository.existsByParentTypeAndParentIdAndTaskName(
                    taskGroup.getParentType(),
                    taskGroup.getParentId(),
                    dto.getTaskName())) {
                throw new IllegalArgumentException(
                        "Task name '" + dto.getTaskName() + "' đã tồn tại trong lesson này");
            }
        }

        taskGroup.setTaskName(dto.getTaskName());
        taskGroup.setInstruction(dto.getInstruction());

        if (dto.getOrderIndex() != null) {
            taskGroup.setOrderIndex(dto.getOrderIndex());
        }

        TaskGroup saved = taskGroupRepository.save(taskGroup);
        return toResponseDTO(saved);
    }

    /**
     * Xóa TaskGroup (và tất cả questions trong đó)
     */
    @Transactional
    public void deleteTaskGroup(Long id) {
        TaskGroup taskGroup = findTaskGroupById(id);
        taskGroupRepository.delete(taskGroup);
        log.info("Deleted TaskGroup id={} with {} questions",
                id, taskGroup.getQuestionCount());
    }

    /**
     * Lấy TaskGroup theo ID
     */
    public TaskGroupResponseDTO getTaskGroupById(Long id) {
        return toResponseDTO(findTaskGroupById(id));
    }

    /**
     * Lấy tất cả TaskGroups của một lesson
     */
    public List<TaskGroupResponseDTO> getTaskGroupsByLesson(
            ParentType parentType,
            Long lessonId) {
        return taskGroupRepository
                .findByParentTypeAndParentIdOrderByOrderIndexAsc(parentType, lessonId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    @Transactional(readOnly = true)
    public TaskGroup findTaskGroupById(Long id) {
        return taskGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TaskGroup not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public TaskGroup findTaskGroupByIdWithQuestions(Long id) {
        // Requires implementing findByIdWithQuestions in repository
        return taskGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TaskGroup not found with id: " + id));
    }

    private Integer getNextOrderIndex(ParentType parentType, Long lessonId) {
        return taskGroupRepository
                .findMaxOrderIndex(parentType, lessonId)
                .orElse(0) + 1;
    }

    private TaskGroupResponseDTO toResponseDTO(TaskGroup taskGroup) {
        return TaskGroupResponseDTO.builder()
                .id(taskGroup.getId())
                .parentType(taskGroup.getParentType())
                .parentId(taskGroup.getParentId())
                .taskName(taskGroup.getTaskName())
                .instruction(taskGroup.getInstruction())
                .orderIndex(taskGroup.getOrderIndex())
                .questionCount(taskGroup.getQuestionCount())
                .totalPoints(taskGroup.getTotalPoints())
                .createdAt(taskGroup.getCreatedAt())
                .build();
    }
}