package com.thanhnb.englishlearning.service.listening;

import com.thanhnb.englishlearning.config.AudioStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AudioStorageService {

    private final AudioStorageProperties audioProperties;

    // ✅ ADD: Inject server base URL from application.yml
    @Value("${app.server.base-url:http://localhost:8980}")
    private String serverBaseUrl;

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/mp4", "audio/ogg");

    /**
     * Upload audio file
     * 
     * ✅ FIX: Return FULL URL instead of relative path
     */
    public String uploadAudio(MultipartFile file, Long lessonId) throws IOException {
        validateAudioFile(file);

        // 1. Lấy thư mục gốc từ cấu hình
        String uploadDir = audioProperties.getUploadDir();

        // 2. Tạo thư mục riêng cho lesson: .../lesson_{id}
        Path lessonDir = Paths.get(uploadDir, "lesson_" + lessonId);
        if (!Files.exists(lessonDir)) {
            Files.createDirectories(lessonDir);
        }

        // 3. Tạo tên file an toàn (UUID + Extension gốc)
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String safeFilename = UUID.randomUUID().toString() + "." + extension;

        Path targetPath = lessonDir.resolve(safeFilename);

        // 4. Lưu file (Ghi đè nếu có)
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Audio uploaded: {} (Size: {} bytes)", targetPath, file.getSize());

        // ✅ FIX 5: Trả về FULL URL
        // OLD: return "/media/listening/lesson_" + lessonId + "/" + safeFilename;
        // NEW: return "http://localhost:8080/media/listening/lesson_1/abc.mp3"
        String relativePath = "/media/listening/lesson_" + lessonId + "/" + safeFilename;
        String fullUrl = serverBaseUrl + relativePath;

        log.info("Generated audio URL: {}", fullUrl);
        return fullUrl;
    }

    /**
     * Delete audio file
     */
    public void deleteAudio(String audioUrl) {
        if (!StringUtils.hasText(audioUrl))
            return;

        try {
            // ✅ FIX: Handle both full URL and relative path
            String relativePath;
            if (audioUrl.startsWith("http")) {
                // Extract path from full URL: http://localhost:8080/media/listening/... →
                // /media/listening/...
                relativePath = audioUrl.substring(audioUrl.indexOf("/media/listening/"));
            } else {
                relativePath = audioUrl;
            }

            // Remove prefix "/media/listening/"
            relativePath = relativePath.replace("/media/listening/", "");

            Path filePath = Paths.get(audioProperties.getUploadDir(), relativePath);

            if (Files.deleteIfExists(filePath)) {
                log.info("Deleted file: {}", filePath);

                // Xóa thư mục cha (lesson_x) nếu rỗng
                Path parentDir = filePath.getParent();
                if (isDirectoryEmpty(parentDir)) {
                    Files.deleteIfExists(parentDir);
                    log.info("Deleted empty directory: {}", parentDir);
                }
            }
        } catch (IOException e) {
            log.error("Failed to delete audio: {}", audioUrl, e);
        }
    }

    public String updateAudio(MultipartFile file, Long lessonId, String oldAudioUrl) throws IOException {
        // Xóa file cũ nếu có
        deleteAudio(oldAudioUrl);
        // Upload file mới
        return uploadAudio(file, lessonId);
    }

    // --- Private Helpers (unchanged) ---

    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File audio không được để trống");
        }
        if (file.getSize() > audioProperties.getMaxFileSize()) {
            throw new IllegalArgumentException(
                    "File quá lớn (Max " + (audioProperties.getMaxFileSize() / 1024 / 1024) + "MB)");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())
                && !contentType.equals("application/octet-stream")) {
            log.warn("Warning: Uploading content-type {}", contentType);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1)
            return "mp3";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private boolean isDirectoryEmpty(Path directory) throws IOException {
        if (!Files.isDirectory(directory))
            return false;
        try (var entries = Files.list(directory)) {
            return entries.findAny().isEmpty();
        }
    }
}