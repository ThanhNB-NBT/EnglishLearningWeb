package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.dto.question.QuestionDTO;
import com.thanhnb.englishlearning.dto.question.QuestionOptionDTO;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.entity.question.QuestionOption;
import com.thanhnb.englishlearning.repository.question.QuestionOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ✅ Service chuyển đổi Entity <-> DTO
 * Centralized conversion logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionConversionService {

    private final QuestionOptionRepository optionRepository;

    /**
     * ✅ Convert Question entity -> QuestionDTO
     */
    public QuestionDTO convertToDTO(Question question) {
        return convertToDTO(question, true);
    }

    /**
     * ✅ Convert Question entity -> QuestionDTO với option show correct answer
     */
    public QuestionDTO convertToDTO(Question question, boolean showCorrectAnswer) {
        List<QuestionOption> options = optionRepository
                .findByQuestionIdOrderByOrderIndexAsc(question.getId());

        List<QuestionOptionDTO> optionDTOs = options.stream()
                .map(option -> new QuestionOptionDTO(
                        option.getId(),
                        question.getId(),
                        option.getOptionText(),
                        showCorrectAnswer ? option.getIsCorrect() : null, // ✅ Hide correct if needed
                        option.getOrderIndex()))
                .collect(Collectors.toList());

        return new QuestionDTO(
                question.getId(),
                question.getParentId(),
                question.getQuestionText(),
                question.getQuestionType(),
                showCorrectAnswer ? question.getCorrectAnswer() : null, // ✅ Hide correct if needed
                question.getExplanation(),
                question.getPoints(),
                question.getOrderIndex(),
                question.getCreatedAt(),
                optionDTOs.isEmpty() ? null : optionDTOs,
                showCorrectAnswer);
    }

    /**
     * ✅ Convert list of questions to DTOs
     */
    public List<QuestionDTO> convertToDTOs(List<Question> questions) {
        return convertToDTOs(questions, true);
    }

    /**
     * ✅ Convert list of questions to DTOs với option show correct answer
     */
    public List<QuestionDTO> convertToDTOs(List<Question> questions, boolean showCorrectAnswer) {
        return questions.stream()
                .map(q -> convertToDTO(q, showCorrectAnswer))
                .collect(Collectors.toList());
    }
}