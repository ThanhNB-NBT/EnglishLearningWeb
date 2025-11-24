package com.thanhnb.englishlearning.dto.reading;

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
@Schema(description = "User reading progress")
public class UserReadingProgressDTO {

    @Schema(description = "Progress ID", example = "1")
    private Long id;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "Lesson ID", example = "1")
    private Long lessonId;

    @Schema(description = "Lesson title")
    private String lessonTitle;

    @Schema(description = "Is completed", example = "true")
    private Boolean isCompleted;

    @Schema(description = "Score percentage", example = "85.5")
    private Double scorePercentage;

    @Schema(description = "Number of attempts", example = "3")
    private Integer attempts;

    @Schema(description = "Completion timestamp")
    private LocalDateTime completedAt;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}