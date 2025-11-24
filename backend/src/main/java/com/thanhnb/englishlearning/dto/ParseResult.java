package com.thanhnb.englishlearning.dto;

import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of parsing grammar lessons from file
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParseResult {
    
    /**
     * List of parsed grammar lessons
     */
    public List<GrammarLessonDTO> lessons;
    
    /**
     * Error message if parsing failed
     */
    private String error;
    
    /**
     * Additional metadata
     */
    private ParseMetadata metadata;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParseMetadata {
        private String fileName;
        private String fileType;
        private Integer pagesProcessed;
        private Integer totalTheoryLessons;
        private Integer totalPracticeLessons;
        private Integer totalQuestions;
    }
    
    /**
     * Check if parsing was successful
     */
    public boolean isSuccess() {
        return error == null && lessons != null && !lessons.isEmpty();
    }
}