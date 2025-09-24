package com.thanhnb.englishlearning.controller;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.dto.grammar.request.SubmitLessonRequest;
import com.thanhnb.englishlearning.dto.grammar.response.LessonResultResponse;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.service.grammar.GrammarService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/grammar")
@RequiredArgsConstructor
@Tag(name = "Grammar", description = "API ngữ pháp")
public class GrammarController {
    
    private final GrammarService grammarService;
    private final UserRepository userRepository;

    @GetMapping("/topics")
    @Operation(summary = "Lấy danh sách các topic có thể học", description = "Trả về danh sách các topic mà người dùng có thể học dựa trên tiến trình hiện tại của họ")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy danh sách topic thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<List<GrammarTopicDTO>>> getAccessibleTopics(
            @Parameter(description = "User authentication details") Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<GrammarTopicDTO> topics = grammarService.getAccessibleTopicsForUser(userId);
            return ResponseEntity.ok(CustomApiResponse.success(topics,
                    "Lấy danh sách topic có thể học thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi khi lấy danh sách topic: " + e.getMessage()));
        }
    }

    @GetMapping("/topics/{topicId}")
    @Operation(summary = "Lấy chi tiết topic", description = "Trả về chi tiết của một topic cụ thể với tiến trình của người dùng")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy chi tiết topic thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<GrammarTopicDTO>> getTopicDetails(
            @Parameter(description = "ID của topic") @PathVariable Long topicId,
            @Parameter(description = "Thông tin xác thực người dùng") Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            GrammarTopicDTO topic = grammarService.getTopicWithProgress(topicId, userId);
            return ResponseEntity.ok(CustomApiResponse.success(topic,
                    "Lấy chi tiết topic thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi khi lấy chi tiết topic: " + e.getMessage()));
        }
    }

    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Lấy nội dung bài học", description = "Trả về nội dung của một bài học cụ thể")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy nội dung bài học thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> getLessonContent(
            @Parameter(description = "ID của bài học") @PathVariable Long lessonId,
            @Parameter(description = "Thông tin xác thực người dùng") Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            GrammarLessonDTO lesson = grammarService.getLessonContent(lessonId, userId);
            return ResponseEntity.ok(CustomApiResponse.success(lesson,
                    "Lấy nội dung bài học thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi khi lấy nội dung bài học: " + e.getMessage()));
        }
    }

    @PostMapping("/lesson/{lessonId}/practice")
    @Operation(summary = "Lấy câu hỏi thực hành", description = "Trả về danh sách các câu hỏi thực hành ngẫu nhiên cho một bài học")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy câu hỏi thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<List<GrammarQuestionDTO>>> getPracticeQuestions(
            @Parameter(description = "ID của bài học") @PathVariable Long lessonId,
            @Parameter(description = "Số lượng câu hỏi cần lấy", example = "10") @RequestParam(defaultValue = "10") int numberOfQuestions,
            @Parameter(description = "Thông tin xác thực người dùng") Authentication authentication) {
        try {
            List<GrammarQuestionDTO> questions = grammarService.getRandomQuestions(lessonId, numberOfQuestions);
            questions.forEach(q -> q.setShowCorrectAnswer(false));
            return ResponseEntity.ok(CustomApiResponse.success(questions,
                    "Lấy câu hỏi trắc nghiệm thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi khi lấy câu hỏi trắc nghiệm: " + e.getMessage()));
        }
    }

    @PostMapping("/lessons/submit")
    @Operation(summary = "Nộp bài tập", description = "Nộp bài tập cho một bài học và trả về kết quả kèm gợi ý")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Nộp bài tập thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<LessonResultResponse>> submitLesson(
            @Valid @RequestBody SubmitLessonRequest request,
            @Parameter(description = "Thông tin xác thực người dùng") Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            LessonResultResponse result = grammarService.submitLesson(userId, request);
            
            String message = result.getIsCompleted()
                ? "Hoàn thành bài học thành công! Điểm: " + result.getScore()
                : "Submit bài học thành công. Xem gợi ý trong kết quả để cải thiện!";

            return ResponseEntity.ok(CustomApiResponse.success(result, message));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi khi submit bài làm: " + e.getMessage()));
        }
    }

    @GetMapping("/progress")
    @Operation(summary = "Lấy tiến trình người dùng", description = "Trả về tiến trình của người dùng trên các chủ đề và bài học")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lấy tiến trình thành công"),
        @ApiResponse(responseCode = "400", description = "Yêu cầu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<Map<String, Object>>> getUserProgress(
            @Parameter(description = "Thông tin xác thực người dùng") Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<GrammarTopicDTO> topics = grammarService.getAccessibleTopicsForUser(userId);
            int totalTopics = topics.size();
            int completedTopics = (int) topics.stream()
                    .filter(t -> t.getCompletedLessons() != null && t.getTotalLessons() != null)
                    .filter(t -> t.getCompletedLessons().equals(t.getTotalLessons()))
                    .count();
            int totalLessons = topics.stream()
                    .mapToInt(t -> t.getTotalLessons() != null ? t.getTotalLessons() : 0)
                    .sum();
            int completedLessons = topics.stream()
                    .mapToInt(t -> t.getCompletedLessons() != null ? t.getCompletedLessons() : 0)
                    .sum();
            Map<String, Object> progress = Map.of(
                "topics", topics,
                "summary", Map.of(
                    "totalTopics", totalTopics,
                    "completedTopics", completedTopics,
                    "totalLessons", totalLessons,
                    "completedLessons", completedLessons,
                    "completionRate", totalLessons > 0 ? (completedLessons * 100 / totalLessons) : 0
                )
            );
            return ResponseEntity.ok(CustomApiResponse.success(progress,
                    "Lấy tiến trình học tập thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi khi lấy tiến trình học tập: " + e.getMessage()));
        }
    }

    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        return user.getId();
    }
}