package com.thanhnb.englishlearning.controller.user;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.topic.TeacherAssignmentDto;
import com.thanhnb.englishlearning.dto.topic.TopicDto;
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

/**
 * ✅ Teacher Portal Controller
 * Purpose: Teachers view their assignment records (for UI display)
 * Note: Assignment records are for TRACKING/UI, not access control
 */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Portal", description = "APIs for teachers to view their assignments")
@Slf4j
public class TeacherController {

    private final TeacherAssignmentService assignmentService;
    private final UserService userService;

    // ==================== VIEW ASSIGNMENTS ====================

    @GetMapping("/my-assignments")
    @Operation(summary = "Get my assignments", 
               description = "Get all topic assignments for current teacher (for UI display)")
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

    @GetMapping("/my-topics")
    @Operation(summary = "Get my assigned topics", 
               description = "Get topics assigned to current teacher (optional filter by module)")
    public ResponseEntity<CustomApiResponse<List<TopicDto>>> getMyTopics(
            @RequestParam(required = false) ModuleType moduleType) {
        try {
            User teacher = userService.getCurrentUser();
            
            log.info("Teacher {} requesting assigned topics (moduleType: {})", 
                    teacher.getUsername(), moduleType);
            
            List<TopicDto> topics;
            if (moduleType != null) {
                topics = assignmentService.getAssignedTopicsForTeacher(teacher.getId(), moduleType);
            } else {
                // Get all assignments and extract topics
                List<TeacherAssignmentDto> assignments = assignmentService.getAssignmentsByTeacher(teacher.getId());
                topics = List.of(); // Can map from assignments if needed
            }
            
            return ResponseEntity.ok(CustomApiResponse.success(
                    topics,
                    "Lấy danh sách chủ đề thành công"));
        } catch (Exception e) {
            log.error("Failed to get teacher topics: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(CustomApiResponse.error(500, e.getMessage()));
        }
    }

    @GetMapping("/my-topics/paginated")
    @Operation(summary = "Get my assigned topics (paginated)", 
               description = "Get assigned topics with pagination")
    public ResponseEntity<CustomApiResponse<PaginatedResponse<TopicDto>>> getMyTopicsPaginated(
            @RequestParam ModuleType moduleType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            User teacher = userService.getCurrentUser();
            
            log.info("Teacher {} requesting paginated topics (module: {}, page: {}, size: {})", 
                    teacher.getUsername(), moduleType, page, size);
            
            PaginatedResponse<TopicDto> response = assignmentService.getAssignedTopicsWithPagination(
                    teacher.getId(), 
                    moduleType, 
                    page, 
                    size);
            
            return ResponseEntity.ok(CustomApiResponse.success(
                    response,
                    "Lấy danh sách chủ đề thành công"));
        } catch (Exception e) {
            log.error("Failed to get paginated topics: {}", e.getMessage(), e);
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
                "assignmentCount", assignmentService.countAssignmentsByTeacher(teacher.getId())
            );
            
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
    @Operation(summary = "Check if assigned to topic", 
               description = "Check if current teacher has assignment record for a topic")
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
                "listeningTopics", listeningCount
            );
            
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