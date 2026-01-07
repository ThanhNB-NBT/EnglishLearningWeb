package com.thanhnb.englishlearning.controller;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.question.request.CreateTaskGroupDTO;
import com.thanhnb.englishlearning.dto.question.response.TaskGroupResponseDTO;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.service.question.TaskGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý TaskGroup (ADMIN & TEACHER)
 * TaskGroup dùng để nhóm các questions theo task/phần trong bài học
 */
@RestController
@RequestMapping("/api/admin/task-groups")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
@Tag(name = "Task Groups Management", description = "API quản lý nhóm task cho questions (ADMIN & TEACHER)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class TaskGroupController {

        private final TaskGroupService taskGroupService;

        // ==================== CREATE ====================

        @PostMapping("/{parentType}/{lessonId}")
        @Operation(summary = "Tạo TaskGroup mới", description = "Tạo một nhóm task mới cho lesson. " +
                        "ADMIN có quyền tạo cho mọi lesson, TEACHER chỉ tạo cho lesson được phân công.")
        public ResponseEntity<CustomApiResponse<TaskGroupResponseDTO>> createTaskGroup(
                        @Parameter(description = "Loại module: GRAMMAR, LISTENING, READING", required = true) @PathVariable ParentType parentType,

                        @Parameter(description = "ID của lesson", required = true) @PathVariable Long lessonId,

                        @Parameter(description = "Thông tin TaskGroup cần tạo", required = true) @Valid @RequestBody CreateTaskGroupDTO dto) {
                try {
                        log.info("Creating TaskGroup: parentType={}, lessonId={}, taskName={}",
                                        parentType, lessonId, dto.getTaskName());

                        TaskGroupResponseDTO created = taskGroupService.createTaskGroup(parentType, lessonId, dto);

                        log.info("TaskGroup created successfully: id={}", created.getId());

                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(
                                                        created,
                                                        "Tạo TaskGroup thành công"));

                } catch (IllegalArgumentException e) {
                        log.error("Validation error: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));

                } catch (Exception e) {
                        log.error("Error creating TaskGroup: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.error(500, "Lỗi tạo TaskGroup: " + e.getMessage()));
                }
        }

        // ==================== READ ====================

        @GetMapping("/{parentType}/{lessonId}")
        @Operation(summary = "Lấy tất cả TaskGroups của một lesson", description = "Lấy danh sách TaskGroups được sắp xếp theo orderIndex")
        public ResponseEntity<CustomApiResponse<List<TaskGroupResponseDTO>>> getTaskGroupsByLesson(
                        @Parameter(description = "Loại module", required = true) @PathVariable ParentType parentType,

                        @Parameter(description = "ID của lesson", required = true) @PathVariable Long lessonId) {
                try {
                        log.debug("Fetching TaskGroups: parentType={}, lessonId={}", parentType, lessonId);

                        List<TaskGroupResponseDTO> taskGroups = taskGroupService.getTaskGroupsByLesson(parentType,
                                        lessonId);

                        return ResponseEntity.ok(CustomApiResponse.success(
                                        taskGroups,
                                        "Lấy danh sách TaskGroups thành công"));

                } catch (Exception e) {
                        log.error("Error fetching TaskGroups: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.error(500, "Lỗi: " + e.getMessage()));
                }
        }

        @GetMapping("/{id}")
        @Operation(summary = "Lấy TaskGroup theo ID", description = "Lấy chi tiết một TaskGroup cụ thể")
        public ResponseEntity<CustomApiResponse<TaskGroupResponseDTO>> getTaskGroupById(
                        @Parameter(description = "ID của TaskGroup", required = true) @PathVariable Long id) {
                try {
                        log.debug("Fetching TaskGroup: id={}", id);

                        TaskGroupResponseDTO taskGroup = taskGroupService.getTaskGroupById(id);

                        return ResponseEntity.ok(CustomApiResponse.success(
                                        taskGroup,
                                        "Lấy TaskGroup thành công"));

                } catch (Exception e) {
                        log.error("Error fetching TaskGroup: ", e);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(CustomApiResponse.error(404,
                                                        "Không tìm thấy TaskGroup: " + e.getMessage()));
                }
        }

        // ==================== UPDATE ====================

        @PutMapping("/{id}")
        @Operation(summary = "Cập nhật TaskGroup", description = "Cập nhật thông tin TaskGroup. " +
                        "ADMIN có quyền cập nhật mọi TaskGroup, TEACHER chỉ cập nhật TaskGroup trong lesson được phân công.")
        public ResponseEntity<CustomApiResponse<TaskGroupResponseDTO>> updateTaskGroup(
                        @Parameter(description = "ID của TaskGroup cần cập nhật", required = true) @PathVariable Long id,

                        @Parameter(description = "Thông tin cập nhật", required = true) @Valid @RequestBody CreateTaskGroupDTO dto) {
                try {
                        log.info("Updating TaskGroup: id={}, newTaskName={}", id, dto.getTaskName());

                        TaskGroupResponseDTO updated = taskGroupService.updateTaskGroup(id, dto);

                        log.info("TaskGroup updated successfully: id={}", updated.getId());

                        return ResponseEntity.ok(CustomApiResponse.success(
                                        updated,
                                        "Cập nhật TaskGroup thành công"));

                } catch (IllegalArgumentException e) {
                        log.error("Validation error: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));

                } catch (Exception e) {
                        log.error("Error updating TaskGroup: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.error(500,
                                                        "Lỗi cập nhật TaskGroup: " + e.getMessage()));
                }
        }

        // ==================== DELETE ====================

        @DeleteMapping("/{id}")
        @Operation(summary = "Xóa TaskGroup", description = "Xóa TaskGroup và TẤT CẢ questions bên trong nó. " +
                        "⚠️ Hành động này không thể hoàn tác! " +
                        "ADMIN có quyền xóa mọi TaskGroup, TEACHER chỉ xóa TaskGroup trong lesson được phân công.")
        public ResponseEntity<CustomApiResponse<Void>> deleteTaskGroup(
                        @Parameter(description = "ID của TaskGroup cần xóa", required = true) @PathVariable Long id) {
                try {
                        log.warn("Deleting TaskGroup: id={}", id);

                        taskGroupService.deleteTaskGroup(id);

                        log.info("TaskGroup deleted successfully: id={}", id);

                        return ResponseEntity.ok(CustomApiResponse.success(
                                        null,
                                        "Xóa TaskGroup và tất cả questions bên trong thành công"));

                } catch (IllegalArgumentException e) {
                        log.error("Validation error: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));

                } catch (Exception e) {
                        log.error("Error deleting TaskGroup: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.error(500, "Lỗi xóa TaskGroup: " + e.getMessage()));
                }
        }
}