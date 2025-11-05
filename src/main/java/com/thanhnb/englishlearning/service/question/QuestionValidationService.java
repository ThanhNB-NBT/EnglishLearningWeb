package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.dto.question.QuestionDTO;
import com.thanhnb.englishlearning.enums.ParentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ✅ Service validate questions - Centralized validation logic
 */
@Service
@Slf4j
public class QuestionValidationService {

    /**
     * ✅ Validate question DTO based on type and parent type
     */
    public void validateQuestionDTO(QuestionDTO dto, ParentType parentType) {
        // Basic validation
        if (dto.getQuestionText() == null || dto.getQuestionText().trim().isEmpty()) {
            throw new RuntimeException("Nội dung câu hỏi không được để trống");
        }

        if (dto.getQuestionType() == null) {
            throw new RuntimeException("Loại câu hỏi không được để trống");
        }

        // Validate based on question type
        switch (dto.getQuestionType()) {
            case MULTIPLE_CHOICE:
                validateMultipleChoice(dto);
                break;

            case TRUE_FALSE:
                validateTrueFalse(dto);
                break;

            case FILL_BLANK:
            case SHORT_ANSWER:
                validateTextAnswer(dto);
                break;

            case TRANSLATE:
            case VERB_FORM:
                if (parentType != ParentType.GRAMMAR) {
                    throw new RuntimeException(
                            dto.getQuestionType() + " chỉ dùng cho bài học ngữ pháp");
                }
                validateTextAnswer(dto);
                break;

            default:
                throw new RuntimeException("Loại câu hỏi không hợp lệ: " + dto.getQuestionType());
        }
    }

    /**
     * ✅ Validate MULTIPLE_CHOICE
     */
    private void validateMultipleChoice(QuestionDTO dto) {
        if (dto.getOptions() == null || dto.getOptions().size() < 2) {
            throw new RuntimeException("Câu hỏi trắc nghiệm phải có ít nhất 2 lựa chọn");
        }

        if (dto.getOptions().size() > 6) {
            throw new RuntimeException("Câu hỏi trắc nghiệm không được có quá 6 lựa chọn");
        }

        // Check empty options
        boolean hasEmptyOption = dto.getOptions().stream()
                .anyMatch(opt -> opt.getOptionText() == null || opt.getOptionText().trim().isEmpty());
        if (hasEmptyOption) {
            throw new RuntimeException("Tất cả lựa chọn phải có nội dung");
        }

        // Check correct answer
        long correctCount = dto.getOptions().stream()
                .filter(opt -> opt.getIsCorrect() != null && opt.getIsCorrect())
                .count();

        if (correctCount == 0) {
            throw new RuntimeException("Phải có ít nhất 1 đáp án đúng");
        }

        if (correctCount > 1) {
            throw new RuntimeException("Chỉ được có 1 đáp án đúng cho câu trắc nghiệm");
        }
    }

    /**
     * ✅ Validate TRUE_FALSE
     */
    private void validateTrueFalse(QuestionDTO dto) {
        if (dto.getOptions() == null || dto.getOptions().size() != 2) {
            throw new RuntimeException("Câu hỏi Đúng/Sai phải có đúng 2 lựa chọn");
        }

        // ✅ AUTO-NORMALIZE option texts to uppercase
        dto.getOptions().forEach(opt -> {
            if (opt.getOptionText() != null) {
                String normalized = opt.getOptionText().trim().toUpperCase();
                opt.setOptionText(normalized);
            }
        });

        // Check option texts are TRUE/FALSE
        boolean hasTrue = dto.getOptions().stream()
                .anyMatch(opt -> opt.getOptionText() != null &&
                        opt.getOptionText().trim().equalsIgnoreCase("TRUE"));

        boolean hasFalse = dto.getOptions().stream()
                .anyMatch(opt -> opt.getOptionText() != null &&
                        opt.getOptionText().trim().equalsIgnoreCase("FALSE"));

        if (!hasTrue || !hasFalse) {
            throw new RuntimeException("Câu hỏi Đúng/Sai phải có đúng 2 lựa chọn: TRUE và FALSE");
        }

        // Check exactly 1 correct answer
        long correctCount = dto.getOptions().stream()
                .filter(opt -> opt.getIsCorrect() != null && opt.getIsCorrect())
                .count();

        if (correctCount != 1) {
            throw new RuntimeException("Câu hỏi Đúng/Sai phải có đúng 1 đáp án đúng");
        }
    }

    /**
     * ✅ Validate text-based answers (FILL_BLANK, SHORT_ANSWER, etc.)
     */
    private void validateTextAnswer(QuestionDTO dto) {
        if (dto.getCorrectAnswer() == null || dto.getCorrectAnswer().trim().isEmpty()) {
            throw new RuntimeException("Đáp án đúng không được để trống cho loại câu hỏi " + dto.getQuestionType());
        }

        // Check correctAnswer length
        if (dto.getCorrectAnswer().length() > 500) {
            throw new RuntimeException("Đáp án đúng không được vượt quá 500 ký tự");
        }
    }
}