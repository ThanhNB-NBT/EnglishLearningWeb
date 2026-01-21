package com.thanhnb.englishlearning.service.ai.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.service.ai.provider.AIServiceRouter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Base service xử lý AI Import
 * Đã tích hợp sẵn: Đọc PDF, Word, Chunking, Gọi AI Router
 */
@Slf4j
public abstract class AIParsingService<T> {

    protected final AIServiceRouter aiServiceRouter;
    protected final ObjectMapper objectMapper;

    protected AIParsingService(AIServiceRouter aiServiceRouter, ObjectMapper objectMapper) {
        this.aiServiceRouter = aiServiceRouter;
        this.objectMapper = objectMapper;
    }

    // --- CÁC HÀM CONCRETE CLASS PHẢI IMPLEMENT ---
    protected abstract String getModuleName();
    protected abstract String buildPrompt();
    protected abstract T parseResponse(String jsonResponse) throws Exception;
    protected abstract double getTemperature(); // AI Creativity
    protected abstract long getMaxFileSize();   // File size limit

    // --- LOGIC CHÍNH: PARSE FILE ---
    public T parseFile(MultipartFile file, List<Integer> pages) throws Exception {
        validateFile(file);
        
        // 1. Extract text (Dùng các hàm helper bên dưới)
        String fullText = extractTextFromFile(file, pages);
        if (fullText == null || fullText.isBlank()) {
            throw new Exception("Không trích xuất được nội dung từ file (Text rỗng)");
        }

        // 2. Chia nhỏ văn bản (Chunking) để tránh lỗi quá token
        List<ContentChunk> chunks = splitIntoLogicalChunks(fullText);
        List<T> results = new ArrayList<>();

        // 3. Gửi từng phần cho AI xử lý
        for (ContentChunk chunk : chunks) {
            String fullPrompt = buildPrompt() + "\n\n--- INPUT TEXT BEGIN ---\n" + chunk.getContent() + "\n--- INPUT TEXT END ---";
            
            try {
                // Gọi AI qua Router (Tự động fallback Groq <-> Gemini)
                String jsonResponse = aiServiceRouter.generateForAnalysis(fullPrompt);
                
                // Parse kết quả JSON về Object T
                T result = parseResponse(jsonResponse);
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                log.error("[{}] Lỗi xử lý chunk {}: {}", getModuleName(), chunk.getTitle(), e.getMessage());
            }
        }

        // 4. Gộp kết quả
        return mergeResults(results);
    }

    // --- CÁC HÀM XỬ LÝ FILE (HELPER) ---

    protected String extractTextFromFile(MultipartFile file, List<Integer> pages) throws Exception {
        String mime = file.getContentType();
        if (isPDF(mime)) return extractTextFromPDF(file, pages);
        if (isDOCX(mime)) return extractTextFromDOCX(file);
        return new String(file.getBytes()); // Fallback text file
    }

    // Đọc PDF bằng PDFBox
    protected String extractTextFromPDF(MultipartFile file, List<Integer> pages) throws Exception {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            // Nếu chỉ định trang cụ thể (Ví dụ: user muốn trang 5-10)
            if (pages != null && !pages.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Integer page : pages) {
                    // PDFBox page index bắt đầu từ 1
                    if (page > 0 && page <= document.getNumberOfPages()) {
                        stripper.setStartPage(page);
                        stripper.setEndPage(page);
                        sb.append(stripper.getText(document));
                    }
                }
                return sb.toString();
            }
            // Mặc định lấy hết
            return stripper.getText(document);
        }
    }

    // Đọc Word bằng Apache POI
    protected String extractTextFromDOCX(MultipartFile file) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText()).append("\n");
            }
            return sb.toString();
        }
    }

    protected void validateFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) throw new Exception("File trống");
        if (file.getSize() > getMaxFileSize()) throw new Exception("File quá lớn. Max: " + getMaxFileSize() + " bytes");
    }

    protected boolean isPDF(String mimeType) {
        return mimeType != null && mimeType.contains("pdf");
    }

    protected boolean isDOCX(String mimeType) {
        return mimeType != null && (mimeType.contains("word") || mimeType.contains("officedocument"));
    }

    // Chia văn bản thành các đoạn nhỏ (khoảng 3000 ký tự)
    protected List<ContentChunk> splitIntoLogicalChunks(String text) {
        List<ContentChunk> chunks = new ArrayList<>();
        int maxChars = 3000;
        int start = 0;
        int part = 1;

        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());
            
            // Cố gắng cắt ở dấu xuống dòng gần nhất để không làm gãy câu
            if (end < text.length()) {
                int lastNewLine = text.lastIndexOf("\n", end);
                if (lastNewLine > start + (maxChars / 2)) {
                    end = lastNewLine;
                }
            }
            
            String content = text.substring(start, end).trim();
            if (!content.isEmpty()) {
                chunks.add(new ContentChunk("Part " + part++, content));
            }
            start = end;
        }
        return chunks;
    }

    // Hàm merge mặc định (lấy cái đầu tiên), override ở con nếu cần gộp List
    protected T mergeResults(List<T> results) {
        if (results != null && !results.isEmpty()) {
            return results.get(0);
        }
        return null;
    }
}