package com.thanhnb.englishlearning.controller.listening;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.listening.ListeningLessonDTO;
import com.thanhnb.englishlearning.dto.listening.ListeningSubmitRequest;
import com.thanhnb.englishlearning.dto.listening.ListeningSubmitResponse;
import com.thanhnb.englishlearning.dto.topic.TopicUserDto;
import com.thanhnb.englishlearning.entity.listening.UserListeningProgress;
import com.thanhnb.englishlearning.security.UserPrincipal;
import com.thanhnb.englishlearning.service.listening.ListeningLearningService;
import com.thanhnb.englishlearning.service.topic.UserTopicService;
import com.thanhnb.englishlearning.enums.ModuleType;

import com.thanhnb.englishlearning.service.listening.ListeningLearningService.ListeningProgressSummary;
import com.thanhnb.englishlearning.service.listening.ListeningLearningService.PlayCountResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;

import java.util.List;

/**
 * ✅ FIXED: Thin Listening Controller
 * - Removed inner classes (use Service classes instead)
 * - Simplified methods (delegate to service)
 * - No business logic in controller
 */
@RestController
@RequestMapping("/api/listening")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "Listening (User)", description = "API bài nghe cho người dùng")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ListeningController {

        private final ListeningLearningService listeningService;
        private final UserTopicService userTopicService;

        // ═══════════════════════════════════════════════════════════
        // TOPIC APIs
        // ═══════════════════════════════════════════════════════════

        @GetMapping("/topics")
        @Operation(summary = "Lấy danh sách Listening topics")
        public ResponseEntity<CustomApiResponse<List<TopicUserDto>>> getListeningTopics(
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} fetching listening topics", currentUser.getId());

                List<TopicUserDto> topics = userTopicService.getTopicsForUser(
                                ModuleType.LISTENING,
                                currentUser.getId());

                log.debug("Found {} listening topics for user {}", topics.size(), currentUser.getId());

                return ResponseEntity.ok(CustomApiResponse.success(
                                topics,
                                "Lấy danh sách chủ đề bài nghe thành công"));
        }

        // ═══════════════════════════════════════════════════════════
        // LESSON APIs
        // ═══════════════════════════════════════════════════════════

        @GetMapping("/topics/{topicId}/lessons")
        @JsonView(Views.Public.class)
        @Operation(summary = "Lấy bài nghe theo topic")
        public ResponseEntity<CustomApiResponse<List<ListeningLessonDTO>>> getLessonsByTopic(
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        @PathVariable Long topicId) {

                log.info("User {} fetching lessons for listening topic {}",
                                currentUser.getId(), topicId);

                List<ListeningLessonDTO> lessons = listeningService
                                .getAllLessonsForUser(currentUser.getId(), topicId);

                log.debug("Found {} lessons in listening topic {} for user {}",
                                lessons.size(), topicId, currentUser.getId());

                return ResponseEntity.ok(CustomApiResponse.success(
                                lessons,
                                "Lấy danh sách bài nghe thành công"));
        }

        @GetMapping("/lessons/{lessonId}")
        @JsonView(Views.Public.class)
        @Operation(summary = "Lấy chi tiết bài nghe")
        public ResponseEntity<CustomApiResponse<ListeningLessonDTO>> getLessonDetail(
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        @PathVariable Long lessonId) {

                log.info("User {} fetching listening lesson {}", currentUser.getId(), lessonId);

                ListeningLessonDTO lesson = listeningService.getLessonDetail(
                                lessonId,
                                currentUser.getId());

                return ResponseEntity.ok(CustomApiResponse.success(
                                lesson,
                                "Lấy chi tiết bài nghe thành công"));
        }

        @PostMapping("/lessons/{lessonId}/submit")
        @Operation(summary = "Nộp bài nghe")
        public ResponseEntity<CustomApiResponse<ListeningSubmitResponse>> submitLesson(
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        @PathVariable Long lessonId,
                        @Valid @RequestBody ListeningSubmitRequest request) {

                log.info("User {} submitting listening lesson {}", currentUser.getId(), lessonId);

                ListeningSubmitResponse response = listeningService.submitLesson(
                                currentUser.getId(), lessonId, request);

                String message = response.getIsPassed()
                                ? String.format("Chúc mừng! Bạn đã hoàn thành bài nghe với điểm %.2f%%",
                                                response.getScorePercentage())
                                : String.format("Bạn đạt %.2f%%. Cần đạt tối thiểu 80%% để qua bài",
                                                response.getScorePercentage());

                log.info("User {} submitted listening lesson {}: passed={}, score={}",
                                currentUser.getId(), lessonId,
                                response.getIsPassed(), response.getScorePercentage());

                return ResponseEntity.ok(CustomApiResponse.success(response, message));
        }

        // ═══════════════════════════════════════════════════════════
        // LISTENING SPECIFIC FEATURES
        // ═══════════════════════════════════════════════════════════

        @PostMapping("/lessons/{lessonId}/play")
        @Operation(summary = "Track play count")
        public ResponseEntity<CustomApiResponse<PlayCountResponse>> trackPlay(
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        @PathVariable Long lessonId) {

                log.debug("User {} playing listening lesson {}", currentUser.getId(), lessonId);

                // ✅ FIXED: Now returns ListeningLearningService.PlayCountResponse
                PlayCountResponse response = listeningService.trackPlayCount(
                                currentUser.getId(), lessonId);

                return ResponseEntity.ok(CustomApiResponse.success(
                                response,
                                response.getMessage()));
        }

        @PostMapping("/lessons/{lessonId}/transcript")
        @Operation(summary = "Unlock transcript")
        public ResponseEntity<CustomApiResponse<Void>> unlockTranscript(
                        @AuthenticationPrincipal UserPrincipal currentUser,
                        @PathVariable Long lessonId) {

                log.info("User {} requesting transcript unlock for lesson {}",
                                currentUser.getId(), lessonId);

                // ✅ FIXED: Service returns message directly
                String message = listeningService.unlockTranscriptForUser(
                                currentUser.getId(), lessonId);

                return ResponseEntity.ok(CustomApiResponse.success(null, message));
        }

        // ═══════════════════════════════════════════════════════════
        // PROGRESS APIs
        // ═══════════════════════════════════════════════════════════

        @GetMapping("/progress/completed")
        @Operation(summary = "Lấy danh sách bài đã hoàn thành")
        public ResponseEntity<CustomApiResponse<List<UserListeningProgress>>> getCompletedLessons(
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.debug("User {} fetching completed listening lessons", currentUser.getId());

                List<UserListeningProgress> completed = listeningService
                                .getCompletedLessons(currentUser.getId());

                return ResponseEntity.ok(CustomApiResponse.success(completed, "Thành công"));
        }

        @GetMapping("/progress/summary")
        @Operation(summary = "Lấy tổng quan tiến độ học tập")
        public ResponseEntity<CustomApiResponse<ListeningProgressSummary>> getProgressSummary(
                        @AuthenticationPrincipal UserPrincipal currentUser) {

                log.info("User {} fetching listening progress summary", currentUser.getId());

                // ✅ FIXED: Now returns ListeningLearningService.ListeningProgressSummary
                ListeningProgressSummary summary = listeningService
                                .getProgressSummary(currentUser.getId());

                return ResponseEntity.ok(CustomApiResponse.success(
                                summary,
                                "Lấy tổng quan tiến độ thành công"));
        }
}