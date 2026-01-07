package com.thanhnb.englishlearning.dto.topic.request;

import com.thanhnb.englishlearning.enums.EnglishLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new topic
 * ModuleType is passed via @PathVariable in Controller, not in request body
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new topic")
public class CreateTopicRequest {

    @NotBlank(message = "Topic name is required")
    @Size(min = 3, max = 200, message = "Topic name must be between 3 and 200 characters")
    @Schema(description = "Topic name", example = "Present Simple Tense")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Topic description", example = "Learn basic present simple grammar rules")
    private String description;

    @NotNull(message = "English level is required")
    @Schema(
        description = "Required English level",
        example = "A1",
        allowableValues = {"A1", "A2", "B1", "B2", "C1", "C2"}
    )
    private EnglishLevel levelRequired;

    @Min(value = 1, message = "Order index must be >= 1")
    @Schema(
        description = "Display order (auto-generated if not provided)",
        example = "1"
    )
    private Integer orderIndex;

    // isActive mặc định = true nếu không truyền
    @Schema(description = "Is topic active (default: true)", example = "true", defaultValue = "true")
    private Boolean isActive;
}