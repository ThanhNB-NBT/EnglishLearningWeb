package com.thanhnb.englishlearning.controller.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.dto.CustomApiResponse;
import com.thanhnb.englishlearning.dto.grammar.GrammarLessonDTO;
import com.thanhnb.englishlearning.dto.listening.ListeningLessonDTO;
import com.thanhnb.englishlearning.dto.reading.ReadingLessonDTO;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.service.ai.UnifiedImportService;
import com.thanhnb.englishlearning.service.grammar.GrammarLessonService;
import com.thanhnb.englishlearning.service.listening.ListeningLessonService;
import com.thanhnb.englishlearning.service.reading.ReadingLessonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * ✅ FINAL: Universal Import Controller
 * 
 * Endpoints:
 * - Parse from file (PDF/DOCX)
 * - Generate from instruction (no file needed)
 */
@RestController
@RequestMapping("/api/admin/ai-import")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin AI Import", description = "AI-powered lesson creation from files or instructions")
public class UniversalImportController {

    private final UnifiedImportService<GrammarLessonDTO> grammarImportService;
    private final UnifiedImportService<ReadingLessonDTO> readingImportService;
    private final UnifiedImportService<ListeningLessonDTO> listeningImportService;

    private final GrammarLessonService grammarLessonService;
    private final ReadingLessonService readingLessonService;
    private final ListeningLessonService listeningLessonService;

    // =========================================================================
    // ✅ SINGLE PARSE ENDPOINT
    // =========================================================================

    @Operation(summary = "Parse/Generate lesson from file or instruction")
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomApiResponse<?>> parseLesson(
            @RequestParam("moduleType") ModuleType moduleType,
            @RequestParam("topicId") Long topicId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "instruction", required = false) String instruction) {

        try {
            // Validate input
            if (file == null && (instruction == null || instruction.isBlank())) {
                return ResponseEntity.badRequest()
                        .body(CustomApiResponse.error(400, "Phải cung cấp file HOẶC instruction"));
            }

            Object result = switch (moduleType) {
                case GRAMMAR -> {
                    GrammarLessonDTO dto = file != null
                            ? grammarImportService.parseLessonFile(file, instruction, ModuleType.GRAMMAR,
                                    GrammarLessonDTO.class)
                            : grammarImportService.generateFromInstruction(instruction, ModuleType.GRAMMAR,
                                    GrammarLessonDTO.class);
                    dto.setTopicId(topicId);
                    yield dto;
                }
                case READING -> {
                    ReadingLessonDTO dto = file != null
                            ? readingImportService.parseLessonFile(file, instruction, ModuleType.READING,
                                    ReadingLessonDTO.class)
                            : readingImportService.generateFromInstruction(instruction, ModuleType.READING,
                                    ReadingLessonDTO.class);
                    dto.setTopicId(topicId);
                    yield dto;
                }
                case LISTENING -> {
                    ListeningLessonDTO dto = file != null
                            ? listeningImportService.parseLessonFile(file, instruction, ModuleType.LISTENING,
                                    ListeningLessonDTO.class)
                            : listeningImportService.generateFromInstruction(instruction, ModuleType.LISTENING,
                                    ListeningLessonDTO.class);
                    dto.setTopicId(topicId);
                    yield dto;
                }
                default -> throw new IllegalArgumentException("Module không hỗ trợ: " + moduleType);
            };

            return ResponseEntity.ok(CustomApiResponse.success(result, "Parse thành công"));

        } catch (Exception e) {
            log.error("Parse failed for module: {}", moduleType, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.error(400, "Lỗi parse: " + e.getMessage()));
        }
    }

    // =========================================================================
    // ✅ SINGLE SAVE ENDPOINT
    // =========================================================================

    @Operation(summary = "Save lesson after review")
    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomApiResponse<?>> saveLesson(
            @RequestParam("moduleType") ModuleType moduleType,
            @RequestPart("lesson") String lessonJson, // JSON string
            @RequestPart(value = "audioFile", required = false) MultipartFile audioFile) {

        try {
            Object result = switch (moduleType) {
                case GRAMMAR -> {
                    GrammarLessonDTO dto = new ObjectMapper().readValue(lessonJson, GrammarLessonDTO.class);
                    yield grammarLessonService.createLessonWithQuestionsAndTasks(dto);
                }
                case READING -> {
                    ReadingLessonDTO dto = new ObjectMapper().readValue(lessonJson, ReadingLessonDTO.class);
                    yield readingLessonService.createLessonWithQuestionsAndTasks(dto);
                }
                case LISTENING -> {
                    ListeningLessonDTO dto = new ObjectMapper().readValue(lessonJson, ListeningLessonDTO.class);
                    yield listeningLessonService.createLessonWithQuestionsAndTasks(dto, audioFile);
                }
                default -> throw new IllegalArgumentException("Module không hỗ trợ: " + moduleType);
            };

            return ResponseEntity.ok(CustomApiResponse.success(result, "Lưu thành công"));

        } catch (Exception e) {
            log.error("Save failed for module: {}", moduleType, e);
            return ResponseEntity.badRequest()
                    .body(CustomApiResponse.error(400, "Lỗi lưu: " + e.getMessage()));
        }
    }
}