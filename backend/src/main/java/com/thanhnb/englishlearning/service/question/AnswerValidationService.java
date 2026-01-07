package com.thanhnb.englishlearning.service.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.dto.question.request.*;
import com.thanhnb.englishlearning.entity.question.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
                // === TRẮC NGHIỆM ===
                case MULTIPLE_CHOICE:
                case TRUE_FALSE:
                case PRONUNCIATION:
                case COMPLETE_CONVERSATION:
                    if (data instanceof CreateMultipleChoiceDTO dto) {
                        Object answer = (selectedOptions != null && !selectedOptions.isEmpty()) ? selectedOptions.get(0)
                                : textAnswer;
                        return validateMultipleChoice(dto, answer, builder);
                    }
                    break;

                // === ĐIỀN TỪ (List / Inline) ===
                case FILL_BLANK:
                case VERB_FORM:
                    if (data instanceof CreateFillBlankDTO dto) {
                        Object parsedAnswer = parseAnswerToMapOrString(textAnswer);
                        return validateFillBlank(dto, parsedAnswer, builder);
                    }
                    break;

                // === NỐI CÂU ===
                case MATCHING:
                    if (data instanceof CreateMatchingDTO dto) {
                        Object parsedMap = parseAnswerToMapOrString(textAnswer);
                        return validateMatching(dto, parsedMap, builder);
                    }
                    break;

                // === SẮP XẾP CÂU (ĐÃ SỬA) ===
                case SENTENCE_BUILDING:
                    if (data instanceof CreateSentenceBuildingDTO dto) {
                        return validateSentenceBuilding(dto, textAnswer, builder);
                    }
                    break;

                // === VIẾT LẠI CÂU ===
                case SENTENCE_TRANSFORMATION:
                    if (data instanceof CreateSentenceTransformationDTO dto) {
                        return validateSentenceTransformation(dto, textAnswer, builder);
                    }
                    break;

                // === TÌM LỖI SAI (ĐÃ SỬA) ===
                case ERROR_CORRECTION:
                    if (data instanceof CreateErrorCorrectionDTO dto) {
                        Object parsedAnswer = parseAnswerToMapOrString(textAnswer);
                        return validateErrorCorrection(dto, parsedAnswer, builder);
                    }
                    break;

                // === TỰ LUẬN ===
                case TEXT_ANSWER:
                case OPEN_ENDED:
                    return builder.isCorrect(true).points(question.getPoints()).feedback("Đã ghi nhận câu trả lời")
                            .build();

                default:
                    return builder.isCorrect(false).points(0).feedback("Loại câu hỏi chưa hỗ trợ chấm tự động").build();
            }
        } catch (Exception e) {
            log.error("Lỗi chấm điểm câu hỏi ID {}: {}", question.getId(), e.getMessage());
            return builder.isCorrect(false).points(0).feedback("Lỗi hệ thống khi chấm điểm").build();
        }

        return builder.isCorrect(false).points(0).feedback("Dữ liệu không khớp").build();
    }

    // =========================================================================
    // CÁC HÀM CHẤM ĐIỂM CHI TIẾT
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

    private QuestionResultDTO validateFillBlank(CreateFillBlankDTO dto, Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        Map<?, ?> userMap = (userAnswer instanceof Map) ? (Map<?, ?>) userAnswer : new HashMap<>();

        // Fix: Nếu user gửi string đơn cho câu hỏi 1 chỗ trống
        if (userAnswer instanceof String && !((String) userAnswer).isEmpty() && dto.getBlanks().size() == 1) {
            userMap = Map.of("0", userAnswer);
        }

        int totalBlanks = dto.getBlanks().size();
        int correctCount = 0;

        for (CreateFillBlankDTO.BlankDTO blank : dto.getBlanks()) {
            int position = blank.getPosition();
            // Hàm lấy đáp án linh hoạt (position 1 vs index 0)
            String userAns = getUserAnswerFlexible(userMap, position);

            if (userAns != null) {
                boolean match = blank.getCorrectAnswers().stream()
                        .anyMatch(ans -> normalizeText(ans).equals(normalizeText(userAns)));
                if (match)
                    correctCount++;
            }
        }

        int maxPoints = builder.build().getPoints();
        int earnedPoints = (totalBlanks > 0) ? (int) Math.round(((double) correctCount / totalBlanks) * maxPoints) : 0;
        boolean allCorrect = correctCount == totalBlanks;

        return builder.isCorrect(allCorrect)
                .points(earnedPoints)
                .explanation(dto.getExplanation())
                .feedback(String.format("Bạn điền đúng %d/%d chỗ trống.", correctCount, totalBlanks))
                .build();
    }

    private QuestionResultDTO validateSentenceBuilding(CreateSentenceBuildingDTO dto, String textAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        if (textAnswer == null || textAnswer.isBlank()) {
            return builder.isCorrect(false).points(0).feedback("Chưa nhập câu trả lời").build();
        }

        // DTO của bạn có trường correctSentence, nên gọi getCorrectSentence() hoàn toàn
        // hợp lệ
        String correct = dto.getCorrectSentence();

        // Fallback: Nếu correctSentence null, dùng danh sách words (giả định words là
        // thứ tự đúng)
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

        // FIX: Dùng safeGetString thay vì getOrDefault để tránh lỗi Generics
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

    private QuestionResultDTO validateMatching(CreateMatchingDTO dto, Object userAnswer,
            QuestionResultDTO.QuestionResultDTOBuilder builder) {
        if (!(userAnswer instanceof Map))
            return builder.isCorrect(false).points(0).build();

        Map<?, ?> userMap = (Map<?, ?>) userAnswer;
        int totalPairs = dto.getPairs().size();
        int correctCount = 0;

        for (CreateMatchingDTO.PairDTO pair : dto.getPairs()) {
            // FIX: Dùng safeGetString thay vì getOrDefault
            String userVal = safeGetString(userMap, pair.getLeft());

            if (normalizeText(userVal).equals(normalizeText(pair.getRight()))) {
                correctCount++;
            }
        }

        int maxPoints = builder.build().getPoints();
        int earnedPoints = (totalPairs > 0) ? (int) Math.round(((double) correctCount / totalPairs) * maxPoints) : 0;
        boolean allCorrect = correctCount == totalPairs;

        return builder.isCorrect(allCorrect)
                .points(earnedPoints)
                .feedback(String.format("Bạn ghép đúng %d/%d cặp", correctCount, totalPairs))
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

    // FIX: Helper method xử lý Map<?,?> an toàn, thay thế getOrDefault gây lỗi
    private String safeGetString(Map<?, ?> map, Object key) {
        if (map == null || key == null)
            return "";
        Object val = map.get(key);
        return val != null ? String.valueOf(val).trim() : "";
    }

    private String getUserAnswerFlexible(Map<?, ?> map, int position) {
        Object val = map.get(position);
        if (val == null)
            val = map.get(String.valueOf(position));

        if (val == null) {
            val = map.get(position - 1);
            if (val == null)
                val = map.get(String.valueOf(position - 1));
        }

        return val != null ? String.valueOf(val).trim() : null;
    }
}