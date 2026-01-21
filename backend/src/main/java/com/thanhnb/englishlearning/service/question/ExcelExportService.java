package com.thanhnb.englishlearning.service.question;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportService {

    private final ObjectMapper objectMapper;

    /**
     * ✅ Export questions to Excel file (WITH 3 SHEETS: Questions + Guide + Examples)
     */
    public ByteArrayInputStream exportQuestionsToExcel(List<QuestionResponseDTO> questions, String lessonTitle)
            throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ✅ Sheet 1: Exported Questions
            Sheet questionsSheet = workbook.createSheet("📋 EXPORTED QUESTIONS");
            createQuestionsSheet(workbook, questionsSheet, questions, lessonTitle);

            // ✅ Sheet 2: Guide
            Sheet guideSheet = workbook.createSheet("📖 HƯỚNG DẪN");
            createGuideSheet(workbook, guideSheet);

            // ✅ Sheet 3: Examples
            Sheet exampleSheet = workbook.createSheet("💡 VÍ DỤ MẪU");
            createExampleSheet(workbook, exampleSheet);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // ========================================================================
    // SHEET 1: EXPORTED QUESTIONS
    // ========================================================================
    
    private void createQuestionsSheet(Workbook workbook, Sheet sheet, List<QuestionResponseDTO> questions, String lessonTitle) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // Create header
        createHeader(workbook, sheet, headerStyle, lessonTitle);

        // Add question data
        int rowIdx = 5; // Start after header
        for (QuestionResponseDTO question : questions) {
            addQuestionRow(sheet, rowIdx++, question, dataStyle);
        }

        // Auto-size columns (8 columns)
        for (int i = 0; i < 8; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1000, 15000));
        }
    }

    private void createHeader(Workbook workbook, Sheet sheet, CellStyle headerStyle, String lessonTitle) {
        // Title row
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("📋 EXPORTED QUESTIONS - " + lessonTitle);
        titleCell.setCellStyle(createTitleStyle(workbook));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 7));

        // Empty rows
        sheet.createRow(1);
        sheet.createRow(2);

        // Column headers
        Row headerRow = sheet.createRow(3);
        String[] headers = {
                "Question Type",
                "Question Text",
                "Points",
                "Correct Answer / Error",
                "All Options / Correction",
                "Explanation",
                "Task Group",
                "Task Instruction"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Sub-instruction row
        Row subRow = sheet.createRow(4);
        CellStyle subStyle = createSubInstructionStyle(workbook);
        String[] subHeaders = {
                "Loại câu hỏi",
                "Nội dung câu hỏi",
                "Điểm số",
                "Đáp án đúng",
                "Tất cả đáp án",
                "Giải thích",
                "Tên task (nếu có)",
                "Hướng dẫn task"
        };
        for (int i = 0; i < subHeaders.length; i++) {
            Cell cell = subRow.createCell(i);
            cell.setCellValue(subHeaders[i]);
            cell.setCellStyle(subStyle);
        }
    }

    private void addQuestionRow(Sheet sheet, int rowIdx, QuestionResponseDTO question, CellStyle dataStyle) {
        Row row = sheet.createRow(rowIdx);

        // Column A: Question Type
        createCell(row, 0, question.getQuestionType().name(), dataStyle);

        // Column B: Question Text (strip HTML)
        String questionText = stripHtml(question.getQuestionText());
        createCell(row, 1, questionText, dataStyle);

        // Column C: Points
        Cell pointsCell = row.createCell(2);
        pointsCell.setCellValue(question.getPoints());
        pointsCell.setCellStyle(dataStyle);

        // Columns D & E: Data based on question type
        ExportData exportData = extractExportData(question);
        createCell(row, 3, exportData.data1, dataStyle);
        createCell(row, 4, exportData.data2, dataStyle);

        // Column F: Explanation
        String explanation = extractExplanation(question);
        createCell(row, 5, explanation, dataStyle);

        // Column G: Task Group Name
        String taskGroupName = question.getTaskGroupName() != null ? question.getTaskGroupName() : "";
        createCell(row, 6, taskGroupName, dataStyle);
        
        // Column H: Task Instruction
        String taskInstruction = question.getTaskInstruction() != null ? question.getTaskInstruction() : "";
        createCell(row, 7, taskInstruction, dataStyle);
    }

    /**
     * ✅ Extract export data from question
     */
    private ExportData extractExportData(QuestionResponseDTO question) {
        ExportData result = new ExportData();

        try {
            JsonNode data = objectMapper.valueToTree(question.getData());

            if (data == null || data.isNull()) {
                return result;
            }

            switch (question.getQuestionType()) {
                case MULTIPLE_CHOICE:
                case TRUE_FALSE:
                case COMPLETE_CONVERSATION:
                    if (data.has("options")) {
                        JsonNode options = data.get("options");
                        StringBuilder allOptions = new StringBuilder();
                        String correctAnswer = "";

                        if (options.isArray()) {
                            for (JsonNode opt : options) {
                                String text = opt.has("text") ? opt.get("text").asText() : "";
                                boolean isCorrect = opt.has("isCorrect") && opt.get("isCorrect").asBoolean();

                                if (isCorrect) {
                                    correctAnswer = text;
                                }
                                if (allOptions.length() > 0) {
                                    allOptions.append(" | ");
                                }
                                allOptions.append(text);
                            }
                        }

                        result.data1 = correctAnswer;
                        result.data2 = allOptions.toString();
                    }
                    break;

                case FILL_BLANK:
                case VERB_FORM:
                case TEXT_ANSWER:
                    if (data.has("blanks")) {
                        JsonNode blanks = data.get("blanks");
                        StringBuilder correctAnswers = new StringBuilder();
                        StringBuilder allAnswers = new StringBuilder();

                        if (blanks.isArray()) {
                            for (JsonNode blank : blanks) {
                                if (blank.has("correctAnswers")) {
                                    JsonNode answers = blank.get("correctAnswers");
                                    if (answers.isArray() && answers.size() > 0) {
                                        String firstAnswer = answers.get(0).asText();
                                        if (correctAnswers.length() > 0) {
                                            correctAnswers.append(", ");
                                        }
                                        correctAnswers.append(firstAnswer);

                                        for (JsonNode ans : answers) {
                                            if (allAnswers.length() > 0) {
                                                allAnswers.append(" | ");
                                            }
                                            allAnswers.append(ans.asText());
                                        }
                                    }
                                }
                            }
                        }

                        result.data1 = correctAnswers.toString();
                        result.data2 = allAnswers.toString();
                    }

                    if (data.has("wordBank")) {
                        JsonNode wordBank = data.get("wordBank");
                        if (wordBank.isArray() && wordBank.size() > 0) {
                            StringBuilder wb = new StringBuilder();
                            for (JsonNode word : wordBank) {
                                if (wb.length() > 0)
                                    wb.append(" | ");
                                wb.append(word.asText());
                            }
                            if (result.data2.isEmpty()) {
                                result.data2 = wb.toString();
                            }
                        }
                    }
                    break;

                case ERROR_CORRECTION:
                    result.data1 = data.has("errorText") ? data.get("errorText").asText() : "";
                    result.data2 = data.has("correction") ? data.get("correction").asText() : "";
                    break;

                case SENTENCE_TRANSFORMATION:
                    result.data1 = data.has("beginningPhrase") ? data.get("beginningPhrase").asText() : "";
                    if (data.has("correctAnswers")) {
                        JsonNode answers = data.get("correctAnswers");
                        if (answers.isArray()) {
                            StringBuilder sb = new StringBuilder();
                            for (JsonNode ans : answers) {
                                if (sb.length() > 0)
                                    sb.append(" | ");
                                sb.append(ans.asText());
                            }
                            result.data2 = sb.toString();
                        }
                    }
                    break;

                case MATCHING:
                    if (data.has("pairs")) {
                        JsonNode pairs = data.get("pairs");
                        StringBuilder pairStr = new StringBuilder();
                        if (pairs.isArray()) {
                            for (JsonNode pair : pairs) {
                                if (pairStr.length() > 0)
                                    pairStr.append(" | ");
                                String left = pair.has("left") ? pair.get("left").asText() : "";
                                String right = pair.has("right") ? pair.get("right").asText() : "";
                                pairStr.append(left).append("-").append(right);
                            }
                        }
                        result.data1 = pairStr.toString();
                    }
                    break;

                case SENTENCE_BUILDING:
                    if (data.has("words")) {
                        JsonNode words = data.get("words");
                        if (words.isArray()) {
                            StringBuilder sb = new StringBuilder();
                            for (JsonNode word : words) {
                                if (sb.length() > 0)
                                    sb.append(" | ");
                                sb.append(word.asText());
                            }
                            result.data1 = sb.toString();
                        }
                    }
                    result.data2 = data.has("correctSentence") ? data.get("correctSentence").asText() : "";
                    break;

                case PRONUNCIATION:
                    if (data.has("words")) {
                        JsonNode words = data.get("words");
                        if (words.isArray()) {
                            StringBuilder sb = new StringBuilder();
                            for (JsonNode word : words) {
                                if (sb.length() > 0)
                                    sb.append(" | ");
                                sb.append(word.asText());
                            }
                            result.data1 = sb.toString();
                        }
                    }
                    if (data.has("categories")) {
                        JsonNode categories = data.get("categories");
                        if (categories.isArray()) {
                            StringBuilder sb = new StringBuilder();
                            for (JsonNode cat : categories) {
                                if (sb.length() > 0)
                                    sb.append(" | ");
                                sb.append(cat.asText());
                            }
                            result.data2 = sb.toString();
                        }
                    }
                    break;

                case OPEN_ENDED:
                    result.data1 = data.has("suggestedAnswer") ? data.get("suggestedAnswer").asText() : "";
                    if (data.has("minWord") && data.has("maxWord")) {
                        result.data2 = "Min: " + data.get("minWord").asInt() +
                                ", Max: " + data.get("maxWord").asInt();
                    }
                    break;

                default:
                    result.data1 = "N/A";
                    result.data2 = "N/A";
            }

        } catch (Exception e) {
            log.error("Error extracting export data for question {}: {}",
                    question.getId(), e.getMessage());
            result.data1 = "Error";
            result.data2 = "Error";
        }

        return result;
    }

    private String extractExplanation(QuestionResponseDTO question) {
        try {
            JsonNode data = objectMapper.valueToTree(question.getData());
            if (data != null && data.has("explanation")) {
                return data.get("explanation").asText();
            }
        } catch (Exception e) {
            log.debug("No explanation found for question {}", question.getId());
        }
        return "";
    }

    // ========================================================================
    // SHEET 2: GUIDE (COPY FROM TEMPLATE SERVICE)
    // ========================================================================
    
    private void createGuideSheet(Workbook workbook, Sheet sheet) {
        CellStyle titleStyle = createTitleStyle(workbook);

        int rowIdx = 0;

        // Title
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("📖 HƯỚNG DẪN SỬ DỤNG TEMPLATE");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 3));

        rowIdx++; // Empty row

        // Section 1: Các loại câu hỏi
        addSection(sheet, workbook, rowIdx++, "1️⃣ CÁC LOẠI CÂU HỎI HỖ TRỢ");
        addContent(sheet, workbook, rowIdx++, "• MULTIPLE_CHOICE", "Trắc nghiệm (A, B, C, D)");
        addContent(sheet, workbook, rowIdx++, "• TRUE_FALSE", "Đúng/Sai");
        addContent(sheet, workbook, rowIdx++, "• FILL_BLANK", "Điền từ vào chỗ trống");
        addContent(sheet, workbook, rowIdx++, "• TEXT_ANSWER", "Trả lời ngắn");
        addContent(sheet, workbook, rowIdx++, "• MATCHING", "Nối từ/cụm từ");
        addContent(sheet, workbook, rowIdx++, "• ERROR_CORRECTION", "Tìm và sửa lỗi sai");
        addContent(sheet, workbook, rowIdx++, "• SENTENCE_TRANSFORMATION", "Viết lại câu");
        addContent(sheet, workbook, rowIdx++, "• SENTENCE_BUILDING", "Sắp xếp từ thành câu");

        rowIdx++; // Empty row

        // Section 2: Cách nhập dữ liệu
        addSection(sheet, workbook, rowIdx++, "2️⃣ CÁCH NHẬP DỮ LIỆU");

        addContent(sheet, workbook, rowIdx++, "MULTIPLE_CHOICE / TRUE_FALSE:");
        addContent(sheet, workbook, rowIdx++, "  • Cột D: Đáp án đúng (VD: Hanoi)");
        addContent(sheet, workbook, rowIdx++, "  • Cột E: Tất cả đáp án ngăn cách bởi | (VD: Hanoi | HCM | Danang)");

        rowIdx++;

        addContent(sheet, workbook, rowIdx++, "FILL_BLANK / TEXT_ANSWER:");
        addContent(sheet, workbook, rowIdx++, "  • Cột D: Đáp án đúng (VD: went)");
        addContent(sheet, workbook, rowIdx++, "  • Cột E: Các đáp án khác (Optional, VD: go | gone)");

        rowIdx++;

        addContent(sheet, workbook, rowIdx++, "ERROR_CORRECTION:");
        addContent(sheet, workbook, rowIdx++, "  • Cột D: Từ/cụm từ sai (VD: goed)");
        addContent(sheet, workbook, rowIdx++, "  • Cột E: Từ/cụm từ đúng (VD: went)");

        rowIdx++;

        addContent(sheet, workbook, rowIdx++, "SENTENCE_TRANSFORMATION:");
        addContent(sheet, workbook, rowIdx++, "  • Cột D: Gợi ý đầu câu (VD: I wish)");
        addContent(sheet, workbook, rowIdx++, "  • Cột E: Câu đúng (VD: I wish I knew | I wish that I knew)");

        rowIdx++;

        addContent(sheet, workbook, rowIdx++, "SENTENCE_BUILDING:");
        addContent(sheet, workbook, rowIdx++, "  • Cột D: Các từ rời (VD: I | go | to | school)");
        addContent(sheet, workbook, rowIdx++, "  • Cột E: Câu đúng (VD: I go to school.)");

        rowIdx++;

        addContent(sheet, workbook, rowIdx++, "MATCHING:");
        addContent(sheet, workbook, rowIdx++, "  • Cột D: Danh sách cặp, định dạng: Left-Right");
        addContent(sheet, workbook, rowIdx++, "  • VD: Hot-Cold | Big-Small | Fast-Slow");

        rowIdx++;

        // Section 3: Task Group
        addSection(sheet, workbook, rowIdx++, "3️⃣ TASK GROUP (NHÓM CÂU HỎI)");
        addContent(sheet, workbook, rowIdx++, "Cột G: Task Group Name");
        addContent(sheet, workbook, rowIdx++, "  • Để trống = Standalone question (không thuộc task nào)");
        addContent(sheet, workbook, rowIdx++, "  • Nhập tên task = Câu hỏi thuộc task đó");
        addContent(sheet, workbook, rowIdx++, "  • VD: Task 1: Multiple Choice");
        addContent(sheet, workbook, rowIdx++, "  • VD: Task 2: Reading Comprehension");

        rowIdx++;

        addContent(sheet, workbook, rowIdx++, "Cột H: Task Instruction");
        addContent(sheet, workbook, rowIdx++, "  • Hướng dẫn cho task (VD: Choose the correct answer)");
        addContent(sheet, workbook, rowIdx++, "  • Chỉ cần nhập 1 lần cho mỗi task");
        addContent(sheet, workbook, rowIdx++, "  • Hệ thống sẽ dùng instruction này khi tạo task mới");

        rowIdx++;

        addContent(sheet, workbook, rowIdx++, "LƯU Ý:");
        addContent(sheet, workbook, rowIdx++, "  • Tên task phải GIỐNG NHAU cho các câu cùng nhóm");
        addContent(sheet, workbook, rowIdx++, "  • Hệ thống tự động tạo task nếu chưa tồn tại");
        addContent(sheet, workbook, rowIdx++, "  • Hoặc gán vào task đã có nếu tên trùng khớp");

        // Set column widths
        sheet.setColumnWidth(0, 20000);
        sheet.setColumnWidth(1, 15000);
    }

    // ========================================================================
    // SHEET 3: EXAMPLES (COPY FROM TEMPLATE SERVICE)
    // ========================================================================
    
    private void createExampleSheet(Workbook workbook, Sheet sheet) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle exampleStyle = createExampleStyle(workbook);

        // Header
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Loại câu hỏi", "Nội dung", "Điểm", "Cột D", "Cột E", "Giải thích", "Task Group", "Task Instruction"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Examples
        int row = 1;

        // Example 1: Multiple Choice (WITH TASK GROUP)
        addExampleData(sheet, row++, exampleStyle,
                "MULTIPLE_CHOICE",
                "What is the capital of Vietnam?",
                "1",
                "Hanoi",
                "Hanoi | Ho Chi Minh City | Danang | Hue",
                "Hanoi là thủ đô của Việt Nam",
                "Task 1: Multiple Choice",
                "Choose the correct answer (A, B, C or D)");

        // Example 2: True/False (WITH TASK GROUP)
        addExampleData(sheet, row++, exampleStyle,
                "TRUE_FALSE",
                "Vietnam is in Southeast Asia.",
                "1",
                "True",
                "True | False",
                "Việt Nam nằm ở Đông Nam Á",
                "Task 1: Multiple Choice",
                "Decide if the statement is True or False");

        // Example 3: Fill Blank (STANDALONE)
        addExampleData(sheet, row++, exampleStyle,
                "FILL_BLANK",
                "I ___ to school yesterday.",
                "1",
                "went",
                "go | went | gone",
                "Thì quá khứ đơn của 'go' là 'went'",
                "",
                "");

        // Example 4: Error Correction (WITH DIFFERENT TASK)
        addExampleData(sheet, row++, exampleStyle,
                "ERROR_CORRECTION",
                "She *goed* to the market.",
                "2",
                "goed",
                "went",
                "Quá khứ của 'go' là 'went', không phải 'goed'",
                "Task 2: Grammar Correction",
                "Find and correct the mistake");

        // Example 5: Sentence Transformation
        addExampleData(sheet, row++, exampleStyle,
                "SENTENCE_TRANSFORMATION",
                "It's a pity I didn't see him.",
                "2",
                "I wish",
                "I wish I had seen him | I wish that I had seen him",
                "Cấu trúc wish + quá khứ hoàn thành",
                "Task 2: Grammar Correction",
                "Rewrite the sentence using the given words");

        // Example 6: Matching
        addExampleData(sheet, row++, exampleStyle,
                "MATCHING",
                "Match the opposites:",
                "2",
                "Hot-Cold | Big-Small | Fast-Slow",
                "",
                "Nối các cặp từ trái nghĩa",
                "Task 3: Vocabulary",
                "Match each word with its opposite");

        // Example 7: Sentence Building
        addExampleData(sheet, row++, exampleStyle,
                "SENTENCE_BUILDING",
                "",
                "1",
                "I | go | to | school",
                "I go to school.",
                "Sắp xếp các từ thành câu đúng",
                "",
                "");

        // Set column widths
        for (int i = 0; i < 8; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
    }

    // ========================================================================
    // HELPER METHODS - STYLES
    // ========================================================================

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createSubInstructionStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 9);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    private CellStyle createSectionHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createContentStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createExampleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    // ========================================================================
    // HELPER METHODS - DATA
    // ========================================================================

    private String stripHtml(String html) {
        if (html == null)
            return "";
        return html.replaceAll("<[^>]*>", "").trim();
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void addSection(Sheet sheet, Workbook wb, int rowIdx, String text) {
        Row row = sheet.createRow(rowIdx);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(createSectionHeaderStyle(wb));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx, rowIdx, 0, 1));
    }

    private void addContent(Sheet sheet, Workbook wb, int rowIdx, String col1) {
        addContent(sheet, wb, rowIdx, col1, "");
    }

    private void addContent(Sheet sheet, Workbook wb, int rowIdx, String col1, String col2) {
        Row row = sheet.createRow(rowIdx);
        CellStyle contentStyle = createContentStyle(wb);

        Cell cell1 = row.createCell(0);
        cell1.setCellValue(col1);
        cell1.setCellStyle(contentStyle);

        if (!col2.isEmpty()) {
            Cell cell2 = row.createCell(1);
            cell2.setCellValue(col2);
            cell2.setCellStyle(contentStyle);
        }
    }

    private void addExampleData(Sheet sheet, int rowIdx, CellStyle style, String... values) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            if (i == 2 && !values[i].isEmpty()) { // Points column
                try {
                    cell.setCellValue(Integer.parseInt(values[i]));
                } catch (NumberFormatException e) {
                    cell.setCellValue(values[i]);
                }
            } else {
                cell.setCellValue(values[i]);
            }
            cell.setCellStyle(style);
        }
    }

    // Helper class
    private static class ExportData {
        String data1 = "";
        String data2 = "";
    }
}