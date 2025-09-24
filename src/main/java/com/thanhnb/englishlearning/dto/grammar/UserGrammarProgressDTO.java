package com.thanhnb.englishlearning.dto.grammar;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User's progress on a grammar lesson")
public class UserGrammarProgressDTO {

    @Schema(description = "ID của tiến trình", example = "1")
    private Long id;

    @Schema(description = "ID của người dùng", example = "1")
    private Long userId;

    @Schema(description = "ID của bài học", example = "1")
    private Long lessonId;

    @Schema(description = "Tiêu đề của bài học", example = "Thì hiện tại đơn")
    private String lessonTitle;

    @Schema(description = "Tên chủ đề của bài học", example = "Thì hiện tại")
    private String topicName;

    @Schema(description = "Trạng thái hoàn thành bài học", example = "true")
    private Boolean isCompleted;

    @Schema(description = "Điểm số của người dùng", example = "85")
    private Integer score;

    @Schema(description = "Số lần thử", example = "3")
    private Integer attempts;

    @Schema(description = "Thời điểm hoàn thành bài học")
    private LocalDateTime completedAt;

    @Schema(description = "Thời điểm tạo tiến trình")
    private LocalDateTime createdAt;

    @Schema(description = "Thời điểm cập nhật tiến trình")
    private LocalDateTime updatedAt;
}
