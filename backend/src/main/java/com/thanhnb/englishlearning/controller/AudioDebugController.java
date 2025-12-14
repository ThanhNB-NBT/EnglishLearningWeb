package com.thanhnb.englishlearning.controller;

import com.thanhnb.englishlearning.config.AudioStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/debug/audio")
@RequiredArgsConstructor
public class AudioDebugController {

    private final AudioStorageProperties audioProperties;

    @GetMapping("/path-check")
    public Map<String, Object> checkPaths(@RequestParam(required = false) String audioUrl) {
        Map<String, Object> result = new HashMap<>();
        
        // Test URL to file path conversion
        if (audioUrl == null) {
            audioUrl = "/media/listening/lesson_1/audio.mp3";
        }
        
        result.put("inputUrl", audioUrl);
        
        // Remove /media/ prefix
        String relativePath = audioUrl.replaceFirst("^/media/", "");
        result.put("relativePath", relativePath);
        
        // Get upload dir
        String uploadDir = audioProperties.getUploadDir();
        result.put("uploadDir", uploadDir);
        
        // Get parent (media root)
        Path mediaRoot = Paths.get(uploadDir).getParent();
        result.put("mediaRoot", mediaRoot != null ? mediaRoot.toString() : "null");
        
        // Build final file path
        Path filePath = mediaRoot != null ? mediaRoot.resolve(relativePath) : null;
        result.put("finalFilePath", filePath != null ? filePath.toString() : "null");
        
        // Check if exists
        if (filePath != null) {
            File file = filePath.toFile();
            result.put("fileExists", file.exists());
            result.put("fileIsFile", file.isFile());
            result.put("fileCanRead", file.canRead());
            if (file.exists()) {
                result.put("fileSize", file.length());
            }
        }
        
        return result;
    }
}
