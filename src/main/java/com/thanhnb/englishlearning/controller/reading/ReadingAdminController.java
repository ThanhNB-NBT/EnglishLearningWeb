package com.thanhnb.englishlearning.controller.reading;

import com.thanhnb.englishlearning.dto.reading.*;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.question.QuestionDTO;
import com.thanhnb.englishlearning.service.reading.*;
import com.thanhnb.englishlearning.service.ai.reading.ReadingAIParsingService;
import com.thanhnb.englishlearning.util.PaginationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import java.util.*;

/**
 * ADMIN Controller cho Reading module
 */
@RestController
@RequestMapping("/api/admin/reading")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Reading Admin", description = "API quản lý bài đọc (dành cho ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ReadingAdminController {

        private final ReadingAdminService readingAdminService;
        private final ReadingQuestionService questionService;
        private final ReadingValidationService validationService;
        private final ReadingStatisticsService statisticsService;
        private final ReadingAIParsingService aiParsingService;

        // ═════════════════════════════════════════════════════════════════
        // AI PARSING ENDPOINTS
        // ═════════════════════════════════════════════════════════════════

        /**
         * ENDPOINT 1: Parse file thành Reading lesson
         */
        @PostMapping("/lessons/parse-file")
        @Operation(summary = "Parse file (PDF/DOCX/Image) thành Reading lesson", description = "Sử dụng AI để phân tích file và tạo Reading lesson với English content, Vietnamese translation, và questions")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Parse thành công"),
                        @ApiResponse(responseCode = "400", description = "File không hợp lệ"),
                        @ApiResponse(responseCode = "500", description = "Lỗi AI hoặc server")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> parseFile(
                        @Parameter(description = "File PDF/DOCX/Image (max 20MB)", required = true) @RequestParam("file") MultipartFile file) {
                try {
                        log.info("[READING PARSE] Received file: {}", file.getOriginalFilename());

                        // Parse file using AI
                        ReadingLessonDTO lesson = aiParsingService.parseFileForImport(file);

                        if (lesson == null || lesson.getTitle() == null) {
                                return ResponseEntity.badRequest()
                                                .body(CustomApiResponse.badRequest(
                                                                "AI không thể parse file này. Vui lòng kiểm tra nội dung."));
                        }

                        // Calculate statistics
                        int questionCount = lesson.getQuestions() != null ? lesson.getQuestions().size() : 0;
                        long mcCount = lesson.getQuestions() != null ? lesson.getQuestions().stream()
                                        .filter(q -> q.getQuestionType() == com.thanhnb.englishlearning.enums.QuestionType.MULTIPLE_CHOICE)
                                        .count() : 0;
                        long fbCount = lesson.getQuestions() != null ? lesson.getQuestions().stream()
                                        .filter(q -> q.getQuestionType() == com.thanhnb.englishlearning.enums.QuestionType.FILL_BLANK)
                                        .count() : 0;

                        // Build response
                        Map<String, Object> summary = new HashMap<>();
                        summary.put("fileName", file.getOriginalFilename());
                        summary.put("fileSize", String.format("%.2f MB", file.getSize() / (1024.0 * 1024.0)));
                        summary.put("fileType", file.getContentType());
                        summary.put("title", lesson.getTitle());
                        summary.put("contentLength", lesson.getContent() != null ? lesson.getContent().length() : 0);
                        summary.put("translationLength",
                                        lesson.getContentTranslation() != null ? lesson.getContentTranslation().length()
                                                        : 0);
                        summary.put("hasTranslation", lesson.getContentTranslation() != null
                                        && !lesson.getContentTranslation().isEmpty());
                        summary.put("orderIndex", lesson.getOrderIndex());
                        summary.put("questionCount", questionCount);
                        summary.put("multipleChoice", mcCount);
                        summary.put("fillBlank", fbCount);

                        Map<String, Object> response = new HashMap<>();
                        response.put("lesson", lesson);
                        response.put("summary", summary);

                        log.info("[READING PARSE] Success: title='{}', orderIndex={}, {} questions",
                                        lesson.getTitle(), lesson.getOrderIndex(), questionCount);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(response,
                                                        String.format("Parse thành công! Tạo bài đọc '%s' với %d câu hỏi.",
                                                                        lesson.getTitle(), questionCount)));

                } catch (IllegalArgumentException e) {
                        log.warn("[READING PARSE] Invalid input: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("[READING PARSE] Error: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi khi parse file: " + e.getMessage()));
                }
        }

        /**
         * ENDPOINT 2: Save parsed lesson to database
         */
        @PostMapping("/lessons/save-parsed-lesson")
        @Operation(summary = "Lưu parsed lesson vào database", description = "Lưu bài đọc đã được parse vào database với questions")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lưu thành công"),
                        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
                        @ApiResponse(responseCode = "500", description = "Lỗi khi lưu vào database")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> saveParsedLesson(
                        @Parameter(description = "Parsed lesson từ endpoint parse-file", required = true) @RequestBody ReadingLessonDTO parsedLesson) {
                try {
                        log.info("[READING SAVE] Saving lesson: {}",
                                        parsedLesson != null ? parsedLesson.getTitle() : "null");

                        // Validate
                        if (parsedLesson == null || parsedLesson.getTitle() == null) {
                                return ResponseEntity.badRequest()
                                                .body(CustomApiResponse.badRequest("Dữ liệu lesson không hợp lệ"));
                        }

                        // Import to database
                        ReadingLessonDTO savedLesson = readingAdminService.importLessonFromFile(parsedLesson);

                        // Count questions
                        int questionCount = parsedLesson.getQuestions() != null ? parsedLesson.getQuestions().size()
                                        : 0;

                        // Build response
                        Map<String, Object> result = new HashMap<>();
                        result.put("lessonId", savedLesson.getId());
                        result.put("title", savedLesson.getTitle());
                        result.put("orderIndex", savedLesson.getOrderIndex());
                        result.put("questionCount", questionCount);
                        result.put("pointsReward", savedLesson.getPointsReward());
                        result.put("isActive", savedLesson.getIsActive());
                        result.put("createdAt", savedLesson.getCreatedAt());

                        log.info("[READING SAVE] Success: id={}, title='{}', orderIndex={}, {} questions",
                                        savedLesson.getId(), savedLesson.getTitle(), savedLesson.getOrderIndex(),
                                        questionCount);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result,
                                                        String.format("Đã lưu bài đọc '%s' với %d câu hỏi!",
                                                                        savedLesson.getTitle(), questionCount)));

                } catch (RuntimeException e) {
                        log.error("[READING SAVE] Business error: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("[READING SAVE] Unexpected error: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi khi lưu bài học: " + e.getMessage()));
                }
        }

        // ═════════════════════════════════════════════════════════════════
        // 📚 LESSON CRUD OPERATIONS
        // ═════════════════════════════════════════════════════════════════

        @GetMapping("/lessons")
        @Operation(summary = "Lấy tất cả bài đọc", description = "Trả về danh sách bài đọc có pagination")
        public ResponseEntity<CustomApiResponse<PaginatedResponse<ReadingLessonDTO>>> getAllLessons(
                        @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(required = false) Integer page,
                        @Parameter(description = "Số items mỗi trang") @RequestParam(required = false) Integer size,
                        @Parameter(description = "Sắp xếp theo") @RequestParam(required = false) String sort) {
                try {
                        Pageable pageable = PaginationHelper.createPageable(page, size, sort);
                        Page<ReadingLessonDTO> lessonPage = readingAdminService.getAllLessons(pageable);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(
                                                        PaginatedResponse.of(lessonPage),
                                                        "Lấy danh sách bài đọc thành công"));
                } catch (Exception e) {
                        log.error("Error getting lessons: ", e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @GetMapping("/lessons/{lessonId}")
        @Operation(summary = "Lấy chi tiết bài đọc", description = "Trả về chi tiết bài đọc kèm questions")
        public ResponseEntity<CustomApiResponse<ReadingLessonDTO>> getLessonDetail(
                        @Parameter(description = "ID của bài đọc") @PathVariable Long lessonId) {
                try {
                        ReadingLessonDTO lesson = readingAdminService.getLessonById(lessonId);
                        return ResponseEntity.ok(CustomApiResponse.success(lesson, "Lấy chi tiết thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons")
        @Operation(summary = "Tạo bài đọc mới", description = "Tạo bài đọc mới với content và questions")
        public ResponseEntity<CustomApiResponse<ReadingLessonDTO>> createLesson(
                        @Valid @RequestBody ReadingLessonDTO dto) {
                try {
                        ReadingLessonDTO created = readingAdminService.createLesson(dto);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(created, "Tạo bài đọc thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PutMapping("/lessons/{id}")
        @Operation(summary = "Cập nhật bài đọc", description = "Cập nhật bài đọc theo ID")
        public ResponseEntity<CustomApiResponse<ReadingLessonDTO>> updateLesson(
                        @Parameter(description = "ID của bài đọc") @PathVariable Long id,
                        @Valid @RequestBody ReadingLessonDTO dto) {
                try {
                        ReadingLessonDTO updated = readingAdminService.updateLesson(id, dto);
                        return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @DeleteMapping("/lessons/{id}")
        @Operation(summary = "Xóa bài đọc (soft delete)", description = "Đặt isActive = false")
        public ResponseEntity<CustomApiResponse<String>> deleteLesson(
                        @Parameter(description = "ID của bài đọc") @PathVariable Long id) {
                try {
                        readingAdminService.deleteLesson(id);
                        return ResponseEntity.ok(CustomApiResponse.success("Xóa thành công", "Xóa thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @DeleteMapping("/lessons/{id}/permanent")
        @Operation(summary = "Xóa bài đọc vĩnh viễn", description = "Xóa hoàn toàn khỏi database")
        public ResponseEntity<CustomApiResponse<String>> permanentlyDeleteLesson(
                        @Parameter(description = "ID của bài đọc") @PathVariable Long id) {
                try {
                        readingAdminService.permanentlyDeleteLesson(id);
                        return ResponseEntity.ok(
                                        CustomApiResponse.success("Xóa vĩnh viễn thành công",
                                                        "Xóa vĩnh viễn thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/{lessonId}/toggle-status")
        @Operation(summary = "Bật/tắt trạng thái bài đọc", description = "Toggle active/inactive")
        public ResponseEntity<CustomApiResponse<String>> toggleLessonStatus(
                        @Parameter(description = "ID của bài đọc") @PathVariable Long lessonId) {
                try {
                        readingAdminService.toggleLessonStatus(lessonId);
                        return ResponseEntity.ok(
                                        CustomApiResponse.success("Thay đổi trạng thái thành công",
                                                        "Thay đổi trạng thái thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ═════════════════════════════════════════════════════════════════
        // ORDER OPERATIONS
        // ═════════════════════════════════════════════════════════════════

        @GetMapping("/lessons/next-order")
        @Operation(summary = "Lấy orderIndex tiếp theo", description = "Trả về orderIndex cho lesson mới")
        public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextLessonOrderIndex() {
                try {
                        Integer nextOrder = readingAdminService.getNextLessonOrderIndex();
                        return ResponseEntity.ok(
                                        CustomApiResponse.success(
                                                        Map.of("nextOrderIndex", nextOrder),
                                                        "Lấy orderIndex thành công"));
                } catch (Exception e) {
                        log.error("Error getting next order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/{lessonId}/reorder")
        @Operation(summary = "Thay đổi thứ tự bài đọc", description = "Di chuyển bài đọc sang vị trí mới")
        public ResponseEntity<CustomApiResponse<String>> reorderLesson(
                        @Parameter(description = "ID của bài đọc") @PathVariable Long lessonId,
                        @Parameter(description = "Vị trí mới") @RequestParam Integer newOrderIndex) {
                try {
                        readingAdminService.reorderLesson(lessonId, newOrderIndex);
                        return ResponseEntity.ok(
                                        CustomApiResponse.success("Sắp xếp lại thành công", "Sắp xếp lại thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/{lessonId1}/swap/{lessonId2}")
        @Operation(summary = "Swap 2 bài đọc", description = "Đổi vị trí 2 bài đọc")
        public ResponseEntity<CustomApiResponse<String>> swapLessons(
                        @Parameter(description = "ID bài đọc 1") @PathVariable Long lessonId1,
                        @Parameter(description = "ID bài đọc 2") @PathVariable Long lessonId2) {
                try {
                        readingAdminService.swapLessons(lessonId1, lessonId2);
                        return ResponseEntity.ok(
                                        CustomApiResponse.success("Swap thành công", "Swap thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ═════════════════════════════════════════════════════════════════
        // QUESTION OPERATIONS
        // ═════════════════════════════════════════════════════════════════

        @GetMapping("/lessons/{lessonId}/questions")
        @Operation(summary = "Lấy questions theo bài đọc", description = "Với pagination")
        public ResponseEntity<CustomApiResponse<PaginatedResponse<QuestionDTO>>> getQuestionsByLesson(
                        @Parameter(description = "ID của bài đọc") @PathVariable Long lessonId,
                        @RequestParam(required = false) Integer page,
                        @RequestParam(required = false) Integer size,
                        @RequestParam(required = false) String sort) {
                try {
                        Pageable pageable = PaginationHelper.createPageable(page, size, sort);
                        Page<QuestionDTO> questionPage = questionService.getQuestionsByLessonPaginated(lessonId,
                                        pageable);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(
                                                        PaginatedResponse.of(questionPage),
                                                        "Lấy danh sách questions thành công"));
                } catch (Exception e) {
                        log.error("Error getting questions: ", e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/questions")
        @Operation(summary = "Tạo question mới", description = "Tạo question với validation")
        public ResponseEntity<CustomApiResponse<QuestionDTO>> createQuestion(
                        @Valid @RequestBody QuestionDTO dto) {
                try {
                        QuestionDTO created = questionService.createQuestion(dto);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(created, "Tạo question thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PutMapping("/questions/{id}")
        @Operation(summary = "Cập nhật question", description = "Cập nhật question theo ID")
        public ResponseEntity<CustomApiResponse<QuestionDTO>> updateQuestion(
                        @PathVariable Long id,
                        @Valid @RequestBody QuestionDTO dto) {
                try {
                        QuestionDTO updated = questionService.updateQuestion(id, dto);
                        return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @DeleteMapping("/questions/{id}")
        @Operation(summary = "Xóa question", description = "Xóa question theo ID")
        public ResponseEntity<CustomApiResponse<String>> deleteQuestion(@PathVariable Long id) {
                try {
                        questionService.deleteQuestion(id);
                        return ResponseEntity.ok(CustomApiResponse.success("Xóa thành công", "Xóa thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/questions/bulk-delete")
        @Operation(summary = "Xóa nhiều questions", description = "Xóa hàng loạt")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> bulkDeleteQuestions(
                        @RequestBody Map<String, List<Long>> payload) {
                try {
                        List<Long> ids = payload.getOrDefault("questionIds", Collections.emptyList());
                        int deleted = questionService.bulkDeleteQuestions(ids);
                        return ResponseEntity.ok(
                                        CustomApiResponse.success(
                                                        Map.of("requested", ids.size(), "deleted", deleted),
                                                        "Đã xóa " + deleted + " câu hỏi"));
                } catch (Exception e) {
                        log.error("Bulk delete error", e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/{lessonId}/questions/bulk")
        @Operation(summary = "Tạo nhiều questions", description = "Bulk insert")
        public ResponseEntity<CustomApiResponse<List<QuestionDTO>>> createQuestionsInBulk(
                        @PathVariable Long lessonId,
                        @RequestBody List<QuestionDTO> questions) {
                try {
                        List<QuestionDTO> created = questionService.createQuestionsInBulk(lessonId, questions);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(created,
                                                        "Tạo thành công " + created.size() + " questions"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/{sourceLessonId}/copy-to/{targetLessonId}")
        @Operation(summary = "Copy questions", description = "Sao chép questions giữa các lesson")
        public ResponseEntity<CustomApiResponse<String>> copyQuestions(
                        @PathVariable Long sourceLessonId,
                        @PathVariable Long targetLessonId) {
                try {
                        readingAdminService.copyQuestionsToLesson(sourceLessonId, targetLessonId);
                        return ResponseEntity.ok(
                                        CustomApiResponse.success("Copy thành công", "Copy thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @GetMapping("/lessons/{lessonId}/questions/next-order")
        @Operation(summary = "Lấy orderIndex tiếp theo cho question")
        public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextQuestionOrderIndex(
                        @PathVariable Long lessonId) {
                try {
                        Integer nextOrder = questionService.getNextOrderIndex(lessonId);
                        return ResponseEntity.ok(
                                        CustomApiResponse.success(
                                                        Map.of("nextOrderIndex", nextOrder),
                                                        "Lấy orderIndex thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ═════════════════════════════════════════════════════════════════
        // STATISTICS OPERATIONS
        // ═════════════════════════════════════════════════════════════════

        @GetMapping("/lessons/{lessonId}/statistics")
        @Operation(summary = "Lấy thống kê bài đọc", description = "Thống kê chi tiết")
        public ResponseEntity<CustomApiResponse<ReadingStatisticsService.ReadingStatisticsDTO>> getLessonStatistics(
                        @PathVariable Long lessonId) {
                try {
                        var stats = statisticsService.getLessonStatistics(lessonId);
                        return ResponseEntity.ok(CustomApiResponse.success(stats, "Lấy thống kê thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @GetMapping("/statistics")
        @Operation(summary = "Lấy thống kê toàn bộ module", description = "Thống kê tổng quan")
        public ResponseEntity<CustomApiResponse<ReadingStatisticsService.ReadingModuleStatisticsDTO>> getModuleStatistics() {
                try {
                        var stats = statisticsService.getModuleStatistics();
                        return ResponseEntity.ok(CustomApiResponse.success(stats, "Lấy thống kê thành công"));
                } catch (Exception e) {
                        log.error("Error getting statistics: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ═════════════════════════════════════════════════════════════════
        // VALIDATION OPERATIONS
        // ═════════════════════════════════════════════════════════════════

        @PostMapping("/lessons/validate-all-order")
        @Operation(summary = "Validate orderIndex của lessons", description = "Kiểm tra và fix")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllLessonsOrder() {
                try {
                        Map<String, Object> result = validationService.validateAllLessonsOrderIndex();
                        String message = result.get("issuesFixed").equals(0)
                                        ? "OrderIndex đã đúng"
                                        : "Đã fix " + result.get("issuesFixed") + " vấn đề";
                        return ResponseEntity.ok(CustomApiResponse.success(result, message));
                } catch (Exception e) {
                        log.error("Validation error: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/{lessonId}/questions/validate-order")
        @Operation(summary = "Validate orderIndex của questions", description = "Kiểm tra và fix")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateQuestionOrder(
                        @PathVariable Long lessonId) {
                try {
                        Map<String, Object> result = validationService.validateQuestionsOrderIndex(lessonId);
                        String message = result.get("issuesFixed").equals(0)
                                        ? "OrderIndex đã đúng"
                                        : "Đã fix " + result.get("issuesFixed") + " vấn đề";
                        return ResponseEntity.ok(CustomApiResponse.success(result, message));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/questions/validate-all-order")
        @Operation(summary = "Validate tất cả questions", description = "Kiểm tra toàn bộ")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllQuestionsOrder() {
                try {
                        Map<String, Object> result = validationService.validateAllQuestionsOrderIndex();
                        String message = result.get("totalIssuesFixed").equals(0)
                                        ? "OrderIndex đã đúng"
                                        : "Đã fix " + result.get("totalIssuesFixed") + " vấn đề";
                        return ResponseEntity.ok(CustomApiResponse.success(result, message));
                } catch (Exception e) {
                        log.error("Validation error: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/health-check")
        @Operation(summary = "Health check toàn bộ module", description = "Kiểm tra tổng thể")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> healthCheck() {
                try {
                        Map<String, Object> result = validationService.healthCheck();

                        Object summaryObj = result.get("summary");
                        String status = "UNKNOWN";

                        if (summaryObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> summary = (Map<String, Object>) summaryObj;
                                status = summary.get("status") != null ? summary.get("status").toString() : "UNKNOWN";
                        }

                        String message = "HEALTHY".equals(status)
                                        ? "Module khỏe mạnh, không có vấn đề"
                                        : "Đã fix " + getIssuesFixed(summaryObj) + " vấn đề";

                        return ResponseEntity.ok(CustomApiResponse.success(result, message));
                } catch (Exception e) {
                        log.error("Health check error: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ═════════════════════════════════════════════════════════════════
        // HELPER METHODS
        // ═════════════════════════════════════════════════════════════════

        /**
         * Helper method để lấy totalIssuesFixed safely
         */
        private String getIssuesFixed(Object summaryObj) {
                if (summaryObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> summary = (Map<String, Object>) summaryObj;
                        Object issuesFixed = summary.get("totalIssuesFixed");
                        return issuesFixed != null ? issuesFixed.toString() : "0";
                }
                return "0";
        }
}