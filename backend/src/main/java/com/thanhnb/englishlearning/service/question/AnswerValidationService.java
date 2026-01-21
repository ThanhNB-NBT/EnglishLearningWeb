package com.thanhnb.englishlearning.service.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.dto.question.request.*;
import com.thanhnb.englishlearning.entity.question.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnswerValidationService {

    private final ObjectMapper objectMapper;

    public QuestionResultDTO validateAnswer(Question question, List<Long> selectedOptions, String textAnswer) {
        QuestionResultDTO.QuestionResultDTOBuilder builder = QuestionResultDTO.builder()
                .questionId(question.getId())
                .questionText(question.getQuestionText())
                .points(question.getPoints());

        QuestionData data = question.getData();
        if (data == null) {
            return builder.isCorrect(false).feedback("Dữ liệu câu hỏi bị thiếu").points(0).build();
        }

        try {
            switch (question.getQuestionType()) {
                case MULTIPLE_CHOICE:
                case TRUE_FALSE:
                case COMPLETE_CONVERSATION:
                    if (data instanceof CreateMultipleChoiceDTO dto) {
                        Object answer = (selectedOptions != null && !selectedOptions.isEmpty()) ? selectedOptions.get(0)
                                : textAnswer;
                        return validateMultipleChoice(dto, answer, builder);
                    }
                    break;

                case PRONUNCIATION:
                    if (data instanceof CreatePronunciationsDTO dto) {
                        Object parsedAnswer = parseAnswerToMapOrString(textAnswer);
                        return validatePronunciation(dto, parsedAnswer, builder);
                    }
                    break;

                case FILL_BLANK:
                case VERB_FORM:
                    if (data instanceof CreateFillBlankDTO dto) {
                        Object parsedAnswer = parseAnswerToMapOrString(textAnswer);
                        return validateFillBlank(dto, parsedAnswer, builder);
                    }
                    break;

                case MATCHING:
                    if (data instanceof CreateMatchingDTO dto) {
                        Object parsedMap = parseAnswerToMapOrString(textAnswer);
                        return validateMatching(dto, parsedMap, builder);
                    }
                    break;

                case SENTENCE_BUILDING:
                    if (data instanceof CreateSentenceBuildingDTO dto) {
                        return validateSentenceBuilding(dto, textAnswer, builder);
                    }
                    break;

                case SENTENCE_TRANSFORMATION:
                    if (data instanceof CreateSentenceTransformationDTO dto) {
                        return validateSentenceTransformation(dto, textAnswer, builder);
                    }
                    break;

                case ERROR_CORRECTION:
                    if (data instanceof CreateErrorCorrectionDTO dto) {
                        Object parsedAnswer = parseAnswerToMapOrString(textAnswer);
                        return validateErrorCorrection(dto, parsedAnswer, builder);
                    }
                    break;

                case TEXT_ANSWER:
                    if (data instanceof CreateFillBlankDTO dto) {
                        return validateTextAnswer(dto, textAnswer, builder);
                    }
                    break;

                case OPEN_ENDED:
                    if (data instanceof CreateOpenEndedDTO dto) {
                        return validateOpenEnded(dto, textAnswer, builder);
                    }
                    break;

                default:
                    return builder.isCorrect(false).points(0).feedback("Loại câu hỏi chưa hỗ trợ chấm tự động").build();
            }
        } catch (Exception e) {
            log.error("Lỗi chấm điểm câu hỏi ID {}: {}", question.getId(), e.getMessage(), e);
            return builder.isCorrect(false).points(0).feedback("Lỗi hệ thống khi chấm điểm").build();
        }

        return builder.isCorrect(false).points(0).feedback("Dữ liệu không khớp").build();
    }

    // =========================================================================
    // ✅ FIXED: FILL_BLANK VALIDATION - Trả về chi tiết từng blank
    // =========================================================================

    private QuestionResultDTO validateFillBlank(CreateFillBlankDTO dto, Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {

        log.debug("Validating FILL_BLANK - DTO blanks: {}, userAnswer type: {}, value: {}",
                dto.getBlanks().size(), userAnswer.getClass().getName(), userAnswer);

        Map<?, ?> userMap = (userAnswer instanceof Map) ? (Map<?, ?>) userAnswer : new HashMap<>();

        if (userAnswer instanceof String && !((String) userAnswer).isEmpty() && dto.getBlanks().size() == 1) {
            userMap = Map.of("0", userAnswer);
        }

        int totalBlanks = dto.getBlanks().size();
        int correctCount = 0;

        // ✅ NEW: Tạo map chi tiết từng blank
        Map<Integer, Map<String, Object>> blankDetails = new LinkedHashMap<>();

        for (CreateFillBlankDTO.BlankDTO blank : dto.getBlanks()) {
            int position = blank.getPosition();
            String userAns = getUserAnswerFlexible(userMap, position);
            String correctAnsDisplay = blank.getCorrectAnswers().isEmpty() ? "" : blank.getCorrectAnswers().get(0);

            log.debug("Blank position {}: userAnswer='{}', correctAnswers={}",
                    position, userAns, blank.getCorrectAnswers());

            boolean isCorrect = false;

            if (userAns != null && !userAns.isEmpty()) {
                isCorrect = blank.getCorrectAnswers().stream()
                        .anyMatch(ans -> {
                            boolean result = normalizeText(ans).equals(normalizeText(userAns));
                            log.debug("  Comparing '{}' vs '{}' → {}", ans, userAns, result);
                            return result;
                        });
            }

            if (isCorrect) {
                correctCount++;
                log.debug("  ✓ CORRECT");
            } else {
                log.debug("  ✗ INCORRECT");
            }

            // ✅ Lưu chi tiết từng blank
            Map<String, Object> detail = new HashMap<>();
            detail.put("position", position);
            detail.put("userAnswer", userAns != null ? userAns : "");
            detail.put("correctAnswer", correctAnsDisplay);
            detail.put("isCorrect", isCorrect);
            blankDetails.put(position, detail);
        }

        int maxPoints = builder.build().getPoints();
        int earnedPoints = (totalBlanks > 0) ? (int) Math.round(((double) correctCount / totalBlanks) * maxPoints) : 0;
        boolean allCorrect = correctCount == totalBlanks;

        log.info("FILL_BLANK result: {}/{} correct, points: {}, allCorrect: {}",
                correctCount, totalBlanks, earnedPoints, allCorrect);

        // ✅ Convert blankDetails to JSON string cho correctAnswer field
        String correctAnswerJson;
        try {
            correctAnswerJson = objectMapper.writeValueAsString(blankDetails);
        } catch (Exception e) {
            log.error("Failed to serialize blank details", e);
            correctAnswerJson = dto.getBlanks().stream()
                    .map(b -> b.getCorrectAnswers().isEmpty() ? "" : b.getCorrectAnswers().get(0))
                    .collect(Collectors.joining(", "));
        }

        return builder.isCorrect(allCorrect)
                .points(earnedPoints)
                .correctAnswer(correctAnswerJson) // ✅ Trả về JSON chi tiết
                .explanation(dto.getExplanation())
                .feedback(String.format("Bạn điền đúng %d/%d chỗ trống", correctCount, totalBlanks))
                .build();
    }

    private String getUserAnswerFlexible(Map<?, ?> map, int position) {
        if (map == null)
            return null;

        Object val = map.get(position);
        log.debug("  Try key={}: {}", position, val);

        if (val == null) {
            val = map.get(String.valueOf(position));
            log.debug("  Try key='{}': {}", position, val);
        }

        if (val == null && position > 0) {
            val = map.get(position - 1);
            log.debug("  Try key={}: {}", position - 1, val);
        }

        if (val == null && position > 0) {
            val = map.get(String.valueOf(position - 1));
            log.debug("  Try key='{}': {}", position - 1, val);
        }

        String result = val != null ? String.valueOf(val).trim() : null;
        log.debug("  Final result for position {}: '{}'", position, result);
        return result;
    }

    // =========================================================================
    // ✅ FIXED: MATCHING VALIDATION - Trả về chi tiết từng cặp
    // =========================================================================

    private QuestionResultDTO validateMatching(CreateMatchingDTO dto, Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {

        if (!(userAnswer instanceof Map)) {
            return builder.isCorrect(false).points(0)
                    .feedback("Định dạng câu trả lời không hợp lệ")
                    .build();
        }

        Map<?, ?> userMap = (Map<?, ?>) userAnswer;
        int totalPairs = dto.getPairs().size();
        int correctCount = 0;

        // ✅ NEW: Tạo map chi tiết từng cặp
        Map<String, Map<String, Object>> pairDetails = new LinkedHashMap<>();

        for (CreateMatchingDTO.PairDTO pair : dto.getPairs()) {
            String userVal = safeGetString(userMap, pair.getLeft());
            boolean isCorrect = normalizeText(userVal).equals(normalizeText(pair.getRight()));

            if (isCorrect) {
                correctCount++;
            }

            // ✅ Lưu chi tiết từng cặp
            Map<String, Object> detail = new HashMap<>();
            detail.put("left", pair.getLeft());
            detail.put("userRight", userVal);
            detail.put("correctRight", pair.getRight());
            detail.put("isCorrect", isCorrect);
            pairDetails.put(pair.getLeft(), detail);
        }

        int maxPoints = builder.build().getPoints();
        int earnedPoints = (totalPairs > 0) ? (int) Math.round(((double) correctCount / totalPairs) * maxPoints) : 0;
        boolean allCorrect = correctCount == totalPairs;

        // ✅ Convert pairDetails to JSON string
        String correctAnswerJson;
        try {
            correctAnswerJson = objectMapper.writeValueAsString(pairDetails);
        } catch (Exception e) {
            log.error("Failed to serialize pair details", e);
            correctAnswerJson = dto.getPairs().stream()
                    .map(p -> p.getLeft() + " → " + p.getRight())
                    .collect(Collectors.joining(", "));
        }

        return builder.isCorrect(allCorrect)
                .points(earnedPoints)
                .correctAnswer(correctAnswerJson) // ✅ Trả về JSON chi tiết
                .feedback(String.format("Bạn ghép đúng %d/%d cặp", correctCount, totalPairs))
                .build();
    }

    // =========================================================================
    // TEXT_ANSWER VALIDATION
    // =========================================================================

    private QuestionResultDTO validateTextAnswer(
            CreateFillBlankDTO dto,
            String textAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {

        log.debug("Validating TEXT_ANSWER - textAnswer: '{}'", textAnswer);

        if (textAnswer == null || textAnswer.isBlank()) {
            return builder.isCorrect(false).points(0)
                    .feedback("Chưa nhập câu trả lời").build();
        }

        if (dto.getBlanks() == null || dto.getBlanks().isEmpty()) {
            log.error("TEXT_ANSWER không có correctAnswers");
            return builder.isCorrect(false).points(0)
                    .feedback("Câu hỏi không có đáp án").build();
        }

        CreateFillBlankDTO.BlankDTO blank = dto.getBlanks().get(0);
        List<String> correctAnswers = blank.getCorrectAnswers();

        boolean isCorrect = correctAnswers.stream()
                .anyMatch(ans -> normalizeText(ans).equals(normalizeText(textAnswer)));

        String displayCorrect = correctAnswers.isEmpty() ? "" : correctAnswers.get(0);

        log.debug("TEXT_ANSWER result - userAnswer: '{}', correctAnswers: {}, isCorrect: {}",
                textAnswer, correctAnswers, isCorrect);

        return builder.isCorrect(isCorrect)
                .points(isCorrect ? builder.build().getPoints() : 0)
                .correctAnswer(displayCorrect)
                .explanation(dto.getExplanation())
                .feedback(isCorrect ? "Chính xác!" : "Chưa đúng")
                .build();
    }

    // =========================================================================
    // OPEN_ENDED VALIDATION
    // =========================================================================

    private QuestionResultDTO validateOpenEnded(
            CreateOpenEndedDTO dto,
            String textAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {

        log.debug("Validating OPEN_ENDED - textAnswer length: {}",
                textAnswer != null ? textAnswer.length() : 0);

        if (textAnswer == null || textAnswer.isBlank()) {
            return builder.isCorrect(false).points(0)
                    .feedback("Chưa nhập câu trả lời").build();
        }

        String trimmed = textAnswer.trim();
        if (trimmed.length() < 10) {
            return builder.isCorrect(false).points(0)
                    .feedback("Câu trả lời quá ngắn (tối thiểu 10 ký tự)").build();
        }

        String[] words = trimmed.split("\\s+");
        int wordCount = words.length;

        if (dto.getMinWord() != null && wordCount < dto.getMinWord()) {
            return builder.isCorrect(false).points(0)
                    .feedback(String.format("Câu trả lời quá ngắn (tối thiểu %d từ, bạn viết %d từ)",
                            dto.getMinWord(), wordCount))
                    .build();
        }

        if (dto.getMaxWord() != null && wordCount > dto.getMaxWord()) {
            return builder.isCorrect(false).points(0)
                    .feedback(String.format("Câu trả lời quá dài (tối đa %d từ, bạn viết %d từ)",
                            dto.getMaxWord(), wordCount))
                    .build();
        }

        log.info("OPEN_ENDED accepted - wordCount: {}, needsManualGrading: true", wordCount);

        return builder.isCorrect(true)
                .points(0)
                .userAnswer(textAnswer)
                .explanation(dto.getExplanation())
                .feedback(String.format("Đã ghi nhận câu trả lời (%d từ). Giáo viên sẽ chấm điểm sau.", wordCount))
                .build();
    }

    // =========================================================================
    // PRONUNCIATION VALIDATION
    // =========================================================================

    private QuestionResultDTO validatePronunciation(CreatePronunciationsDTO dto, Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {

        log.debug("Validating PRONUNCIATION - classifications: {}, userAnswer type: {}, value: {}",
                dto.getClassifications().size(), userAnswer != null ? userAnswer.getClass().getName() : "null",
                userAnswer);

        if (!(userAnswer instanceof Map)) {
            return builder.isCorrect(false).points(0).feedback("Định dạng câu trả lời không hợp lệ").build();
        }

        Map<?, ?> userMap = (Map<?, ?>) userAnswer;
        int totalWords = dto.getClassifications().size();
        int correctCount = 0;

        for (CreatePronunciationsDTO.ClassificationDTO classification : dto.getClassifications()) {
            String word = classification.getWord();
            String correctCategory = classification.getCategory();
            String userCategory = safeGetString(userMap, word);

            log.debug("Word '{}': expected='{}', user='{}'", word, correctCategory, userCategory);

            if (normalizeText(userCategory).equals(normalizeText(correctCategory))) {
                correctCount++;
                log.debug("  ✓ CORRECT");
            } else {
                log.debug("  ✗ INCORRECT");
            }
        }

        int maxPoints = builder.build().getPoints();
        int earnedPoints = (totalWords > 0) ? (int) Math.round(((double) correctCount / totalWords) * maxPoints) : 0;
        boolean allCorrect = correctCount == totalWords;

        log.info("PRONUNCIATION result: {}/{} correct, points: {}, allCorrect: {}",
                correctCount, totalWords, earnedPoints, allCorrect);

        return builder.isCorrect(allCorrect)
                .points(earnedPoints)
                .explanation(dto.getExplanation())
                .feedback(String.format("Bạn phân loại đúng %d/%d từ.", correctCount, totalWords))
                .build();
    }

    // =========================================================================
    // OTHER VALIDATION METHODS
    // =========================================================================

    private QuestionResultDTO validateMultipleChoice(CreateMultipleChoiceDTO dto, Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        if (userAnswer == null)
            return builder.isCorrect(false).points(0).feedback("Chưa chọn đáp án").build();

        String userStr = String.valueOf(userAnswer).trim();

        String correctAnswerText = dto.getOptions().stream()
                .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                .map(CreateMultipleChoiceDTO.OptionDTO::getText)
                .findFirst()
                .orElse("");

        boolean isCorrect = dto.getOptions().stream().anyMatch(opt -> {
            if (!Boolean.TRUE.equals(opt.getIsCorrect()))
                return false;
            boolean textMatch = normalizeText(opt.getText()).equals(normalizeText(userStr));
            boolean indexMatch = String.valueOf(dto.getOptions().indexOf(opt)).equals(userStr);
            return textMatch || indexMatch;
        });

        return builder.isCorrect(isCorrect)
                .points(isCorrect ? builder.build().getPoints() : 0)
                .correctAnswer(correctAnswerText)
                .explanation(dto.getExplanation())
                .feedback(isCorrect ? "Chính xác!" : "Sai rồi")
                .build();
    }

    private QuestionResultDTO validateSentenceBuilding(CreateSentenceBuildingDTO dto, String textAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        if (textAnswer == null || textAnswer.isBlank()) {
            return builder.isCorrect(false).points(0).feedback("Chưa nhập câu trả lời").build();
        }

        String correct = dto.getCorrectSentence();
        if (correct == null || correct.isBlank()) {
            if (dto.getWords() != null) {
                correct = String.join(" ", dto.getWords());
            } else {
                correct = "";
            }
        }

        boolean isCorrect = normalizeText(textAnswer).equals(normalizeText(correct));

        return builder.isCorrect(isCorrect)
                .points(isCorrect ? builder.build().getPoints() : 0)
                .correctAnswer(correct)
                .feedback(isCorrect ? "Chính xác!" : "Sai thứ tự từ")
                .build();
    }

    private QuestionResultDTO validateSentenceTransformation(CreateSentenceTransformationDTO dto, String textAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        if (textAnswer == null || textAnswer.isBlank()) {
            return builder.isCorrect(false).points(0).feedback("Chưa nhập câu trả lời").build();
        }

        String beginning = dto.getBeginningPhrase() != null ? dto.getBeginningPhrase().trim() : "";
        String fullUserAnswer = (beginning + " " + textAnswer).trim();

        boolean isCorrectSuffix = dto.getCorrectAnswers().stream()
                .anyMatch(ans -> normalizeText(ans).equals(normalizeText(fullUserAnswer)));

        boolean isCorrectFull = dto.getCorrectAnswers().stream()
                .anyMatch(ans -> normalizeText(ans).equals(normalizeText(textAnswer)));

        boolean isCorrect = isCorrectSuffix || isCorrectFull;
        String displayCorrect = dto.getCorrectAnswers().isEmpty() ? "" : dto.getCorrectAnswers().get(0);

        return builder.isCorrect(isCorrect)
                .points(isCorrect ? builder.build().getPoints() : 0)
                .correctAnswer(displayCorrect)
                .explanation(dto.getExplanation())
                .feedback(isCorrect ? "Chính xác!" : "Chưa đúng mẫu câu yêu cầu")
                .build();
    }

    private QuestionResultDTO validateErrorCorrection(CreateErrorCorrectionDTO dto, Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        if (!(userAnswer instanceof Map)) {
            if (userAnswer instanceof String) {
                userAnswer = parseAnswerToMapOrString((String) userAnswer);
            }
            if (!(userAnswer instanceof Map)) {
                return builder.isCorrect(false).points(0).feedback("Định dạng câu trả lời không hợp lệ").build();
            }
        }

        Map<?, ?> map = (Map<?, ?>) userAnswer;

        String userError = safeGetString(map, "error");
        String userCorrection = safeGetString(map, "correction");

        boolean errorMatch = normalizeText(dto.getErrorText()).equals(normalizeText(userError));
        boolean correctionMatch = normalizeText(dto.getCorrection()).equals(normalizeText(userCorrection));

        boolean isCorrect = errorMatch && correctionMatch;

        return builder.isCorrect(isCorrect)
                .points(isCorrect ? builder.build().getPoints() : 0)
                .correctAnswer("Sai: " + dto.getErrorText() + " -> Sửa: " + dto.getCorrection())
                .explanation(dto.getExplanation())
                .feedback(isCorrect ? "Chính xác!" : "Chưa tìm đúng lỗi hoặc sửa chưa đúng")
                .build();
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    private Object parseAnswerToMapOrString(String textAnswer) {
        if (textAnswer == null)
            return "";
        try {
            if (textAnswer.trim().startsWith("{")) {
                return objectMapper.readValue(textAnswer, Map.class);
            }
        } catch (Exception e) {
            // Không phải JSON, trả về nguyên chuỗi
        }
        return textAnswer;
    }

    private String normalizeText(String input) {
        if (input == null)
            return "";
        String normalized = input.trim().toLowerCase();

        if (normalized.endsWith(".") || normalized.endsWith("!") || normalized.endsWith("?")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        normalized = normalized.replaceAll("\\s+", " ");
        return normalized.trim();
    }

    private String safeGetString(Map<?, ?> map, Object key) {
        if (map == null || key == null)
            return "";
        Object val = map.get(key);
        return val != null ? String.valueOf(val).trim() : "";
    }
}