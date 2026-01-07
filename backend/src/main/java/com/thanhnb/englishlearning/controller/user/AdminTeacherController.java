package com.thanhnb.englishlearning.controller.user;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.topic.TeacherAssignmentDto;
import com.thanhnb.englishlearning.dto.topic.request.AssignTeacherRequest;
import com.thanhnb.englishlearning.service.topic.TeacherAssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ✅ Admin controller for managing teacher-topic assignments
 * Purpose: Track which teachers are assigned to which topics (for UI/reporting)
 * Note: This is for TRACKING ONLY, not access control
 */
@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Teacher Assignment", description = "APIs for admin to manage teacher assignments")
@Slf4j
public class AdminTeacherController {

    private final TeacherAssignmentService assignmentService;

    // ==================== ASSIGNMENT MANAGEMENT ====================

    @PostMapping("/assign")
    @Operation(summary = "Assign teacher to topic", description = "Create assignment record (for tracking)")
    public ResponseEntity<CustomApiResponse<TeacherAssignmentDto>> assignTeacher(
            @Valid @RequestBody AssignTeacherRequest request,
            @RequestAttribute("userId") Long adminId) {

        try {
            TeacherAssignmentDto assignment = assignmentService.assignTeacherToTopic(request, adminId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CustomApiResponse.created(assignment, "Phân công giáo viên thành công"));
        } catch (Exception e) {
            log.error("Failed to assign teacher: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest(e.getMessage()));
        }
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @Operation(summary = "Deactivate teacher assignment", description = "Soft delete assignment record")
    public ResponseEntity<CustomApiResponse<String>> deactivateAssignment(
            @PathVariable Long assignmentId) {

        try {
            assignmentService.deactivateAssignment(assignmentId);

            return ResponseEntity.ok(CustomApiResponse.success(
                    "Đã hủy phân công giáo viên",
                    "Hủy phân công thành công"));
        } catch (Exception e) {
            log.error("Failed to deactivate assignment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest(e.getMessage()));
        }
    }

    @DeleteMapping("/assignments/{teacherId}/{topicId}/hard-delete")
    @Operation(summary = "Hard delete teacher assignment", description = "Permanently delete assignment (use with caution)")
    public ResponseEntity<CustomApiResponse<String>> hardDeleteAssignment(
            @PathVariable Long teacherId,
            @PathVariable Long topicId) {

        try {
            assignmentService.revokeAssignmentByTeacherAndTopic(teacherId, topicId);

            return ResponseEntity.ok(CustomApiResponse.success(
                    "Đã xóa vĩnh viễn phân công",
                    "Xóa phân công thành công"));
        } catch (Exception e) {
            log.error("Failed to hard delete assignment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest(e.getMessage()));
        }
    }

    // ==================== QUERY ASSIGNMENTS ====================

    @GetMapping("/{teacherId}/assignments")
    @Operation(summary = "Get teacher assignments", description = "Get all topic assignments for a teacher")
    public ResponseEntity<CustomApiResponse<List<TeacherAssignmentDto>>> getTeacherAssignments(
            @PathVariable Long teacherId) {

        try {
            List<TeacherAssignmentDto> assignments = assignmentService.getAssignmentsByTeacher(teacherId);

            return ResponseEntity.ok(CustomApiResponse.success(
                    assignments,
                    "Lấy danh sách phân công thành công"));
        } catch (Exception e) {
            log.error("Failed to get teacher assignments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/topics/{topicId}/teachers")
    @Operation(summary = "Get teachers for topic", description = "Get all teachers assigned to a specific topic")
    public ResponseEntity<CustomApiResponse<List<TeacherAssignmentDto>>> getTopicTeachers(
            @PathVariable Long topicId) {

        try {
            List<TeacherAssignmentDto> assignments = assignmentService.getAssignmentsByTopic(topicId);

            return ResponseEntity.ok(CustomApiResponse.success(
                    assignments,
                    "Lấy danh sách giáo viên thành công"));
        } catch (Exception e) {
            log.error("Failed to get topic teachers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest(e.getMessage()));
        }
    }

    @GetMapping("/assignments")
    @Operation(summary = "Get all assignments", description = "Get all active teacher-topic assignments")
    public ResponseEntity<CustomApiResponse<List<TeacherAssignmentDto>>> getAllAssignments() {
        try {
            List<TeacherAssignmentDto> assignments = assignmentService.getAllActiveAssignments();

            return ResponseEntity.ok(CustomApiResponse.success(
                    assignments,
                    "Lấy danh sách phân công thành công"));
        } catch (Exception e) {
            log.error("Failed to get all assignments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomApiResponse.error(500, e.getMessage()));
        }
    }

    // ==================== BATCH OPERATIONS ====================

    @DeleteMapping("/{teacherId}/assignments")
    @Operation(summary = "Revoke all teacher assignments", description = "Deactivate all topic assignments for a teacher")
    public ResponseEntity<CustomApiResponse<String>> revokeAllTeacherAssignments(
            @PathVariable Long teacherId) {

        try {
            int count = assignmentService.revokeAllAssignmentsByTeacher(teacherId);

            return ResponseEntity.ok(CustomApiResponse.success(
                    "Đã hủy " + count + " phân công",
                    "Hủy tất cả phân công thành công"));
        } catch (Exception e) {
            log.error("Failed to revoke all teacher assignments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest(e.getMessage()));
        }
    }

    @DeleteMapping("/topics/{topicId}/assignments")
    @Operation(summary = "Revoke all topic assignments", description = "Deactivate all teacher assignments for a topic")
    public ResponseEntity<CustomApiResponse<String>> revokeAllTopicAssignments(
            @PathVariable Long topicId) {

        try {
            int count = assignmentService.revokeAllAssignmentsByTopic(topicId);

            return ResponseEntity.ok(CustomApiResponse.success(
                    "Đã hủy " + count + " phân công",
                    "Hủy tất cả phân công thành công"));
        } catch (Exception e) {
            log.error("Failed to revoke all topic assignments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest(e.getMessage()));
        }
    }

    // ==================== STATISTICS ====================

    @GetMapping("/{teacherId}/stats")
    @Operation(summary = "Get teacher assignment stats")
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> getTeacherStats(
            @PathVariable Long teacherId) {
        try {
            long count = assignmentService.countAssignmentsByTeacher(teacherId);
            List<Long> topicIds = assignmentService.getTopicIdsByTeacher(teacherId);

            Map<String, Object> stats = Map.of(
                    "totalAssignments", count,
                    "assignedTopicIds", topicIds);

            return ResponseEntity.ok(CustomApiResponse.success(stats, "Lấy thống kê thành công"));
        } catch (Exception e) {
            log.error("Failed to get teacher stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CustomApiResponse.badRequest(e.getMessage()));
        }
    }
}