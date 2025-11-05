package com.thanhnb.englishlearning.controller.reading;

import com.thanhnb.englishlearning.dto.reading.*;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.entity.reading.UserReadingProgress;
import com.thanhnb.englishlearning.service.reading.ReadingService;
import com.thanhnb.englishlearning.security.UserPrincipal;
import com.thanhnb.englishlearning.util.PaginationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import java.util.List;

/**
 * USER Controller cho Reading module
 */
@RestController
@RequestMapping("/api/reading")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Reading", description = "API học bài đọc hiểu (dành cho USER)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ReadingController {

        private final ReadingService readingService;

        // ═════════════════════════════════════════════════════════════════
        // GET LESSONS
        // ═════════════════════════════════════════════════════════════════

        /**
         * [USER] Lấy danh sách bài đọc với progress
         */
        @GetMapping("/lessons")
        @Operation(summary = "Lấy danh sách bài đọc", description = "Lấy tất cả bài đọc active với tiến độ của user")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
                        @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
        })
        public ResponseEntity<CustomApiResponse<PaginatedResponse<ReadingLessonDTO>>> getAllLessons(
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(required = false) Integer page,
                        @Parameter(description = "Số items mỗi trang (max: 100)") @RequestParam(required = false) Integer size,
                        @Parameter(description = "Sắp xếp theo") @RequestParam(required = false) String sort) {
                try {
                        log.info("User {} requesting reading lessons (page={}, size={})",
                                        currentUser.getId(), page, size);

                        Pageable pageable = PaginationHelper.createPageable(page, size, sort);
                        Page<ReadingLessonDTO> lessons = readingService.getAllLessonsForUser(
                                        currentUser.getId(), pageable);

                        PaginatedResponse<ReadingLessonDTO> response = PaginatedResponse.of(lessons);

                        log.info("User {} retrieved page {}/{} with {} reading lessons",
                                        currentUser.getId(),
                                        response.getPagination().getCurrentPage() + 1,
                                        response.getPagination().getTotalPages(),
                                        lessons.getNumberOfElements());

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(response, "Lấy danh sách bài đọc thành công"));

                } catch (Exception e) {
                        log.error("Error getting reading lessons for user {}: ", currentUser.getId(), e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ═════════════════════════════════════════════════════════════════
        // GET LESSON DETAIL
        // ═════════════════════════════════════════════════════════════════

        /**
         * [USER] Lấy chi tiết bài đọc với questions
         */
        @GetMapping("/lessons/{lessonId}")
        @Operation(summary = "Lấy chi tiết bài đọc", description = "Lấy nội dung bài đọc và câu hỏi (ẩn đáp án nếu chưa hoàn thành)")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
                        @ApiResponse(responseCode = "400", description = "Bài đọc không tồn tại"),
                        @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
        })
        public ResponseEntity<CustomApiResponse<ReadingLessonDTO>> getLessonDetail(
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        @Parameter(description = "ID của bài đọc") @PathVariable Long lessonId) {
                try {
                        log.info("User {} accessing reading lesson {}", currentUser.getId(), lessonId);

                        ReadingLessonDTO lesson = readingService.getLessonDetail(lessonId, currentUser.getId());

                        log.info("User {} loaded lesson {} (completed: {}, score: {}%)",
                                        currentUser.getId(),
                                        lessonId,
                                        lesson.getIsCompleted() != null && lesson.getIsCompleted(),
                                        lesson.getScorePercentage() != null ? lesson.getScorePercentage() : 0);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(lesson, "Lấy chi tiết bài đọc thành công"));

                } catch (RuntimeException e) {
                        log.warn("User {} failed to access lesson {}: {}",
                                        currentUser.getId(), lessonId, e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                } catch (Exception e) {
                        log.error("Error getting reading lesson detail: ", e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ═════════════════════════════════════════════════════════════════
        // SUBMIT LESSON
        // ═════════════════════════════════════════════════════════════════

        /**
         * [USER] Nộp bài đọc và nhận kết quả
         */
        @PostMapping("/lessons/{lessonId}/submit")
        @Operation(summary = "Nộp bài đọc", description = "Nộp câu trả lời và nhận kết quả (với anti-cheat 30 giây)")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Nộp bài thành công"),
                        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc bài đọc không tồn tại"),
                        @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
                        @ApiResponse(responseCode = "429", description = "Submit quá nhanh (< 30 giây)")
        })
        public ResponseEntity<CustomApiResponse<ReadingSubmitResponse>> submitLesson(
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        @Parameter(description = "ID của bài đọc") @PathVariable Long lessonId,
                        @Valid @RequestBody ReadingSubmitRequest request) {
                try {
                        log.info("User {} submitting reading lesson {} with {} answers",
                                        currentUser.getId(),
                                        lessonId,
                                        request.getAnswers() != null ? request.getAnswers().size() : 0);

                        ReadingSubmitResponse response = readingService.submitLesson(
                                        currentUser.getId(), lessonId, request);

                        // Build message based on result
                        String message;
                        if (response.isCompleted()) {
                                message = String.format(
                                                "🎉 Chúc mừng! Bạn đã hoàn thành bài đọc với điểm %.2f%% (%d/%d câu đúng)",
                                                response.getScorePercentage(),
                                                response.getCorrectCount(),
                                                response.getTotalQuestions());
                        } else {
                                message = String.format(
                                                "📊 Bạn đã đạt %.2f%% (%d/%d câu đúng). Cần đạt tối thiểu 80%% để hoàn thành bài",
                                                response.getScorePercentage(),
                                                response.getCorrectCount(),
                                                response.getTotalQuestions());
                        }

                        log.info("User {} completed reading lesson {} - Score: {:.2f}%, Passed: {}",
                                        currentUser.getId(),
                                        lessonId,
                                        response.getScorePercentage(),
                                        response.isCompleted());

                        return ResponseEntity.ok(CustomApiResponse.success(response, message));

                } catch (RuntimeException e) {
                        log.warn("User {} submit failed for lesson {}: {}",
                                        currentUser.getId(), lessonId, e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                } catch (Exception e) {
                        log.error("Error submitting reading lesson: ", e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ═════════════════════════════════════════════════════════════════
        // PROGRESS & HISTORY
        // ═════════════════════════════════════════════════════════════════

        /**
         * [USER] Lấy danh sách bài đã hoàn thành
         */
        @GetMapping("/progress/completed")
        @Operation(summary = "Lấy danh sách bài đã hoàn thành", description = "Xem lịch sử các bài đọc đã hoàn thành của user")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
                        @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
        })
        public ResponseEntity<CustomApiResponse<List<UserReadingProgress>>> getCompletedLessons(
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                try {
                        log.info("User {} requesting completed reading lessons", currentUser.getId());

                        List<UserReadingProgress> completedLessons = readingService
                                        .getCompletedLessons(currentUser.getId());

                        log.info("User {} has completed {} reading lessons",
                                        currentUser.getId(), completedLessons.size());

                        String message = completedLessons.isEmpty()
                                        ? "Bạn chưa hoàn thành bài đọc nào"
                                        : String.format("Bạn đã hoàn thành %d bài đọc", completedLessons.size());

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(completedLessons, message));

                } catch (Exception e) {
                        log.error("Error getting completed reading lessons for user {}: ",
                                        currentUser.getId(), e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ═════════════════════════════════════════════════════════════════
        // OPTIONAL: GET PROGRESS SUMMARY
        // ═════════════════════════════════════════════════════════════════

        /**
         * [USER] Lấy tổng quan tiến độ học (optional - có thể bổ sung sau)
         */
        @GetMapping("/progress/summary")
        @Operation(summary = "Lấy tổng quan tiến độ", description = "Thống kê tổng quan về tiến độ học Reading của user")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy thống kê thành công"),
                        @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
        })
        public ResponseEntity<CustomApiResponse<UserReadingProgressSummary>> getProgressSummary(
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                try {
                        log.info("User {} requesting reading progress summary", currentUser.getId());

                        // Get completed lessons
                        List<UserReadingProgress> completedLessons = readingService
                                        .getCompletedLessons(currentUser.getId());

                        // Calculate summary
                        int totalCompleted = completedLessons.size();
                        double avgScore = completedLessons.stream()
                                        .mapToDouble(p -> p.getScorePercentage().doubleValue())
                                        .average()
                                        .orElse(0.0);

                        int totalAttempts = completedLessons.stream()
                                        .mapToInt(p -> p.getAttemps() != null ? p.getAttemps() : 0)
                                        .sum();

                        UserReadingProgressSummary summary = UserReadingProgressSummary.builder()
                                        .userId(currentUser.getId())
                                        .totalCompleted(totalCompleted)
                                        .averageScore(avgScore)
                                        .totalAttempts(totalAttempts)
                                        .recentCompletions(completedLessons.stream()
                                                        .limit(5)
                                                        .map(p -> new RecentCompletion(
                                                                        p.getLesson().getId(),
                                                                        p.getLesson().getTitle(),
                                                                        p.getScorePercentage().doubleValue(),
                                                                        p.getCompletedAt()))
                                                        .toList())
                                        .build();

                        log.info("User {} progress: {} completed, avg score: {:.2f}%",
                                        currentUser.getId(), totalCompleted, avgScore);

                        return ResponseEntity.ok(
                                        CustomApiResponse.success(summary, "Lấy tổng quan tiến độ thành công"));

                } catch (Exception e) {
                        log.error("Error getting progress summary for user {}: ",
                                        currentUser.getId(), e);
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
                }
        }

        // ═════════════════════════════════════════════════════════════════
        // INNER DTOs (Optional - có thể tách ra file riêng)
        // ═════════════════════════════════════════════════════════════════

        /**
         * DTO cho progress summary
         */
        @lombok.Data
        @lombok.Builder
        private static class UserReadingProgressSummary {
                private Long userId;
                private int totalCompleted;
                private double averageScore;
                private int totalAttempts;
                private java.util.List<RecentCompletion> recentCompletions;
        }

        /**
         * DTO cho recent completion
         */
        private record RecentCompletion(
                        Long lessonId,
                        String lessonTitle,
                        double score,
                        java.time.LocalDateTime completedAt) {
        }
}