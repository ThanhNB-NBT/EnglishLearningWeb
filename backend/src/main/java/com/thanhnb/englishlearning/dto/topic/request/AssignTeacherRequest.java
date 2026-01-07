package com.thanhnb.englishlearning.dto.topic.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for assigning a teacher to a topic
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to assign a teacher to a topic")
public class AssignTeacherRequest {

    @NotNull(message = "Teacher ID is required")
    @Schema(description = "Teacher user ID", example = "5")
    private Long teacherId;

    @NotNull(message = "Topic ID is required")
    @Schema(description = "Topic ID to assign", example = "3")
    private Long topicId;
}