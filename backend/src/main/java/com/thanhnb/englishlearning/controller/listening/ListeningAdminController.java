package com.thanhnb.englishlearning.controller. listening;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning. dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.listening.ListeningLessonDTO;
import com.thanhnb.englishlearning.dto.listening.request.CreateListeningLessonRequest;
import com.thanhnb. englishlearning.dto.listening.request.UpdateListeningLessonRequest;
import com.thanhnb. englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto. question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.service.listening.ListeningAdminService;
import com.thanhnb.englishlearning. service.listening.ListeningQuestionService;
import com.thanhnb.englishlearning. util.PaginationHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io. swagger.v3.oas. annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data. domain.Page;
import org. springframework.data.domain.Pageable;
import org.springframework. http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security. access.prepost.PreAuthorize;
import org.springframework. web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * ADMIN Controller cho Listening module
 * Refactored:  Nhất quán với ReadingAdminController
 * - Inject specialized services (không inject trực tiếp repositories)
 * - Response format chuẩn với CustomApiResponse
 * - Proper error handling và logging
 */
@RestController
@RequestMapping("/api/admin/listening")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Listening Admin", description = "API quản lý bài nghe (dành cho ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ListeningAdminController {

    private final ListeningAdminService listeningAdminService;
    private final ListeningQuestionService questionService;

    // ═════════════════════════════════════════════════════════════════
    // LESSON CRUD
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Lấy tất cả bài nghe (bao gồm inactive)
     */
    @GetMapping("/lessons")
    @Operation(summary = "Lấy tất cả bài nghe", description = "Trả về danh sách bài nghe có pagination (bao gồm inactive)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi lấy danh sách")
    })
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
            log.error("Error getting listening lessons: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse. badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Lấy chi tiết bài nghe theo ID
     */
    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Lấy chi tiết bài nghe", description = "Trả về thông tin chi tiết của một bài nghe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
            @ApiResponse(responseCode = "400", description = "Bài nghe không tồn tại")
    })
    public ResponseEntity<CustomApiResponse<ListeningLessonDTO>> getLessonById(
            @Parameter(description = "ID của bài nghe") @PathVariable Long lessonId) {
        try {
            ListeningLessonDTO lesson = listeningAdminService.getLessonById(lessonId);
            return ResponseEntity.ok(CustomApiResponse.success(lesson, "Lấy chi tiết bài nghe thành công"));
        } catch (Exception e) {
            log.error("Error getting lesson {}: ", lessonId, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e. getMessage()));
        }
    }

    /**
     * [ADMIN] Tạo bài nghe mới với audio file
     */
    @PostMapping("/lessons")
    @Operation(summary = "Tạo bài nghe mới", description = "Tạo bài nghe mới với audio file (multipart/form-data)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo bài nghe thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<ListeningLessonDTO>> createLesson(
            @Valid @ModelAttribute CreateListeningLessonRequest request,
            @RequestPart(value = "audio", required = false) MultipartFile audioFile) {
        try {
            log.info("[ADMIN] Creating listening lesson:  {}", request.getTitle());

            ListeningLessonDTO lesson = listeningAdminService.createLesson(request, audioFile);

            log.info("[ADMIN] Created listening lesson: id={}, title='{}'", lesson.getId(), lesson.getTitle());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CustomApiResponse.success(lesson, "Tạo bài nghe thành công"));

        } catch (Exception e) {
            log.error("Error creating listening lesson: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse. badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Cập nhật bài nghe
     */
    @PutMapping("/lessons/{id}")
    @Operation(summary = "Cập nhật bài nghe", description = "Cập nhật thông tin bài nghe (có thể upload audio mới)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<ListeningLessonDTO>> updateLesson(
            @Parameter(description = "ID của bài nghe") @PathVariable Long id,
            @Valid @ModelAttribute UpdateListeningLessonRequest request,
            @RequestPart(value = "audio", required = false) MultipartFile audioFile) {
        try {
            log.info("[ADMIN] Updating listening lesson: id={}", id);

            ListeningLessonDTO lesson = listeningAdminService.updateLesson(id, request, audioFile);

            log.info("[ADMIN] Updated listening lesson: id={}", id);

            return ResponseEntity.ok(CustomApiResponse.success(lesson, "Cập nhật bài nghe thành công"));

        } catch (Exception e) {
            log.error("Error updating listening lesson {}: ", id, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Xóa bài nghe vĩnh viễn (bao gồm audio file)
     */
    @DeleteMapping("/lessons/{id}")
    @Operation(summary = "Xóa bài nghe", description = "Xóa vĩnh viễn bài nghe (bao gồm questions, progress, audio file)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "400", description = "Không thể xóa")
    })
    public ResponseEntity<CustomApiResponse<Void>> deleteLesson(
            @Parameter(description = "ID của bài nghe") @PathVariable Long id) {
        try {
            log. info("[ADMIN] Deleting listening lesson: id={}", id);

            listeningAdminService.deleteLesson(id);

            log.info("[ADMIN] Deleted listening lesson: id={}", id);

            return ResponseEntity. ok(CustomApiResponse.success(null, "Xóa bài nghe thành công"));

        } catch (Exception e) {
            log.error("Error deleting listening lesson {}: ", id, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e. getMessage()));
        }
    }

    /**
     * [ADMIN] Toggle lesson status (activate/deactivate)
     */
    @PostMapping("/lessons/{lessonId}/toggle-status")
    @Operation(summary = "Bật/tắt bài nghe", description = "Chuyển đổi trạng thái active/inactive")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi cập nhật")
    })
    public ResponseEntity<CustomApiResponse<Void>> toggleLessonStatus(
            @Parameter(description = "ID của bài nghe") @PathVariable Long lessonId) {
        try {
            log.info("[ADMIN] Toggling lesson status: lessonId={}", lessonId);

            listeningAdminService.toggleLessonStatus(lessonId);

            log.info("[ADMIN] Toggled lesson status: lessonId={}", lessonId);

            return ResponseEntity.ok(CustomApiResponse.success(null, "Cập nhật trạng thái thành công"));

        } catch (Exception e) {
            log.error("Error toggling lesson status {}: ", lessonId, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e. getMessage()));
        }
    }

    /**
     * [ADMIN] Upload/Replace audio file
     */
    @PostMapping("/lessons/{id}/audio")
    @Operation(summary = "Upload/Replace audio", description = "Chỉ upload audio file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi upload")
    })
    public ResponseEntity<CustomApiResponse<ListeningLessonDTO>> uploadAudio(
            @PathVariable Long id,
            @RequestPart("audio") MultipartFile audioFile) {
        try {
            log.info("[ADMIN] Uploading audio for lesson: id={}", id);

            UpdateListeningLessonRequest request = new UpdateListeningLessonRequest();
            ListeningLessonDTO lesson = listeningAdminService.updateLesson(id, request, audioFile);

            log.info("[ADMIN] Uploaded audio for lesson: id={}", id);

            return ResponseEntity.ok(CustomApiResponse.success(lesson, "Upload audio thành công"));

        } catch (Exception e) {
            log.error("Error uploading audio for lesson {}: ", id, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e. getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // ORDER OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Lấy orderIndex tiếp theo
     */
    @GetMapping("/lessons/next-order")
    @Operation(summary = "Lấy orderIndex tiếp theo", description = "Trả về orderIndex cho bài nghe mới")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy orderIndex thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi lấy orderIndex")
    })
    public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextOrderIndex() {
        try {
            Integer nextOrderIndex = listeningAdminService.getNextLessonOrderIndex();
            return ResponseEntity.ok(
                    CustomApiResponse.success(
                            Map.of("nextOrderIndex", nextOrderIndex),
                            "Lấy orderIndex thành công"));
        } catch (Exception e) {
            log.error("Error getting next order index:  ", e);
            return ResponseEntity.badRequest()
                    . body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Thay đổi thứ tự bài nghe
     */
    @PostMapping("/lessons/{lessonId}/reorder")
    @Operation(summary = "Thay đổi thứ tự bài nghe", description = "Di chuyển bài nghe đến vị trí mới")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reorder thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi reorder")
    })
    public ResponseEntity<CustomApiResponse<Void>> reorderLesson(
            @Parameter(description = "ID của bài nghe") @PathVariable Long lessonId,
            @Parameter(description = "Vị trí mới (bắt đầu từ 1)") @RequestParam Integer newOrderIndex) {
        try {
            log.info("[ADMIN] Reordering lesson {} to position {}", lessonId, newOrderIndex);

            listeningAdminService.reorderLesson(lessonId, newOrderIndex);

            log.info("[ADMIN] Reordered lesson {} to position {}", lessonId, newOrderIndex);

            return ResponseEntity.ok(CustomApiResponse.success(null, "Thay đổi thứ tự thành công"));

        } catch (Exception e) {
            log.error("Error reordering lesson {}: ", lessonId, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e. getMessage()));
        }
    }

    /**
     * [ADMIN] Swap 2 lessons
     */
    @PostMapping("/lessons/{lessonId1}/swap/{lessonId2}")
    @Operation(summary = "Đổi vị trí 2 bài nghe", description = "Hoán đổi orderIndex của 2 bài nghe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Swap thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi swap")
    })
    public ResponseEntity<CustomApiResponse<Void>> swapLessons(
            @Parameter(description = "ID bài nghe thứ nhất") @PathVariable Long lessonId1,
            @Parameter(description = "ID bài nghe thứ hai") @PathVariable Long lessonId2) {
        try {
            log.info("[ADMIN] Swapping lessons {} and {}", lessonId1, lessonId2);

            listeningAdminService.swapLessons(lessonId1, lessonId2);

            log.info("[ADMIN] Swapped lessons {} and {}", lessonId1, lessonId2);

            return ResponseEntity.ok(CustomApiResponse.success(null, "Đổi vị trí thành công"));

        } catch (Exception e) {
            log.error("Error swapping lessons {} and {}: ", lessonId1, lessonId2, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e. getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // QUESTION OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Lấy câu hỏi theo lesson
     */
    @GetMapping("/lessons/{lessonId}/questions")
    @Operation(summary = "Lấy câu hỏi của bài nghe", description = "Trả về danh sách câu hỏi có pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi lấy danh sách")
    })
    public ResponseEntity<CustomApiResponse<PaginatedResponse<QuestionResponseDTO>>> getQuestionsByLesson(
            @Parameter(description = "ID của bài nghe") @PathVariable Long lessonId,
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Số items mỗi trang") @RequestParam(required = false) Integer size,
            @Parameter(description = "Sắp xếp theo") @RequestParam(required = false) String sort) {
        try {
            Pageable pageable = PaginationHelper. createPageable(page, size, sort);
            Page<QuestionResponseDTO> questionPage = questionService.getQuestionsByLessonPaginated(lessonId, pageable);

            return ResponseEntity.ok(
                    CustomApiResponse.success(
                            PaginatedResponse.of(questionPage),
                            "Lấy danh sách câu hỏi thành công"));
        } catch (Exception e) {
            log.error("Error getting questions for lesson {}: ", lessonId, e);
            return ResponseEntity. badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Lấy chi tiết câu hỏi
     */
    @GetMapping("/questions/{questionId}")
    @Operation(summary = "Lấy chi tiết câu hỏi", description = "Trả về thông tin chi tiết của một câu hỏi")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
            @ApiResponse(responseCode = "400", description = "Câu hỏi không tồn tại")
    })
    public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> getQuestionById(
            @Parameter(description = "ID của câu hỏi") @PathVariable Long questionId) {
        try {
            QuestionResponseDTO question = questionService.getQuestionById(questionId);
            return ResponseEntity.ok(CustomApiResponse.success(question, "Lấy chi tiết câu hỏi thành công"));
        } catch (Exception e) {
            log.error("Error getting question {}: ", questionId, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Tạo câu hỏi mới
     */
    @PostMapping("/questions")
    @Operation(summary = "Tạo câu hỏi mới", description = "Tạo một câu hỏi mới cho bài nghe")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo câu hỏi thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> createQuestion(
            @Valid @RequestBody CreateQuestionDTO createDTO) {
        try {
            log.info("[ADMIN] Creating question for lesson {}", createDTO.getParentId());

            QuestionResponseDTO question = questionService.createQuestion(createDTO);

            log.info("[ADMIN] Created question: id={}", question.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CustomApiResponse.success(question, "Tạo câu hỏi thành công"));

        } catch (Exception e) {
            log.error("Error creating question: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse. badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Cập nhật câu hỏi
     */
    @PutMapping("/questions/{id}")
    @Operation(summary = "Cập nhật câu hỏi", description = "Cập nhật thông tin câu hỏi")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> updateQuestion(
            @Parameter(description = "ID của câu hỏi") @PathVariable Long id,
            @Valid @RequestBody CreateQuestionDTO updateDTO) {
        try {
            log.info("[ADMIN] Updating question:  id={}", id);

            QuestionResponseDTO question = questionService.updateQuestion(id, updateDTO);

            log.info("[ADMIN] Updated question: id={}", id);

            return ResponseEntity.ok(CustomApiResponse.success(question, "Cập nhật câu hỏi thành công"));

        } catch (Exception e) {
            log.error("Error updating question {}: ", id, e);
            return ResponseEntity.badRequest()
                    . body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Xóa câu hỏi
     */
    @DeleteMapping("/questions/{id}")
    @Operation(summary = "Xóa câu hỏi", description = "Xóa vĩnh viễn một câu hỏi")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "400", description = "Không thể xóa")
    })
    public ResponseEntity<CustomApiResponse<Void>> deleteQuestion(
            @Parameter(description = "ID của câu hỏi") @PathVariable Long id) {
        try {
            log.info("[ADMIN] Deleting question: id={}", id);

            questionService.deleteQuestion(id);

            log.info("[ADMIN] Deleted question: id={}", id);

            return ResponseEntity.ok(CustomApiResponse.success(null, "Xóa câu hỏi thành công"));

        } catch (Exception e) {
            log.error("Error deleting question {}:  ", id, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Tạo nhiều câu hỏi cùng lúc
     */
    @PostMapping("/lessons/{lessonId}/questions/bulk")
    @Operation(summary = "Tạo nhiều câu hỏi", description = "Tạo nhiều câu hỏi cùng lúc cho một bài nghe")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<List<QuestionResponseDTO>>> createQuestionsInBulk(
            @Parameter(description = "ID của bài nghe") @PathVariable Long lessonId,
            @Valid @RequestBody List<CreateQuestionDTO> createDTOs) {
        try {
            log. info("[ADMIN] Creating {} questions in bulk for lesson {}", createDTOs.size(), lessonId);

            List<QuestionResponseDTO> questions = listeningAdminService.createQuestionsInBulk(lessonId, createDTOs);

            log.info("[ADMIN] Created {} questions for lesson {}", questions.size(), lessonId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CustomApiResponse.success(questions, "Tạo " + questions.size() + " câu hỏi thành công"));

        } catch (Exception e) {
            log.error("Error bulk creating questions for lesson {}: ", lessonId, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Xóa nhiều câu hỏi
     */
    @PostMapping("/questions/bulk-delete")
    @Operation(summary = "Xóa nhiều câu hỏi", description = "Xóa nhiều câu hỏi cùng lúc")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi xóa")
    })
    public ResponseEntity<CustomApiResponse<Map<String, Integer>>> bulkDeleteQuestions(
            @RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> questionIds = request.get("questionIds");

            log.info("[ADMIN] Bulk deleting {} questions", questionIds. size());

            int deleted = listeningAdminService.bulkDeleteQuestions(questionIds);

            log.info("[ADMIN] Deleted {} questions", deleted);

            return ResponseEntity. ok(
                    CustomApiResponse.success(
                            Map.of("deleted", deleted),
                            "Đã xóa " + deleted + " câu hỏi"));

        } catch (Exception e) {
            log.error("Error bulk deleting questions: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e. getMessage()));
        }
    }

    /**
     * [ADMIN] Lấy orderIndex tiếp theo cho question
     */
    @GetMapping("/lessons/{lessonId}/questions/next-order")
    @Operation(summary = "Lấy orderIndex tiếp theo", description = "Trả về orderIndex cho câu hỏi mới")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy orderIndex thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi lấy orderIndex")
    })
    public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextQuestionOrderIndex(
            @Parameter(description = "ID của bài nghe") @PathVariable Long lessonId) {
        try {
            Integer nextOrderIndex = questionService.getNextOrderIndex(lessonId);
            return ResponseEntity.ok(
                    CustomApiResponse.success(
                            Map.of("nextOrderIndex", nextOrderIndex),
                            "Lấy orderIndex thành công"));
        } catch (Exception e) {
            log.error("Error getting next question order index: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Copy questions từ lesson khác
     */
    @PostMapping("/lessons/{sourceLessonId}/copy-to/{targetLessonId}")
    @Operation(summary = "Copy câu hỏi", description = "Copy tất cả câu hỏi từ bài nghe này sang bài nghe khác")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Copy thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi copy")
    })
    public ResponseEntity<CustomApiResponse<Void>> copyQuestions(
            @Parameter(description = "ID bài nghe nguồn") @PathVariable Long sourceLessonId,
            @Parameter(description = "ID bài nghe đích") @PathVariable Long targetLessonId) {
        try {
            log.info("[ADMIN] Copying questions from lesson {} to lesson {}", sourceLessonId, targetLessonId);

            listeningAdminService.copyQuestionsToLesson(sourceLessonId, targetLessonId);

            log.info("[ADMIN] Copied questions from lesson {} to lesson {}", sourceLessonId, targetLessonId);

            return ResponseEntity.ok(CustomApiResponse.success(null, "Copy câu hỏi thành công"));

        } catch (Exception e) {
            log.error("Error copying questions from {} to {}: ", sourceLessonId, targetLessonId, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // VALIDATION OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Validate và fix orderIndex của tất cả lessons
     */
    @PostMapping("/lessons/validate-all-order")
    @Operation(summary = "Validate orderIndex lessons", description = "Validate và fix orderIndex của tất cả bài nghe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi validation")
    })
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllLessonsOrder() {
        try {
            log.info("[ADMIN] Validating all listening lessons orderIndex");

            Map<String, Object> result = listeningAdminService.validateAllLessonsOrderIndex();

            log.info("[ADMIN] Lesson order validation result: {}", result);

            return ResponseEntity.ok(CustomApiResponse.success(result, "Validation hoàn tất"));

        } catch (Exception e) {
            log.error("Error validating lessons order: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse. badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Validate orderIndex của questions trong 1 lesson
     */
    @PostMapping("/lessons/{lessonId}/questions/validate-order")
    @Operation(summary = "Validate orderIndex questions", description = "Validate và fix orderIndex của câu hỏi trong bài nghe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi validation")
    })
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateQuestionsOrder(
            @Parameter(description = "ID của bài nghe") @PathVariable Long lessonId) {
        try {
            log.info("[ADMIN] Validating questions orderIndex for lesson {}", lessonId);

            Map<String, Object> result = listeningAdminService.validateQuestionsOrderIndex(lessonId);

            log.info("[ADMIN] Question order validation result for lesson {}: {}", lessonId, result);

            return ResponseEntity.ok(CustomApiResponse.success(result, "Validation hoàn tất"));

        } catch (Exception e) {
            log.error("Error validating questions order for lesson {}: ", lessonId, e);
            return ResponseEntity.badRequest()
                    . body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Validate tất cả questions orderIndex
     */
    @PostMapping("/questions/validate-all-order")
    @Operation(summary = "Validate tất cả questions", description = "Validate và fix orderIndex của tất cả câu hỏi")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi validation")
    })
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllQuestionsOrder() {
        try {
            log.info("[ADMIN] Validating all listening questions orderIndex");

            Map<String, Object> result = listeningAdminService.validateAllQuestionsOrderIndex();

            log.info("[ADMIN] All questions order validation result: {}", result);

            return ResponseEntity.ok(CustomApiResponse.success(result, "Validation hoàn tất"));

        } catch (Exception e) {
            log.error("Error validating all questions order:  ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Validate audio files
     */
    @PostMapping("/validate-audio-files")
    @Operation(summary = "Validate audio files", description = "Kiểm tra audio files có tồn tại trên disk không")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi validation")
    })
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAudioFiles() {
        try {
            log.info("[ADMIN] Validating audio files");

            Map<String, Object> result = listeningAdminService. validateAudioFiles();

            log.info("[ADMIN] Audio files validation result: {}", result);

            return ResponseEntity.ok(CustomApiResponse.success(result, "Validation hoàn tất"));

        } catch (Exception e) {
            log.error("Error validating audio files: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e. getMessage()));
        }
    }

    /**
     * [ADMIN] Health check toàn bộ module
     */
    @PostMapping("/health-check")
    @Operation(summary = "Health check", description = "Kiểm tra toàn bộ Listening module (lessons, questions, audio files)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Health check thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi health check")
    })
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> healthCheck() {
        try {
            log. info("[ADMIN] Running health check for Listening module");

            Map<String, Object> result = listeningAdminService.healthCheck();

            log.info("[ADMIN] Health check completed: {}", result.get("summary"));

            return ResponseEntity.ok(CustomApiResponse.success(result, "Health check hoàn tất"));

        } catch (Exception e) {
            log.error("Error during health check: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }
}