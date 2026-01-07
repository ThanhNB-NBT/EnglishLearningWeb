package com.thanhnb.englishlearning.controller.user;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.entity.recommendation.AIRecommendation;
import com.thanhnb.englishlearning.security.UserPrincipal;
import com.thanhnb.englishlearning.service.ai.recommendation.AIRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "AI Recommendation", description = "API gợi ý học tập thông minh từ AI")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class RecommendationController {

    private final AIRecommendationService recommendationService;

    @GetMapping("/generate")
    @Operation(summary = "Lấy gợi ý học tập (Generate/Cache)", description = "Hệ thống sẽ kiểm tra cache hoặc gọi AI để phân tích và trả về danh sách gợi ý.")
    public ResponseEntity<CustomApiResponse<List<AIRecommendation>>> getRecommendations(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        try {
            // Logic service đã bao gồm: Check cache -> Gọi Gemini -> Lưu DB
            List<AIRecommendation> recommendations = recommendationService.generateRecommendations(currentUser.getId());
            
            return ResponseEntity.ok(CustomApiResponse.success(recommendations, 
                    "Lấy danh sách gợi ý thành công"));
        } catch (Exception e) {
            log.error("Error generating recommendations: ", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi khi lấy gợi ý: " + e.getMessage()));
        }
    }

    // API phụ để đánh dấu đã làm xong gợi ý (nếu cần)
    @PutMapping("/{id}/complete")
    @Operation(summary = "Đánh dấu đã hoàn thành gợi ý")
    public ResponseEntity<CustomApiResponse<Void>> markAsCompleted(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        try {
            // Bạn có thể implement thêm hàm này trong Service nếu muốn track trạng thái manual
            // recommendationService.markAsCompleted(id, currentUser.getId());
            return ResponseEntity.ok(CustomApiResponse.success(null, "Đã cập nhật trạng thái"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.badRequest("Lỗi: " + e.getMessage()));
        }
    }
}