package com. thanhnb.englishlearning.controller.listening;

import com.thanhnb.englishlearning.dto. CustomApiResponse;
import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.listening.ListeningLessonDTO;
import com.thanhnb.englishlearning. dto.listening.request.CreateListeningLessonRequest;
import com.thanhnb.englishlearning.dto.listening.request.UpdateListeningLessonRequest;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto. question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.service.listening.*;
import com.thanhnb.englishlearning.util.PaginationHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework. security.access.prepost.PreAuthorize;
import org. springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * ADMIN Controller cho Listening module
 * Đầy đủ tính năng tương tự ReadingAdminController
 */
@RestController
@RequestMapping("/api/admin/listening")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Listening Admin", description = "API quản lý bài nghe (dành cho ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AdminListeningController {

    private final ListeningAdminService listeningAdminService;
    private final ListeningQuestionService questionService;
    private final ListeningValidationService validationService;
    private final ListeningStatisticsService statisticsService;

    // ═════════════════════════════════════════════════════════════════
    // LESSON CRUD
    // ═════════════════════════════════════════════════════════════════

    @GetMapping("/lessons")
    @Operation(summary = "Lấy tất cả bài nghe", description = "Trả về danh sách bài nghe có pagination")
    public ResponseEntity<CustomApiResponse<PaginatedResponse<ListeningLessonDTO>>> getAllLessons(
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Số items mỗi trang") @RequestParam(required = false) Integer size,
            @Parameter(description = "Sắp xếp theo") @RequestParam(required = false) String sort) {
        try {
            Pageable pageable = PaginationHelper.createPageable(page, size, sort);
            Page<ListeningLessonDTO> lessonPage = listeningAdminService.getAllLessons(pageable);

            return ResponseEntity.ok(
                    CustomApiResponse.success(
                            PaginatedResponse.of(lessonPage),
                            "Lấy danh sách bài nghe thành công"));
        } catch (Exception e) {
            log.error("Error getting lessons: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi:  " + e.getMessage()));
        }
    }

    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Lấy chi tiết bài nghe", description = "Trả về chi tiết bài nghe kèm questions")
    public ResponseEntity<CustomApiResponse<ListeningLessonDTO>> getLessonDetail(
            @Parameter(description = "ID của bài nghe") @PathVariable Long lessonId) {
        try {
            ListeningLessonDTO lesson = listeningAdminService.getLessonById(lessonId);
            return ResponseEntity.ok(CustomApiResponse.success(lesson, "Lấy chi tiết thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/lessons", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tạo bài nghe mới", description = "Tạo bài nghe với audio file")
    public ResponseEntity<CustomApiResponse<ListeningLessonDTO>> createLesson(
            @RequestPart("request") @Valid CreateListeningLessonRequest request,
            @RequestPart("audio") MultipartFile audioFile) {
        try {
            ListeningLessonDTO created = listeningAdminService.createLesson(request, audioFile);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CustomApiResponse.created(created, "Tạo bài nghe thành công"));
        } catch (Exception e) {
            log.error("Error creating lesson: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/lessons/{id}", consumes = MediaType. MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật bài nghe", description = "Cập nhật bài nghe (audio optional)")
    public ResponseEntity<CustomApiResponse<ListeningLessonDTO>> updateLesson(
            @PathVariable Long id,
            @RequestPart("request") @Valid UpdateListeningLessonRequest request,
            @RequestPart(value = "audio", required = false) MultipartFile audioFile) {
        try {
            ListeningLessonDTO updated = listeningAdminService.updateLesson(id, request, audioFile);
            return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @DeleteMapping("/lessons/{id}")
    @Operation(summary = "Xóa bài nghe", description = "Xóa vĩnh viễn bao gồm audio file")
    public ResponseEntity<CustomApiResponse<String>> deleteLesson(@PathVariable Long id) {
        try {
            listeningAdminService.deleteLesson(id);
            return ResponseEntity.ok(CustomApiResponse.success("Xóa thành công", "Xóa thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi:  " + e.getMessage()));
        }
    }

    @PostMapping("/lessons/{lessonId}/toggle-status")
    @Operation(summary = "Bật/tắt trạng thái bài nghe")
    public ResponseEntity<CustomApiResponse<String>> toggleLessonStatus(@PathVariable Long lessonId) {
        try {
            listeningAdminService.toggleLessonStatus(lessonId);
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
    @Operation(summary = "Lấy orderIndex tiếp theo")
    public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextLessonOrderIndex() {
        try {
            Integer nextOrder = listeningAdminService. getNextLessonOrderIndex();
            return ResponseEntity.ok(
                    CustomApiResponse.success(
                            Map.of("nextOrderIndex", nextOrder),
                            "Lấy orderIndex thành công"));
        } catch (Exception e) {
            log.error("Error getting next order:  ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomApiResponse.badRequest("Lỗi:  " + e.getMessage()));
        }
    }

    @PostMapping("/lessons/{lessonId}/reorder")
    @Operation(summary = "Thay đổi thứ tự bài nghe")
    public ResponseEntity<CustomApiResponse<String>> reorderLesson(
            @PathVariable Long lessonId,
            @RequestParam Integer newOrderIndex) {
        try {
            listeningAdminService.reorderLesson(lessonId, newOrderIndex);
            return ResponseEntity.ok(
                    CustomApiResponse.success("Sắp xếp lại thành công", "Sắp xếp lại thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // QUESTION OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    @GetMapping("/lessons/{lessonId}/questions")
    @Operation(summary = "Lấy questions theo bài nghe")
    public ResponseEntity<CustomApiResponse<PaginatedResponse<QuestionResponseDTO>>> getQuestionsByLesson(
            @PathVariable Long lessonId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        try {
            Pageable pageable = PaginationHelper.createPageable(page, size, sort);
            Page<QuestionResponseDTO> questionPage = questionService.getQuestionsByLessonPaginated(
                    lessonId, pageable);

            return ResponseEntity.ok(
                    CustomApiResponse. success(
                            PaginatedResponse.of(questionPage),
                            "Lấy danh sách questions thành công"));
        } catch (Exception e) {
            log.error("Error getting questions: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/questions/{id}")
    @Operation(summary = "Lấy chi tiết question")
    public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> getQuestionById(@PathVariable Long id) {
        try {
            QuestionResponseDTO question = questionService.getQuestionById(id);
            return ResponseEntity.ok(CustomApiResponse.success(question, "Lấy chi tiết thành công"));
        } catch (Exception e) {
            log.error("Error getting question: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/questions")
    @Operation(summary = "Tạo question mới")
    public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> createQuestion(
            @Valid @RequestBody CreateQuestionDTO createDTO) {
        try {
            QuestionResponseDTO created = questionService.createQuestion(createDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CustomApiResponse.created(created, "Tạo question thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping("/questions/{id}")
    @Operation(summary = "Cập nhật question")
    public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody CreateQuestionDTO dto) {
        try {
            QuestionResponseDTO updated = questionService.updateQuestion(id, dto);
            return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @DeleteMapping("/questions/{id}")
    @Operation(summary = "Xóa question")
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
    @Operation(summary = "Xóa nhiều questions")
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
    @Operation(summary = "Tạo nhiều questions")
    public ResponseEntity<CustomApiResponse<List<QuestionResponseDTO>>> createQuestionsInBulk(
            @PathVariable Long lessonId,
            @RequestBody List<CreateQuestionDTO> createDTOs) {
        try {
            List<QuestionResponseDTO> created = questionService.createQuestionsInBulk(lessonId, createDTOs);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CustomApiResponse.created(created,
                            "Tạo thành công " + created.size() + " questions"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/lessons/{sourceLessonId}/copy-to/{targetLessonId}")
    @Operation(summary = "Copy questions")
    public ResponseEntity<CustomApiResponse<String>> copyQuestions(
            @PathVariable Long sourceLessonId,
            @PathVariable Long targetLessonId) {
        try {
            listeningAdminService.copyQuestionsToLesson(sourceLessonId, targetLessonId);
            return ResponseEntity. ok(
                    CustomApiResponse.success("Copy thành công", "Copy thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    . body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
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
    @Operation(summary = "Lấy thống kê bài nghe")
    public ResponseEntity<CustomApiResponse<ListeningStatisticsService.ListeningStatisticsDTO>> getLessonStatistics(
            @PathVariable Long lessonId) {
        try {
            var stats = statisticsService.getLessonStatistics(lessonId);
            return ResponseEntity.ok(CustomApiResponse.success(stats, "Lấy thống kê thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse. badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Lấy thống kê toàn bộ module")
    public ResponseEntity<CustomApiResponse<ListeningStatisticsService.ListeningModuleStatisticsDTO>> getModuleStatistics() {
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
    @Operation(summary = "Validate orderIndex của lessons")
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllLessonsOrder() {
        try {
            Map<String, Object> result = validationService.validateAllLessonsOrderIndex();
            String message = result.get("issuesFixed").equals(0)
                    ? "OrderIndex đã đúng"
                    : "Đã fix " + result.get("issuesFixed") + " vấn đề";
            return ResponseEntity.ok(CustomApiResponse.success(result, message));
        } catch (Exception e) {
            log.error("Validation error:  ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomApiResponse.badRequest("Lỗi:  " + e.getMessage()));
        }
    }

    @PostMapping("/lessons/{lessonId}/questions/validate-order")
    @Operation(summary = "Validate orderIndex của questions")
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
    @Operation(summary = "Validate tất cả questions")
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllQuestionsOrder() {
        try {
            Map<String, Object> result = validationService.validateAllQuestionsOrderIndex();
            String message = result.get("totalIssuesFixed").equals(0)
                    ? "OrderIndex đã đúng"
                    : "Đã fix " + result.get("totalIssuesFixed") + " vấn đề";
            return ResponseEntity.ok(CustomApiResponse. success(result, message));
        } catch (Exception e) {
            log.error("Validation error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomApiResponse. badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/audio/validate")
    @Operation(summary = "Validate audio files", description = "Kiểm tra audio files tồn tại")
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAudioFiles() {
        try {
            Map<String, Object> result = validationService.validateAudioFiles();
            String status = result.get("status").toString();
            String message = "ALL_HEALTHY".equals(status)
                    ? "Tất cả audio files đều tồn tại"
                    : "Phát hiện " + result.get("missingCount") + " audio files bị thiếu";
            return ResponseEntity.ok(CustomApiResponse.success(result, message));
        } catch (Exception e) {
            log.error("Audio validation error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomApiResponse.badRequest("Lỗi:  " + e.getMessage()));
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
                    : "Có vấn đề cần xử lý";

            return ResponseEntity.ok(CustomApiResponse.success(result, message));
        } catch (Exception e) {
            log.error("Health check error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/lessons/{id}/audio")
    @Operation(summary = "Upload/Replace audio", description = "Chỉ upload audio file")
    public ResponseEntity<CustomApiResponse<ListeningLessonDTO>> uploadAudio(
            @PathVariable Long id,
            @RequestPart("audio") MultipartFile audioFile) {
        try {
            UpdateListeningLessonRequest request = new UpdateListeningLessonRequest();
            ListeningLessonDTO lesson = listeningAdminService.updateLesson(id, request, audioFile);
            return ResponseEntity.ok(CustomApiResponse.success(lesson, "Upload audio thành công"));
        } catch (Exception e) {
            log.error("Failed to upload audio", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }
}