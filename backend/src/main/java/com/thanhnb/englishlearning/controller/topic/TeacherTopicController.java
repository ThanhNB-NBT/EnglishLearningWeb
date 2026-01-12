package com.thanhnb.englishlearning.controller.topic;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.topic.TopicDto;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.service.topic.TeacherAssignmentService;
import com.thanhnb.englishlearning.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ✅ Teacher Topic Controller
 * Endpoints for teachers to view their assigned topics and check assignments
 * 
 * Note: This works together with ModuleTopicController
 * - ModuleTopicController: /api/admin/topics/{moduleType} - For listing topics
 * - TeacherTopicController: /api/teacher/* - For checking assignments
 */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Topics", description = "APIs for teachers to manage their assigned topics")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class TeacherTopicController {

    private final TeacherAssignmentService assignmentService;
    private final UserService userService;

    /**
     * ✅ Get paginated topics assigned to current teacher
     * GET
     * /api/teacher/my-topics/paginated?moduleType=GRAMMAR&page=0&size=10&sort=orderIndex:asc
     * 
     * Called by: topicTeacher.js store line 99
     */
    @GetMapping("/my-topics/paginated")
    @Operation(summary = "Get my assigned topics (paginated)", description = "Get topics assigned to current teacher with pagination")
    public ResponseEntity<CustomApiResponse<PaginatedResponse<TopicDto>>> getMyTopicsPaginated(
            @RequestParam(required = true) ModuleType moduleType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderIndex:asc") String sort) {

        try {
            User teacher = userService.getCurrentUser();

            log.info("📄 Teacher {} requesting paginated topics: module={}, page={}, size={}",
                    teacher.getUsername(), moduleType, page, size);

            // Frontend sends page=0 (0-indexed), backend expects page=1 (1-indexed)
            PaginatedResponse<TopicDto> response = assignmentService.getAssignedTopicsWithPagination(
                    teacher.getId(),
                    moduleType,
                    page + 1, // Convert to 1-indexed
                    size);

            // Adjust page back to 0-indexed for frontend
            response.setPage(page);

            log.info("✅ Teacher {} loaded {} assigned topics (page {}, total {})",
                    teacher.getUsername(), response.getContent().size(), page, response.getTotalElements());

            return ResponseEntity.ok(CustomApiResponse.success(
                    response,
                    "Lấy danh sách chủ đề thành công"));

        } catch (Exception e) {
            log.error("❌ Failed to get teacher topics: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(CustomApiResponse.error(500, "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * ✅ Get all topics assigned to current teacher (no pagination)
     * GET /api/teacher/my-topics?moduleType=GRAMMAR
     * 
     * Called by: topicTeacher.js store line 28 (if needed)
     */
    @GetMapping("/my-topics")
    @Operation(summary = "Get my assigned topics (all)", description = "Get all topics assigned to current teacher for a module")
    public ResponseEntity<CustomApiResponse<List<TopicDto>>> getMyTopicsByModule(
            @RequestParam(required = true) ModuleType moduleType) {

        try {
            User teacher = userService.getCurrentUser();

            log.info("📄 Teacher {} requesting all topics for module {}",
                    teacher.getUsername(), moduleType);

            List<TopicDto> topics = assignmentService.getAssignedTopicsForTeacher(
                    teacher.getId(),
                    moduleType);

            log.info("✅ Teacher {} loaded {} assigned topics",
                    teacher.getUsername(), topics.size());

            return ResponseEntity.ok(CustomApiResponse.success(
                    topics,
                    "Lấy danh sách chủ đề thành công"));

        } catch (Exception e) {
            log.error("❌ Failed to get teacher topics: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(CustomApiResponse.error(500, "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * ✅ Check if teacher is assigned to a specific topic
     * GET /api/teacher/check-assignment/{topicId}
     * 
     * Called by: topicTeacher.js store line 264
     */
    @GetMapping("/check-assignment/{topicId}")
    @Operation(summary = "Check if assigned to topic", description = "Check if current teacher is assigned to a specific topic")
    public ResponseEntity<CustomApiResponse<Boolean>> checkAssignment(@PathVariable Long topicId) {
        try {
            User teacher = userService.getCurrentUser();

            boolean isAssigned = assignmentService.isTeacherAssignedToTopic(
                    teacher.getId(),
                    topicId);

            log.info("Teacher {} assignment check for topic {}: {}",
                    teacher.getUsername(), topicId, isAssigned);

            return ResponseEntity.ok(CustomApiResponse.success(
                    isAssigned,
                    isAssigned ? "Bạn được phân công quản lý chủ đề này" : "Bạn chưa được phân công"));

        } catch (Exception e) {
            log.error("❌ Error checking assignment: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(CustomApiResponse.error(500, "Lỗi: " + e.getMessage()));
        }
    }
}