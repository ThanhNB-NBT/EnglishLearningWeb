package com.thanhnb.englishlearning.service.ai.base;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Interface for parsing different file types
 */
public interface FileParser {
    
    /**
     * Extract text from file
     */
    String extractText(MultipartFile file) throws Exception;
    
    /**
     * Extract text from selected pages (for PDF)
     */
    String extractTextFromPages(MultipartFile file, List<Integer> pages) throws Exception;
    
    /**
     * Check if file type is supported
     */
    boolean supports(String mimeType);
    
    /**
     * Get supported file extensions
     */
    List<String> getSupportedExtensions();
}