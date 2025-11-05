package com.thanhnb.englishlearning.service.ai.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a chunk of content for AI processing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentChunk {
    
    private String title;
    private String content;
    private Integer pageNumber;
    private ChunkType type;
    
    public enum ChunkType {
        THEORY,
        PRACTICE,
        MIXED,
        UNKNOWN
    }
    
    public ContentChunk(String title, String content) {
        this.title = title;
        this.content = content;
        this.type = ChunkType.UNKNOWN;
    }
    
    public ContentChunk(String title, String content, Integer pageNumber) {
        this.title = title;
        this.content = content;
        this.pageNumber = pageNumber;
        this.type = ChunkType.UNKNOWN;
    }
}