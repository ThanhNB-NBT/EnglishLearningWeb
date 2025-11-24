package com.thanhnb.englishlearning.controller.grammar;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.ParseResult;
import com.thanhnb.englishlearning.dto.grammar.request.ReorderLessonRequest;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.service.grammar.GrammarAdminService;
import com.thanhnb.englishlearning.service.ai.grammar.GrammarAIParsingService;
import com.thanhnb.englishlearning.util.PaginationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;
import java.util.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/admin/grammar")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Grammar Admin", description = "API quản lý ngữ pháp (dành cho ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class GrammarAdminController {

        private final GrammarAdminService grammarAdminService;
        private final GrammarAIParsingService grammarAIParsingService;

        // ═══════════════════════════════════════════════════════════════════════
        // GEMINI AI PARSING (PDF/DOCX/Image)
        // ═══════════════════════════════════════════════════════════════════════

        @PostMapping("/topics/{topicId}/parse-file")
        @Operation(summary = "Parse file (PDF/DOCX/Image) thành Grammar lessons", description = "Sử dụng AI (Gemini) để phân tích file và tạo lessons với questions. "
                        +
                        "Hỗ trợ PDF (có thể chọn pages), DOCX, và Image (JPG/PNG/WEBP).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Parse thành công, trả về danh sách lessons"),
                        @ApiResponse(responseCode = "400", description = "File không hợp lệ hoặc topic không tồn tại"),
                        @ApiResponse(responseCode = "500", description = "Lỗi server hoặc Gemini API")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> parseFile(
                        @Parameter(description = "ID của grammar topic", required = true) @PathVariable Long topicId,

                        @Parameter(description = "File PDF/DOCX/Image (max 20MB)", required = true) @RequestParam("file") MultipartFile file,

                        @Parameter(description = "Danh sách số trang cần parse (chỉ cho PDF). VD: [1,2,3,5,7]. Nếu không có thì parse toàn bộ.") @RequestParam(required = false) List<Integer> pages) {

                try {
                        log.info("Parse file request: file={}, topicId={}, pages={}",
                                        file.getOriginalFilename(), topicId,
                                        pages != null ? pages.size() + " selected" : "all");

                        ParseResult result = grammarAIParsingService.parseFileWithTopicId(file, topicId, pages);

                        if (result == null || result.lessons == null || result.lessons.isEmpty()) {
                                log.warn("AI returned empty result");
                                return ResponseEntity.badRequest()
                                                .body(CustomApiResponse.badRequest(
                                                                "AI không trả về lessons nào. Vui lòng kiểm tra nội dung file."));
                        }

                        // Calculate statistics
                        long theoryCount = result.lessons.stream()
                                        .filter(l -> l.getLessonType() != null
                                                        && "THEORY".equals(l.getLessonType().name()))
                                        .count();

                        long practiceCount = result.lessons.stream()
                                        .filter(l -> l.getLessonType() != null
                                                        && "PRACTICE".equals(l.getLessonType().name()))
                                        .count();

                        int totalQuestions = result.lessons.stream()
                                        .filter(l -> l.getQuestions() != null)
                                        .mapToInt(l -> l.getQuestions().size())
                                        .sum();

                        Map<String, Object> summary = new HashMap<>();
                        summary.put("fileName", file.getOriginalFilename());
                        summary.put("fileSize", String.format("%.2f MB", file.getSize() / (1024.0 * 1024.0)));
                        summary.put("fileType", file.getContentType());
                        summary.put("pagesProcessed", pages != null ? pages.size() : "all");
                        summary.put("topicId", topicId);
                        summary.put("totalLessons", result.lessons.size());
                        summary.put("theoryLessons", theoryCount);
                        summary.put("practiceLessons", practiceCount);
                        summary.put("totalQuestions", totalQuestions);

                        Map<String, Object> response = new HashMap<>();
                        response.put("parsedData", result);
                        response.put("summary", summary);

                        log.info("Parse success: {} lessons ({} theory, {} practice), {} questions",
                                        result.lessons.size(), theoryCount, practiceCount, totalQuestions);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(response,
                                                        String.format("Phân tích thành công! Tạo được %d bài học với %d câu hỏi.",
                                                                        result.lessons.size(), totalQuestions)));

                } catch (IllegalArgumentException e) {
                        log.warn("Invalid request: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));

                } catch (Exception e) {
                        log.error("Parse file error: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest(
                                                        String.format("Lỗi khi parse file: %s",
                                                                        e.getMessage() != null ? e.getMessage()
                                                                                        : "Unknown error")));
                }
        }

        @PostMapping("/topics/{topicId}/save-parsed-lessons")
        @Operation(summary = "Lưu parsed lessons vào database", description = "Lưu các bài học đã được parse từ file vào database. "
                        +
                        "Tự động tạo lessons và questions với orderIndex đã được adjust.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Import thành công vào database"),
                        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc topic không tồn tại"),
                        @ApiResponse(responseCode = "500", description = "Lỗi khi lưu vào database")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> saveParsedLessons(
                        @Parameter(description = "ID của grammar topic", required = true) @PathVariable Long topicId,

                        @Parameter(description = "ParseResult từ endpoint parse-file", required = true) @RequestBody ParseResult parsedResult) {

                try {
                        log.info("Saving {} parsed lessons for topicId={}",
                                        parsedResult.lessons != null ? parsedResult.lessons.size() : 0, topicId);

                        if (parsedResult == null || parsedResult.lessons == null || parsedResult.lessons.isEmpty()) {
                                log.warn("Empty parsed result");
                                return ResponseEntity.badRequest()
                                                .body(CustomApiResponse.badRequest("Không có lesson nào để import"));
                        }

                        List<GrammarLessonDTO> savedLessons = grammarAdminService.importLessonsFromFile(
                                        topicId, parsedResult.lessons);

                        if (savedLessons.isEmpty()) {
                                log.warn("No lessons were saved");
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(CustomApiResponse
                                                                .badRequest("Không thể lưu lessons vào database"));
                        }

                        // Calculate statistics
                        long theoryCount = savedLessons.stream()
                                        .filter(l -> l.getLessonType() != null
                                                        && "THEORY".equals(l.getLessonType().name()))
                                        .count();

                        long practiceCount = savedLessons.stream()
                                        .filter(l -> l.getLessonType() != null
                                                        && "PRACTICE".equals(l.getLessonType().name()))
                                        .count();

                        int totalQuestionsCreated = savedLessons.stream()
                                        .mapToInt(GrammarLessonDTO::getQuestionCount)
                                        .sum();

                        Map<String, Object> summary = new HashMap<>();
                        summary.put("topicId", topicId);
                        summary.put("lessonsCreated", savedLessons.size());
                        summary.put("theoryLessons", theoryCount);
                        summary.put("practiceLessons", practiceCount);
                        summary.put("questionsCreated", totalQuestionsCreated);

                        Map<String, Object> result = new HashMap<>();
                        result.put("summary", summary);
                        result.put("lessons", savedLessons);

                        log.info("Save success: {} lessons ({} theory, {} practice), {} questions created",
                                        savedLessons.size(), theoryCount, practiceCount, totalQuestionsCreated);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result,
                                                        String.format("Import thành công %d bài học và %d câu hỏi!",
                                                                        savedLessons.size(), totalQuestionsCreated)));

                } catch (RuntimeException e) {
                        log.error("Business logic error: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(String.format("Lỗi: %s", e.getMessage())));

                } catch (Exception e) {
                        log.error("Unexpected error: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest(
                                                        String.format("Lỗi khi lưu bài học: %s",
                                                                        e.getMessage() != null ? e.getMessage()
                                                                                        : "Unknown error")));
                }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // TOPIC MANAGEMENT
        // ═══════════════════════════════════════════════════════════════════════

        @GetMapping("/topics")
        @Operation(summary = "Lấy tất cả topics với phân trang")
        public ResponseEntity<CustomApiResponse<PaginatedResponse<GrammarTopicDTO>>> getAllTopics(
                        @RequestParam(required = false) Integer page,
                        @RequestParam(required = false) Integer size,
                        @RequestParam(required = false) String sort) {
                try {
                        Pageable pageable = PaginationHelper.createPageable(page, size, sort);
                        Page<GrammarTopicDTO> topicPage = grammarAdminService.getAllTopicsPaginated(pageable);
                        PaginatedResponse<GrammarTopicDTO> response = PaginatedResponse.of(topicPage);

                        return ResponseEntity
                                        .ok(CustomApiResponse.success(response, "Lấy danh sách topics thành công"));
                } catch (Exception e) {
                        log.error("Error getting topics: ", e);
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @GetMapping("/topics/{id}")
        @Operation(summary = "Lấy chi tiết topic")
        public ResponseEntity<CustomApiResponse<GrammarTopicDTO>> getTopicById(@PathVariable Long id) {
                try {
                        GrammarTopicDTO topic = grammarAdminService.getTopicById(id);
                        return ResponseEntity.ok(CustomApiResponse.success(topic, "Lấy chi tiết thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/topics")
        @Operation(summary = "Tạo topic mới")
        public ResponseEntity<CustomApiResponse<GrammarTopicDTO>> createTopic(@Valid @RequestBody GrammarTopicDTO dto) {
                try {
                        GrammarTopicDTO created = grammarAdminService.createTopic(dto);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(created, "Tạo topic thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PutMapping("/topics/{id}")
        @Operation(summary = "Cập nhật topic")
        public ResponseEntity<CustomApiResponse<GrammarTopicDTO>> updateTopic(
                        @PathVariable Long id, @Valid @RequestBody GrammarTopicDTO dto) {
                try {
                        GrammarTopicDTO updated = grammarAdminService.updateTopic(id, dto);
                        return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @DeleteMapping("/topics/{id}")
        @Operation(summary = "Xóa topic")
        public ResponseEntity<CustomApiResponse<String>> deleteTopic(@PathVariable Long id) {
                try {
                        grammarAdminService.deleteTopic(id);
                        return ResponseEntity.ok(CustomApiResponse.success("Xóa thành công", "Xóa thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PatchMapping("/topics/{id}/deactivate")
        @Operation(summary = "Tắt topic (set isActive = false)")
        public ResponseEntity<CustomApiResponse<String>> deactivateTopic(@PathVariable Long id) {
                try {
                        grammarAdminService.deactivateTopic(id);
                        return ResponseEntity.ok(CustomApiResponse.success("Đã tắt topic", "Đã tắt topic"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @GetMapping("/topics/next-order")
        @Operation(summary = "Lấy orderIndex tiếp theo cho topic mới")
        public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextTopicOrderIndex() {
                try {
                        Integer nextOrder = grammarAdminService.getNextTopicOrderIndex();
                        Map<String, Integer> result = new HashMap<>();
                        result.put("nextOrderIndex", nextOrder);
                        return ResponseEntity.ok(CustomApiResponse.success(result, "Lấy orderIndex thành công"));
                } catch (Exception e) {
                        log.error("Error getting next topic orderIndex: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // LESSON MANAGEMENT
        // ═══════════════════════════════════════════════════════════════════════

        @GetMapping("/topics/{topicId}/lessons")
        @Operation(summary = "Lấy lessons theo topic với phân trang")
        public ResponseEntity<CustomApiResponse<PaginatedResponse<GrammarLessonDTO>>> getLessonsByTopic(
                        @PathVariable Long topicId,
                        @RequestParam(required = false) Integer page,
                        @RequestParam(required = false) Integer size,
                        @RequestParam(required = false) String sort) {
                try {
                        Pageable pageable = PaginationHelper.createPageable(page, size, sort);
                        Page<GrammarLessonDTO> lessonPage = grammarAdminService.getLessonsByTopicPaginated(topicId,
                                        pageable);
                        PaginatedResponse<GrammarLessonDTO> response = PaginatedResponse.of(lessonPage);

                        return ResponseEntity
                                        .ok(CustomApiResponse.success(response, "Lấy danh sách lessons thành công"));
                } catch (Exception e) {
                        log.error("Error getting lessons: ", e);
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @GetMapping("/lessons/{lessonId}")
        @Operation(summary = "Lấy chi tiết lesson")
        public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> getLessonDetail(@PathVariable Long lessonId) {
                try {
                        GrammarLessonDTO lesson = grammarAdminService.getLessonDetail(lessonId);
                        return ResponseEntity.ok(CustomApiResponse.success(lesson, "Lấy chi tiết thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons")
        @Operation(summary = "Tạo lesson mới")
        public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> createLesson(
                        @Valid @RequestBody GrammarLessonDTO dto) {
                try {
                        GrammarLessonDTO created = grammarAdminService.createLesson(dto);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(created, "Tạo lesson thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PutMapping("/lessons/{id}")
        @Operation(summary = "Cập nhật lesson")
        public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> updateLesson(
                        @PathVariable Long id, @Valid @RequestBody GrammarLessonDTO dto) {
                try {
                        GrammarLessonDTO updated = grammarAdminService.updateLesson(id, dto);
                        return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @DeleteMapping("/lessons/{id}")
        @Operation(summary = "Xóa lesson")
        public ResponseEntity<CustomApiResponse<String>> deleteLesson(
                        @PathVariable Long id,
                        @RequestParam(required = false, defaultValue = "false") boolean cascade) {
                try {
                        grammarAdminService.deleteLesson(id, cascade);
                        return ResponseEntity.ok(CustomApiResponse.success("Xóa thành công", "Xóa thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PatchMapping("/lessons/{id}/deactivate")
        @Operation(summary = "Tắt lesson (set isActive = false)")
        public ResponseEntity<CustomApiResponse<String>> deactivateLesson(@PathVariable Long id) {
                try {
                        grammarAdminService.deactivateLesson(id);
                        return ResponseEntity.ok(CustomApiResponse.success("Đã tắt lesson", "Đã tắt lesson"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @GetMapping("/topics/{topicId}/lessons/next-order")
        @Operation(summary = "Lấy orderIndex tiếp theo cho lesson mới")
        public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextLessonOrderIndex(
                        @PathVariable Long topicId) {
                try {
                        Integer nextOrder = grammarAdminService.getNextLessonOrderIndex(topicId);
                        Map<String, Integer> result = new HashMap<>();
                        result.put("nextOrderIndex", nextOrder);
                        return ResponseEntity.ok(CustomApiResponse.success(result, "Lấy orderIndex thành công"));
                } catch (RuntimeException e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("Error getting next lesson orderIndex: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/topics/{topicId}/lessons/reorder")
        @Operation(summary = "Sắp xếp lại thứ tự lessons")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> reorderLessons(
                        @PathVariable Long topicId,
                        @Valid @RequestBody ReorderLessonRequest request) {
                try {
                        int affectedCount = grammarAdminService.reorderLessons(
                                        topicId, request.getInsertPosition(), request.getExcludeLessonId());

                        Map<String, Object> result = new HashMap<>();
                        result.put("topicId", topicId);
                        result.put("insertPosition", request.getInsertPosition());
                        result.put("affectedLessons", affectedCount);

                        return ResponseEntity.ok(CustomApiResponse.success(result,
                                        "Đã sắp xếp lại " + affectedCount + " bài học"));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("Error reordering lessons: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi khi sắp xếp lại: " + e.getMessage()));
                }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // QUESTION MANAGEMENT (Metadata-based)
        // ═══════════════════════════════════════════════════════════════════════

        @GetMapping("/lessons/{lessonId}/questions")
        @Operation(summary = "Lấy questions theo lesson với phân trang")
        public ResponseEntity<CustomApiResponse<PaginatedResponse<QuestionResponseDTO>>> getQuestionsByLesson(
                        @PathVariable Long lessonId,
                        @RequestParam(required = false) Integer page,
                        @RequestParam(required = false) Integer size,
                        @RequestParam(required = false) String sort) {
                try {
                        Pageable pageable = PaginationHelper.createPageable(page, size, sort);
                        Page<QuestionResponseDTO> questionPage = grammarAdminService
                                        .getQuestionsByLessonPaginated(lessonId, pageable);
                        PaginatedResponse<QuestionResponseDTO> response = PaginatedResponse.of(questionPage);

                        return ResponseEntity
                                        .ok(CustomApiResponse.success(response, "Lấy danh sách questions thành công"));
                } catch (Exception e) {
                        log.error("Error getting questions: ", e);
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @GetMapping("/questions/{id}")
        @Operation(summary = "Lấy chi tiết question")
        public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> getQuestionById(@PathVariable Long id) {
                try {
                        QuestionResponseDTO question = grammarAdminService.getQuestionById(id);
                        return ResponseEntity.ok(CustomApiResponse.success(question, "Lấy chi tiết thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/questions")
        @Operation(summary = "Tạo question mới")
        public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> createQuestion(
                        @Valid @RequestBody CreateQuestionDTO dto) {
                try {
                        QuestionResponseDTO created = grammarAdminService.createQuestion(dto);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(created, "Tạo question thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PutMapping("/questions/{id}")
        @Operation(summary = "Cập nhật question")
        public ResponseEntity<CustomApiResponse<QuestionResponseDTO>> updateQuestion(
                        @PathVariable Long id, @Valid @RequestBody QuestionResponseDTO dto) {
                try {
                        QuestionResponseDTO updated = grammarAdminService.updateQuestion(id, dto);
                        return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @DeleteMapping("/questions/{id}")
        @Operation(summary = "Xóa question")
        public ResponseEntity<CustomApiResponse<String>> deleteQuestion(@PathVariable Long id) {
                try {
                        grammarAdminService.deleteQuestion(id);
                        return ResponseEntity.ok(CustomApiResponse.success("Xóa thành công", "Xóa thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/questions/bulk-delete")
        @Operation(summary = "Xóa nhiều questions")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> bulkDeleteQuestions(
                        @RequestBody Map<String, List<Long>> payload) {
                try {
                        List<Long> ids = payload.getOrDefault("questionIds", Collections.emptyList());
                        int deleted = grammarAdminService.bulkDeleteQuestions(ids);
                        Map<String, Object> result = Map.of("requested", ids.size(), "deleted", deleted);
                        return ResponseEntity.ok(CustomApiResponse.success(result, "Đã xóa " + deleted + " câu hỏi"));
                } catch (Exception e) {
                        log.error("Bulk delete questions error", e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi khi xóa hàng loạt: " + e.getMessage()));
                }
        }

        @GetMapping("/lessons/{lessonId}/questions/next-order")
        @Operation(summary = "Lấy orderIndex tiếp theo cho question mới")
        public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextQuestionOrderIndex(
                        @PathVariable Long lessonId) {
                try {
                        Integer nextOrder = grammarAdminService.getNextQuestionOrderIndex(lessonId);
                        Map<String, Integer> result = new HashMap<>();
                        result.put("nextOrderIndex", nextOrder);
                        return ResponseEntity.ok(CustomApiResponse.success(result, "Lấy orderIndex thành công"));
                } catch (RuntimeException e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("Error getting next question orderIndex: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/{lessonId}/questions/bulk")
        @Operation(summary = "Tạo nhiều questions cùng lúc")
        public ResponseEntity<CustomApiResponse<List<QuestionResponseDTO>>> createQuestionsInBulk(
                        @PathVariable Long lessonId,
                        @RequestBody List<CreateQuestionDTO> questions) {
                try {
                        List<QuestionResponseDTO> created = grammarAdminService.createQuestionsInBulk(lessonId,
                                        questions);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(created,
                                                        "Tạo thành công " + created.size() + " questions"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/{sourceLessonId}/copy-to/{targetLessonId}")
        @Operation(summary = "Copy questions giữa lessons")
        public ResponseEntity<CustomApiResponse<String>> copyQuestions(
                        @PathVariable Long sourceLessonId,
                        @PathVariable Long targetLessonId) {
                try {
                        grammarAdminService.copyQuestionsToLesson(sourceLessonId, targetLessonId);
                        return ResponseEntity.ok(CustomApiResponse.success("Copy questions thành công",
                                        "Copy questions thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // VALIDATION ENDPOINTS (Health check / Maintenance)
        // ═══════════════════════════════════════════════════════════════════════

        @PostMapping("/topics/validate-all-order")
        @Operation(summary = "Validate và fix orderIndex của tất cả topics")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllTopicsOrder() {
                try {
                        Map<String, Object> result = grammarAdminService.validateAllTopicsOrderIndex();
                        String message = result.get("issuesFixed").equals(0)
                                        ? "OrderIndex của topics đã đúng, không cần fix"
                                        : "Đã fix " + result.get("issuesFixed") + " vấn đề orderIndex trong topics";
                        return ResponseEntity.ok(CustomApiResponse.success(result, message));
                } catch (Exception e) {
                        log.error("Error validating all topics order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/topics/{topicId}/lessons/validate-order")
        @Operation(summary = "Validate và fix orderIndex của lessons trong topic")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateLessonOrder(@PathVariable Long topicId) {
                try {
                        Map<String, Object> result = grammarAdminService.validateLessonsOrderIndex(topicId);
                        String message = result.get("issuesFixed").equals(0)
                                        ? "OrderIndex của lessons đã đúng, không cần fix"
                                        : "Đã fix " + result.get("issuesFixed") + " vấn đề orderIndex trong lessons";
                        return ResponseEntity.ok(CustomApiResponse.success(result, message));
                } catch (RuntimeException e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("Error validating lesson order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/validate-all-order")
        @Operation(summary = "Validate và fix orderIndex của tất cả lessons")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllLessonsOrder() {
                try {
                        Map<String, Object> result = grammarAdminService.validateAllLessonsOrderIndex();
                        String message = result.get("totalIssuesFixed").equals(0)
                                        ? "OrderIndex của tất cả lessons đã đúng, không cần fix"
                                        : "Đã fix " + result.get("totalIssuesFixed") + " vấn đề orderIndex trong "
                                                        + result.get("totalLessons") + " lessons";
                        return ResponseEntity.ok(CustomApiResponse.success(result, message));
                } catch (Exception e) {
                        log.error("Error validating all lessons order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/{lessonId}/questions/validate-order")
        @Operation(summary = "Validate và fix orderIndex của questions trong lesson")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateQuestionOrder(
                        @PathVariable Long lessonId) {
                try {
                        Map<String, Object> result = grammarAdminService.validateQuestionsOrderIndex(lessonId);
                        String message = result.get("issuesFixed").equals(0)
                                        ? "OrderIndex của questions đã đúng, không cần fix"
                                        : "Đã fix " + result.get("issuesFixed") + " vấn đề orderIndex trong questions";
                        return ResponseEntity.ok(CustomApiResponse.success(result, message));
                } catch (RuntimeException e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("Error validating question order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/topics/{topicId}/questions/validate-all-order")
        @Operation(summary = "Validate và fix orderIndex của tất cả questions trong topic")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllQuestionsInTopic(
                        @PathVariable Long topicId) {
                try {
                        Map<String, Object> result = grammarAdminService.validateAllQuestionsInTopic(topicId);
                        String message = result.get("totalIssuesFixed").equals(0)
                                        ? "OrderIndex của tất cả questions đã đúng, không cần fix"
                                        : "Đã fix " + result.get("totalIssuesFixed") + " vấn đề orderIndex trong "
                                                        + result.get("totalQuestions") + " questions";
                        return ResponseEntity.ok(CustomApiResponse.success(result, message));
                } catch (RuntimeException e) {
                        return ResponseEntity.badRequest().body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("Error validating all questions in topic: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/questions/validate-all-order")
        @Operation(summary = "Validate và fix orderIndex của TẤT CẢ questions")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllQuestionsOrder() {
                try {
                        Map<String, Object> result = grammarAdminService.validateAllQuestionsOrderIndex();
                        String message = result.get("totalIssuesFixed").equals(0)
                                        ? "OrderIndex của tất cả questions đã đúng, không cần fix"
                                        : "Đã fix " + result.get("totalIssuesFixed") + " vấn đề orderIndex trong "
                                                        + result.get("totalQuestions") + " questions";
                        return ResponseEntity.ok(CustomApiResponse.success(result, message));
                } catch (Exception e) {
                        log.error("Error validating all questions order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }
}