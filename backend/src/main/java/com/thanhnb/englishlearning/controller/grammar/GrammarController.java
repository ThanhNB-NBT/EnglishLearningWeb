package com.thanhnb.englishlearning.controller.grammar;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.dto.topic.TopicUserDto;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.entity.grammar.UserGrammarProgress;
import com.thanhnb.englishlearning.security.UserPrincipal;
import com.thanhnb.englishlearning.service.grammar.GrammarLearningService;
import com.thanhnb.englishlearning.service.topic.UserTopicService;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@Slf4j
@RequestMapping("/api/grammar")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "Grammar", description = "API ngữ pháp cho user")
@SecurityRequirement(name = "bearerAuth")
public class GrammarController {

        private final GrammarLearningService grammarService;
        private final UserTopicService userTopicService;

        // ═══════════════════════════════════════════════════════════
        // TOPIC APIs
        // ═══════════════════════════════════════════════════════════

        @GetMapping("/topics")
        @Operation(summary = "Lấy danh sách Grammar topics", description = "Trả về danh sách chủ đề ngữ pháp kèm bài học")
        public ResponseEntity<CustomApiResponse<List<TopicUserDto>>> getGrammarTopics(
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} fetching grammar topics", currentUser.getId());

                List<TopicUserDto> topics = userTopicService.getTopicsForUser(
                                ModuleType.GRAMMAR,
                                currentUser.getId());

                log.debug("Found {} grammar topics for user {}", topics.size(), currentUser.getId());

