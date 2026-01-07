package com.thanhnb.englishlearning.service.question;

import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateFillBlankDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateMatchingDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateMultipleChoiceDTO;
import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerValidationService answerValidationService;

    // =========================================================================
    // LOAD OPERATIONS
    // =========================================================================

    /**
     * Load questions by parent
     */
    public List<Question> loadQuestionsByParent(ParentType parentType, Long parentId) {
        return questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(parentType, parentId);
    }

    /**
     * Count questions by parent
     */
    public long countQuestionsByParent(ParentType parentType, Long parentId) {
        return questionRepository.countByParentTypeAndParentId(parentType, parentId);
    }

    /**
     * Get single question by ID
     */
    public Question getQuestionById(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại với id: " + questionId));
    }

    /**
     * Get questions by IDs
     */
    public List<Question> getQuestionsByIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return questionRepository.findAllById(questionIds);
    }

    // =========================================================================
    // CONVERSION OPERATIONS (Simple - No Converter needed)
    // =========================================================================

    /**
     * Convert Question entity to QuestionResponseDTO
     * Direct mapping, no complex conversion
     */
    public QuestionResponseDTO convertToDTO(Question question) {
        return QuestionResponseDTO.builder()
                .id(question.getId())
                .parentType(question.getParentType())
                .parentId(question.getParentId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .points(question.getPoints())
                .orderIndex(question.getOrderIndex())
                .createdAt(question.getCreatedAt())
                .data(question.getData())
                .build();
    }

    /**
     * Convert list of questions to DTOs
     */
    public List<QuestionResponseDTO> convertToDTOs(List<Question> questions) {
        if (questions == null)
            return List.of();
        return questions.stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Convert with optional shuffle (for compatibility)
     * Note: Shuffle logic removed - frontend should handle randomization
     */
    @Deprecated
    public List<QuestionResponseDTO> convertToDTOs(List<Question> questions, boolean shuffleOptions) {
        // Shuffle removed - frontend handles randomization for better UX
        return convertToDTOs(questions);
    }

    // =========================================================================
    // ANSWER PROCESSING
    // =========================================================================

    @Transactional(readOnly = true)
    public List<QuestionResultDTO> processAnswers(List<SubmitAnswerRequest> answers, ParentType parentType) {
        if (answers == null || answers.isEmpty()) {
            return Collections.emptyList();
        }

        List<QuestionResultDTO> results = new ArrayList<>();

        for (SubmitAnswerRequest request : answers) {
            try {
                // 1. Tìm câu hỏi trong DB
                Question question = questionRepository.findById(request.getQuestionId())
                        .orElse(null);

                if (question == null) {
                    log.warn("Question ID {} not found during submission processing", request.getQuestionId());
                    continue;
                }

                // 2. Validate (Gọi hàm mới đã update ở AnswerValidationService)
                QuestionResultDTO result = answerValidationService.validateAnswer(
                        question,
                        request.getSelectedOptions(),
                        request.getTextAnswer());

                // 3. Đảm bảo result có đủ thông tin (nếu AnswerValidationService chưa điền đủ)
                // (Thường validateAnswer đã trả về đủ, nhưng gán lại userAnswer để hiển thị lại
                // cho user)

                // Nếu result builder ở service kia chưa set UserAnswer thì set thủ công ở đây
                // để hiển thị
                if (result.getUserAnswer() == null) {
                    String displayAnswer = "";
                    if (request.getTextAnswer() != null) {
                        displayAnswer = request.getTextAnswer();
                    } else if (request.getSelectedOptions() != null) {
                        displayAnswer = request.getSelectedOptions().toString();
                    }
                    result.setUserAnswer(displayAnswer);
                }

                results.add(result);

            } catch (Exception e) {
                log.error("Error processing answer for question ID {}", request.getQuestionId(), e);
                // Add error result để không làm gián đoạn toàn bộ bài thi
                results.add(QuestionResultDTO.builder()
                        .questionId(request.getQuestionId())
                        .isCorrect(false)
                        .points(0)
                        .feedback("Error processing this answer")
                        .build());
            }
        }

        return results;
    }

    /**
     * Validate answer count
     */
    public void validateAnswerCount(
            List<SubmitAnswerRequest> answers,
            ParentType parentType,
            Long parentId) {

        long expectedCount = countQuestionsByParent(parentType, parentId);

        if (answers == null || answers.isEmpty()) {
            throw new RuntimeException("Bài này cần có câu trả lời");
        }

        if (answers.size() != expectedCount) {
            log.warn("Answer count mismatch: expected {}, got {}", expectedCount, answers.size());
        }
    }

    /**
     * Validate all questions belong to specific parent
     */
    public void validateQuestionsParent(
            List<Long> questionIds,
            ParentType parentType,
            Long parentId) {

        List<Question> questions = getQuestionsByIds(questionIds);

        boolean allMatch = questions.stream()
                .allMatch(q -> q.getParentType() == parentType && q.getParentId().equals(parentId));

        if (!allMatch) {
            throw new RuntimeException(
                    String.format("Một số câu hỏi không thuộc %s với id %d",
                            parentType, parentId));
        }
    }

    // =========================================================================
    // SHUFFLE METHODS FOR USER ENDPOINTS
    // =========================================================================

    /**
     * Convert questions for user learning (with shuffle)
     * SECURITY: Shuffle options to prevent pattern recognition
     */
    public List<QuestionResponseDTO> convertToDTOsForLearning(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }

        return questions.stream()
                .map(this::convertToDTOWithShuffle)
                .collect(Collectors.toList());
    }

    /**
     * Convert single question with shuffle applied
     */
    private QuestionResponseDTO convertToDTOWithShuffle(Question question) {
        QuestionResponseDTO dto = convertToDTO(question);

        // Apply shuffle based on question type
        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE ||
                question.getQuestionType() == QuestionType.TRUE_FALSE) {
            shuffleMultipleChoiceOptions(dto);
        } else if (question.getQuestionType() == QuestionType.MATCHING) {
            shuffleMatchingItems(dto);
        } else if (question.getQuestionType() == QuestionType.FILL_BLANK ||
                question.getQuestionType() == QuestionType.VERB_FORM) {
            shuffleFillBlankWordBank(dto);
        }

        return dto;
    }

    /**
     * Shuffle Multiple Choice options (keeps order field for identification)
     */
    private void shuffleMultipleChoiceOptions(QuestionResponseDTO dto) {
        if (dto.getData() instanceof CreateMultipleChoiceDTO mcDto) {
            List<CreateMultipleChoiceDTO.OptionDTO> options = mcDto.getOptions();
            if (options != null && options.size() > 1) {
                // Shuffle the list
                Collections.shuffle(options);
                log.debug("Shuffled {} options for question {}", options.size(), dto.getId());
            }
        }
    }

    /**
     * Shuffle Matching right items (left stays in order)
     */
    private void shuffleMatchingItems(QuestionResponseDTO dto) {
        if (dto.getData() instanceof CreateMatchingDTO matchDto) {
            // 1. Nếu left/right chưa có dữ liệu (do DB chỉ lưu pairs), hãy trích xuất từ
            // pairs
            if ((matchDto.getLeftItems() == null || matchDto.getLeftItems().isEmpty())
                    && matchDto.getPairs() != null) {

                List<String> left = matchDto.getPairs().stream()
                        .map(CreateMatchingDTO.PairDTO::getLeft)
                        .collect(Collectors.toList());

                List<String> right = matchDto.getPairs().stream()
                        .map(CreateMatchingDTO.PairDTO::getRight)
                        .collect(Collectors.toList());

                matchDto.setLeftItems(left);
                matchDto.setRightItems(right);
            }

            // 2. Xáo trộn cột phải
            List<String> rightItems = matchDto.getRightItems();
            if (rightItems != null && !rightItems.isEmpty()) {
                Collections.shuffle(rightItems);
            }
        }
    }

    /**
     * Shuffle Fill Blank word bank
     */
    private void shuffleFillBlankWordBank(QuestionResponseDTO dto) {
        if (dto.getData() instanceof CreateFillBlankDTO fbDto) {
            List<String> wordBank = fbDto.getWordBank();
            if (wordBank != null && wordBank.size() > 1) {
                Collections.shuffle(wordBank);
                fbDto.setWordBank(wordBank);
                log.debug("Shuffled {} words in word bank for question {}", wordBank.size(), dto.getId());
            }
        }
    }

    // =========================================================================
    // SCORING UTILITIES
    // =========================================================================

    /**
     * Calculate total score from results
     */
    public int calculateTotalScore(List<QuestionResultDTO> results) {
        return results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                .filter(r -> r.getPoints() != null)
                .mapToInt(QuestionResultDTO::getPoints)
                .sum();
    }

    /**
     * Calculate correct count
     */
    public int calculateCorrectCount(List<QuestionResultDTO> results) {
        return (int) results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                .count();
    }

    /**
     * Calculate score percentage
     */
    public double calculateScorePercentage(int correctCount, int totalQuestions) {
        if (totalQuestions == 0) {
            return 0;
        }
        return (double) correctCount / totalQuestions * 100;
    }

    /**
     * Calculate score percentage from results
     */
    public double calculateScorePercentage(List<QuestionResultDTO> results) {
        if (results == null || results.isEmpty()) {
            return 0;
        }
        int correctCount = calculateCorrectCount(results);
        return calculateScorePercentage(correctCount, results.size());
    }
}