package com.thanhnb.englishlearning.controller.question;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.service.grammar.GrammarQuestionService;
import com.thanhnb.englishlearning.service.listening.ListeningQuestionService;
import com.thanhnb.englishlearning.service.question.ExcelExportService;
import com.thanhnb.englishlearning.service.question.ExcelTemplateService;
import com.thanhnb.englishlearning.service.question.SmartExcelImportService;
import com.thanhnb.englishlearning.service.reading.ReadingQuestionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/admin/questions/import")
@RequiredArgsConstructor
@Slf4j
public class QuestionImportController {

    private final ExcelTemplateService excelTemplateService;
    private final ExcelExportService excelExportService;
    private final SmartExcelImportService smartExcelImportService;

    // ✅ Inject Question Services by module
    private final GrammarQuestionService grammarQuestionService;
    private final ReadingQuestionService readingQuestionService;
    private final ListeningQuestionService listeningQuestionService;

    @GetMapping("/template")
    public ResponseEntity<InputStreamResource> downloadTemplate() throws Exception {
        ByteArrayInputStream in = excelTemplateService.generateQuestionTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=questions_import_template.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    /**
     * ✅ FIXED: Parse Excel with parentType and lessonId
     */
    @PostMapping("/parse-excel")
    public ResponseEntity<CustomApiResponse<List<CreateQuestionDTO>>> parseExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("parentType") ParentType parentType,
            @RequestParam("lessonId") Long lessonId) {
        try {
            // ✅ Pass parentType and lessonId to service
            List<CreateQuestionDTO> result = smartExcelImportService.parseExcel(
                    file, parentType, lessonId);

            return ResponseEntity.ok(CustomApiResponse.success(
                    result,
                    "Phân tích " + result.size() + " câu hỏi thành công"));
        } catch (Exception e) {
            log.error("Parse Excel failed", e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.error(400, "Lỗi đọc file: " + e.getMessage()));
        }
    }

    /**
     * Save batch through service (with validation & permission)
     */
    @PostMapping("/save-batch")
    public ResponseEntity<CustomApiResponse<List<QuestionResponseDTO>>> saveBatch(
            @RequestBody List<CreateQuestionDTO> questions) {
        try {
            if (questions == null || questions.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(CustomApiResponse.error(400, "Danh sách câu hỏi rỗng"));
            }

            // Validate all questions have same parentType and lessonId
            CreateQuestionDTO first = questions.get(0);
            ParentType parentType = first.getParentType();
            Long lessonId = first.getParentId();

            if (parentType == null || lessonId == null) {
                return ResponseEntity.badRequest()
                        .body(CustomApiResponse.error(400, "ParentType và LessonId không được null"));
            }

            boolean inconsistent = questions.stream()
                    .anyMatch(q -> !parentType.equals(q.getParentType())
                            || !lessonId.equals(q.getParentId()));

            if (inconsistent) {
                return ResponseEntity.badRequest()
                        .body(CustomApiResponse.error(400, "Tất cả câu hỏi phải thuộc cùng một bài học"));
            }

            // ✅ Route to correct service based on parentType
            List<QuestionResponseDTO> saved = switch (parentType) {
                case GRAMMAR -> grammarQuestionService.createQuestionsInBulk(lessonId, questions);
                case READING -> readingQuestionService.createQuestionsInBulk(lessonId, questions);
                case LISTENING -> listeningQuestionService.createQuestionsInBulk(lessonId, questions);
                default -> throw new IllegalArgumentException("ParentType không hợp lệ: " + parentType);
            };

            return ResponseEntity.ok(CustomApiResponse.success(
                    saved,
                    "Đã lưu " + saved.size() + " câu hỏi thành công"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("Save batch failed", e);
            return ResponseEntity.status(500)
                    .body(CustomApiResponse.error(500, "Lỗi lưu DB: " + e.getMessage()));
        }
    }

    /**
     * Export questions to Excel
     * GET /api/admin/questions/import/export
     */
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportQuestions(
            @RequestParam("parentType") ParentType parentType,
            @RequestParam("lessonId") Long lessonId,
            @RequestParam(value = "lessonTitle", required = false) String lessonTitle) {

        try {
            // Get questions based on module
            List<QuestionResponseDTO> questions;

            switch (parentType) {
                case GRAMMAR:
                    questions = grammarQuestionService.getQuestionsByLessonId(lessonId);
                    break;
                case READING:
                    questions = readingQuestionService.getQuestionsByLessonId(lessonId);
                    break;
                case LISTENING:
                    questions = listeningQuestionService.getQuestionsByLessonId(lessonId);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid parentType: " + parentType);
            }

            if (questions.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(null);
            }

            questions.forEach(q -> {
                log.info("Question ID={}, Type={}, TaskGroupName={}",
                        q.getId(), q.getQuestionType(), q.getTaskGroupName());
            });

            // Export to Excel
            String title = lessonTitle != null ? lessonTitle : "Lesson " + lessonId;
            ByteArrayInputStream in = excelExportService.exportQuestionsToExcel(questions, title);

            // Prepare response
            HttpHeaders headers = new HttpHeaders();
            String filename = "questions_export_" + lessonId + "_" + System.currentTimeMillis() + ".xlsx";
            headers.add("Content-Disposition", "attachment; filename=" + filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(in));

        } catch (Exception e) {
            log.error("Export failed", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}