package com.thanhnb.englishlearning.controller;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.service.grammar.GrammarAdminService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
public class GrammarAdminController {
    private final GrammarAdminService grammarAdminService;

    @GetMapping("/endpoints")
    @Operation(summary = "Lấy danh sách các endpoint quản trị", description = "Trả về danh sách tất cả các endpoint API quản trị ngữ pháp có sẵn")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy danh sách endpoint thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<Object>> getAdminEndpoints() {
        var endpoints = Arrays.asList(
            Map.of("method", "GET", "url", "/api/admin/grammar/topics", "description", "📚 Quản lý topics"),
            Map.of("method", "POST", "url", "/api/admin/grammar/topics", "description", "➕ Tạo topic mới"),
            Map.of("method", "PUT", "url", "/api/admin/grammar/topics/{id}", "description", "✏️ Cập nhật topic"),
            Map.of("method", "DELETE", "url", "/api/admin/grammar/topics/{id}", "description", "🗑️ Xóa topic"),
            Map.of("method", "GET", "url", "/api/admin/grammar/topics/{topicId}/lessons", "description", "📝 Quản lý lessons"),
            Map.of("method", "POST", "url", "/api/admin/grammar/lessons", "description", "➕ Tạo lesson mới"),
            Map.of("method", "PUT", "url", "/api/admin/grammar/lessons/{id}", "description", "✏️ Cập nhật lesson"),
            Map.of("method", "DELETE", "url", "/api/admin/grammar/lessons/{id}", "description", "🗑️ Xóa lesson"),
            Map.of("method", "GET", "url", "/api/admin/grammar/lessons/{lessonId}/questions", "description", "❓ Quản lý questions"),
            Map.of("method", "POST", "url", "/api/admin/grammar/questions", "description", "➕ Tạo question mới"),
            Map.of("method", "PUT", "url", "/api/admin/grammar/questions/{id}", "description", "✏️ Cập nhật question"),
            Map.of("method", "DELETE", "url", "/api/admin/grammar/questions/{id}", "description", "🗑️ Xóa question")
        );

        return ResponseEntity.ok(CustomApiResponse.success(Map.of(
            "controller", "GrammarAdminController - Content Management",
            "baseUrl", "http://localhost:8980/api/admin/grammar",
            "totalEndpoints", endpoints.size(),
            "endpoints", endpoints,
            "note", "⚠️ Tất cả endpoints yêu cầu ADMIN role"
        ), "Danh sách Grammar Admin API endpoints"));
    }

    // ===== TOPIC MANAGEMENT =====

    @GetMapping("/topics")
    @Operation(summary = "Lấy tất cả các chủ đề ngữ pháp", description = "Trả về danh sách tất cả các chủ đề ngữ pháp để quản lý admin")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy danh sách chủ đề thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<List<GrammarTopicDTO>>> getAllTopics() {
        try {
            List<GrammarTopicDTO> topics = grammarAdminService.getAllTopics();
            return ResponseEntity.ok(CustomApiResponse.success(topics, 
                "Lấy danh sách topics thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Lỗi khi lấy danh sách topics: " + e.getMessage()));
        }
    }

    @PostMapping("/topics")
    @Operation(summary = "Tạo chủ đề ngữ pháp mới", description = "Tạo một chủ đề ngữ pháp mới với các thông tin đã cung cấp")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tạo chủ đề thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - đầu vào không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<GrammarTopicDTO>> createTopic(@Valid @RequestBody GrammarTopicDTO dto) {
        try {
            GrammarTopicDTO created = grammarAdminService.createTopic(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomApiResponse.created(created, "Tạo topic thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Lỗi khi tạo topic: " + e.getMessage()));
        }
    }

