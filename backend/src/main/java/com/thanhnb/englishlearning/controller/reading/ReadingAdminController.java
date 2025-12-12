package com.thanhnb.englishlearning.controller.reading;

import com.thanhnb.englishlearning.dto.reading.*;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.service.reading.*;
import com.thanhnb.englishlearning.util.PaginationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

        // ═════════════════════════════════════════════════════════════════
        // AI PARSING ENDPOINTS
        // ═════════════════════════════════════════════════════════════════

        // ═════════════════════════════════════════════════════════════════
        // LESSON CRUD
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
        @Operation(summary = "Xóa bài đọc", description = "Xóa vĩnh viễn")
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
        public ResponseEntity<CustomApiResponse<PaginatedResponse<QuestionResponseDTO>>> getQuestionsByLesson(
                        @Parameter(description = "ID của bài đọc") @PathVariable Long lessonId,
                        @RequestParam(required = false) Integer page,
                        @RequestParam(required = false) Integer size,
                        @RequestParam(required = false) String sort) {
                try {
                        Pageable pageable = PaginationHelper.createPageable(page, size, sort);
                        Page<QuestionResponseDTO> questionPage = questionService.getQuestionsByLessonPaginated(lessonId,
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
        @Operation(summary = "Cập nhật question", description = "Cập nhật question theo ID")
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

        // ═══════════════════════════════════════════════════════════════
        // 📚 EXAMPLE REQUEST BODIES (FOR DOCUMENTATION)
        // ═══════════════════════════════════════════════════════════════

        /*
         * MULTIPLE_CHOICE Example:
         * {
         * "parentType": "READING",
         * "parentId": 1,
         * "questionText": "What is the main idea?",
         * "explanation": "The passage focuses on...",
         * "points": 5,
         * "orderIndex": 1,
         * "hint": "Read the first paragraph",
         * "options": [
         * {"text": "Answer A", "isCorrect": true, "order": 1},
         * {"text": "Answer B", "isCorrect": false, "order": 2},
         * {"text": "Answer C", "isCorrect": false, "order": 3}
         * ]
         * }
         * 
         * TRUE_FALSE Example:
         * {
         * "parentType": "READING",
         * "parentId": 1,
         * "questionText": "The author agrees with this statement.",
         * "explanation": "According to paragraph 2...",
         * "points": 3,
         * "orderIndex": 2,
         * "hint": "Check paragraph 2",
         * "correctAnswer": true
         * }
         * 
         * FILL_BLANK Example:
         * {
         * "parentType": "READING",
         * "parentId": 1,
         * "questionText": "The capital of France is ___.",
         * "explanation": "Paris is the capital",
         * "points": 2,
         * "orderIndex": 3,
         * "hint": "It starts with P",
         * "correctAnswer": "Paris|paris",
         * "caseSensitive": false,
         * "type": "FILL_BLANK"
         * }
         * 
         * MATCHING Example:
         * {
         * "parentType": "READING",
         * "parentId": 1,
         * "questionText": "Match the countries with their capitals",
         * "explanation": "Geography matching",
         * "points": 5,
         * "orderIndex": 4,
         * "hint": "Think about Europe",
         * "pairs": [
         * {"left": "France", "right": "Paris", "order": 1},
         * {"left": "Germany", "right": "Berlin", "order": 2},
         * {"left": "Italy", "right": "Rome", "order": 3}
         * ]
         * }
         * 
         * SENTENCE_BUILDING Example:
         * {
         * "parentType": "READING",
         * "parentId": 1,
         * "questionText": "Rearrange the words to form a correct sentence",
         * "explanation": "Subject-Verb-Object order",
         * "points": 5,
         * "orderIndex": 5,
         * "hint": "Start with 'I'",
         * "words": ["love", "I", "cats"],
         * "correctSentence": "I love cats"
         * }
         * 
         * COMPLETE_CONVERSATION Example:
         * {
         * "parentType": "READING",
         * "parentId": 1,
         * "questionText": "Complete the conversation",
         * "explanation": "Polite response",
         * "points": 3,
         * "orderIndex": 6,
         * "hint": "Use 'thank you'",
         * "conversationContext": "A: How are you?\nB: ___",
         * "options": ["I'm fine, thank you", "No", "Yes", "Maybe"],
         * "correctAnswer": "I'm fine, thank you"
         * }
         * 
         * PRONUNCIATION Example:
         * {
         * "parentType": "READING",
         * "parentId": 1,
         * "questionText": "Classify the words by their vowel sound",
         * "explanation": "Long vs short vowels",
         * "points": 5,
         * "orderIndex": 7,
         * "hint": "Listen carefully",
         * "words": ["cat", "cake", "car", "care"],
         * "categories": ["short-a", "long-a"],
         * "classifications": [
         * {"word": "cat", "category": "short-a"},
         * {"word": "cake", "category": "long-a"},
         * {"word": "car", "category": "short-a"},
         * {"word": "care", "category": "long-a"}
         * ]
         * }
         * 
         * READING_COMPREHENSION Example:
         * {
         * "parentType": "READING",
         * "parentId": 1,
         * "questionText": "Fill in the blanks in the passage",
         * "explanation": "Context clues",
         * "points": 10,
         * "orderIndex": 8,
         * "hint": "Read the whole passage first",
         * "passage": "Paris is the ___ of France. It is known for the ___ Tower.",
         * "blanks": [
         * {
         * "position": 1,
         * "options": ["capital", "city", "country", "village"],
         * "correctAnswer": "capital"
         * },
         * {
         * "position": 2,
         * "options": ["Eiffel", "Big", "Liberty", "Clock"],
         * "correctAnswer": "Eiffel"
         * }
         * ]
         * }
         * 
         * OPEN_ENDED Example:
         * {
         * "parentType": "READING",
         * "parentId": 1,
         * "questionText":
         * "Summarize the main points of the passage in your own words.",
         * "explanation": "This tests comprehension and writing skills",
         * "points": 10,
         * "orderIndex": 9,
         * "hint": "Include at least 3 main ideas",
         * "suggestedAnswer":
         * "The passage discusses three main topics: climate change, renewable energy, and sustainable living..."
         * ,
         * "timeLimitSeconds": 300,
         * "minWord": 50,
         * "maxWord": 200
         * }
         */

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