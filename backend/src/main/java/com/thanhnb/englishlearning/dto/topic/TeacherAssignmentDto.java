package com.thanhnb.englishlearning.dto.topic;

import com.thanhnb.englishlearning.enums.ModuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for TeacherTopicAssignment entity
 * Used in API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Teacher assignment information")
public class TeacherAssignmentDto {

    @Schema(description = "Assignment ID", example = "1")
    private Long id;

    @Schema(description = "Teacher ID", example = "5")
    private Long teacherId;

    @Schema(description = "Teacher username", example = "john_teacher")
    private String teacherUsername;

    @Schema(description = "Teacher full name", example = "John Doe")
    private String teacherFullName;

    @Schema(description = "Topic ID", example = "3")
    private Long topicId;

    @Schema(description = "Topic name", example = "A1 Grammar")
    private String topicName;

    @Schema(description = "Module type", example = "GRAMMAR")
    private ModuleType moduleType;

    @Schema(description = "Assignment active status", example = "true")
    private Boolean isActive;

    @Schema(description = "Assigned at timestamp")
    private LocalDateTime assignedAt;

    @Schema(description = "Assigned by admin ID", example = "1")
    private Long assignedBy;

    @Schema(description = "Assigned by admin username", example = "admin")
    private String assignedByUsername;
}