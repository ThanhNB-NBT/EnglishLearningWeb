package com.thanhnb.englishlearning.controller.reading;

import com.thanhnb.englishlearning.dto.reading.*;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.topic.TopicUserDto;
import com.thanhnb.englishlearning.entity.reading.UserReadingProgress;
import com.thanhnb.englishlearning.service.reading.ReadingLearningService;
import com.thanhnb.englishlearning.service.topic.UserTopicService;
import com.thanhnb.englishlearning.security.UserPrincipal;
import com.thanhnb.englishlearning.enums.ModuleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ FIXED: Reading Controller
 * - Loại bỏ try-catch (dùng GlobalExceptionHandler)
 * - Thêm logging
 * - API design nhất quán (lessonId trong path)
 */
@RestController
@RequestMapping("/api/reading")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "Reading", description = "API học bài đọc hiểu (dành cho USER)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ReadingController {

        private final ReadingLearningService readingService;
        private final UserTopicService userTopicService;

        // ═══════════════════════════════════════════════════════════
        // TOPIC APIs
        // ═══════════════════════════════════════════════════════════

        @GetMapping("/topics")
        @Operation(summary = "Lấy danh sách Reading topics")
        public ResponseEntity<CustomApiResponse<List<TopicUserDto>>> getReadingTopics(
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} fetching reading topics", currentUser.getId());

                List<TopicUserDto> topics = userTopicService.getTopicsForUser(
                                ModuleType.READING,
                                currentUser.getId());

                log.debug("Found {} reading topics for user {}", topics.size(), currentUser.getId());

                return ResponseEntity.ok(CustomApiResponse.success(
                                topics,
                                "Lấy danh sách chủ đề bài đọc thành công"));
        }

        // ═══════════════════════════════════════════════════════════
        // LESSON APIs
        // ═══════════════════════════════════════════════════════════

        @GetMapping("/topics/{topicId}/lessons")
        @JsonView(Views.Public.class)
        @Operation(summary = "Lấy bài đọc theo topic")
        public ResponseEntity<CustomApiResponse<List<ReadingLessonDTO>>> getLessonsByTopic(
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        @PathVariable Long topicId) {

                log.info("User {} fetching lessons for reading topic {}",
                                currentUser.getId(), topicId);

                List<ReadingLessonDTO> lessons = readingService
                                .getAllLessonsForUser(currentUser.getId(), topicId);

                log.debug("Found {} lessons in reading topic {} for user {}",
                                lessons.size(), topicId, currentUser.getId());

                return ResponseEntity.ok(CustomApiResponse.success(
                                lessons,
                                "Lấy danh sách bài đọc thành công"));
        }

        @GetMapping("/lessons/{lessonId}")
        @JsonView(Views.Public.class)
        @Operation(summary = "Lấy chi tiết bài đọc")
        public ResponseEntity<CustomApiResponse<ReadingLessonDTO>> getLessonDetail(
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        @PathVariable Long lessonId) {

                log.info("User {} fetching reading lesson {}", currentUser.getId(), lessonId);

                ReadingLessonDTO lesson = readingService.getLessonDetail(
                                lessonId,
                                currentUser.getId());

                return ResponseEntity.ok(CustomApiResponse.success(
                                lesson,
                                "Lấy chi tiết bài đọc thành công"));
        }

        @PostMapping("/lessons/{lessonId}/submit")
        @Operation(summary = "Nộp bài đọc")
        public ResponseEntity<CustomApiResponse<ReadingSubmitResponse>> submitLesson(
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        @PathVariable Long lessonId, // ✅ THAY ĐỔI: Lấy từ path
                        @Valid @RequestBody ReadingSubmitRequest request) {

                log.info("User {} submitting reading lesson {}", currentUser.getId(), lessonId);

                // ✅ Set lessonId từ path vào request
                request.setLessonId(lessonId);

                ReadingSubmitResponse response = readingService.submitLesson(
                                currentUser.getId(), request);

                String message = response.getIsCompleted()
                                ? String.format("Chúc mừng! Hoàn thành với %.2f%% điểm",
                                                response.getScorePercentage())
                                : String.format("Bạn đạt %.2f%%. Cần 80%% để qua bài",
                                                response.getScorePercentage());

                log.info("User {} submitted reading lesson {}: completed={}, score={}",
                                currentUser.getId(), lessonId,
                                response.getIsCompleted(), response.getScorePercentage());

                return ResponseEntity.ok(CustomApiResponse.success(response, message));
        }

        // ═══════════════════════════════════════════════════════════
        // PROGRESS APIs
        // ═══════════════════════════════════════════════════════════

        @GetMapping("/progress/completed")
        @Operation(summary = "Lấy danh sách bài đã hoàn thành")
        public ResponseEntity<CustomApiResponse<List<UserReadingProgress>>> getCompletedLessons(
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.debug("User {} fetching completed reading lessons", currentUser.getId());

                List<UserReadingProgress> completed = readingService
                                .getCompletedLessons(currentUser.getId());

                return ResponseEntity.ok(CustomApiResponse.success(completed, "Thành công"));
        }

        @GetMapping("/progress/summary")
        @Operation(summary = "Lấy tổng quan tiến độ")
        public ResponseEntity<CustomApiResponse<UserReadingProgressSummary>> getProgressSummary(
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} fetching reading progress summary", currentUser.getId());

                List<UserReadingProgress> completedLessons = readingService
                                .getCompletedLessons(currentUser.getId());

                int totalCompleted = completedLessons.size();

                // ✅ Chỉ tính average cho các lesson có điểm > 0
                double avgScore = completedLessons.stream()
                                .filter(p -> p.getScorePercentage() != null && p.getScorePercentage() > 0)
                                .mapToDouble(UserReadingProgress::getScorePercentage)
                                .average()
                                .orElse(0.0);

                int totalAttempts = completedLessons.stream()
                                .filter(p -> p.getAttempts() != null)
                                .mapToInt(UserReadingProgress::getAttempts)
                                .sum();

                // ✅ Sắp xếp theo thời gian hoàn thành, lấy 5 bài gần nhất
                List<RecentCompletion> recentCompletions = completedLessons.stream()
                                .filter(p -> p.getCompletedAt() != null)
                                .sorted((p1, p2) -> p2.getCompletedAt().compareTo(p1.getCompletedAt()))
                                .limit(5)
                                .map(p -> new RecentCompletion(
                                                p.getLesson().getId(),
                                                p.getLesson().getTitle(),
                                                p.getScorePercentage() != null ? p.getScorePercentage() : 0.0,
                                                p.getCompletedAt()))
                                .toList();

                UserReadingProgressSummary summary = UserReadingProgressSummary.builder()
                                .userId(currentUser.getId())
                                .totalCompleted(totalCompleted)
                                .averageScore(Math.round(avgScore * 100.0) / 100.0)
                                .totalAttempts(totalAttempts)
                                .recentCompletions(recentCompletions)
                                .build();

                log.debug("Reading progress summary for user {}: completed={}, avgScore={}",
                                currentUser.getId(), totalCompleted, summary.getAverageScore());

                return ResponseEntity.ok(CustomApiResponse.success(
                                summary,
                                "Lấy tổng quan tiến độ thành công"));
        }

        // ═══════════════════════════════════════════════════════════
        // INNER CLASSES
        // ═══════════════════════════════════════════════════════════

        @lombok.Data
        @lombok.Builder
        private static class UserReadingProgressSummary {
                private Long userId;
                private int totalCompleted;
                private double averageScore;
                private int totalAttempts;
                private List<RecentCompletion> recentCompletions;
        }

        private record RecentCompletion(
                        Long lessonId,
                        String lessonTitle,
                        double score,
                        LocalDateTime completedAt) {
        }
}