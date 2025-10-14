package com.thanhnb.englishlearning.dto.grammar;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Tiến độ học ngữ pháp của user")
public class UserGrammarProgressDTO {

    @Schema(description = "ID tiến trình", example = "1")
    private Long id;

    @Schema(description = "ID user", example = "1")
    private Long userId;

    @Schema(description = "ID bài học", example = "1")
    private Long lessonId;

    @Schema(description = "Tiêu đề bài học")
    private String lessonTitle;

    @Schema(description = "Tên chủ đề")
    private String topicName;

    @Schema(description = "Đã hoàn thành chưa", example = "true")
    private Boolean isCompleted;

    @Schema(description = "Điểm phần trăm", example = "85")
    private Integer scorePercentage;

    @Schema(description = "Số lần thử", example = "3")
    private Integer attempts;

    @Schema(description = "Thời điểm hoàn thành")
    private LocalDateTime completedAt;

    @Schema(description = "Thời điểm tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Thời điểm cập nhật")
    private LocalDateTime updatedAt;
}