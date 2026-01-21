package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.dto.question.request.*;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.QuestionType;

import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SmartExcelImportService {

    /**
     * ✅ Parse Excel with Task Group support
     * 
     * Column structure:
     * A = Question Type
     * B = Question Text
     * C = Points
     * D = Data 1 (Correct Answer / Error)
     * E = Data 2 (All Options / Correction)
     * F = Explanation
     * G = Task Group Name
     * H = Task Instruction (IGNORED - we get from existing task or use default)
     */
    public List<CreateQuestionDTO> parseExcel(
            MultipartFile file,
            ParentType parentType,
            Long lessonId) throws Exception {

        List<CreateQuestionDTO> questions = new ArrayList<>();

        // ✅ Track task names to group questions
        Map<String, String> taskInstructions = new HashMap<>(); // taskName -> instruction
        List<CreateQuestionDTO> standaloneQuestions = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            // ✅ Start from row 6 (after headers: 0=title, 1=empty, 2=warning, 3=empty, 4=header, 5=subheader)
            int startRow = 6;

            // If first row is example, skip it
            Row firstDataRow = sheet.getRow(startRow);
            if (firstDataRow != null) {
                String firstType = getVal(firstDataRow.getCell(0)).trim();
                String firstQuestion = getVal(firstDataRow.getCell(1)).trim();
                // Check if it's the example row
                if (firstType.equals("MULTIPLE_CHOICE") &&
                        firstQuestion.contains("What is the capital of Vietnam")) {
                    startRow = 7; // Skip example row
                }
            }

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                // Skip empty rows
                if (row == null || getVal(row.getCell(0)).isEmpty())
                    continue;

                CreateQuestionDTO dto = new CreateQuestionDTO();

                // ✅ Set context from parameters
                dto.setParentType(parentType);
                dto.setParentId(lessonId);

                // 1. Read Question Type
                String typeStr = getVal(row.getCell(0)).trim();
                try {
                    dto.setQuestionType(QuestionType.valueOf(typeStr));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid question type at row {}: {}", i + 1, typeStr);
                    continue; // Skip invalid type
                }

                // 2. Read common columns
                dto.setQuestionText(getVal(row.getCell(1)));
                String points = getVal(row.getCell(2));
                dto.setPoints(points.isEmpty() ? 1 : (int) Double.parseDouble(points));

                // 3. Read data columns
                String colData1 = getVal(row.getCell(3)); // Column D (Data 1)
                String colData2 = getVal(row.getCell(4)); // Column E (Data 2)
                String explanation = getVal(row.getCell(5)); // Column F (Explanation)

                // ✅ Read Task Group Name from Column G
                String taskGroupName = getVal(row.getCell(6)).trim(); // Column G (Task Group)
                
                // ✅ NEW: Read Task Instruction from Column H (but we'll ignore it in import)
                String taskInstruction = getVal(row.getCell(7)).trim(); // Column H

                // Remove trailing colons and clean up
                if (!taskGroupName.isEmpty()) {
                    taskGroupName = taskGroupName.replaceAll(":+\\s*$", "").trim();
                    
                    // Store instruction if provided (will be used when creating new tasks)
                    if (!taskInstruction.isEmpty() && !taskInstructions.containsKey(taskGroupName)) {
                        taskInstructions.put(taskGroupName, taskInstruction);
                    }
                }

                // 4. Map data based on question type
                dto.setData(mapData(dto.getQuestionType(), colData1, colData2, explanation));

                // ✅ Store task name AND instruction
                if (!taskGroupName.isEmpty()) {
                    dto.setTaskGroupName(taskGroupName);
                    
                    // ✅ Store instruction (will be used when creating new tasks)
                    if (!taskInstruction.isEmpty()) {
                        dto.setTaskInstruction(taskInstruction);
                    }
                    
                    questions.add(dto);

                    log.debug("Row {}: Question type={}, TaskGroup='{}', TaskInstruction='{}'",
                            i + 1, dto.getQuestionType(), taskGroupName, taskInstruction);
                } else {
                    standaloneQuestions.add(dto);
                }
            }
        }

        // ✅ Log task grouping results
        Map<String, Long> taskCounts = questions.stream()
            .filter(q -> q.getTaskGroupName() != null)
            .collect(Collectors.groupingBy(
                CreateQuestionDTO::getTaskGroupName, 
                Collectors.counting()
            ));

        log.info("Parsed {} task groups, {} standalone questions", taskCounts.size(), standaloneQuestions.size());

        taskCounts.forEach((name, count) -> {
            String instruction = taskInstructions.get(name);
            log.info("  Task '{}': {} questions, instruction: '{}'", name, count, instruction);
        });

        // ✅ Combine: standalone first, then grouped
        List<CreateQuestionDTO> result = new ArrayList<>();
        result.addAll(standaloneQuestions);
        result.addAll(questions);

        log.info("Total questions returned: {}", result.size());
        return result;
    }

    private QuestionData mapData(QuestionType type, String d1, String d2, String explanation) {
        QuestionData data = null;

        switch (type) {
            case MULTIPLE_CHOICE:
            case TRUE_FALSE:
            case COMPLETE_CONVERSATION:
                CreateMultipleChoiceDTO mc = new CreateMultipleChoiceDTO();
                List<String> allOps = split(d2);
                List<CreateMultipleChoiceDTO.OptionDTO> options = new ArrayList<>();
                int order = 1;
                for (String txt : allOps) {
                    CreateMultipleChoiceDTO.OptionDTO op = new CreateMultipleChoiceDTO.OptionDTO();
                    op.setText(txt);
                    op.setIsCorrect(txt.trim().equalsIgnoreCase(d1.trim()));
                    op.setOrder(order++);
                    options.add(op);
                }
                mc.setOptions(options);
                data = mc;
                break;

            case FILL_BLANK:
            case TEXT_ANSWER:
            case VERB_FORM:
                CreateFillBlankDTO fb = new CreateFillBlankDTO();
                CreateFillBlankDTO.BlankDTO blank = new CreateFillBlankDTO.BlankDTO();
                blank.setPosition(1);
                blank.setCorrectAnswers(split(d1));
                fb.setBlanks(List.of(blank));
                if (!d2.isEmpty())
                    fb.setWordBank(split(d2));
                data = fb;
                break;

            case ERROR_CORRECTION:
                CreateErrorCorrectionDTO ec = new CreateErrorCorrectionDTO();
                ec.setErrorText(d1);
                ec.setCorrection(d2);
                data = ec;
                break;

            case SENTENCE_TRANSFORMATION:
                CreateSentenceTransformationDTO st = new CreateSentenceTransformationDTO();
                st.setOriginalSentence(d1.isEmpty() ? "Rewrite the sentence" : d1);
                st.setBeginningPhrase(d1);
                st.setCorrectAnswers(split(d2));
                data = st;
                break;

            case SENTENCE_BUILDING:
                CreateSentenceBuildingDTO sb = new CreateSentenceBuildingDTO();
                if (!d1.isEmpty()) {
                    sb.setWords(split(d1));
                }
                if (!d2.isEmpty()) {
                    sb.setCorrectSentence(d2);
                }
                data = sb;
                break;

            case MATCHING:
                CreateMatchingDTO ma = new CreateMatchingDTO();
                List<String> pairsRaw = split(d1);
                List<CreateMatchingDTO.PairDTO> pairs = new ArrayList<>();
                int pairOrder = 1;
                for (String p : pairsRaw) {
                    String[] parts = p.split("-");
                    if (parts.length >= 2) {
                        CreateMatchingDTO.PairDTO pair = new CreateMatchingDTO.PairDTO();
                        pair.setLeft(parts[0].trim());
                        pair.setRight(parts[1].trim());
                        pair.setOrder(pairOrder++);
                        pairs.add(pair);
                    }
                }
                ma.setPairs(pairs);
                data = ma;
                break;

            default:
                return null;
        }

        // Gán explanation vào data
        if (data != null) {
            data.setExplanation(explanation);
        }
        return data;
    }

    private String getVal(Cell c) {
        if (c == null)
            return "";

        switch (c.getCellType()) {
            case STRING:
                return c.getStringCellValue().trim();
            case NUMERIC:
                // Check if it's a date
                if (DateUtil.isCellDateFormatted(c)) {
                    return c.getDateCellValue().toString();
                }
                // Return number as string
                double numVal = c.getNumericCellValue();
                // If it's a whole number, don't show decimal
                if (numVal == Math.floor(numVal)) {
                    return String.valueOf((int) numVal);
                }
                return String.valueOf(numVal);
            case BOOLEAN:
                return String.valueOf(c.getBooleanCellValue());
            case FORMULA:
                // Try to get the cached value
                try {
                    return c.getStringCellValue().trim();
                } catch (IllegalStateException e) {
                    try {
                        return String.valueOf(c.getNumericCellValue());
                    } catch (IllegalStateException e2) {
                        return "";
                    }
                }
            case BLANK:
            default:
                return "";
        }
    }

    private List<String> split(String input) {
        if (input == null || input.trim().isEmpty())
            return new ArrayList<>();
        return Arrays.stream(input.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}