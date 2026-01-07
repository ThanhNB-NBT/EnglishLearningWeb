package com.thanhnb.englishlearning.dto.question.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi ghép đôi (MATCHING)")
public class CreateMatchingDTO extends QuestionData {

    @NotNull
    @Size(min = 2)
    @JsonView(Views.Admin.class)
    private List<PairDTO> pairs;

    @JsonView(Views.Public.class)
    private List<String> leftItems;
    
    @JsonView(Views.Public.class)
    private List<String> rightItems;  // Will be shuffled
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PairDTO {
        @NotBlank
        @JsonView(Views.Public.class)
        private String left;

        @NotBlank
        @JsonView(Views.Admin.class)
        private String right;
        
        @NotNull
        @Min(1)
        @JsonView(Views.Public.class)
        private Integer order;
    }
}
