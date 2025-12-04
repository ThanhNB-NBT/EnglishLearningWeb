package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.enums.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Centralized metadata structure validation
 * Validates metadata BEFORE saving to database
 */
@Component
@Slf4j
public class QuestionMetadataValidator {

    /**
     * Validate metadata structure based on question type
     * 
     * @param questionType Question type
     * @param metadata     Metadata map
     * @throws RuntimeException if validation fails
     */
    public void validate(QuestionType questionType, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            throw new RuntimeException(
                    String.format("Metadata không được null/empty cho question type: %s",
                            questionType));
        }

        switch (questionType) {
            case MULTIPLE_CHOICE, TRUE_FALSE -> validateMultipleChoice(metadata, questionType);
            case FILL_BLANK, VERB_FORM, TEXT_ANSWER -> validateFillBlank(metadata, questionType);
            case ERROR_CORRECTION -> validateErrorCorrection(metadata, questionType);
            case MATCHING -> validateMatching(metadata);
            case SENTENCE_BUILDING -> validateSentenceBuilding(metadata);
            case PRONUNCIATION -> validatePronunciation(metadata);
            case READING_COMPREHENSION -> validateReadingComprehension(metadata);
            case OPEN_ENDED -> validateOpenEnded(metadata);
            case SENTENCE_TRANSFORMATION -> validateSentenceTransformation(metadata);
            default -> throw new IllegalArgumentException("Unsupported question type: " + questionType);
        }

