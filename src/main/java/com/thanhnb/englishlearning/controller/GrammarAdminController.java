package com.thanhnb.englishlearning.controller;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.ParseResult;
import com.thanhnb.englishlearning.service.GeminiPDFService;
import com.thanhnb.englishlearning.service.grammar.GrammarAdminService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import java.util.*;
import java.util.stream.Collectors;

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
    private final GeminiPDFService geminiPDFService;

    // ===== GEMINI PDF PARSING =====

    @PostMapping("/topics/{topicId}/parse-pdf")
    @Operation(summary = "Parse PDF với Gemini AI", description = "Upload file PDF/DOCX và AI sẽ tự động phân tích thành các bài học riêng biệt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parse thành công"),
            @ApiResponse(responseCode = "400", description = "File không hợp lệ hoặc lỗi parse"),
            @ApiResponse(responseCode = "500", description = "Lỗi server hoặc Gemini API")
    })
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> parsePDF(
            @Parameter(description = "ID của topic") @PathVariable Long topicId,
            @Parameter(description = "File PDF hoặc DOCX (max 20MB)") @RequestParam("file") MultipartFile file) {
        try {
            log.info("📄 Received PDF parsing request: file={}, topicId={}",
                    file.getOriginalFilename(), topicId);

            // ✅ GỌI SERVICE - KHÔNG VIẾT LẠI LOGIC
            ParseResult result = geminiPDFService.parsePDF(file, topicId);

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
                    "totalLessons", result.lessons.size(),
                    "theoryLessons", theoryCount,
                    "practiceLessons", practiceCount,
                    "totalQuestions", totalQuestions,
                    "averageQuestionsPerPractice", practiceCount > 0 ? totalQuestions / practiceCount : 0));

            log.info("✅ PDF parsed successfully: {} lessons, {} questions",
                    result.lessons.size(), totalQuestions);

            return ResponseEntity.ok(
                    CustomApiResponse.success(response,
                            "✅ Phân tích PDF thành công! " + result.lessons.size() + " bài học được tạo."));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error parsing PDF: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomApiResponse.badRequest("Lỗi khi parse PDF: " + e.getMessage()));
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
                            "questionCount", l.getQuestionCount() != null ? l.getQuestionCount() : 0))
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
    @Operation(summary = "Lấy tất cả topics", description = "Trả về danh sách tất cả topics để quản lý")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<List<GrammarTopicDTO>>> getAllTopics() {
        try {
            List<GrammarTopicDTO> topics = grammarAdminService.getAllTopics();
            return ResponseEntity.ok(CustomApiResponse.success(topics,
                    "Lấy danh sách topics thành công"));
        } catch (Exception e) {
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

    // ===== LESSON MANAGEMENT =====

    @GetMapping("/topics/{topicId}/lessons")
    @Operation(summary = "Lấy lessons theo topic", description = "Trả về danh sách lessons thuộc topic")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "400", description = "Topic không tồn tại")
    })
    public ResponseEntity<CustomApiResponse<List<GrammarLessonDTO>>> getLessonsByTopic(
            @Parameter(description = "ID của topic") @PathVariable Long topicId) {
        try {
            List<GrammarLessonDTO> lessons = grammarAdminService.getLessonsByTopic(topicId);
            return ResponseEntity.ok(CustomApiResponse.success(lessons, "Lấy danh sách thành công"));
        } catch (Exception e) {
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
            @Parameter(description = "ID của lesson") @PathVariable Long id) {
        try {
            grammarAdminService.deleteLesson(id);
            return ResponseEntity.ok(CustomApiResponse.success("Xóa thành công", "Xóa thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    // ===== QUESTION MANAGEMENT =====

    @GetMapping("/lessons/{lessonId}/questions")
    @Operation(summary = "Lấy questions theo lesson", description = "Trả về danh sách questions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "400", description = "Lesson không tồn tại")
    })
    public ResponseEntity<CustomApiResponse<List<GrammarQuestionDTO>>> getQuestionsByLesson(
            @Parameter(description = "ID của lesson") @PathVariable Long lessonId) {
        try {
            List<GrammarQuestionDTO> questions = grammarAdminService.getQuestionsByLesson(lessonId);
            return ResponseEntity.ok(CustomApiResponse.success(questions, "Lấy danh sách thành công"));
        } catch (Exception e) {
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
            List<GrammarQuestionDTO> created = grammarAdminService.createQuestionsInBulk(lessonId, questions);
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
}