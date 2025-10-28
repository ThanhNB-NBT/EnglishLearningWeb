package com.thanhnb.englishlearning.controller;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.ParseResult;
import com.thanhnb.englishlearning.service.GeminiPDFService;
import com.thanhnb.englishlearning.service.grammar.GrammarAdminService;
import com.thanhnb.englishlearning.util.PaginationHelper;
import com.thanhnb.englishlearning.dto.grammar.request.ReorderLessonRequest;

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
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/admin/grammar")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Grammar Admin", description = "API quản lý ngữ pháp (dành cho ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class GrammarAdminController {

        private final GrammarAdminService grammarAdminService;
        private final GeminiPDFService geminiPDFService;

        // ===== GEMINI PDF PARSING =====

        @PostMapping("/topics/{topicId}/parse-file")
        @Operation(summary = "Parse PDF/Image với page selection", description = "Upload PDF (chọn pages) hoặc Image để AI phân tích thành lessons")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Parse thành công"),
                        @ApiResponse(responseCode = "400", description = "File không hợp lệ"),
                        @ApiResponse(responseCode = "500", description = "Lỗi server hoặc Gemini API")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> parseFile(
                        @Parameter(description = "ID của topic") @PathVariable Long topicId,
                        @Parameter(description = "File PDF hoặc Image (max 20MB)") @RequestParam("file") MultipartFile file,
                        @Parameter(description = "Danh sách pages cần parse (nếu là PDF). VD: [1,2,3,5,7]") @RequestParam(required = false) List<Integer> pages) {
                try {
                        log.info("📄 Received file parsing request: file={}, topicId={}, pages={}",
                                        file.getOriginalFilename(), topicId, pages);

                        // ✅ Call service with page selection
                        ParseResult result = geminiPDFService.parseFile(file, topicId, pages);

                        // Tính summary
                        long theoryCount = result.lessons.stream()
                                        .filter(l -> "THEORY".equals(l.getLessonType().name()))
                                        .count();

                        long practiceCount = result.lessons.stream()
                                        .filter(l -> "PRACTICE".equals(l.getLessonType().name()))
                                        .count();

                        int totalQuestions = result.lessons.stream()
                                        .filter(l -> l.getQuestions() != null)
                                        .mapToInt(l -> l.getQuestions().size())
                                        .sum();

                        Map<String, Object> response = new HashMap<>();
                        response.put("parsedData", result);
                        response.put("summary", Map.of(
                                        "fileName", file.getOriginalFilename(),
                                        "fileSize", String.format("%.2f MB", file.getSize() / (1024.0 * 1024.0)),
                                        "pagesProcessed", pages != null ? pages.size() : "all",
                                        "totalLessons", result.lessons.size(),
                                        "theoryLessons", theoryCount,
                                        "practiceLessons", practiceCount,
                                        "totalQuestions", totalQuestions,
                                        "averageQuestionsPerPractice",
                                        practiceCount > 0 ? totalQuestions / practiceCount : 0));

                        log.info("✅ File parsed successfully: {} lessons, {} questions",
                                        result.lessons.size(), totalQuestions);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(response,
                                                        "✅ Phân tích thành công! " + result.lessons.size()
                                                                        + " bài học được tạo."));

                } catch (IllegalArgumentException e) {
                        log.warn("⚠️ Invalid request: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("❌ Error parsing file: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi khi parse file: " + e.getMessage()));
                }
        }

        @PostMapping("/topics/{topicId}/save-parsed-lessons")
        @Operation(summary = "Import lessons từ kết quả parse", description = "Lưu các bài học đã được parse từ PDF vào database với questions")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Import thành công"),
                        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
                        @ApiResponse(responseCode = "500", description = "Lỗi khi lưu vào database")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> saveParsedLessons(
                        @Parameter(description = "ID của topic") @PathVariable Long topicId,
                        @RequestBody ParseResult parsedResult) {
                try {
                        log.info("💾 Saving {} parsed lessons for topicId={}",
                                        parsedResult.lessons.size(), topicId);

                        if (parsedResult.lessons == null || parsedResult.lessons.isEmpty()) {
                                return ResponseEntity.badRequest()
                                                .body(CustomApiResponse.badRequest("Không có lesson nào để import"));
                        }

                        // ✅ GỌI SERVICE METHOD MỚI - Import cả lessons và questions
                        List<GrammarLessonDTO> savedLessons = grammarAdminService.importLessonsFromPDF(
                                        topicId, parsedResult.lessons);

                        // Tính tổng số questions
                        int totalQuestionsCreated = savedLessons.stream()
                                        .filter(l -> l.getQuestionCount() != null)
                                        .mapToInt(GrammarLessonDTO::getQuestionCount)
                                        .sum();

                        Map<String, Object> result = new HashMap<>();
                        result.put("lessonsCreated", savedLessons.size());
                        result.put("questionsCreated", totalQuestionsCreated);
                        result.put("lessons", savedLessons.stream()
                                        .map(l -> Map.of(
                                                        "id", l.getId(),
                                                        "title", l.getTitle(),
                                                        "lessonType", l.getLessonType(),
                                                        "orderIndex", l.getOrderIndex(),
                                                        "questionCount",
                                                        l.getQuestionCount() != null ? l.getQuestionCount() : 0))
                                        .collect(Collectors.toList()));

                        log.info("✅ Successfully imported {} lessons with {} questions",
                                        savedLessons.size(), totalQuestionsCreated);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result,
                                                        "✅ Import thành công " + savedLessons.size() + " bài học và "
                                                                        + totalQuestionsCreated + " câu hỏi!"));

                } catch (Exception e) {
                        log.error("❌ Error saving parsed lessons: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi khi lưu bài học: " + e.getMessage()));
                }
        }

        // ===== TOPIC MANAGEMENT =====

        @GetMapping("/topics")
        @Operation(summary = "Lấy tất cả topics với phân trang", description = "Trả về danh sách topics có phân trang và sorting")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
                        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
        })
        public ResponseEntity<CustomApiResponse<PaginatedResponse<GrammarTopicDTO>>> getAllTopics(
                        @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(required = false) Integer page,

                        @Parameter(description = "Số items mỗi trang (max: 100)") @RequestParam(required = false) Integer size,

                        @Parameter(description = "Sắp xếp theo (VD: orderIndex,asc hoặc createdAt,desc | name,asc)") @RequestParam(required = false) String sort) {
                try {
                        Pageable pageable = PaginationHelper.createPageable(page, size, sort);
                        Page<GrammarTopicDTO> topicPage = grammarAdminService.getAllTopicsPaginated(pageable);

                        PaginatedResponse<GrammarTopicDTO> response = PaginatedResponse.of(topicPage);

                        log.info("✅ Retrieved page {}/{} with {} topics",
                                        response.getPagination().getCurrentPage() + 1,
                                        response.getPagination().getTotalPages(),
                                        topicPage.getNumberOfElements());

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(response, "Lấy danh sách topics thành công"));

                } catch (Exception e) {
                        log.error("❌ Error getting paginated topics: ", e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/topics")
        @Operation(summary = "Tạo topic mới", description = "Tạo một topic ngữ pháp mới")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Tạo thành công"),
                        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
        })
        public ResponseEntity<CustomApiResponse<GrammarTopicDTO>> createTopic(
                        @Valid @RequestBody GrammarTopicDTO dto) {
                try {
                        GrammarTopicDTO created = grammarAdminService.createTopic(dto);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(created, "Tạo topic thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PutMapping("/topics/{id}")
        @Operation(summary = "Cập nhật topic", description = "Cập nhật topic theo ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
                        @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc dữ liệu không hợp lệ")
        })
        public ResponseEntity<CustomApiResponse<GrammarTopicDTO>> updateTopic(
                        @Parameter(description = "ID của topic") @PathVariable Long id,
                        @Valid @RequestBody GrammarTopicDTO dto) {
                try {
                        GrammarTopicDTO updated = grammarAdminService.updateTopic(id, dto);
                        return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @DeleteMapping("/topics/{id}")
        @Operation(summary = "Xóa topic", description = "Xóa topic theo ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Xóa thành công"),
                        @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc xóa thất bại")
        })
        public ResponseEntity<CustomApiResponse<String>> deleteTopic(
                        @Parameter(description = "ID của topic") @PathVariable Long id) {
                try {
                        grammarAdminService.deleteTopic(id);
                        return ResponseEntity.ok(CustomApiResponse.success("Xóa thành công", "Xóa thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @GetMapping("/topics/next-order")
        @Operation(summary = "Lấy orderIndex tiếp theo cho topic mới", description = "Trả về orderIndex tiếp theo dựa trên topic có orderIndex lớn nhất")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy thành công"),
                        @ApiResponse(responseCode = "500", description = "Lỗi server")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextTopicOrderIndex() {
                try {
                        Integer nextOrder = grammarAdminService.getNextTopicOrderIndex();
                        Map<String, Integer> result = new HashMap<>();
                        result.put("nextOrderIndex", nextOrder);

                        log.info("✅ Next topic orderIndex: {}", nextOrder);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result, "Lấy orderIndex thành công"));
                } catch (Exception e) {
                        log.error("❌ Error getting next topic orderIndex: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ===== LESSON MANAGEMENT =====

        @GetMapping("/topics/{topicId}/lessons")
        @Operation(summary = "Lấy lessons theo topic với phân trang", description = "Trả về danh sách lessons thuộc topic có phân trang")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
                        @ApiResponse(responseCode = "400", description = "Topic không tồn tại")
        })
        public ResponseEntity<CustomApiResponse<PaginatedResponse<GrammarLessonDTO>>> getLessonsByTopic(
                        @Parameter(description = "ID của topic") @PathVariable Long topicId,

                        @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(required = false) Integer page,

                        @Parameter(description = "Số items mỗi trang (max: 100)") @RequestParam(required = false) Integer size,

                        @Parameter(description = "Sắp xếp theo") @RequestParam(required = false) String sort) {
                try {
                        Pageable pageable = PaginationHelper.createPageable(page, size, sort);
                        Page<GrammarLessonDTO> lessonPage = grammarAdminService.getLessonsByTopicPaginated(topicId,
                                        pageable);

                        PaginatedResponse<GrammarLessonDTO> response = PaginatedResponse.of(lessonPage);

                        log.info("✅ Retrieved page {}/{} with {} lessons for topicId={}",
                                        response.getPagination().getCurrentPage() + 1,
                                        response.getPagination().getTotalPages(),
                                        lessonPage.getNumberOfElements(),
                                        topicId);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(response, "Lấy danh sách lessons thành công"));

                } catch (Exception e) {
                        log.error("❌ Error getting paginated lessons: ", e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @GetMapping("/lessons/{lessonId}")
        @Operation(summary = "Lấy chi tiết lesson", description = "Trả về chi tiết lesson kèm questions")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
                        @ApiResponse(responseCode = "400", description = "Lesson không tồn tại")
        })
        public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> getLessonDetail(
                        @Parameter(description = "ID của lesson") @PathVariable Long lessonId) {
                try {
                        GrammarLessonDTO lesson = grammarAdminService.getLessonDetail(lessonId);
                        return ResponseEntity.ok(CustomApiResponse.success(lesson, "Lấy chi tiết thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons")
        @Operation(summary = "Tạo lesson mới", description = "Tạo lesson ngữ pháp mới")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Tạo thành công"),
                        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
        })
        public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> createLesson(
                        @Valid @RequestBody GrammarLessonDTO dto) {
                try {
                        GrammarLessonDTO created = grammarAdminService.createLesson(dto);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(created, "Tạo lesson thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PutMapping("/lessons/{id}")
        @Operation(summary = "Cập nhật lesson", description = "Cập nhật lesson theo ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
                        @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc dữ liệu không hợp lệ")
        })
        public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> updateLesson(
                        @Parameter(description = "ID của lesson") @PathVariable Long id,
                        @Valid @RequestBody GrammarLessonDTO dto) {
                try {
                        GrammarLessonDTO updated = grammarAdminService.updateLesson(id, dto);
                        return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @DeleteMapping("/lessons/{id}")
        @Operation(summary = "Xóa lesson", description = "Xóa lesson theo ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Xóa thành công"),
                        @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc xóa thất bại")
        })
        public ResponseEntity<CustomApiResponse<String>> deleteLesson(
                        @Parameter(description = "ID của lesson") @PathVariable Long id,
                        @Parameter(description = "cascade = true để xóa cả questions, default là false") @RequestParam(required = false, defaultValue = "false") boolean cascade) {
                try {
                        grammarAdminService.deleteLesson(id, cascade);
                        return ResponseEntity.ok(CustomApiResponse.success("Xóa thành công", "Xóa thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @GetMapping("/topics/{topicId}/lessons/next-order")
        @Operation(summary = "Lấy orderIndex tiếp theo cho lesson mới", description = "Trả về orderIndex tiếp theo trong topic")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy thành công"),
                        @ApiResponse(responseCode = "400", description = "Topic không tồn tại"),
                        @ApiResponse(responseCode = "500", description = "Lỗi server")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextLessonOrderIndex(
                        @Parameter(description = "ID của topic") @PathVariable Long topicId) {
                try {
                        Integer nextOrder = grammarAdminService.getNextLessonOrderIndex(topicId);
                        Map<String, Integer> result = new HashMap<>();
                        result.put("nextOrderIndex", nextOrder);

                        log.info("✅ Next lesson orderIndex for topic {}: {}", topicId, nextOrder);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result, "Lấy orderIndex thành công"));
                } catch (RuntimeException e) {
                        log.warn("⚠️ Invalid request: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("❌ Error getting next lesson orderIndex: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ===== LESSON REORDERING =====

        @PostMapping("/topics/{topicId}/lessons/reorder")
        @Operation(summary = "Sắp xếp lại thứ tự lessons", description = "Tự động đẩy các bài học ra sau khi chèn vào vị trí mới")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Sắp xếp lại thành công"),
                        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> reorderLessons(
                        @Parameter(description = "ID của topic") @PathVariable Long topicId,
                        @Valid @RequestBody ReorderLessonRequest request) {
                try {
                        log.info("🔄 Reordering lessons for topicId={}, insertPosition={}, excludeLessonId={}",
                                        topicId, request.getInsertPosition(), request.getExcludeLessonId());

                        int affectedCount = grammarAdminService.reorderLessons(
                                        topicId,
                                        request.getInsertPosition(),
                                        request.getExcludeLessonId());

                        Map<String, Object> result = new HashMap<>();
                        result.put("topicId", topicId);
                        result.put("insertPosition", request.getInsertPosition());
                        result.put("affectedLessons", affectedCount);

                        log.info("✅ Reordered {} lessons successfully", affectedCount);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result,
                                                        "✅ Đã sắp xếp lại " + affectedCount + " bài học"));

                } catch (IllegalArgumentException e) {
                        log.warn("⚠️ Invalid reorder request: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("❌ Error reordering lessons: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi khi sắp xếp lại: " + e.getMessage()));
                }
        }

        // ===== QUESTION MANAGEMENT =====

        @GetMapping("/lessons/{lessonId}/questions")
        @Operation(summary = "Lấy questions theo lesson với phân trang", description = "Trả về danh sách questions có phân trang")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
                        @ApiResponse(responseCode = "400", description = "Lesson không tồn tại")
        })
        public ResponseEntity<CustomApiResponse<PaginatedResponse<GrammarQuestionDTO>>> getQuestionsByLesson(
                        @Parameter(description = "ID của lesson") @PathVariable Long lessonId,

                        @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(required = false) Integer page,

                        @Parameter(description = "Số items mỗi trang (max: 100)") @RequestParam(required = false) Integer size,

                        @Parameter(description = "Sắp xếp theo") @RequestParam(required = false) String sort) {
                try {
                        Pageable pageable = PaginationHelper.createPageable(page, size, sort);
                        Page<GrammarQuestionDTO> questionPage = grammarAdminService
                                        .getQuestionsByLessonPaginated(lessonId, pageable);

                        PaginatedResponse<GrammarQuestionDTO> response = PaginatedResponse.of(questionPage);

                        log.info("✅ Retrieved page {}/{} with {} questions for lessonId={}",
                                        response.getPagination().getCurrentPage() + 1,
                                        response.getPagination().getTotalPages(),
                                        questionPage.getNumberOfElements(),
                                        lessonId);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(response, "Lấy danh sách questions thành công"));

                } catch (Exception e) {
                        log.error("❌ Error getting paginated questions: ", e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/questions")
        @Operation(summary = "Tạo question mới", description = "Tạo question với validation theo loại")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Tạo thành công"),
                        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
        })
        public ResponseEntity<CustomApiResponse<GrammarQuestionDTO>> createQuestion(
                        @Valid @RequestBody GrammarQuestionDTO dto) {
                try {
                        GrammarQuestionDTO created = grammarAdminService.createQuestion(dto);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(created, "Tạo question thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PutMapping("/questions/{id}")
        @Operation(summary = "Cập nhật question", description = "Cập nhật question theo ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
                        @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc dữ liệu không hợp lệ")
        })
        public ResponseEntity<CustomApiResponse<GrammarQuestionDTO>> updateQuestion(
                        @Parameter(description = "ID của question") @PathVariable Long id,
                        @Valid @RequestBody GrammarQuestionDTO dto) {
                try {
                        GrammarQuestionDTO updated = grammarAdminService.updateQuestion(id, dto);
                        return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @DeleteMapping("/questions/{id}")
        @Operation(summary = "Xóa question", description = "Xóa question theo ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Xóa thành công"),
                        @ApiResponse(responseCode = "400", description = "Không tìm thấy hoặc xóa thất bại")
        })
        public ResponseEntity<CustomApiResponse<String>> deleteQuestion(
                        @Parameter(description = "ID của question") @PathVariable Long id) {
                try {
                        grammarAdminService.deleteQuestion(id);
                        return ResponseEntity.ok(CustomApiResponse.success("Xóa thành công", "Xóa thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/questions/bulk-delete")
        @Operation(summary = "Xóa nhiều questions", description = "Xóa hàng loạt và reindex theo từng lesson")
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> bulkDeleteQuestions(
                        @RequestBody Map<String, List<Long>> payload) {
                try {
                        List<Long> ids = payload.getOrDefault("questionIds", Collections.emptyList());
                        int deleted = grammarAdminService.bulkDeleteQuestions(ids); // Ủy quyền xuống service facade
                        Map<String, Object> result = Map.of("requested", ids.size(), "deleted", deleted);
                        return ResponseEntity.ok(CustomApiResponse.success(result, "Đã xóa " + deleted + " câu hỏi"));
                } catch (Exception e) {
                        log.error("❌ Bulk delete questions error", e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi khi xóa hàng loạt: " + e.getMessage()));
                }
        }

        @GetMapping("/lessons/{lessonId}/questions/next-order")
        @Operation(summary = "Lấy orderIndex tiếp theo cho question mới", description = "Trả về orderIndex tiếp theo trong lesson")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy thành công"),
                        @ApiResponse(responseCode = "400", description = "Lesson không tồn tại"),
                        @ApiResponse(responseCode = "500", description = "Lỗi server")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Integer>>> getNextQuestionOrderIndex(
                        @Parameter(description = "ID của lesson") @PathVariable Long lessonId) {
                try {
                        Integer nextOrder = grammarAdminService.getNextQuestionOrderIndex(lessonId);
                        Map<String, Integer> result = new HashMap<>();
                        result.put("nextOrderIndex", nextOrder);

                        log.info("✅ Next question orderIndex for lesson {}: {}", lessonId, nextOrder);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result, "Lấy orderIndex thành công"));
                } catch (RuntimeException e) {
                        log.warn("⚠️ Invalid request: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("❌ Error getting next question orderIndex: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ===== BULK OPERATIONS =====

        @PostMapping("/lessons/{lessonId}/questions/bulk")
        @Operation(summary = "Tạo nhiều questions cùng lúc", description = "Bulk insert questions cho lesson")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Tạo thành công"),
                        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
        })
        public ResponseEntity<CustomApiResponse<List<GrammarQuestionDTO>>> createQuestionsInBulk(
                        @Parameter(description = "ID của lesson") @PathVariable Long lessonId,
                        @RequestBody List<GrammarQuestionDTO> questions) {
                try {
                        List<GrammarQuestionDTO> created = grammarAdminService.createQuestionsInBulk(lessonId,
                                        questions);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(CustomApiResponse.created(created,
                                                        "Tạo thành công " + created.size() + " questions"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/{sourceLessonId}/copy-to/{targetLessonId}")
        @Operation(summary = "Copy questions giữa lessons", description = "Sao chép tất cả questions từ lesson này sang lesson khác")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Copy thành công"),
                        @ApiResponse(responseCode = "400", description = "Lesson không tồn tại")
        })
        public ResponseEntity<CustomApiResponse<String>> copyQuestions(
                        @Parameter(description = "ID lesson nguồn") @PathVariable Long sourceLessonId,
                        @Parameter(description = "ID lesson đích") @PathVariable Long targetLessonId) {
                try {
                        grammarAdminService.copyQuestionsToLesson(sourceLessonId, targetLessonId);
                        return ResponseEntity.ok(CustomApiResponse.success("Copy questions thành công",
                                        "Copy questions thành công"));
                } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ===== MAINTENANCE & VALIDATION ENDPOINTS (OPTIONAL) =====

        // ========== TOPIC VALIDATION ==========

        @PostMapping("/topics/validate-all-order")
        @Operation(summary = "Validate và fix orderIndex của tất cả topics", description = "Kiểm tra và sửa orderIndex cho tất cả topics")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Validation thành công")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllTopicsOrder() {
                try {
                        Map<String, Object> result = grammarAdminService.validateAllTopicsOrderIndex();

                        String message = result.get("issuesFixed").equals(0)
                                        ? "✅ OrderIndex của topics đã đúng, không cần fix"
                                        : "✅ Đã fix " + result.get("issuesFixed") + " vấn đề orderIndex trong topics";

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result, message));
                } catch (Exception e) {
                        log.error("❌ Error validating all topics order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ========== LESSON VALIDATION ==========

        @PostMapping("/topics/{topicId}/lessons/validate-order")
        @Operation(summary = "Validate và fix orderIndex của lessons trong topic", description = "Kiểm tra và tự động sửa gap/duplicate trong orderIndex của lessons")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Validation thành công"),
                        @ApiResponse(responseCode = "400", description = "Topic không tồn tại")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateLessonOrder(
                        @Parameter(description = "ID của topic") @PathVariable Long topicId) {
                try {
                        Map<String, Object> result = grammarAdminService.validateLessonsOrderIndex(topicId);

                        String message = result.get("issuesFixed").equals(0)
                                        ? "✅ OrderIndex của lessons đã đúng, không cần fix"
                                        : "✅ Đã fix " + result.get("issuesFixed") + " vấn đề orderIndex trong lessons";

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result, message));
                } catch (RuntimeException e) {
                        log.warn("⚠️ Invalid request: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("❌ Error validating lesson order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/lessons/validate-all-order")
        @Operation(summary = "Validate và fix orderIndex của tất cả lessons", description = "Kiểm tra và sửa orderIndex cho tất cả lessons trong tất cả topics")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Validation thành công")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllLessonsOrder() {
                try {
                        Map<String, Object> result = grammarAdminService.validateAllLessonsOrderIndex();

                        String message = result.get("totalIssuesFixed").equals(0)
                                        ? "✅ OrderIndex của tất cả lessons đã đúng, không cần fix"
                                        : "✅ Đã fix " + result.get("totalIssuesFixed") + " vấn đề orderIndex trong "
                                                        + result.get("totalLessons") + " lessons";

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result, message));
                } catch (Exception e) {
                        log.error("❌ Error validating all lessons order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ========== QUESTION VALIDATION ==========

        @PostMapping("/lessons/{lessonId}/questions/validate-order")
        @Operation(summary = "Validate và fix orderIndex của questions trong lesson", description = "Kiểm tra và tự động sửa gap/duplicate trong orderIndex của questions")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Validation thành công"),
                        @ApiResponse(responseCode = "400", description = "Lesson không tồn tại")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateQuestionOrder(
                        @Parameter(description = "ID của lesson") @PathVariable Long lessonId) {
                try {
                        Map<String, Object> result = grammarAdminService.validateQuestionsOrderIndex(lessonId);

                        String message = result.get("issuesFixed").equals(0)
                                        ? "✅ OrderIndex của questions đã đúng, không cần fix"
                                        : "✅ Đã fix " + result.get("issuesFixed")
                                                        + " vấn đề orderIndex trong questions";

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result, message));
                } catch (RuntimeException e) {
                        log.warn("⚠️ Invalid request: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("❌ Error validating question order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/topics/{topicId}/questions/validate-all-order")
        @Operation(summary = "Validate và fix orderIndex của tất cả questions trong topic", description = "Kiểm tra và sửa orderIndex cho tất cả questions trong tất cả lessons của topic")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Validation thành công"),
                        @ApiResponse(responseCode = "400", description = "Topic không tồn tại")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllQuestionsInTopic(
                        @Parameter(description = "ID của topic") @PathVariable Long topicId) {
                try {
                        Map<String, Object> result = grammarAdminService.validateAllQuestionsInTopic(topicId);

                        String message = result.get("totalIssuesFixed").equals(0)
                                        ? "✅ OrderIndex của tất cả questions đã đúng, không cần fix"
                                        : "✅ Đã fix " + result.get("totalIssuesFixed") + " vấn đề orderIndex trong "
                                                        + result.get("totalQuestions") + " questions";

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result, message));
                } catch (RuntimeException e) {
                        log.warn("⚠️ Invalid request: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest(e.getMessage()));
                } catch (Exception e) {
                        log.error("❌ Error validating all questions in topic: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        @PostMapping("/questions/validate-all-order")
        @Operation(summary = "Validate và fix orderIndex của TẤT CẢ questions", description = "Kiểm tra và sửa orderIndex cho tất cả questions trong toàn bộ hệ thống")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Validation thành công")
        })
        public ResponseEntity<CustomApiResponse<Map<String, Object>>> validateAllQuestionsOrder() {
                try {
                        Map<String, Object> result = grammarAdminService.validateAllQuestionsOrderIndex();

                        String message = result.get("totalIssuesFixed").equals(0)
                                        ? "✅ OrderIndex của tất cả questions đã đúng, không cần fix"
                                        : "✅ Đã fix " + result.get("totalIssuesFixed") + " vấn đề orderIndex trong "
                                                        + result.get("totalQuestions") + " questions";

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(result, message));
                } catch (Exception e) {
                        log.error("❌ Error validating all questions order: ", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }
}