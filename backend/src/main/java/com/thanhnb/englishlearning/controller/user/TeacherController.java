package com.thanhnb.englishlearning.controller.user;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.topic.TeacherAssignmentDto;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.service.topic.TeacherAssignmentService;
import com.thanhnb.englishlearning.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/portal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Portal", description = "APIs for teachers to view their assignments")
@Slf4j
public class TeacherController {

    private final TeacherAssignmentService assignmentService;
    private final UserService userService;

    // ==================== VIEW ASSIGNMENTS ====================

    @GetMapping("/my-assignments")
    @Operation(summary = "Get my assignments", description = "Get all topic assignments for current teacher (for UI display)")
    public ResponseEntity<CustomApiResponse<List<TeacherAssignmentDto>>> getMyAssignments() {
        try {
            User teacher = userService.getCurrentUser();

            log.info("Teacher {} requesting their assignments", teacher.getUsername());

            List<TeacherAssignmentDto> assignments = assignmentService.getAssignmentsByTeacher(teacher.getId());

            return ResponseEntity.ok(CustomApiResponse.success(
                    assignments,
                    "Lấy danh sách phân công thành công"));
        } catch (Exception e) {
            log.error("Failed to get teacher assignments: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(CustomApiResponse.error(500, e.getMessage()));
        }
    }

    // ==================== PROFILE & STATS ====================

    @GetMapping("/profile")
    @Operation(summary = "Get teacher profile")
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> getProfile() {
        try {
            User teacher = userService.getCurrentUser();

            Map<String, Object> profileData = Map.of(
                    "id", teacher.getId(),
                    "username", teacher.getUsername(),
                    "email", teacher.getEmail(),
                    "fullName", teacher.getFullName(),
                    "role", teacher.getRole().name(),
                    "isActive", teacher.getIsActive(),
                    "isVerified", teacher.getIsVerified(),
                    "assignmentCount", assignmentService.countAssignmentsByTeacher(teacher.getId()));

            return ResponseEntity.ok(CustomApiResponse.success(
                    profileData,
                    "Lấy thông tin giáo viên thành công"));
        } catch (Exception e) {
            log.error("Failed to get teacher profile: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(CustomApiResponse.error(500, e.getMessage()));
        }
    }

    @GetMapping("/check-assignment/{topicId}")
    @Operation(summary = "Check if assigned to topic", description = "Check if current teacher has assignment record for a topic")
    public ResponseEntity<CustomApiResponse<Boolean>> checkAssignment(@PathVariable Long topicId) {
        try {
            User teacher = userService.getCurrentUser();

            boolean isAssigned = assignmentService.isTeacherAssignedToTopic(teacher.getId(), topicId);

            return ResponseEntity.ok(CustomApiResponse.success(
                    isAssigned,
                    isAssigned ? "Bạn được phân công quản lý chủ đề này" : "Bạn chưa được phân công"));
        } catch (Exception e) {
            log.error("Failed to check assignment: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(CustomApiResponse.error(500, e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get teacher statistics")
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> getTeacherStats() {
        try {
            User teacher = userService.getCurrentUser();

            List<TeacherAssignmentDto> assignments = assignmentService.getAssignmentsByTeacher(teacher.getId());

            long grammarCount = assignments.stream()
                    .filter(a -> a.getModuleType() == ModuleType.GRAMMAR)
                    .count();
            long readingCount = assignments.stream()
                    .filter(a -> a.getModuleType() == ModuleType.READING)
                    .count();
            long listeningCount = assignments.stream()
                    .filter(a -> a.getModuleType() == ModuleType.LISTENING)
                    .count();

            Map<String, Object> stats = Map.of(
                    "totalAssignments", assignments.size(),
                    "grammarTopics", grammarCount,
                    "readingTopics", readingCount,
                    "listeningTopics", listeningCount);

            return ResponseEntity.ok(CustomApiResponse.success(
                    stats,
                    "Lấy thống kê thành công"));
        } catch (Exception e) {
            log.error("Failed to get teacher stats: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(CustomApiResponse.error(500, e.getMessage()));
        }
    }
}