                return ResponseEntity.ok(CustomApiResponse.success(
                                topics,
                                "Lấy danh sách chủ đề ngữ pháp thành công"));
        }

        // ═══════════════════════════════════════════════════════════
        // LESSON APIs
        // ═══════════════════════════════════════════════════════════

        @GetMapping("/topics/{topicId}/lessons")
        @JsonView(Views.Public.class)
        @Operation(summary = "Lấy bài học theo topic")
        public ResponseEntity<CustomApiResponse<List<GrammarLessonDTO>>> getLessonsByTopic(
                        @Parameter(description = "ID của topic") @PathVariable Long topicId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} fetching lessons for grammar topic {}",
                                currentUser.getId(), topicId);

                List<GrammarLessonDTO> lessons = grammarService.getAllLessonsForUser(
                                currentUser.getId(),
                                topicId);

                log.debug("Found {} lessons in grammar topic {} for user {}",
                                lessons.size(), topicId, currentUser.getId());

                return ResponseEntity.ok(CustomApiResponse.success(
                                lessons,
                                "Lấy danh sách bài học thành công"));
        }

        // ✅ FIXED: Thêm try-catch để debug
        @GetMapping("/lessons/{lessonId}")
        @JsonView(Views.Public.class)
        @Operation(summary = "Lấy nội dung bài học")
        public ResponseEntity<CustomApiResponse<GrammarLessonDTO>> getLessonContent(
                        @Parameter(description = "ID của bài học") @PathVariable Long lessonId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} fetching grammar lesson {}", currentUser.getId(), lessonId);

                try {
                        GrammarLessonDTO lesson = grammarService.getLessonDetail(
                                        lessonId,
                                        currentUser.getId());

                        log.info("✅ Service returned lesson: {}", lesson.getTitle());
                        log.info("✅ Has groupedQuestions: {}", lesson.getGroupedQuestions() != null);

                        if (lesson.getGroupedQuestions() != null) {
                                log.info("  - Tasks: {}",
                                                lesson.getGroupedQuestions().getTasks() != null
                                                                ? lesson.getGroupedQuestions().getTasks().size()
                                                                : 0);
                                log.info("  - Standalone: {}",
                                                lesson.getGroupedQuestions().getStandaloneQuestions() != null
                                                                ? lesson.getGroupedQuestions().getStandaloneQuestions()
                                                                                .size()
                                                                : 0);
                        }

                        CustomApiResponse<GrammarLessonDTO> response = CustomApiResponse.success(
                                        lesson,
                                        "Lấy nội dung bài học thành công");

                        log.info("✅ Built response successfully");

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        log.error("❌ ERROR in getLessonContent: {}", e.getMessage(), e);
                        throw e;
                }
        }

        // ✅ TEST ENDPOINT - Thêm vào GrammarController

        @GetMapping("/lessons/{lessonId}/test")
        @Operation(summary = "TEST - Lấy lesson đơn giản")
        public ResponseEntity<Map<String, Object>> testGetLesson(
                        @PathVariable Long lessonId,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("🧪 TEST: User {} fetching lesson {}", currentUser.getId(), lessonId);

                try {
                        // Step 1: Get lesson from service
                        GrammarLessonDTO lesson = grammarService.getLessonDetail(lessonId, currentUser.getId());
                        log.info("✅ Step 1: Got lesson from service");

                        // Step 2: Build simple response (NO groupedQuestions)
                        Map<String, Object> response = new HashMap<>();
                        response.put("id", lesson.getId());
                        response.put("title", lesson.getTitle());
                        response.put("lessonType", lesson.getLessonType());
                        response.put("topicId", lesson.getTopicId());
                        log.info("✅ Step 2: Built simple response");

                        // Step 3: Try to get questions count
                        if (lesson.getGroupedQuestions() != null) {
                                log.info("✅ Step 3: Has groupedQuestions");
                                response.put("hasQuestions", true);

                                // DON'T serialize groupedQuestions yet
                                // response.put("groupedQuestions", lesson.getGroupedQuestions());
                        } else {
                                log.warn("⚠️ Step 3: No groupedQuestions!");
                                response.put("hasQuestions", false);
                        }

                        log.info("✅ Step 4: Returning response");
                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        log.error("❌ TEST ERROR: {}", e.getMessage(), e);

                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", e.getMessage());
                        errorResponse.put("class", e.getClass().getSimpleName());

                        return ResponseEntity.status(500).body(errorResponse);
                }
        }

        @PostMapping("/lessons/submit")
        @Operation(summary = "Nộp bài tập")
        public ResponseEntity<CustomApiResponse<GrammarSubmitResponse>> submitLesson(
                        @Valid @RequestBody GrammarSubmitRequest request,
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} submitting grammar lesson {}",
                                currentUser.getId(), request.getLessonId());

                GrammarSubmitResponse result = grammarService.submitLesson(
                                currentUser.getId(),
                                request);

                String message = result.getIsPassed()
                                ? "Hoàn thành bài học thành công! Điểm: " + result.getTotalScore()
                                : "Đã nộp bài. Hãy xem lại các gợi ý để cải thiện điểm số!";

                log.info("User {} submitted grammar lesson {}: passed={}, score={}",
                                currentUser.getId(), request.getLessonId(),
                                result.getIsPassed(), result.getScorePercentage());

                return ResponseEntity.ok(CustomApiResponse.success(result, message));
        }

        // ═══════════════════════════════════════════════════════════
        // PROGRESS APIs
        // ═══════════════════════════════════════════════════════════

        @GetMapping("/progress/completed")
        @Operation(summary = "Lấy danh sách bài đã hoàn thành")
        public ResponseEntity<CustomApiResponse<List<UserGrammarProgress>>> getCompletedLessons(
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.debug("User {} fetching completed grammar lessons", currentUser.getId());

                List<UserGrammarProgress> completed = grammarService.getCompletedLessons(
                                currentUser.getId());

                return ResponseEntity.ok(CustomApiResponse.success(completed, "Thành công"));
        }

        @GetMapping("/progress/summary")
        @Operation(summary = "Lấy tổng quan tiến độ học tập")
        public ResponseEntity<CustomApiResponse<GrammarProgressSummary>> getProgressSummary(
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} fetching grammar progress summary", currentUser.getId());

                List<UserGrammarProgress> completedLessons = grammarService.getCompletedLessons(
                                currentUser.getId());

                int totalCompleted = completedLessons.size();

                // ✅ Chỉ tính average cho các lesson có điểm > 0
                double avgScore = completedLessons.stream()
                                .filter(p -> p.getScorePercentage() != null && p.getScorePercentage() > 0)
                                .mapToDouble(UserGrammarProgress::getScorePercentage)
                                .average()
                                .orElse(0.0);

                int totalAttempts = completedLessons.stream()
                                .filter(p -> p.getAttempts() != null)
                                .mapToInt(UserGrammarProgress::getAttempts)
                                .sum();

                // ✅ Sắp xếp đúng theo completed_at
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

                GrammarProgressSummary summary = GrammarProgressSummary.builder()
                                .userId(currentUser.getId())
                                .totalCompleted(totalCompleted)
                                .averageScore(Math.round(avgScore * 100.0) / 100.0)
                                .totalAttempts(totalAttempts)
                                .recentCompletions(recentCompletions)
                                .build();

                log.debug("Grammar progress summary for user {}: completed={}, avgScore={}",
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
        private static class GrammarProgressSummary {
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