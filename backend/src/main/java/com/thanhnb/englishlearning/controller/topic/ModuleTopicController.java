package com.thanhnb.englishlearning.controller.topic;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.topic.TopicDto;
import com.thanhnb.englishlearning.dto.topic.request.CreateTopicRequest;
import com.thanhnb.englishlearning.dto.topic.request.UpdateTopicRequest;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.service.topic.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/topics")
@RequiredArgsConstructor
@Tag(name = "Topic Management", description = "Admin & Teacher manage Topics")
public class ModuleTopicController {

    private final TopicService topicService;

    // ==================== READ (ADMIN & TEACHER) ====================

    /**
     * ✅ Get all topics by module
     * ADMIN: Can see all topics
     * TEACHER: Can see all topics (read-only for teachers)
     */
    @GetMapping("/{moduleType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get all topics by module", description = "Both Admin and Teacher can view all topics")
    public ResponseEntity<CustomApiResponse<PaginatedResponse<TopicDto>>> getTopicsByModule(
            @PathVariable ModuleType moduleType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderIndex") String sort) {

        PaginatedResponse<TopicDto> response = topicService.getAllTopics(moduleType, page, size, sort);

        return ResponseEntity.ok(CustomApiResponse.success(
                response,
                "Lấy danh sách topic thành công"));
    }

    /**
     * ✅ Get topic by ID
     */
    @GetMapping("/detail/{topicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get topic details by ID")
    public ResponseEntity<CustomApiResponse<TopicDto>> getTopicById(@PathVariable Long topicId) {
        TopicDto topic = topicService.getTopicById(topicId);
        return ResponseEntity.ok(CustomApiResponse.success(topic, "Lấy topic thành công"));
    }

    // ==================== WRITE (ADMIN ONLY) ====================

    /**
     * ✅ Create topic - ADMIN ONLY
     */
    @PostMapping("/{moduleType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Topic (ADMIN ONLY)", description = "Only Admin can create topics")
    public ResponseEntity<CustomApiResponse<TopicDto>> createTopic(
            @PathVariable ModuleType moduleType,
            @Valid @RequestBody CreateTopicRequest request) {

        TopicDto topic = topicService.createTopic(request, moduleType);
        return ResponseEntity.ok(CustomApiResponse.success(topic, "Tạo topic thành công"));
    }

    /**
     * ✅ Update topic - ADMIN ONLY
     */
    @PutMapping("/{topicId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Topic (ADMIN ONLY)", description = "Only Admin can update topics")
    public ResponseEntity<CustomApiResponse<TopicDto>> updateTopic(
            @PathVariable Long topicId,
            @Valid @RequestBody UpdateTopicRequest request) {

        TopicDto topic = topicService.updateTopic(topicId, request);

        return ResponseEntity.ok(CustomApiResponse.success(
                topic,
                "Cập nhật thông tin topic thành công"));
    }

    /**
     * ✅ Delete topic - ADMIN ONLY
     */
    @DeleteMapping("/{topicId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Topic (ADMIN ONLY)", description = "Only Admin can delete topics")
    public ResponseEntity<CustomApiResponse<String>> deleteTopic(@PathVariable Long topicId) {
        topicService.deleteTopic(topicId);
        return ResponseEntity.ok(CustomApiResponse.success("Đã xóa topic và sắp xếp lại danh sách", "Thành công"));
    }

    /**
     * ✅ Toggle status - ADMIN ONLY
     */
    @PatchMapping("/{topicId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle Status (ADMIN ONLY)", description = "Only Admin can toggle topic status")
    public ResponseEntity<CustomApiResponse<TopicDto>> toggleStatus(@PathVariable Long topicId) {
        TopicDto topic = topicService.toggleStatus(topicId);
        String status = Boolean.TRUE.equals(topic.getIsActive()) ? "Kích hoạt" : "Vô hiệu hóa";
        return ResponseEntity.ok(CustomApiResponse.success(topic, "Đã " + status + " topic thành công"));
    }

    /**
     * ✅ Fix order indexes - ADMIN ONLY
     */
    @PostMapping("/{moduleType}/fix-index")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fix Order Indexes (ADMIN ONLY)", description = "Only Admin can fix order indexes")
    public ResponseEntity<CustomApiResponse<String>> fixOrderIndexes(@PathVariable ModuleType moduleType) {
        topicService.fixOrderIndexes(moduleType);
        return ResponseEntity.ok(CustomApiResponse.success("Đã chuẩn hóa thứ tự topic", "Thành công"));
    }
}