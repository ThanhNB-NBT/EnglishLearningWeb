package com.thanhnb.englishlearning.controller.grammar;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.dto.question.response.TaskGroupedQuestionsDTO;
import com.thanhnb.englishlearning.service.grammar.GrammarLessonService;
import com.thanhnb.englishlearning.service.grammar.GrammarQuestionService;
import com.thanhnb.englishlearning.util.PaginationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/admin/grammar")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
@Tag(name = "Grammar Management", description = "API quản lý ngữ pháp (ADMIN và TEACHER)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class GrammarAdminController {

        private final GrammarLessonService grammarLessonService;
        private final GrammarQuestionService grammarQuestionService;

        // ═══════════════════════════════════════════════════════════════════════════
        // LESSON MANAGEMENT
        // ═══════════════════════════════════════════════════════════════════════════

        @GetMapping("/topics/{topicId}/lessons")
        @Operation(summary = "Lấy danh sách lessons theo topic (có phân trang)")
        @PreAuthorize("permitAll()")
        public ResponseEntity<CustomApiResponse<PaginatedResponse<GrammarLessonDTO>>> getLessonsByTopic(
                        @PathVariable Long topicId,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "orderIndex: asc") String sort) {

                try {
                        // Tạo Pageable từ PaginationHelper
                        Pageable pageable = PaginationHelper.createPageable(page, size, sort);

                        // Lấy Page từ service
                        Page<GrammarLessonDTO> lessonPage = grammarLessonService.getLessonsByTopic(topicId, pageable);

                        // Convert sang PaginatedResponse bằng factory method
                        PaginatedResponse<GrammarLessonDTO> response = PaginatedResponse.of(lessonPage);

                        return ResponseEntity.ok(CustomApiResponse.success(
                                        response,
                                        "Lấy danh sách lessons thành công"));

                } catch (Exception e) {
                        log.error("Error getting lessons: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.error(500, "Lỗi:  " + e.getMessage()));
                }
        }

        /**
         * Get lesson by ID
         * PUBLIC READ ACCESS
         */
        @GetMapping("/lessons/{lessonId}")
        @Operation(summary = "Lấy chi tiết lesson theo ID")
        @JsonView(Views.Admin.class)
        @PreAuthorize("permitAll()")
        public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> getLessonById(
                        @Parameter(description = "ID của lesson") @PathVariable Long lessonId) {

                try {
                        GrammarLessonDTO lesson = grammarLessonService.getLessonById(lessonId);

                        return ResponseEntity.ok(CustomApiResponse.success(
                                        lesson,
                                        "Lấy lesson thành công"));

                } catch (Exception e) {
                        log.error("Error getting lesson: ", e);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(CustomApiResponse.error(404,
                                                        "Không tìm thấy lesson:  " + e.getMessage()));
                }
        }

        /**
         * Create new lesson
         * ADMIN: Can create for any topic
         * TEACHER: Can only create for assigned topics
         */
        @PostMapping("/lessons")
        @Operation(summary = "Tạo lesson mới", description = "ADMIN có quyền tạo lesson cho mọi topic, TEACHER chỉ tạo cho topic được phân công.")
        public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> createLesson(
                        @Parameter(description = "Thông tin lesson cần tạo", required = true) @Valid @RequestBody GrammarLessonDTO lessonDTO) {

                try {
                        // Access validation is done in service layer
                        GrammarLessonDTO createdLesson = grammarLessonService.createLesson(lessonDTO);

                        log.info("Lesson created: id={}, title={}",
                                        createdLesson.getId(), createdLesson.getTitle());

                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(
                                                        createdLesson,
                                                        "Tạo lesson thành công"));

                } catch (IllegalArgumentException e) {
                        log.error("Validation error: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("Error creating lesson: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.error(500, "Lỗi tạo lesson: " + e.getMessage()));
                }
        }

        /**
         * Update lesson
         * ADMIN: Can update any lesson
         * TEACHER: Can only update lessons in assigned topics
         */
        @PutMapping("/lessons/{lessonId}")
        @Operation(summary = "Cập nhật lesson", description = "ADMIN có quyền cập nhật mọi lesson, TEACHER chỉ cập nhật lesson trong topic được phân công.")
        public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> updateLesson(
                        @Parameter(description = "ID của lesson cần cập nhật") @PathVariable Long lessonId,

                        @Parameter(description = "Thông tin cập nhật", required = true) @Valid @RequestBody GrammarLessonDTO lessonDTO) {

                try {
                        // Access validation is done in service layer
                        GrammarLessonDTO updatedLesson = grammarLessonService.updateLesson(lessonId, lessonDTO);

                        log.info("Lesson updated: id={}, title={}",
                                        updatedLesson.getId(), updatedLesson.getTitle());

                        return ResponseEntity.ok(CustomApiResponse.success(
                                        updatedLesson,
                                        "Cập nhật lesson thành công"));

                } catch (IllegalArgumentException e) {
                        log.error("Validation error:  {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("Error updating lesson: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.error(500, "Lỗi cập nhật lesson: " + e.getMessage()));
                }
        }

        /**
         * Delete lesson
         * ADMIN: Can delete any lesson
         * TEACHER: Can only delete lessons in assigned topics
         */
        @DeleteMapping("/lessons/{lessonId}")
        @Operation(summary = "Xóa lesson", description = "ADMIN có quyền xóa mọi lesson, TEACHER chỉ xóa lesson trong topic được phân công.")
        public ResponseEntity<CustomApiResponse<String>> deleteLesson(
                        @Parameter(description = "ID của lesson cần xóa") @PathVariable Long lessonId) {

                try {
                        // Access validation is done in service layer
                        grammarLessonService.deleteLesson(lessonId);

                        log.info("Lesson deleted: id={}", lessonId);

                        return ResponseEntity.ok(CustomApiResponse.success(
                                        "Đã xóa lesson",
                                        "Xóa lesson thành công"));

                } catch (IllegalArgumentException e) {
                        log.error("Validation error: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("Error deleting lesson: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.error(500, "Lỗi xóa lesson: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/{lessonId}/toggle-status")
        @Operation(summary = "Toggle trạng thái lesson")
        public ResponseEntity<CustomApiResponse<String>> toggleLessonStatus(@PathVariable Long lessonId) {
                try {
                        grammarLessonService.toggleStatus(lessonId);
                        return ResponseEntity.ok(CustomApiResponse.success("Đã thay đổi trạng thái", "Thành công"));
                } catch (Exception e) {
                        log.error("Error toggling lesson status: ", e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi:  " + e.getMessage()));
                }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // QUESTION MANAGEMENT
        // ═══════════════════════════════════════════════════════════════════════════

        @GetMapping("/lessons/{lessonId}/questions")
        @JsonView(Views.Admin.class)
        @Operation(summary = "Lấy danh sách câu hỏi (Tự động phát hiện cấu trúc nhóm)")
        public ResponseEntity<CustomApiResponse<TaskGroupedQuestionsDTO>> getQuestionsByLesson(
                        @PathVariable Long lessonId) {
                // Hàm này trong Service sẽ tự check:
                // - Nếu không có nhóm: trả về standaloneQuestions (List phẳng)
                // - Nếu có nhóm: trả về tasks
                TaskGroupedQuestionsDTO questions = grammarQuestionService.getGroupedQuestions(lessonId);
                return ResponseEntity.ok(CustomApiResponse.success(questions));
        }

        @GetMapping("/lessons/{lessonId}/task-stats")
        @Operation(summary = "Lấy thống kê về các nhóm Task")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> getTaskStats(
                        @PathVariable Long lessonId) {
                return ResponseEntity.ok(CustomApiResponse.success(
                                grammarQuestionService.getTaskStats(lessonId)));
        }

        // ==================== CRUD OPERATIONS (Giữ nguyên) ====================

        @PostMapping("/lessons/{lessonId}/questions")
        public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> createQuestion(
                        @PathVariable Long lessonId,
                        @Valid @RequestBody CreateQuestionDTO dto) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(CustomApiResponse.success(
                                                grammarQuestionService.createQuestion(lessonId, dto),
                                                "Tạo thành công"));
        }

        @PostMapping("/lessons/{lessonId}/questions/bulk")
        public ResponseEntity<CustomApiResponse<List<QuestionResponseDTO>>> createQuestionsBulk(
                        @PathVariable Long lessonId,
                        @Valid @RequestBody List<CreateQuestionDTO> dtos) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(CustomApiResponse.success(
                                                grammarQuestionService.createQuestionsInBulk(lessonId, dtos),
                                                "Tạo hàng loạt thành công"));
        }

        @PutMapping("/questions/{id}")
        public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> updateQuestion(
                        @PathVariable Long id,
                        @Valid @RequestBody CreateQuestionDTO dto) {
                return ResponseEntity.ok(CustomApiResponse.success(
                                grammarQuestionService.updateQuestion(id, dto),
                                "Cập nhật thành công"));
        }

        @DeleteMapping("/questions/{id}")
        public ResponseEntity<CustomApiResponse<Void>> deleteQuestion(@PathVariable Long id) {
                grammarQuestionService.deleteQuestion(id);
                return ResponseEntity.ok(CustomApiResponse.success(null, "Xóa thành công"));
        }

        @DeleteMapping("/questions/bulk")
        public ResponseEntity<CustomApiResponse<Void>> bulkDeleteQuestions(@RequestBody List<Long> ids) {
                grammarQuestionService.bulkDeleteQuestions(ids);
                return ResponseEntity.ok(CustomApiResponse.success(null, "Đã xóa các câu hỏi"));
        }

        // ==================== UTILS ====================

        @GetMapping("/lessons/{lessonId}/questions/next-order")
        public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextQuestionOrder(
                        @PathVariable Long lessonId) {
                Integer next = grammarQuestionService.getNextOrderIndex(lessonId);
                return ResponseEntity.ok(CustomApiResponse.success(Map.of("nextOrderIndex", next)));
        }

        @PostMapping("/lessons/{lessonId}/questions/fix-order")
        public ResponseEntity<CustomApiResponse<Void>> fixQuestionOrder(@PathVariable Long lessonId) {
                grammarQuestionService.fixOrderIndexes(lessonId);
                return ResponseEntity.ok(CustomApiResponse.success(null, "Đã sắp xếp lại thứ tự"));
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ORDER MANAGEMENT (DELEGATE TO LESSON SERVICE)
        // ═══════════════════════════════════════════════════════════════════════════

        @PostMapping("/topics/{topicId}/lessons/fix-order")
        @Operation(summary = "Chuẩn hóa orderIndex của lessons trong topic")
        public ResponseEntity<CustomApiResponse<String>> fixLessonOrder(
                        @PathVariable Long topicId) {

                try {
                        // Delegate to GrammarLessonService
                        grammarLessonService.fixOrderIndexes(topicId);

                        return ResponseEntity.ok(CustomApiResponse.success(
                                        "Đã chuẩn hóa orderIndex",
                                        "Chuẩn hóa thành công"));

                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("Error fixing lesson order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.error(500,
                                                        "Lỗi chuẩn hóa orderIndex: " + e.getMessage()));
                }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // UTILITY ENDPOINTS
        // ═══════════════════════════════════════════════════════════════════════════

        @GetMapping("/topics/{topicId}/lessons/next-order")
        @Operation(summary = "Lấy orderIndex tiếp theo cho lesson mới")
        public ResponseEntity<CustomApiResponse<Integer>> getNextOrderIndex(
                        @PathVariable Long topicId) {

                try {

                        // Delegate to GrammarLessonService
                        Integer nextOrder = grammarLessonService.getNextOrderIndex(topicId);

                        return ResponseEntity.ok(CustomApiResponse.success(
                                        nextOrder,
                                        "Lấy orderIndex tiếp theo thành công"));

                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("Error getting next order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.error(500, "Lỗi:  " + e.getMessage()));
                }
        }
}