    @PutMapping("/topics/{id}")
    @Operation(summary = "Cập nhật chủ đề ngữ pháp", description = "Cập nhật một chủ đề ngữ pháp hiện có theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cập nhật chủ đề thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - đầu vào không hợp lệ hoặc không tìm thấy chủ đề")
    })
    public ResponseEntity<CustomApiResponse<GrammarTopicDTO>> updateTopic(
            @Parameter(description = "ID của chủ đề cần cập nhật") @PathVariable Long id,
            @Valid @RequestBody GrammarTopicDTO dto) {
        try {
            GrammarTopicDTO updated = grammarAdminService.updateTopic(id, dto);
            return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật topic thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Lỗi khi cập nhật topic: " + e.getMessage()));
        }
    }

    @DeleteMapping("/topics/{id}")
    @Operation(summary = "Xóa chủ đề ngữ pháp", description = "Xóa một chủ đề ngữ pháp theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Xóa chủ đề thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - không tìm thấy chủ đề hoặc xóa không thành công")
    })
    public ResponseEntity<CustomApiResponse<String>> deleteTopic(
            @Parameter(description = "ID của chủ đề cần xóa") @PathVariable Long id) {
        try {
            grammarAdminService.deleteTopic(id);
            return ResponseEntity.ok(CustomApiResponse.success("Xóa topic thành công", 
                "Xóa topic thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Lỗi khi xóa topic: " + e.getMessage()));
        }
    }

    // ===== LESSON MANAGEMENT =====

    @GetMapping("/topics/{topicId}/lessons")
    @Operation(summary = "Lấy danh sách bài học theo chủ đề", description = "Trả về tất cả bài học thuộc về một chủ đề cụ thể")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy danh sách bài học thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - không tìm thấy chủ đề")
    })
    public ResponseEntity<CustomApiResponse<List<GrammarLessonDTO>>> getLessonsByTopic(
            @Parameter(description = "ID của chủ đề") @PathVariable Long topicId) {
        try {
            List<GrammarLessonDTO> lessons = grammarAdminService.getLessonsByTopic(topicId);
            return ResponseEntity.ok(CustomApiResponse.success(lessons,
                "Lấy danh sách bài học thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Lỗi khi lấy danh sách bài học: " + e.getMessage()));
        }
    }

    @PostMapping("/lessons")
    @Operation(summary = "Tạo bài học ngữ pháp mới", description = "Tạo một bài học ngữ pháp mới với các thông tin đã cung cấp")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tạo bài học thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - dữ liệu đầu vào không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> createLesson(@Valid @RequestBody GrammarLessonDTO dto) {
        try {
            GrammarLessonDTO created = grammarAdminService.createLesson(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomApiResponse.created(created, "Tạo lesson thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Lỗi khi tạo lesson: " + e.getMessage()));
        }
    }

    @PutMapping("/lessons/{id}")
    @Operation(summary = "Cập nhật bài học ngữ pháp", description = "Cập nhật một bài học ngữ pháp hiện có theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cập nhật bài học thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - dữ liệu đầu vào không hợp lệ hoặc không tìm thấy bài học")
    })
    public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> updateLesson(
            @Parameter(description = "ID của bài học cần cập nhật") @PathVariable Long id,
            @Valid @RequestBody GrammarLessonDTO dto) {
        try {
            GrammarLessonDTO updated = grammarAdminService.updateLesson(id, dto);
            return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật bài học thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Lỗi khi cập nhật bài học: " + e.getMessage()));
        }
    }

    @DeleteMapping("/lessons/{id}")
    @Operation(summary = "Xóa bài học ngữ pháp", description = "Xóa một bài học ngữ pháp theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Xóa bài học thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - không tìm thấy bài học hoặc xóa thất bại")
    })
    public ResponseEntity<CustomApiResponse<String>> deleteLesson(
            @Parameter(description = "ID của bài học cần xóa") @PathVariable Long id) {
        try {
            grammarAdminService.deleteLesson(id);
            return ResponseEntity.ok(CustomApiResponse.success("Xóa bài học thành công", 
                "Xóa bài học thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Lỗi khi xóa bài học: " + e.getMessage()));
        }
    }

    // ===== QUESTION MANAGEMENT =====

    @GetMapping("/lessons/{lessonId}/questions")
    @Operation(summary = "Lấy câu hỏi theo bài học", description = "Lấy tất cả câu hỏi thuộc về một bài học cụ thể")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy câu hỏi thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - không tìm thấy bài học")
    })
    public ResponseEntity<CustomApiResponse<List<GrammarQuestionDTO>>> getQuestionsByLesson(
            @Parameter(description = "ID của bài học") @PathVariable Long lessonId) {
        try {
            List<GrammarQuestionDTO> questions = grammarAdminService.getQuestionsByLesson(lessonId);
            return ResponseEntity.ok(CustomApiResponse.success(questions,
                "Lấy danh sách câu hỏi thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Lỗi khi lấy danh sách câu hỏi: " + e.getMessage()));
        }
    }

    @PostMapping("/questions")
    @Operation(summary = "Tạo câu hỏi ngữ pháp mới", description = "Tạo một câu hỏi ngữ pháp mới với xác thực dựa trên loại câu hỏi")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tạo câu hỏi thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - đầu vào không hợp lệ hoặc xác thực không thành công")
    })
    public ResponseEntity<CustomApiResponse<GrammarQuestionDTO>> createQuestion(@Valid @RequestBody GrammarQuestionDTO dto) {

        // Kiểm tra số lượng lựa chọn cho câu trắc nghiệm
        if (dto.getQuestionType() == QuestionType.MULTIPLE_CHOICE && 
            (dto.getOptions() == null || dto.getOptions().size() < 2)) {
            throw new RuntimeException("Câu hỏi trắc nghiệm phải có ít nhất 2 lựa chọn");
        }
        // Kiểm tra correctAnswer cho FILL_BLANK và TRANSLATION
        if (dto.getQuestionType() == QuestionType.FILL_BLANK || 
            dto.getQuestionType() == QuestionType.TRANSLATION_VI_EN || 
            dto.getQuestionType() == QuestionType.TRANSLATION_EN_VI) {
            if (dto.getCorrectAnswer() == null || dto.getCorrectAnswer().trim().isEmpty()) {
                throw new RuntimeException("Đáp án đúng không được để trống");
            }
            String[] correctAnswers = dto.getCorrectAnswer().split("\\|");
            if (Arrays.stream(correctAnswers).anyMatch(String::isBlank)) {
                throw new RuntimeException("Đáp án đúng không được chứa phần tử trống");
            }
        }
        
        try {
            GrammarQuestionDTO created = grammarAdminService.createQuestion(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomApiResponse.created(created, "Tạo question thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Lỗi khi tạo question: " + e.getMessage()));
        }
    }

    @PutMapping("/questions/{id}")
    @Operation(summary = "Cập nhật câu hỏi ngữ pháp", description = "Cập nhật một câu hỏi ngữ pháp hiện có theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cập nhật câu hỏi thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - đầu vào không hợp lệ hoặc không tìm thấy câu hỏi")
    })
    public ResponseEntity<CustomApiResponse<GrammarQuestionDTO>> updateQuestion(
            @Parameter(description = "ID của câu hỏi cần cập nhật") @PathVariable Long id,
            @Valid @RequestBody GrammarQuestionDTO dto) {
        try {
            GrammarQuestionDTO updated = grammarAdminService.updateQuestion(id, dto);
            return ResponseEntity.ok(CustomApiResponse.success(updated, "Cập nhật câu hỏi thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Lỗi khi cập nhật câu hỏi: " + e.getMessage()));
        }
    }

    @DeleteMapping("/questions/{id}")
    @Operation(summary = "Xóa câu hỏi ngữ pháp", description = "Xóa một câu hỏi ngữ pháp theo ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Xóa câu hỏi thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ - không tìm thấy câu hỏi hoặc xóa không thành công")
    })
    public ResponseEntity<CustomApiResponse<String>> deleteQuestion(
            @Parameter(description = "ID của câu hỏi cần xóa") @PathVariable Long id) {
        try {
            grammarAdminService.deleteQuestion(id);
            return ResponseEntity.ok(CustomApiResponse.success("Xóa câu hỏi thành công", 
                "Xóa câu hỏi thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(CustomApiResponse.badRequest("Lỗi khi xóa câu hỏi: " + e.getMessage()));
        }
    }
}