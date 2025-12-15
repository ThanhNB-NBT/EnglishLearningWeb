package com.thanhnb.englishlearning.controller. listening;

import com.thanhnb.englishlearning.dto. CustomApiResponse;
import com.thanhnb.englishlearning.dto.listening.request. SubmitListeningRequest;
import com.thanhnb.englishlearning.dto.listening. response.ListeningLessonDetailResponse;
import com.thanhnb.englishlearning.dto.listening.response.ListeningLessonListResponse;
import com.thanhnb.englishlearning.dto.listening.response. SubmitListeningResponse;
import com.thanhnb.englishlearning.entity.listening. UserListeningProgress;
import com.thanhnb.englishlearning.security.UserPrincipal;
import com. thanhnb.englishlearning.service.listening.ListeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io. swagger.v3.oas. annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework. security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web. bind.annotation.*;

import java. util.List;

/**
 * USER Controller cho Listening module
 * Refactored:  Đổi từ UserListeningController -> ListeningController
 * - Sử dụng @AuthenticationPrincipal UserPrincipal thay vì Authentication
 * - Response format nhất quán với Reading/Grammar modules
 * - Proper error handling và logging
 */
@RestController
@RequestMapping("/api/listening")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "Listening (User)", description = "API bài nghe cho người dùng")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ListeningController {

    private final ListeningService listeningService;

    // ═════════════════════════════════════════════════════════════════
    // LESSON LIST & DETAIL
    // ═════════════════════════════════════════════════════════════════

    /**
     * [USER] Lấy tất cả bài nghe với progress và unlock status
     */
    @GetMapping("/lessons")
    @Operation(summary = "Lấy danh sách bài nghe", description = "Trả về tất cả bài nghe active với progress của user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi lấy danh sách")
    })
    public ResponseEntity<CustomApiResponse<List<ListeningLessonListResponse>>> getAllLessons(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            log.info("User {} loading listening lessons", currentUser.getId());

            List<ListeningLessonListResponse> lessons = listeningService.getAllLessonsForUser(currentUser.getId());

            log.info("User {} loaded {} listening lessons", currentUser.getId(), lessons.size());

            return ResponseEntity.ok(
                    CustomApiResponse.success(lessons, "Lấy danh sách bài nghe thành công"));

        } catch (Exception e) {
            log.error("Error loading listening lessons for user {}:  ", currentUser.getId(), e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi:  " + e.getMessage()));
        }
    }

    /**
     * [USER] Lấy chi tiết bài nghe với questions
     */
    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Lấy chi tiết bài nghe", description = "Trả về nội dung, audio, transcript (nếu đã unlock) và câu hỏi")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
            @ApiResponse(responseCode = "400", description = "Bài nghe không tồn tại hoặc chưa unlock")
    })
    public ResponseEntity<CustomApiResponse<ListeningLessonDetailResponse>> getLessonDetail(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "ID của bài nghe") @PathVariable Long lessonId) {
        try {
            log.info("User {} loading listening lesson detail:  lessonId={}", currentUser.getId(), lessonId);

            ListeningLessonDetailResponse lesson = listeningService.getLessonDetail(lessonId, currentUser.getId());

            log.info("User {} loaded listening lesson {} with {} questions",
                    currentUser.getId(), lessonId, lesson.getQuestions().size());

            return ResponseEntity.ok(
                    CustomApiResponse.success(lesson, "Lấy chi tiết bài nghe thành công"));

        } catch (RuntimeException e) {
            log.warn("User {} failed to load listening lesson {}: {}",
                    currentUser.getId(), lessonId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error loading listening lesson detail:  ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // SUBMIT LESSON
    // ═════════════════════════════════════════════════════════════════

    /**
     * [USER] Nộp bài nghe và nhận kết quả
     */
    @PostMapping("/lessons/{lessonId}/submit")
    @Operation(summary = "Nộp bài nghe", description = "Nộp câu trả lời và nhận kết quả chi tiết")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nộp bài thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public ResponseEntity<CustomApiResponse<SubmitListeningResponse>> submitLesson(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "ID của bài nghe") @PathVariable Long lessonId,
            @Valid @RequestBody SubmitListeningRequest request) {
        try {
            log.info("User {} submitting listening lesson {} with {} answers",
                    currentUser.getId(),
                    lessonId,
                    request.getAnswers() != null ? request.getAnswers().size() : 0);

            SubmitListeningResponse response = listeningService.submitLesson(
                    currentUser.getId(), lessonId, request);

            // Build message based on result
            String message;
            if (response.isPassed()) {
                message = String.format(
                        "Chúc mừng! Bạn đã hoàn thành bài nghe với điểm %. 2f%% (%d/%d câu đúng)",
                        response.getScorePercentage(),
                        response.getCorrectCount(),
                        response.getTotalQuestions());
            } else {
                message = String.format(
                        "Bạn đã đạt %. 2f%% (%d/%d câu đúng). Cần đạt tối thiểu 80%% để hoàn thành bài",
                        response.getScorePercentage(),
                        response.getCorrectCount(),
                        response.getTotalQuestions());
            }

            log.info("User {} completed listening lesson {} - Score: {:. 2f}%, Passed: {}",
                    currentUser.getId(),
                    lessonId,
                    response.getScorePercentage(),
                    response.isPassed());

            return ResponseEntity.ok(CustomApiResponse.success(response, message));

        } catch (RuntimeException e) {
            log.warn("User {} submit failed for listening lesson {}: {}",
                    currentUser.getId(), lessonId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error submitting listening lesson:  ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // AUDIO PLAYBACK & TRANSCRIPT
    // ═════════════════════════════════════════════════════════════════

    /**
     * [USER] Track play count (user click play audio)
     */
    @PostMapping("/lessons/{lessonId}/play")
    @Operation(summary = "Track play count", description = "Tăng số lần nghe khi user click play audio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Track play count thành công"),
            @ApiResponse(responseCode = "400", description = "Đã hết lượt nghe")
    })
    public ResponseEntity<CustomApiResponse<Void>> trackPlay(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "ID của bài nghe") @PathVariable Long lessonId) {
        try {
            log.info("User {} playing audio for lesson {}", currentUser.getId(), lessonId);

            listeningService.incrementPlayCount(currentUser.getId(), lessonId);

            return ResponseEntity.ok(
                    CustomApiResponse.success(null, "Đã ghi nhận lượt nghe"));

        } catch (RuntimeException e) {
            log.warn("User {} play failed for lesson {}: {}",
                    currentUser.getId(), lessonId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error tracking play count:  ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * [USER] Unlock transcript (after completion)
     */
    @PostMapping("/lessons/{lessonId}/transcript")
    @Operation(summary = "Unlock transcript", description = "Mở khóa và đánh dấu đã xem transcript (chỉ sau khi hoàn thành)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unlock transcript thành công"),
            @ApiResponse(responseCode = "400", description = "Chưa hoàn thành bài nghe")
    })
    public ResponseEntity<CustomApiResponse<Void>> unlockTranscript(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "ID của bài nghe") @PathVariable Long lessonId) {
        try {
            log.info("User {} unlocking transcript for lesson {}", currentUser.getId(), lessonId);

            listeningService.unlockTranscript(currentUser.getId(), lessonId);

            return ResponseEntity.ok(
                    CustomApiResponse.success(null, "Đã mở khóa transcript"));

        } catch (RuntimeException e) {
            log.warn("User {} unlock transcript failed for lesson {}: {}",
                    currentUser.getId(), lessonId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error unlocking transcript: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e. getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // PROGRESS & HISTORY
    // ═════════════════════════════════════════════════════════════════

    /**
     * [USER] Lấy danh sách bài đã hoàn thành
     */
    @GetMapping("/progress/completed")
    @Operation(summary = "Lấy danh sách bài đã hoàn thành", description = "Xem lịch sử các bài nghe đã hoàn thành")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi lấy danh sách")
    })
    public ResponseEntity<CustomApiResponse<List<UserListeningProgress>>> getCompletedLessons(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            log.info("User {} loading completed listening lessons", currentUser.getId());

            List<UserListeningProgress> completedLessons = listeningService.getCompletedLessons(currentUser.getId());

            log.info("User {} has {} completed listening lessons", currentUser.getId(), completedLessons.size());

            return ResponseEntity. ok(
                    CustomApiResponse.success(completedLessons, "Lấy danh sách bài đã hoàn thành thành công"));

        } catch (Exception e) {
            log.error("Error loading completed lessons for user {}: ", currentUser.getId(), e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }
}