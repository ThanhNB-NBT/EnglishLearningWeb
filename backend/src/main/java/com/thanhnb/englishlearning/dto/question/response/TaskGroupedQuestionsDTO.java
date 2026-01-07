package com.thanhnb.englishlearning.dto.question.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;

import java.util.List;

/**
 * ✅ UPDATED: Support both Admin and User views
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Questions được nhóm theo Task (nếu có)")
public class TaskGroupedQuestionsDTO {
    
    @Schema(description = "Có cấu trúc task hay không", example = "true")
    @JsonView(Views.Public.class)
    private Boolean hasTaskStructure;
    
    @Schema(description = "Danh sách các task groups")
    @JsonView(Views.Public.class)
    private List<TaskGroup> tasks;
    
    @Schema(description = "Câu hỏi standalone (không thuộc task nào)")
    @JsonView(Views.Public.class)
    private List<QuestionResponseDTO> standaloneQuestions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Một nhóm task")
    public static class TaskGroup {
        
        @Schema(description = "ID của TaskGroup", example = "1")
        @JsonView(Views.Public.class)
        private Long taskGroupId;
        
        @Schema(description = "Tên task", example = "Task 1: Multiple Choice")
        @JsonView(Views.Public.class)
        private String taskName;
        
        @Schema(description = "Hướng dẫn task", example = "Choose the correct answer (A, B, C or D)")
        @JsonView(Views.Public.class)
        private String taskInstruction;
        
        @Schema(description = "Thứ tự task", example = "1")
        @JsonView(Views.Public.class)
        private Integer taskOrder;
        
        @Schema(description = "Danh sách câu hỏi trong task")
        @JsonView(Views.Public.class)
        private List<QuestionResponseDTO> questions;
        
        // Helper methods
        @JsonView(Views.Admin.class)
        public int getQuestionCount() {
            return questions != null ? questions.size() : 0;
        }
        
        @JsonView(Views.Admin.class)
        public int getTotalPoints() {
            return questions != null 
                ? questions.stream().mapToInt(QuestionResponseDTO::getPoints).sum() 
                : 0;
        }
    }
}