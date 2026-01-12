package com.thanhnb.englishlearning.dto.topic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.ModuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Topic entity
 * Used in API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Topic information")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicDto {

    @Schema(description = "Topic ID", example = "1")
    private Long id;

    @Schema(description = "Topic name", example = "A1 Grammar")
    private String name;

    @Schema(description = "Topic description", example = "Basic grammar rules for beginners")
    private String description;

    @Schema(description = "Module type", example = "GRAMMAR")
    private ModuleType moduleType;

    @Schema(description = "Required English level", example = "A1")
    private EnglishLevel levelRequired;

    @Schema(description = "Display order", example = "0")
    private Integer orderIndex;

    @Schema(description = "Is topic active", example = "true")
    private Boolean isActive;

    @Schema(description = "Number of lessons in this topic", example = "15")
    private Integer lessonCount;

    @Schema(description = "Number of teachers managing this topic", example = "2")
    private Integer teacherCount;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Updated timestamp")
    private LocalDateTime updatedAt;
    
}