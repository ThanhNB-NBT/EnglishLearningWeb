package com.thanhnb.englishlearning.dto.topic.request;

import com.thanhnb.englishlearning.enums.EnglishLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update a topic")
public class UpdateTopicRequest {

    @Size(min = 3, max = 200, message = "Topic name must be between 3 and 200 characters")
    @Schema(description = "Topic name", example = "A1 Grammar - Updated")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Schema(description = "Topic description", example = "Updated description")
    private String description;

    @Schema(description = "Required English level", example = "INTERMEDIATE")
    private EnglishLevel levelRequired;

    @Min(value = 0, message = "Order index must be >= 0")
    @Schema(description = "Display order", example = "2")
    private Integer orderIndex;

    @Schema(description = "Is topic active", example = "false")
    private Boolean isActive;
}