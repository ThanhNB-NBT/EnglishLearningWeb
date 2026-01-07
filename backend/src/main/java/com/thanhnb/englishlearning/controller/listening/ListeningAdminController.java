package com.thanhnb.englishlearning.controller.listening;

import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.listening.ListeningLessonDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.dto.question.response.TaskGroupedQuestionsDTO;
import com.thanhnb.englishlearning.service.listening.ListeningLessonService;
import com.thanhnb.englishlearning.service.listening.ListeningQuestionService;
import com.thanhnb.englishlearning.util.PaginationHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/listening")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
@Tag(name = "Listening Admin", description = "API quản lý bài nghe (dành cho ADMIN và TEACHER)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ListeningAdminController {

    private final ListeningLessonService lessonService;
    private final ListeningQuestionService listeningQuestionService;

    // ==================== LESSON MANAGEMENT ====================

    @GetMapping("/topics/{topicId}/lessons")
    @Operation(summary = "Lấy danh sách lessons theo topic (có phân trang)")
    public ResponseEntity<CustomApiResponse<PaginatedResponse<ListeningLessonDTO>>> getLessonsByTopic(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderIndex: asc") String sort) {
        try {
            Pageable pageable = PaginationHelper.createPageable(page, size, sort);
            Page<ListeningLessonDTO> lessonPage = lessonService.getLessonsByTopic(topicId, pageable);

            PaginatedResponse<ListeningLessonDTO> response = PaginatedResponse.of(lessonPage);

            return ResponseEntity.ok(CustomApiResponse.success(response, "Lấy danh sách lessons thành công"));
        } catch (Exception e) {
            log.error("Error getting lessons:  ", e);
            return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi:  " + e.getMessage()));
        }
    }

    @GetMapping("/lessons/{lessonId}")
    @JsonView(Views.Admin.class)
    @Operation(summary = "Lấy chi tiết lesson")
    public ResponseEntity<CustomApiResponse<ListeningLessonDTO>> getLessonById(@PathVariable Long lessonId) {
        try {
            ListeningLessonDTO lesson = lessonService.getLessonById(lessonId);
            return ResponseEntity.ok(CustomApiResponse.success(lesson, "Lấy chi tiết lesson thành công"));
        } catch (Exception e) {
            log.error("Error getting lesson: ", e);
            return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/lessons")
    @Operation(summary = "Tạo lesson mới")
    public ResponseEntity<CustomApiResponse<ListeningLessonDTO>> createLesson(
            @Valid @ModelAttribute ListeningLessonDTO request,
            @RequestPart(value = "audio", required = false) MultipartFile audioFile) {
        try {
            ListeningLessonDTO dto = ListeningLessonDTO.builder()
                    .topicId(request.getTopicId())
                    .title(request.getTitle())
                    .transcript(request.getTranscript())
                    .transcriptTranslation(request.getTranscriptTranslation())
                    .orderIndex(request.getOrderIndex())
                    .timeLimitSeconds(request.getTimeLimitSeconds())
                    .pointsReward(request.getPointsReward())
                    .allowUnlimitedReplay(request.getAllowUnlimitedReplay())
                    .maxReplayCount(request.getMaxReplayCount())
                    .isActive(request.getIsActive())
                    .build();

            ListeningLessonDTO lesson = lessonService.createLesson(dto, audioFile);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CustomApiResponse.created(lesson, "Tạo lesson thành công"));
        } catch (Exception e) {
            log.error("Error creating lesson: ", e);
            return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping("/lessons/{id}")
    @Operation(summary = "Cập nhật lesson")
    public ResponseEntity<CustomApiResponse<ListeningLessonDTO>> updateLesson(
            @PathVariable Long id,
            @Valid @ModelAttribute ListeningLessonDTO request,
            @RequestPart(value = "audio", required = false) MultipartFile audioFile) {
        try {
            ListeningLessonDTO dto = ListeningLessonDTO.builder()
                    .title(request.getTitle())
                    .transcript(request.getTranscript())
                    .transcriptTranslation(request.getTranscriptTranslation())
                    .orderIndex(request.getOrderIndex())
                    .timeLimitSeconds(request.getTimeLimitSeconds())
                    .pointsReward(request.getPointsReward())
                    .allowUnlimitedReplay(request.getAllowUnlimitedReplay())
                    .maxReplayCount(request.getMaxReplayCount())
                    .isActive(request.getIsActive())
                    .build();

            ListeningLessonDTO lesson = lessonService.updateLesson(id, dto, audioFile);
            return ResponseEntity.ok(CustomApiResponse.success(lesson, "Cập nhật lesson thành công"));
        } catch (Exception e) {
            log.error("Error updating lesson: ", e);
            return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @DeleteMapping("/lessons/{id}")
    @Operation(summary = "Xóa lesson")
    public ResponseEntity<CustomApiResponse<String>> deleteLesson(@PathVariable Long id) {
        try {
            lessonService.deleteLesson(id);
            return ResponseEntity.ok(CustomApiResponse.success("Đã xóa lesson", "Xóa lesson thành công"));
        } catch (Exception e) {
            log.error("Error deleting lesson: ", e);
            return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/lessons/{lessonId}/toggle-status")
    @Operation(summary = "Toggle trạng thái lesson")
    public ResponseEntity<CustomApiResponse<String>> toggleLessonStatus(@PathVariable Long lessonId) {
        try {
            lessonService.toggleStatus(lessonId);
            return ResponseEntity.ok(CustomApiResponse.success("Đã thay đổi trạng thái", "Thành công"));
        } catch (Exception e) {
            log.error("Error toggling lesson status: ", e);
            return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/topics/{topicId}/lessons/fix-order")
    @Operation(summary = "Chuẩn hóa orderIndex của lessons trong topic")
    public ResponseEntity<CustomApiResponse<String>> fixLessonOrder(@PathVariable Long topicId) {
        try {
            lessonService.fixOrderIndexes(topicId);
            return ResponseEntity.ok(CustomApiResponse.success("Đã chuẩn hóa orderIndex", "Thành công"));
        } catch (Exception e) {
            log.error("Error fixing lesson order: ", e);
            return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/topics/{topicId}/lessons/next-order")
    @Operation(summary = "Lấy orderIndex tiếp theo cho lesson mới")
    public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextOrderIndex(@PathVariable Long topicId) {
        try {
            Integer nextOrder = lessonService.getNextOrderIndex(topicId);
            return ResponseEntity.ok(CustomApiResponse.success(
                    Map.of("nextOrderIndex", nextOrder),
                    "Thành công"));
        } catch (Exception e) {
            log.error("Error getting next order:  ", e);
            return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    // ==================== QUESTION MANAGEMENT ====================

    @GetMapping("/lessons/{lessonId}/questions")
    @JsonView(Views.Admin.class)
    @Operation(summary = "Lấy danh sách câu hỏi (Grouped structure)")
    public ResponseEntity<CustomApiResponse<TaskGroupedQuestionsDTO>> getQuestionsByLesson(
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(CustomApiResponse.success(
                listeningQuestionService.getGroupedQuestions(lessonId)));
    }
    
    @GetMapping("/lessons/{lessonId}/task-stats")
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> getTaskStats(
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(CustomApiResponse.success(
                listeningQuestionService.getTaskStats(lessonId)));
    }

    @PostMapping("/lessons/{lessonId}/questions")
    public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> createQuestion(
            @PathVariable Long lessonId, @Valid @RequestBody CreateQuestionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomApiResponse.success(
                listeningQuestionService.createQuestion(lessonId, dto), "Tạo thành công"));
    }

    @PostMapping("/lessons/{lessonId}/questions/bulk")
    public ResponseEntity<CustomApiResponse<List<QuestionResponseDTO>>> createQuestionsBulk(
            @PathVariable Long lessonId, @Valid @RequestBody List<CreateQuestionDTO> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomApiResponse.success(
                listeningQuestionService.createQuestionsInBulk(lessonId, dtos), "Tạo hàng loạt thành công"));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> updateQuestion(
            @PathVariable Long id, @Valid @RequestBody CreateQuestionDTO dto) {
        return ResponseEntity.ok(CustomApiResponse.success(
                listeningQuestionService.updateQuestion(id, dto), "Cập nhật thành công"));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<CustomApiResponse<Void>> deleteQuestion(@PathVariable Long id) {
        listeningQuestionService.deleteQuestion(id);
        return ResponseEntity.ok(CustomApiResponse.success(null, "Xóa thành công"));
    }

    @DeleteMapping("/questions/bulk")
    public ResponseEntity<CustomApiResponse<Void>> bulkDeleteQuestions(@RequestBody List<Long> ids) {
        listeningQuestionService.bulkDeleteQuestions(ids);
        return ResponseEntity.ok(CustomApiResponse.success(null, "Đã xóa các câu hỏi"));
    }

    @GetMapping("/lessons/{lessonId}/questions/next-order")
    public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextOrder(@PathVariable Long lessonId) {
        return ResponseEntity.ok(CustomApiResponse.success(
                Map.of("nextOrderIndex", listeningQuestionService.getNextOrderIndex(lessonId))));
    }

    @PostMapping("/lessons/{lessonId}/questions/fix-order")
    public ResponseEntity<CustomApiResponse<Void>> fixOrder(@PathVariable Long lessonId) {
        listeningQuestionService.fixOrderIndexes(lessonId);
        return ResponseEntity.ok(CustomApiResponse.success(null, "Đã sắp xếp lại thứ tự"));
    }
}