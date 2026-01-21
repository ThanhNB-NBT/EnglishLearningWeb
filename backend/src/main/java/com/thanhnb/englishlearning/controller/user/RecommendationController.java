package com.thanhnb.englishlearning.controller.user;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.ai.AIRecommendationDto;
import com.thanhnb.englishlearning.entity.recommendation.AIRecommendation;
import com.thanhnb.englishlearning.mapper.RecommendationMapper;
import com.thanhnb.englishlearning.security.UserPrincipal;
import com.thanhnb.englishlearning.service.ai.recommendation.AIRecommendationService;
import com.thanhnb.englishlearning.service.ai.recommendation.RecommendationTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "AI Recommendation", description = "Smart AI-powered learning recommendations")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class RecommendationController {

        private final AIRecommendationService recommendationService;
        private final RecommendationTrackingService trackingService;
        private final RecommendationMapper recommendationMapper;

        @GetMapping
        @Operation(summary = "Get enriched recommendations")
        public ResponseEntity<CustomApiResponse<List<AIRecommendationDto>>> getRecommendations(
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                try {
                        List<AIRecommendation> recommendations = recommendationService
                                        .generateRecommendations(currentUser.getId());

                        recommendations.forEach(rec -> {
                                if (rec.getId() != null && !rec.isShown()) {
                                        try {
                                                trackingService.markAsShown(rec.getId(), currentUser.getId());
                                        } catch (Exception e) {
                                                log.warn("Failed to mark recommendation {} as shown", rec.getId());
                                        }
                                }
                        });

                        List<AIRecommendationDto> enrichedDtos = recommendations.stream()
                                        .map(recommendationMapper::toDto)
                                        .collect(Collectors.toList());

                        String message = enrichedDtos.isEmpty()
                                        ? "Chưa có gợi ý nào phù hợp lúc này"
                                        : "Đã tạo " + enrichedDtos.size() + " gợi ý phù hợp với bạn";

                        return ResponseEntity.ok(CustomApiResponse.success(enrichedDtos, message));

                } catch (IllegalArgumentException e) {
                        log.error("Invalid user: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(CustomApiResponse.badRequest("Người dùng không tồn tại"));

                } catch (Exception e) {
                        log.error("Error getting recommendations for user {}: {}",
                                        currentUser.getId(), e.getMessage(), e);
                        return ResponseEntity.status(500)
                                        .body(CustomApiResponse.error(500,
                                                        "Lỗi hệ thống khi tạo gợi ý. Vui lòng thử lại sau."));
                }
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get specific recommendation with full details")
        public ResponseEntity<CustomApiResponse<AIRecommendationDto>> getRecommendation(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                try {
                        AIRecommendation rec = recommendationService.getRecommendationById(id, currentUser.getId());
                        AIRecommendationDto dto = recommendationMapper.toDto(rec);

                        return ResponseEntity.ok(CustomApiResponse.success(dto, "Chi tiết gợi ý"));

                } catch (IllegalArgumentException e) {
                        return ResponseEntity.status(404)
                                        .body(CustomApiResponse.error(404, "Không tìm thấy gợi ý này"));

                } catch (SecurityException e) {
                        return ResponseEntity.status(403)
                                        .body(CustomApiResponse.error(403, "Không có quyền truy cập"));

                } catch (Exception e) {
                        log.error("Error getting recommendation {}: {}", id, e.getMessage());
                        return ResponseEntity.status(500)
                                        .body(CustomApiResponse.error(500, "Lỗi khi lấy chi tiết gợi ý"));
                }
        }

        @PostMapping("/{id}/accept")
        @Operation(summary = "Accept recommendation")
        public ResponseEntity<CustomApiResponse<Void>> acceptRecommendation(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                try {
                        trackingService.markAsAccepted(id, currentUser.getId());
                        return ResponseEntity.ok(CustomApiResponse.success(null,
                                        "Đã ghi nhận bạn chấp nhận gợi ý này"));

                } catch (IllegalArgumentException e) {
                        return ResponseEntity.status(404)
                                        .body(CustomApiResponse.error(404, "Không tìm thấy gợi ý này"));

                } catch (SecurityException e) {
                        return ResponseEntity.status(403)
                                        .body(CustomApiResponse.error(403, "Không có quyền truy cập"));

                } catch (Exception e) {
                        log.error("Error accepting recommendation {}: {}", id, e.getMessage());
                        return ResponseEntity.status(500)
                                        .body(CustomApiResponse.error(500, "Lỗi khi ghi nhận gợi ý"));
                }
        }

        @PostMapping("/{id}/complete")
        @Operation(summary = "Mark recommendation as completed")
        public ResponseEntity<CustomApiResponse<Void>> completeRecommendation(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                try {
                        trackingService.markAsCompleted(id, currentUser.getId());
                        return ResponseEntity.ok(CustomApiResponse.success(null,
                                        "Tuyệt vời! Bạn đã hoàn thành gợi ý này 🎉"));

                } catch (Exception e) {
                        log.error("Error completing recommendation {}: {}", id, e.getMessage());
                        return ResponseEntity.status(500)
                                        .body(CustomApiResponse.error(500, "Lỗi khi ghi nhận hoàn thành"));
                }
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Dismiss recommendation")
        public ResponseEntity<CustomApiResponse<Void>> dismissRecommendation(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                try {
                        trackingService.dismissRecommendation(id, currentUser.getId());
                        return ResponseEntity.ok(CustomApiResponse.success(null, "Đã ẩn gợi ý này"));

                } catch (Exception e) {
                        log.error("Error dismissing recommendation {}: {}", id, e.getMessage());
                        return ResponseEntity.status(500)
                                        .body(CustomApiResponse.error(500, "Lỗi khi ẩn gợi ý"));
                }
        }

        @PostMapping("/refresh")
        @Operation(summary = "Force refresh recommendations")
        public ResponseEntity<CustomApiResponse<List<AIRecommendationDto>>> refreshRecommendations(
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                try {
                        log.info("User {} requested recommendation refresh", currentUser.getId());

                        // ✅ Step 1: Invalidate old recommendations (separate transaction)
                        recommendationService.invalidateRecommendations(currentUser.getId());

                        // ✅ Step 2: Generate new recommendations (separate transaction via
                        // @Transactional)
                        List<AIRecommendation> recommendations = recommendationService.generateNewRecommendations(currentUser.getId());

                        // ✅ Step 3: Map to DTOs
                        List<AIRecommendationDto> enrichedDtos = recommendations.stream()
                                        .map(recommendationMapper::toDto)
                                        .collect(Collectors.toList());

                        String message = enrichedDtos.isEmpty()
                                        ? "Chưa có gợi ý mới lúc này"
                                        : "Đã tạo lại " + enrichedDtos.size() + " gợi ý mới!";

                        return ResponseEntity.ok(CustomApiResponse.success(enrichedDtos, message));

                } catch (Exception e) {
                        log.error("Error refreshing recommendations: {}", e.getMessage(), e);
                        return ResponseEntity.status(500)
                                        .body(CustomApiResponse.error(500,
                                                        "Lỗi khi làm mới gợi ý. Vui lòng thử lại sau."));
                }
        }

        @GetMapping("/metrics")
        @Operation(summary = "Get recommendation metrics")
        public ResponseEntity<CustomApiResponse<RecommendationTrackingService.RecommendationMetrics>> getMetrics(
                        @AuthenticationPrincipal UserPrincipal currentUser) {
                try {
                        RecommendationTrackingService.RecommendationMetrics metrics = trackingService
                                        .getMetrics(currentUser.getId());

                        return ResponseEntity.ok(CustomApiResponse.success(metrics,
                                        "Thống kê gợi ý của bạn"));

                } catch (Exception e) {
                        log.error("Error getting metrics: {}", e.getMessage());
                        return ResponseEntity.status(500)
                                        .body(CustomApiResponse.error(500, "Lỗi khi lấy thống kê"));
                }
        }
}