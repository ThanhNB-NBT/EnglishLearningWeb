package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.enums.QuestionType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

@Service
public class ExcelTemplateService {

    public ByteArrayInputStream generateQuestionTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Sheet 1: Template chính để nhập liệu
            Sheet mainSheet = workbook.createSheet("📝 NHẬP CÂU HỎI TẠI ĐÂY");
            createMainTemplate(workbook, mainSheet);

            // Sheet 2: Hướng dẫn chi tiết
            Sheet guideSheet = workbook.createSheet("📖 HƯỚNG DẪN");
            createGuideSheet(workbook, guideSheet);

            // Sheet 3: Ví dụ đầy đủ
            Sheet exampleSheet = workbook.createSheet("💡 VÍ DỤ MẪU");
            createExampleSheet(workbook, exampleSheet);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // ========================================================================
    // SHEET 1: MAIN TEMPLATE (WITH TASK GROUP COLUMN)
    // ========================================================================
    private void createMainTemplate(Workbook workbook, Sheet sheet) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle instructionStyle = createInstructionStyle(workbook);

        // Row 0: Title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("📋 TEMPLATE IMPORT CÂU HỎI - ENGLISH LEARNING SYSTEM");
        titleCell.setCellStyle(createTitleStyle(workbook));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 6));

        // Row 1: Empty
        sheet.createRow(1);

        // Row 2: Instructions
        Row instructionRow = sheet.createRow(2);
        Cell instrCell = instructionRow.createCell(0);
        instrCell.setCellValue("⚠️ LƯU Ý: Bắt đầu nhập dữ liệu từ dòng 6 trở xuống. Không xóa/sửa dòng header!");
        instrCell.setCellStyle(instructionStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 6));

        // Row 3: Empty
        sheet.createRow(3);

        // Row 4: Column Headers (✅ ADDED TASK GROUP)
        Row headerRow = sheet.createRow(4);
        String[] headers = {
                "A. Loại câu hỏi\n(Chọn từ dropdown)",
                "B. Nội dung câu hỏi\n(Text/HTML)",
                "C. Điểm số\n(1-100)",
                "D. Đáp án đúng / Từ sai\n(Tùy loại câu)",
                "E. Các đáp án / Từ sửa\n(Ngăn cách bởi |)",
                "F. Giải thích\n(Optional)",
                "G. Task Group\n(Optional - tên task)"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Row 5: Sub-instructions (✅ ADDED TASK GROUP HINT)
        Row subInstrRow = sheet.createRow(5);
        String[] subInstructions = {
                "Click vào ô để chọn",
                "Nhập câu hỏi hoặc đoạn văn",
                "Số nguyên dương",
                "Xem cột E bên phải →",
                "← Xem cột D bên trái",
                "Giải thích đáp án",
                "Task 1, Task 2, etc."
        };

        CellStyle subInstrStyle = createSubInstructionStyle(workbook);
        for (int i = 0; i < subInstructions.length; i++) {
            Cell cell = subInstrRow.createCell(i);
            cell.setCellValue(subInstructions[i]);
            cell.setCellStyle(subInstrStyle);
        }

        // Add example row (row 6)
        addExampleRow(sheet, 6, workbook);

        // Set column widths (✅ ADDED TASK GROUP WIDTH)
        sheet.setColumnWidth(0, 7000); // Question Type
        sheet.setColumnWidth(1, 15000); // Content
        sheet.setColumnWidth(2, 3000); // Points
        sheet.setColumnWidth(3, 10000); // Data 1
        sheet.setColumnWidth(4, 12000); // Data 2
        sheet.setColumnWidth(5, 10000); // Explanation
        sheet.setColumnWidth(6, 5000); // Task Group (NEW)

        // Add dropdown validation for column A (from row 7 onwards)
        createDropdownValidation(sheet);

        // Freeze panes (freeze first 6 rows)
        sheet.createFreezePane(0, 6);
    }

    // ========================================================================
    // SHEET 2: GUIDE (UPDATED WITH TASK GROUP INFO)
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

        // Section 3: Task Group (✅ NEW)
        addSection(sheet, workbook, rowIdx++, "3️⃣ TASK GROUP (NHÓM CÂU HỎI)");
        addContent(sheet, workbook, rowIdx++, "Cột G: Task Group Name");
        addContent(sheet, workbook, rowIdx++, "  • Để trống = Standalone question (không thuộc task nào)");
        addContent(sheet, workbook, rowIdx++, "  • Nhập tên task = Câu hỏi thuộc task đó");
        addContent(sheet, workbook, rowIdx++, "  • VD: Task 1: Multiple Choice");
        addContent(sheet, workbook, rowIdx++, "  • VD: Task 2: Reading Comprehension");

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
    // SHEET 3: EXAMPLES (UPDATED WITH TASK GROUP)
    // ========================================================================
    private void createExampleSheet(Workbook workbook, Sheet sheet) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle exampleStyle = createExampleStyle(workbook);

        // Header (✅ ADDED TASK GROUP)
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Loại câu hỏi", "Nội dung", "Điểm", "Cột D", "Cột E", "Giải thích", "Task Group"
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
                "Task 1: Multiple Choice");

        // Example 2: True/False (WITH TASK GROUP)
        addExampleData(sheet, row++, exampleStyle,
                "TRUE_FALSE",
                "Vietnam is in Southeast Asia.",
                "1",
                "True",
                "True | False",
                "Việt Nam nằm ở Đông Nam Á",
                "Task 1: Multiple Choice");

        // Example 3: Fill Blank (STANDALONE)
        addExampleData(sheet, row++, exampleStyle,
                "FILL_BLANK",
                "I ___ to school yesterday.",
                "1",
                "went",
                "go | went | gone",
                "Thì quá khứ đơn của 'go' là 'went'",
                "");

        // Example 4: Error Correction (WITH DIFFERENT TASK)
        addExampleData(sheet, row++, exampleStyle,
                "ERROR_CORRECTION",
                "She *goed* to the market.",
                "2",
                "goed",
                "went",
                "Quá khứ của 'go' là 'went', không phải 'goed'",
                "Task 2: Grammar Correction");

        // Example 5: Sentence Transformation
        addExampleData(sheet, row++, exampleStyle,
                "SENTENCE_TRANSFORMATION",
                "It's a pity I didn't see him.",
                "2",
                "I wish",
                "I wish I had seen him | I wish that I had seen him",
                "Cấu trúc wish + quá khứ hoàn thành",
                "Task 2: Grammar Correction");

        // Example 6: Matching
        addExampleData(sheet, row++, exampleStyle,
                "MATCHING",
                "Match the opposites:",
                "2",
                "Hot-Cold | Big-Small | Fast-Slow",
                "",
                "Nối các cặp từ trái nghĩa",
                "Task 3: Vocabulary");

        // Example 7: Sentence Building
        addExampleData(sheet, row++, exampleStyle,
                "SENTENCE_BUILDING",
                "",
                "1",
                "I | go | to | school",
                "I go to school.",
                "Sắp xếp các từ thành câu đúng",
                "");

        // Set column widths
        for (int i = 0; i < 7; i++) {
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
        font.setFontHeightInPoints((short) 16);
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
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
        return style;
    }

    private CellStyle createInstructionStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
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

    // ========================================================================
    // HELPER METHODS - DATA
    // ========================================================================

    private void addExampleRow(Sheet sheet, int rowIdx, Workbook wb) {
        Row row = sheet.createRow(rowIdx);
        CellStyle exampleStyle = createExampleStyle(wb);

        String[] data = {
                "MULTIPLE_CHOICE",
                "What is the capital of Vietnam?",
                "1",
                "Hanoi",
                "Hanoi | Ho Chi Minh City | Danang",
                "Hanoi là thủ đô của Việt Nam",
                "Task 1: Multiple Choice" // ✅ Task Group example
        };

        for (int i = 0; i < data.length; i++) {
            Cell cell = row.createCell(i);
            if (i == 2) {
                cell.setCellValue(Integer.parseInt(data[i]));
            } else {
                cell.setCellValue(data[i]);
            }
            cell.setCellStyle(exampleStyle);
        }
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

    // ✅ UPDATED: Added taskGroup parameter
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

    private void createDropdownValidation(Sheet sheet) {
        String[] types = Arrays.stream(QuestionType.values())
                .map(Enum::name)
                .toArray(String[]::new);

        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(types);
        CellRangeAddressList addressList = new CellRangeAddressList(6, 1000, 0, 0);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Lỗi nhập liệu", "Vui lòng chọn loại câu hỏi từ dropdown");
        sheet.addValidationData(validation);
    }
}