        log.debug("Validated metadata structure for {}", questionType);
    }

    // ========== PRIVATE VALIDATION METHODS ==========

    private void validateMultipleChoice(Map<String, Object> metadata, QuestionType type) {
        if (type == QuestionType.COMPLETE_CONVERSATION && metadata.containsKey("conversationContext")) {
            // Validate kiểu conversation (có options, correctAnswer string)
            if (!metadata.containsKey("options") || !metadata.containsKey("correctAnswer")) {
                throw new RuntimeException("Hội thoại thiếu options hoặc correctAnswer");
            }
            return;
        }

        if (!metadata.containsKey("options")) {
            throw new RuntimeException(type + " cần có 'options' trong metadata");
        }

        Object optionsObj = metadata.get("options");
        if (!(optionsObj instanceof List)) {
            throw new RuntimeException(type + " 'options' phải là List");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) optionsObj;

        if (options.isEmpty()) {
            throw new RuntimeException(type + " cần ít nhất 1 option");
        }

        int correctCount = 0; // ĐẾM SỐ OPTION ĐÚNG
        Set<Integer> seenOrders = new HashSet<>();

        for (int i = 0; i < options.size(); i++) {
            Map<String, Object> option = options.get(i);

            // Validate text
            if (!option.containsKey("text")) {
                throw new RuntimeException(type + " mỗi option cần có 'text'");
            }

            Object textObj = option.get("text");
            if (!(textObj instanceof String)) {
                throw new RuntimeException(type + " option 'text' phải là String");
            }

            String text = (String) textObj;
            if (text.trim().isEmpty()) {
                throw new RuntimeException(type + " option 'text' không được rỗng (index: " + i + ")");
            }

            // Validate isCorrect
            if (!option.containsKey("isCorrect")) {
                throw new RuntimeException(type + " mỗi option cần có 'isCorrect'");
            }

            if (!(option.get("isCorrect") instanceof Boolean)) {
                throw new RuntimeException(type + " option 'isCorrect' phải là Boolean");
            }

            // ĐẾM SỐ OPTION ĐÚNG
            if (Boolean.TRUE.equals(option.get("isCorrect"))) {
                correctCount++;
            }

            // Validate order
            if (!option.containsKey("order")) {
                throw new RuntimeException(type + " mỗi option cần có 'order'");
            }

            Object orderObj = option.get("order");
            if (!(orderObj instanceof Integer)) {
                throw new RuntimeException(type + " option 'order' phải là Integer");
            }

            Integer order = (Integer) orderObj;
            if (order < 1) {
                throw new RuntimeException(type + " option 'order' phải >= 1 (found: " + order + ")");
            }

            if (seenOrders.contains(order)) {
                throw new RuntimeException(type + " có order bị trùng: " + order);
            }
            seenOrders.add(order);
        }

        // KIỂM TRA CHÍNH XÁC 1 OPTION ĐÚNG
        if (correctCount == 0) {
            throw new RuntimeException(type + " cần có ít nhất 1 option đúng (isCorrect=true)");
        }

        if (correctCount > 1) {
            throw new RuntimeException(
                    String.format("%s chỉ được có đúng 1 option đúng (tìm thấy %d options đúng)",
                            type, correctCount));
        }
    }

    private void validateFillBlank(Map<String, Object> metadata, QuestionType type) {
        if (type == QuestionType.TEXT_ANSWER && metadata.containsKey("correctAnswer")) return;
        
        if (!metadata.containsKey("blanks")) {
            throw new RuntimeException(type + " cần có 'blanks' trong metadata");
        }

        Object blanksObj = metadata.get("blanks");
        if (!(blanksObj instanceof List)) {
            throw new RuntimeException(type + " 'blanks' phải là List");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> blanks = (List<Map<String, Object>>) blanksObj;

        if (blanks.isEmpty()) {
            throw new RuntimeException(type + " cần ít nhất 1 blank");
        }

        for (int i = 0; i < blanks.size(); i++) {
            Map<String, Object> blank = blanks.get(i);

            if (!blank.containsKey("position")) {
                throw new RuntimeException(type + " blank thứ " + (i + 1) + " thiếu 'position'");
            }

            if (!blank.containsKey("correctAnswers")) {
                throw new RuntimeException(type + " blank thứ " + (i + 1) + " thiếu 'correctAnswers'");
            }

            Object answersObj = blank.get("correctAnswers");
            if (!(answersObj instanceof List)) {
                throw new RuntimeException(type + " blank 'correctAnswers' phải là List");
            }

            @SuppressWarnings("unchecked")
            List<String> correctAnswers = (List<String>) answersObj;

            // KIỂM TRA CỤ THỂ
            if (correctAnswers.isEmpty()) {
                throw new RuntimeException(
                        String.format("%s - Chỗ trống #%d chưa có đáp án đúng.  Vui lòng nhập ít nhất 1 đáp án.",
                                type, blank.get("position")));
            }

            // KIỂM TRA TỪNG ĐÁP ÁN KHÔNG RỖNG
            for (int j = 0; j < correctAnswers.size(); j++) {
                String answer = correctAnswers.get(j);
                if (answer == null || answer.trim().isEmpty()) {
                    throw new RuntimeException(
                            String.format("%s - Chỗ trống #%d có đáp án rỗng (vị trí %d)",
                                    type, blank.get("position"), j + 1));
                }
            }
        }
    }

    private void validateErrorCorrection(Map<String, Object> metadata, QuestionType type) {
        if (!metadata.containsKey("errorText")) {
            throw new RuntimeException(type + " cần có 'errorText' trong metadata");
        }

        Object errorTextObj = metadata.get("errorText");
        if (errorTextObj == null || (errorTextObj instanceof String && ((String) errorTextObj).trim().isEmpty())) {
            throw new RuntimeException(type + " 'errorText' không được để trống");
        }

        if (!metadata.containsKey("correction")) {
            throw new RuntimeException(type + " cần có 'correction' trong metadata");
        }

        Object correctionObj = metadata.get("correction");
        if (correctionObj == null || (correctionObj instanceof String && ((String) correctionObj).trim().isEmpty())) {
            throw new RuntimeException(type + " 'correction' không được để trống");
        }
    }

    private void validateMatching(Map<String, Object> metadata) {
        if (!metadata.containsKey("pairs")) {
            throw new RuntimeException("MATCHING cần có 'pairs' trong metadata");
        }

        Object pairsObj = metadata.get("pairs");
        if (!(pairsObj instanceof List)) {
            throw new RuntimeException("MATCHING 'pairs' phải là List");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pairs = (List<Map<String, Object>>) pairsObj;

        if (pairs.size() < 2) {
            throw new RuntimeException("MATCHING cần ít nhất 2 pairs");
        }

        // Validate pair structure
        for (Map<String, Object> pair : pairs) {
            if (!pair.containsKey("left")) {
                throw new RuntimeException("MATCHING mỗi pair cần có 'left'");
            }

            if (!pair.containsKey("right")) {
                throw new RuntimeException("MATCHING mỗi pair cần có 'right'");
            }

            if (!pair.containsKey("order")) {
                throw new RuntimeException("MATCHING mỗi pair cần có 'order'");
            }
        }
    }

    private void validateSentenceBuilding(Map<String, Object> metadata) {
        if (!metadata.containsKey("words")) {
            throw new RuntimeException("SENTENCE_BUILDING cần có 'words' trong metadata");
        }

        if (!metadata.containsKey("correctSentence")) {
            throw new RuntimeException("SENTENCE_BUILDING cần có 'correctSentence' trong metadata");
        }

        Object wordsObj = metadata.get("words");
        if (!(wordsObj instanceof List)) {
            throw new RuntimeException("SENTENCE_BUILDING 'words' phải là List");
        }

        @SuppressWarnings("unchecked")
        List<String> words = (List<String>) wordsObj;
        if (words.size() < 2) {
            throw new RuntimeException("SENTENCE_BUILDING cần ít nhất 2 từ");
        }

        Object sentenceObj = metadata.get("correctSentence");
        if (sentenceObj == null || ((String) sentenceObj).trim().isEmpty()) {
            throw new RuntimeException("SENTENCE_BUILDING 'correctSentence' không được để trống");
        }
    }

    private void validatePronunciation(Map<String, Object> metadata) {
        if (!metadata.containsKey("words")) {
            throw new RuntimeException("PRONUNCIATION cần có 'words'");
        }

        if (!metadata.containsKey("categories")) {
            throw new RuntimeException("PRONUNCIATION cần có 'categories'");
        }

        if (!metadata.containsKey("correctClassifications")) {
            throw new RuntimeException("PRONUNCIATION cần có 'correctClassifications'");
        }

        Object wordsObj = metadata.get("words");
        if (!(wordsObj instanceof List)) {
            throw new RuntimeException("PRONUNCIATION 'words' phải là List");
        }

        Object categoriesObj = metadata.get("categories");
        if (!(categoriesObj instanceof List)) {
            throw new RuntimeException("PRONUNCIATION 'categories' phải là List");
        }

        Object classificationsObj = metadata.get("correctClassifications");
        if (!(classificationsObj instanceof List)) {
            throw new RuntimeException("PRONUNCIATION 'correctClassifications' phải là List");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> classifications = (List<Map<String, Object>>) classificationsObj;

        if (classifications.isEmpty()) {
            throw new RuntimeException("PRONUNCIATION 'correctClassifications' không được rỗng");
        }

        // Validate classification structure
        for (Map<String, Object> classification : classifications) {
            if (!classification.containsKey("word")) {
                throw new RuntimeException("PRONUNCIATION mỗi classification cần có 'word'");
            }

            if (!classification.containsKey("category")) {
                throw new RuntimeException("PRONUNCIATION mỗi classification cần có 'category'");
            }
        }
    }

    private void validateReadingComprehension(Map<String, Object> metadata) {
        if (!metadata.containsKey("passage")) {
            throw new RuntimeException("READING_COMPREHENSION cần có 'passage'");
        }

        Object passageObj = metadata.get("passage");
        if (passageObj == null || ((String) passageObj).trim().isEmpty()) {
            throw new RuntimeException("READING_COMPREHENSION 'passage' không được để trống");
        }

        if (!metadata.containsKey("blanks")) {
            throw new RuntimeException("READING_COMPREHENSION cần có 'blanks'");
        }

        Object blanksObj = metadata.get("blanks");
        if (!(blanksObj instanceof List)) {
            throw new RuntimeException("READING_COMPREHENSION 'blanks' phải là List");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> blanks = (List<Map<String, Object>>) blanksObj;

        if (blanks.isEmpty()) {
            throw new RuntimeException("READING_COMPREHENSION cần ít nhất 1 blank");
        }

        // Validate blank structure
        for (Map<String, Object> blank : blanks) {
            if (!blank.containsKey("position")) {
                throw new RuntimeException("READING_COMPREHENSION mỗi blank cần có 'position'");
            }

            if (!blank.containsKey("options")) {
                throw new RuntimeException("READING_COMPREHENSION mỗi blank cần có 'options'");
            }

            if (!blank.containsKey("correctAnswer")) {
                throw new RuntimeException("READING_COMPREHENSION mỗi blank cần có 'correctAnswer'");
            }

            Object optionsObj = blank.get("options");
            if (!(optionsObj instanceof List)) {
                throw new RuntimeException("READING_COMPREHENSION blank 'options' phải là List");
            }
        }
    }

    private void validateOpenEnded(Map<String, Object> metadata) {
        // OPEN_ENDED có thể không cần validate strict
        // Chỉ cần suggestedAnswer (optional), timeLimitSeconds (optional)
        log.debug("OPEN_ENDED validation: minimal checks (suggestedAnswer optional)");
    }

    private void validateSentenceTransformation(Map<String, Object> metadata) {
        if (!metadata.containsKey("originalSentence")) {
            throw new RuntimeException("SENTENCE_TRANSFORMATION thiếu trường 'originalSentence'");
        }

        Object originalObj = metadata.get("originalSentence");
        if (!(originalObj instanceof String) || ((String) originalObj).trim().isEmpty()) {
            throw new RuntimeException("SENTENCE_TRANSFORMATION 'originalSentence' không được để trống");
        }

        if (!metadata.containsKey("correctAnswers")) {
            throw new RuntimeException("SENTENCE_TRANSFORMATION thiếu trường 'correctAnswers'");
        }

        Object answersObj = metadata.get("correctAnswers");
        if (!(answersObj instanceof List)) {
            throw new RuntimeException("SENTENCE_TRANSFORMATION 'correctAnswers' phải là một danh sách (List)");
        }

        List<?> list = (List<?>) answersObj;
        if (list.isEmpty()) {
            throw new RuntimeException("SENTENCE_TRANSFORMATION danh sách 'correctAnswers' không được rỗng");
        }
    }
}