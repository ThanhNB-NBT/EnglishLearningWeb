package com.thanhnb.englishlearning.controller.test;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.thanhnb.englishlearning.service.test.PlacementTestService;
import com.thanhnb.englishlearning.service.test.PlacementTestService.PlacementResultResponse;
import com.thanhnb.englishlearning.service.test.PlacementTestService.PlacementSubmitRequest;
import com.thanhnb.englishlearning.service.test.PlacementTestService.PlacementTestResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/user/placement-test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Placement Test", description = "API kiểm tra trình độ đầu vào")
public class PlacementTestController {

    private final PlacementTestService placementTestService;

    /**
     * GET /api/user/placement-test
     * Lấy đề thi placement test (30 câu từ Grammar, Reading, Listening)
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')") // Chỉ user đã đăng nhập mới làm được
    @Operation(summary = "Lấy đề thi placement test", description = "Trả về đề thi gồm 3 phần: Grammar, Reading, Listening. "
            +
            "Đáp án đã được shuffle để tránh ghi nhớ vị trí.")
    public ResponseEntity<PlacementTestResponse> getPlacementTest() {
        log.info("User requesting placement test");

        PlacementTestResponse response = placementTestService.getPlacementTest();

        log.info("Placement test generated: {} questions, {} sections",
                response.getTotalQuestions(), response.getSections().size());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/user/placement-test/submit
     * Nộp bài thi và nhận kết quả + cập nhật English Level
     */
    @PostMapping("/submit")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Nộp bài placement test", description = "Chấm điểm, xác định level (A1-C1) và cập nhật vào hồ sơ user. "
            +
            "User sẽ nhận được: điểm số, số câu đúng/tổng câu, level mới.")
    public ResponseEntity<PlacementResultResponse> submitTest(
            @Valid @RequestBody PlacementSubmitRequest request) {

        log.info("User submitting placement test with {} answers",
                request.getAnswers().size());

        PlacementResultResponse result = placementTestService.submitTest(request);

        log.info("Placement test completed: Score={}%, Level={}",
                result.getScore(), result.getAssignedLevel());

        return ResponseEntity.ok(result);
    }
}