package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.dto.question.QuestionDTO;
import com.thanhnb.englishlearning.dto.question.QuestionOptionDTO;
import com.thanhnb.englishlearning.dto.question.QuestionResultDTO;
import com.thanhnb.englishlearning.dto.question.SubmitAnswerRequest;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.entity.question.QuestionOption;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.repository.question.QuestionOptionRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ✅ Service xử lý questions chung cho tất cả module
 * Chứa logic:
 * - Convert Question entity -> DTO
 * - Shuffle options cho MULTIPLE_CHOICE
 * - Process answers và tính điểm
 * - Load questions by parent
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final AnswerCheckingService answerCheckingService;

    /**
     * ✅ Load questions by parentType and parentId
     */
    public List<Question> loadQuestionsByParent(ParentType parentType, Long parentId) {
        return questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(parentType, parentId);
    }

    /**
     * ✅ Count questions by parentType and parentId
     */
    public long countQuestionsByParent(ParentType parentType, Long parentId) {
        return questionRepository.countByParentTypeAndParentId(parentType, parentId);
    }

    /**
     * ✅ Convert Question entity -> QuestionDTO
     * Tự động shuffle options cho MULTIPLE_CHOICE
     * 
     * @param question          Question entity
     * @param showCorrectAnswer Hiển thị đáp án đúng hay không
     */
    public QuestionDTO convertToDTO(Question question, boolean showCorrectAnswer) {
        List<QuestionOption> options = questionOptionRepository
                .findByQuestionIdOrderByOrderIndexAsc(question.getId());

        List<QuestionOptionDTO> optionDTOs = options.stream()
                .map(option -> new QuestionOptionDTO(
                        option.getId(),
                        question.getId(),
                        option.getOptionText(),
                        option.getIsCorrect(),
                        option.getOrderIndex()))
                .collect(Collectors.toList());

        // ✅ Shuffle options cho MULTIPLE_CHOICE để tránh gian lận
        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE && !optionDTOs.isEmpty()) {
            Collections.shuffle(optionDTOs);
            // Re-index after shuffle
            for (int i = 0; i < optionDTOs.size(); i++) {
                optionDTOs.get(i).setOrderIndex(i + 1);
            }
            log.debug("🔀 Shuffled {} options for question {}", optionDTOs.size(), question.getId());
        }

        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setLessonId(question.getParentId());
        dto.setQuestionText(question.getQuestionText());
        dto.setQuestionType(question.getQuestionType());
        dto.setCorrectAnswer(question.getCorrectAnswer());
        dto.setExplanation(question.getExplanation());
        dto.setPoints(question.getPoints());
        dto.setOrderIndex(question.getOrderIndex());
        dto.setCreatedAt(question.getCreatedAt());
        dto.setOptions(optionDTOs.isEmpty() ? null : optionDTOs);
        dto.setShowCorrectAnswer(showCorrectAnswer);

        return dto;
    }

    /**
     * ✅ Convert Question entity -> QuestionDTO (default show correct answer)
     */
    public QuestionDTO convertToDTO(Question question) {
        return convertToDTO(question, true);
    }

    /**
     * ✅ Convert list of questions to DTOs
     */
    public List<QuestionDTO> convertToDTOs(List<Question> questions, boolean showCorrectAnswer) {
        return questions.stream()
                .map(q -> convertToDTO(q, showCorrectAnswer))
                .collect(Collectors.toList());
    }

    /**
     * ✅ Process answers và trả về kết quả chi tiết
     * 
     * @param answers            Danh sách câu trả lời của user
     * @param expectedParentType ParentType expected (GRAMMAR/READING/LISTENING)
     * @return Danh sách kết quả chi tiết từng câu
     */
    public List<QuestionResultDTO> processAnswers(
            List<SubmitAnswerRequest> answers,
            ParentType expectedParentType) {

        return answers.stream().map(answerRequest -> {
            // Load question với options
            Question question = questionRepository.findByIdWithOptions(answerRequest.getQuestionId())
                    .orElseThrow(() -> new RuntimeException(
                            "Question không tồn tại với id: " + answerRequest.getQuestionId()));

            // Validate parentType
            if (question.getParentType() != expectedParentType) {
                throw new RuntimeException(
                        String.format("Question %d không thuộc %s module",
                                question.getId(), expectedParentType));
            }

            // Check answer
            boolean isCorrect = answerCheckingService.checkAnswer(question, answerRequest);
            int points = isCorrect ? question.getPoints() : 0;
            String hint = isCorrect ? null : answerCheckingService.generateHint(question, answerRequest);
            String correctAnswerDisplay = getCorrectAnswerDisplay(question);
            log.debug("Question {}: {} -> {} points",
                    question.getId(), isCorrect ? "✅ Correct" : "❌ Wrong", points);

            return new QuestionResultDTO(
                    question.getId(),
                    question.getQuestionText(),
                    answerRequest.getAnswer(),
                    correctAnswerDisplay,
                    isCorrect,
                    question.getExplanation(),
                    points,
                    hint);
        }).collect(Collectors.toList());
    }

    private String getCorrectAnswerDisplay(Question question) {
        // For MULTIPLE_CHOICE and TRUE_FALSE: get from options
        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE || 
            question.getQuestionType() == QuestionType.TRUE_FALSE) {
            
            return question.getOptions().stream()
                    .filter(opt -> opt.getIsCorrect() != null && opt.getIsCorrect())
                    .map(QuestionOption::getOptionText)
                    .findFirst()
                    .orElse("N/A");
        }
        
        // For other types: use correctAnswer field
        return question.getCorrectAnswer() != null ? question.getCorrectAnswer() : "N/A";
    }

    /**
     * ✅ Validate answer count
     * 
     * @throws RuntimeException if answer count mismatch
     */
    public void validateAnswerCount(List<SubmitAnswerRequest> answers,
            ParentType parentType,
            Long parentId) {
        long expectedCount = countQuestionsByParent(parentType, parentId);

        if (answers == null || answers.isEmpty()) {
            throw new RuntimeException("Bài này cần có câu trả lời");
        }

        if (answers.size() < expectedCount) {
            throw new RuntimeException(
                    String.format("Vui lòng trả lời tất cả %d câu hỏi", expectedCount));
        }

        log.debug("✅ Validated {} answers (expected: {})", answers.size(), expectedCount);
    }

    /**
     * ✅ Calculate total score from results
     */
    public int calculateTotalScore(List<QuestionResultDTO> results) {
        return results.stream()
                .mapToInt(QuestionResultDTO::points)
                .sum();
    }

    /**
     * ✅ Calculate correct count from results
     */
    public int calculateCorrectCount(List<QuestionResultDTO> results) {
        return (int) results.stream()
                .filter(QuestionResultDTO::isCorrect)
                .count();
    }

    /**
     * ✅ Calculate score percentage
     */
    public double calculateScorePercentage(int correctCount, int totalQuestions) {
        if (totalQuestions == 0) {
            return 0;
        }
        return (double) correctCount / totalQuestions * 100;
    }
}