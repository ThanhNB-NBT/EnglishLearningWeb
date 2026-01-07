package com.thanhnb.englishlearning.dto.grammar;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ✅ User Grammar Progress DTO - Tiến độ học ngữ pháp
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User Grammar Progress - Tiến độ học ngữ pháp của user")
public class UserGrammarProgressDTO {

    @Schema(description = "ID tiến trình", example = "1")
    private Long id;

    @Schema(description = "ID user", example = "1")
    private Long userId;

    @Schema(description = "ID bài học", example = "1")
    private Long lessonId;

    @Schema(description = "Tiêu đề bài học", example = "Present Simple Tense")
    private String lessonTitle;

    @Schema(description = "Tên chủ đề", example = "Basic Tenses")
    private String topicName;

    @Schema(description = "Đã hoàn thành chưa", example = "true")
    private Boolean isCompleted;

    @Schema(description = "Điểm phần trăm", example = "85.5")
    private Double scorePercentage;

    @Schema(description = "Số lần thử", example = "3")
    private Integer attempts;

    @Schema(description = "Thời gian đọc (giây)", example = "240")
    private Integer readingTime;

    @Schema(description = "Thời điểm hoàn thành", example = "2024-01-20T15:30:00")
    private LocalDateTime completedAt;

    @Schema(description = "Thời điểm tạo", example = "2024-01-15T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Thời điểm cập nhật", example = "2024-01-20T15:30:00")
    private LocalDateTime updatedAt;
}