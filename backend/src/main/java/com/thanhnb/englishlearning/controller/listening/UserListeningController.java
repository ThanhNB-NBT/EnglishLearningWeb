package com.thanhnb.englishlearning.controller.listening;

import com.thanhnb.englishlearning.dto.listening.request.SubmitListeningRequest;
import com.thanhnb.englishlearning.dto.listening.response.ListeningLessonDetailResponse;
import com.thanhnb.englishlearning.dto.listening.response.ListeningLessonListResponse;
import com.thanhnb.englishlearning.dto.listening.response.SubmitListeningResponse;
import com.thanhnb.englishlearning.service.listening.ListeningLessonService;
import com.thanhnb.englishlearning.service.listening.ListeningLessonService.ListeningStatisticsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User API for listening lessons
 */
@RestController
@RequestMapping("/api/listening")
@RequiredArgsConstructor
@Slf4j
public class ListeningLessonController {

    private final ListeningLessonService listeningService;

    /**
     * Get all listening lessons with progress
     * 
     * GET /api/listening/lessons
     */
    @GetMapping("/lessons")
    public ResponseEntity<List<ListeningLessonListResponse>> getAllLessons(Authentication auth) {
        Long userId = getUserId(auth);
        List<ListeningLessonListResponse> lessons = listeningService.getLessonsForUser(userId);
        return ResponseEntity.ok(lessons);
    }

    /**
     * Get lesson detail with questions
     * 
     * GET /api/listening/lessons/{id}
     */
    @GetMapping("/lessons/{id}")
    public ResponseEntity<ListeningLessonDetailResponse> getLessonDetail(
            @PathVariable Long id,
            Authentication auth) {
        
        Long userId = getUserId(auth);
        ListeningLessonDetailResponse response = listeningService.getLessonDetail(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Track play (increment play count)
     * 
     * POST /api/listening/lessons/{id}/play
     */
    @PostMapping("/lessons/{id}/play")
    public ResponseEntity<Void> trackPlay(@PathVariable Long id, Authentication auth) {
        Long userId = getUserId(auth);
        listeningService.trackPlay(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * View transcript (unlock and mark as viewed)
     * 
     * POST /api/listening/lessons/{id}/transcript
     */
    @PostMapping("/lessons/{id}/transcript")
    public ResponseEntity<Void> viewTranscript(@PathVariable Long id, Authentication auth) {
        Long userId = getUserId(auth);
        listeningService.viewTranscript(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Submit answers
     * 
     * POST /api/listening/lessons/{id}/submit
     */
    @PostMapping("/lessons/{id}/submit")
    public ResponseEntity<SubmitListeningResponse> submitAnswers(
            @PathVariable Long id,
            @Valid @RequestBody SubmitListeningRequest request,
            Authentication auth) {
        
        Long userId = getUserId(auth);
        
        // Validate lesson ID matches
        if (!id.equals(request.getLessonId())) {
            throw new IllegalArgumentException("Lesson ID mismatch");
        }
        
        SubmitListeningResponse response = listeningService.submitAnswers(
            id, userId, request.getAnswers()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get user statistics
     * 
     * GET /api/listening/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ListeningStatisticsResponse> getStatistics(Authentication auth) {
        Long userId = getUserId(auth);
        ListeningStatisticsResponse stats = listeningService.getUserStatistics(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Helper: Extract user ID from authentication
     */
    private Long getUserId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}