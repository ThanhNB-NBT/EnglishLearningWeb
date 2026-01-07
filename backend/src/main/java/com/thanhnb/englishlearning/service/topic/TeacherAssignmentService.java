package com.thanhnb.englishlearning.service.topic;

import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.topic.TeacherAssignmentDto;
import com.thanhnb.englishlearning.dto.topic.TopicDto;
import com.thanhnb.englishlearning.dto.topic.request.AssignTeacherRequest;
import com.thanhnb.englishlearning.entity.topic.TeacherTopicAssignment;
import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.UserRole;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.topic.TeacherTopicAssignmentRepository;
import com.thanhnb.englishlearning.repository.topic.TopicRepository;
import com.thanhnb.englishlearning.repository.user.UserRepository;
import com.thanhnb.englishlearning.util.PaginationHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ✅ TeacherAssignmentService - FOR TRACKING ONLY
 * 
 * Purpose: Manage teacher-topic assignment records
 * Note: NOT for access control - Spring Security handles that
 * 
 * Use cases:
 * - Admin assigns teachers to topics (tracking)
 * - View assignment history
 * - Statistics & reporting
 * - UI display (show which teacher manages which topic)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherAssignmentService {

    private final TeacherTopicAssignmentRepository assignmentRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    // ==================== ASSIGNMENT CRUD ====================

    @Transactional
    public TeacherAssignmentDto assignTeacherToTopic(AssignTeacherRequest request, Long adminId) {
        log.info("Assigning teacher ID {} to topic ID {} by admin ID {}",
                request.getTeacherId(), request.getTopicId(), adminId);

        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with ID: " + request.getTeacherId()));

        if (teacher.getRole() != UserRole.TEACHER) {
            throw new IllegalArgumentException(
                    "User ID " + request.getTeacherId() + " is not a TEACHER");
        }

        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Topic not found with ID: " + request.getTopicId()));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Admin not found with ID: " + adminId));

        TeacherTopicAssignment assignment = assignmentRepository
                .findByTeacherAndTopic(teacher, topic)
                .orElse(null);

        if (assignment != null) {
            if (assignment.getIsActive()) {
                throw new IllegalArgumentException(
                        "Teacher is already assigned to this topic");
            }

            log.info("Reactivating existing assignment ID: {}", assignment.getId());
            assignment.setIsActive(true);
            assignment.setAssignedBy(admin);
        } else {
            log.info("Creating new assignment for teacher {} and topic {}",
                    teacher.getId(), topic.getId());
            assignment = TeacherTopicAssignment.builder()
                    .teacher(teacher)
                    .topic(topic)
                    .assignedBy(admin)
                    .isActive(true)
                    .build();
        }

        TeacherTopicAssignment saved = assignmentRepository.save(assignment);
        log.info("Assignment saved successfully: ID={}", saved.getId());

        return mapToDto(saved);
    }

    @Transactional
    public void deactivateAssignment(Long assignmentId) {
        log.info("Deactivating assignment ID: {}", assignmentId);

        TeacherTopicAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found with ID: " + assignmentId));

        assignment.deactivate();
        assignmentRepository.save(assignment);
        log.info("Assignment deactivated successfully: ID={}", assignmentId);
    }

    @Transactional
    public void revokeAssignmentByTeacherAndTopic(Long teacherId, Long topicId) {
        log.info("Revoking assignment for teacher ID {} and topic ID {}", teacherId, topicId);

        TeacherTopicAssignment assignment = assignmentRepository
                .findByTeacherIdAndTopicId(teacherId, topicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found for teacher ID " + teacherId +
                                " and topic ID " + topicId));

        assignmentRepository.delete(assignment);
        log.info("Assignment revoked successfully (hard deleted)");
    }

    // ==================== QUERIES ====================

    @Transactional(readOnly = true)
    public List<TeacherAssignmentDto> getAssignmentsByTeacher(Long teacherId) {
        List<TeacherTopicAssignment> assignments = assignmentRepository
                .findByTeacherIdAndIsActiveTrueOrderByModuleTypeAscTopicOrderIndexAsc(teacherId);

        return assignments.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeacherAssignmentDto> getAssignmentsByTopic(Long topicId) {
        List<TeacherTopicAssignment> assignments = assignmentRepository.findByTopicIdAndIsActiveTrue(topicId);

        return assignments.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeacherAssignmentDto getAssignmentById(Long assignmentId) {
        TeacherTopicAssignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found with ID: " + assignmentId));

        return mapToDto(assignment);
    }

    @Transactional(readOnly = true)
    public List<TeacherAssignmentDto> getAllActiveAssignments() {
        List<TeacherTopicAssignment> assignments = assignmentRepository.findByIsActiveTrueOrderByAssignedAtDesc();

        return assignments.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TopicDto> getAssignedTopicsForTeacher(Long teacherId, ModuleType moduleType) {
        List<TeacherTopicAssignment> assignments = assignmentRepository.findByTeacherIdAndModuleType(teacherId,
                moduleType);

        return assignments.stream()
                .map(assignment -> mapTopicToDto(assignment.getTopic()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<TopicDto> getAssignedTopicsWithPagination(
            Long teacherId,
            ModuleType moduleType,
            int page,
            int size) {

        Pageable pageable = PaginationHelper.createPageable(page, size, "topic.orderIndex");

        Page<Topic> topicPage = assignmentRepository.findTopicsByTeacherAndModule(
                teacherId,
                moduleType,
                pageable);

        return PaginationHelper.toPaginatedResponse(topicPage, this::mapTopicToDto);
    }

    // ==================== BATCH OPERATIONS ====================

    @Transactional
    public int revokeAllAssignmentsByTeacher(Long teacherId) {
        log.info("Revoking all assignments for teacher ID: {}", teacherId);
        return assignmentRepository.deactivateAllByTeacherId(teacherId);
    }

    @Transactional
    public int revokeAllAssignmentsByTopic(Long topicId) {
        log.info("Revoking all assignments for topic ID: {}", topicId);
        return assignmentRepository.deactivateAllByTopicId(topicId);
    }

    // ==================== STATISTICS ====================

    @Transactional(readOnly = true)
    public boolean isTeacherAssignedToTopic(Long teacherId, Long topicId) {
        return assignmentRepository.existsByTeacherIdAndTopicIdAndIsActiveTrue(teacherId, topicId);
    }

    @Transactional(readOnly = true)
    public long countAssignmentsByTeacher(Long teacherId) {
        return assignmentRepository.countByTeacherIdAndIsActiveTrue(teacherId);
    }

    @Transactional(readOnly = true)
    public long countAssignmentsByTopic(Long topicId) {
        return assignmentRepository.countByTopicIdAndIsActiveTrue(topicId);
    }

    @Transactional(readOnly = true)
    public List<Long> getTeacherIdsByTopic(Long topicId) {
        return assignmentRepository.findTeacherIdsByTopicId(topicId);
    }

    @Transactional(readOnly = true)
    public List<Long> getTopicIdsByTeacher(Long teacherId) {
        return assignmentRepository.findTopicIdsByTeacherId(teacherId);
    }

    // ==================== HELPER METHODS ====================

    private TeacherAssignmentDto mapToDto(TeacherTopicAssignment assignment) {
        return TeacherAssignmentDto.builder()
                .id(assignment.getId())
                .teacherId(assignment.getTeacher().getId())
                .teacherUsername(assignment.getTeacher().getUsername())
                .teacherFullName(assignment.getTeacher().getFullName())
                .topicId(assignment.getTopic().getId())
                .topicName(assignment.getTopic().getName())
                .moduleType(assignment.getModuleType())
                .isActive(assignment.getIsActive())
                .assignedAt(assignment.getAssignedAt())
                .assignedBy(assignment.getAssignedBy() != null ? assignment.getAssignedBy().getId() : null)
                .assignedByUsername(
                        assignment.getAssignedBy() != null ? assignment.getAssignedBy().getUsername() : null)
                .build();
    }

    private TopicDto mapTopicToDto(Topic topic) {
        return TopicDto.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .moduleType(topic.getModuleType())
                .orderIndex(topic.getOrderIndex())
                .isActive(topic.getIsActive())
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .build();
    }
}