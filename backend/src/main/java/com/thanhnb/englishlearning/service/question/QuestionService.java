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

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * QUESTION SERVICE - Quản lý câu hỏi và xử lý trả lời
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * CHỨC NĂNG CHÍNH:
 * 1. Load câu hỏi từ database
 * 2. Convert Entity → DTO (có/không shuffle)
 * 3. Xử lý và chấm điểm câu trả lời
 * 4. Tính toán điểm số và thống kê
 * 
 * LUỒNG XỬ LÝ:
 * [Frontend Request] → [Load Questions] → [Convert to DTO] → [Shuffle (optional)]
 *                                                                      ↓
 * [Frontend Display] ← [Return DTOs] ←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←
 * 
 * [User Submit] → [Process Answers] → [Validate] → [Calculate Score] → [Results]
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerValidationService answerValidationService;

    // =========================================================================
    // LOAD OPERATIONS - Lấy dữ liệu từ Database
    // =========================================================================
    // Mục đích: Truy vấn câu hỏi từ DB theo điều kiện khác nhau
    // Sử dụng: Cả Admin (quản lý) và User (làm bài)
    // =========================================================================

    /**
     * Load tất cả câu hỏi của một parent (lesson/topic)
     * 
     * FLOW: DB → List<Question> (sorted by orderIndex)
     * 
     * @param parentType Loại parent (GRAMMAR, READING, LISTENING, etc.)
     * @param parentId ID của parent (lessonId)
     * @return Danh sách câu hỏi đã sắp xếp theo thứ tự
     */
    public List<Question> loadQuestionsByParent(ParentType parentType, Long parentId) {
        return questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(parentType, parentId);
    }

    /**
     * Đếm số lượng câu hỏi của một parent
     * 
     * Sử dụng: Validate số lượng câu trả lời khi submit
     */
    public long countQuestionsByParent(ParentType parentType, Long parentId) {
        return questionRepository.countByParentTypeAndParentId(parentType, parentId);
    }

    /**
     * Lấy 1 câu hỏi theo ID
     * 
     * Sử dụng: Xem chi tiết, chỉnh sửa
     * @throws RuntimeException nếu không tìm thấy
     */
    public Question getQuestionById(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại với id: " + questionId));
    }

    /**
     * Lấy nhiều câu hỏi theo danh sách IDs
     * 
     * Sử dụng: Bulk operations, validation
     */
    public List<Question> getQuestionsByIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return questionRepository.findAllById(questionIds);
    }

    // =========================================================================
    // CONVERSION OPERATIONS - Chuyển đổi Entity → DTO
    // =========================================================================
    // Mục đích: 
    // - Entity (DB) chứa tất cả dữ liệu kể cả đáp án đúng
    // - DTO (Response) chỉ trả về dữ liệu cần thiết cho từng role
    // 
    // PHÂN BIỆT:
    // - convertToDTO(): Không shuffle, dùng cho ADMIN
    // - convertToDTOsForLearning(): CÓ shuffle, dùng cho USER làm bài
    // =========================================================================

    /**
     * Convert 1 Question Entity → DTO (KHÔNG SHUFFLE)
     * 
     * FLOW: Question (DB) → QuestionResponseDTO (API Response)
     * 
     * Sử dụng:
     * - Admin xem/sửa câu hỏi (cần giữ nguyên thứ tự)
     * - Preview trước khi lưu
     * 
     * @param question Entity từ database
     * @return DTO chứa đầy đủ thông tin (theo JsonView)
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
                .data(question.getData()) // Deserialize JSON → QuestionData object
                .build();
    }

    /**
     * Convert nhiều Questions → DTOs (KHÔNG SHUFFLE)
     * 
     * Sử dụng: Admin quản lý danh sách câu hỏi
     */
    public List<QuestionResponseDTO> convertToDTOs(List<Question> questions) {
        if (questions == null) return List.of();
        return questions.stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * @deprecated Legacy method, không còn sử dụng shuffle flag
     * Giữ lại để backward compatibility
     */
    @Deprecated
    public List<QuestionResponseDTO> convertToDTOs(List<Question> questions, boolean shuffleOptions) {
        return convertToDTOs(questions);
    }

    // =========================================================================
    // ANSWER PROCESSING - Xử lý và chấm điểm câu trả lời
    // =========================================================================
    // FLOW: [User Submit] → Validate → Calculate Score → Return Results
    // 
    // THỨ TỰ CHẠY:
    // 1. Frontend gửi List<SubmitAnswerRequest> (questionId + answers)
    // 2. Load Questions từ DB để lấy đáp án đúng
    // 3. Loop qua từng câu, so sánh với AnswerValidationService
    // 4. Tính điểm, tạo QuestionResultDTO cho từng câu
    // 5. Return danh sách results về Frontend
    // =========================================================================

    /**
     * Xử lý danh sách câu trả lời và chấm điểm
     * 
     * FLOW:
     * ┌──────────────────────────────────────────────────────────────┐
     * │ 1. Nhận answers từ User                                      │
     * │ 2. Load Questions từ DB (có đáp án đúng)                     │
     * │ 3. Map answers theo questionId                               │
     * │ 4. Loop: Validate từng câu bằng AnswerValidationService      │
     * │ 5. Tạo QuestionResultDTO (isCorrect, points, feedback)       │
     * │ 6. Return List<QuestionResultDTO>                            │
     * └──────────────────────────────────────────────────────────────┘
     * 
     * @param answers Danh sách câu trả lời từ User
     * @param parentType Loại bài học (để tracking)
     * @return Danh sách kết quả từng câu (đúng/sai, điểm, phản hồi)
     */
    @Transactional(readOnly = true)
    public List<QuestionResultDTO> processAnswers(List<SubmitAnswerRequest> answers, ParentType parentType) {
        if (answers == null || answers.isEmpty()) {
            return Collections.emptyList();
        }

        List<QuestionResultDTO> results = new ArrayList<>();

        for (SubmitAnswerRequest request : answers) {
            try {
                // Bước 1: Tìm câu hỏi trong DB (có đáp án đúng)
                Question question = questionRepository.findById(request.getQuestionId())
                        .orElse(null);

                if (question == null) {
                    log.warn("Question ID {} not found during submission processing", request.getQuestionId());
                    continue;
                }

                // Bước 2: Validate câu trả lời (Gọi AnswerValidationService)
                // Service này sẽ:
                // - So sánh câu trả lời user với đáp án đúng
                // - Tính điểm cho câu hỏi
                // - Tạo feedback
                QuestionResultDTO result = answerValidationService.validateAnswer(
                        question,
                        request.getSelectedOptions(), // Cho Multiple Choice
                        request.getTextAnswer());      // Cho Fill Blank, Open Ended

                // Bước 3: Đảm bảo result có userAnswer để hiển thị lại cho user
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
                // Bước 4: Error handling - không làm gián đoạn toàn bộ bài thi
                log.error("Error processing answer for question ID {}", request.getQuestionId(), e);
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
     * Validate số lượng câu trả lời
     * 
     * Mục đích: Đảm bảo user trả lời đủ số câu hỏi
     * 
     * @throws RuntimeException nếu không có câu trả lời hoặc số lượng không khớp
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
     * Validate tất cả questions thuộc về một parent cụ thể
     * 
     * Mục đích: Bảo mật - Tránh user submit câu hỏi từ lesson khác
     * 
     * @throws RuntimeException nếu có câu hỏi không thuộc parent
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
    // ✅ SHUFFLE METHODS FOR USER ENDPOINTS - Xáo trộn câu hỏi cho User
    // =========================================================================
    // MỤC ĐÍCH:
    // - Chống gian lận: Mỗi lần làm bài có thứ tự khác nhau
    // - Tăng tính ngẫu nhiên: User phải hiểu bài, không nhớ vị trí
    // - Bảo mật: Không thể chia sẻ đáp án theo vị trí
    // 
    // CHIẾN LƯỢC 2 LỚP SHUFFLE:
    // ┌─────────────────────────────────────────────────────────────────┐
    // │ LỚP 1: SHUFFLE THỨ TỰ CÁC CÂU HỎI                               │
    // │        [Q1, Q2, Q3, Q4] → [Q3, Q1, Q4, Q2]                      │
    // │                                                                  │
    // │ LỚP 2: SHUFFLE NỘI DUNG TỪNG CÂU                                │
    // │        - Multiple Choice: Shuffle options (A,B,C,D → C,A,D,B)   │
    // │        - Matching: Shuffle cột phải                             │
    // │        - Fill Blank: Shuffle word bank                          │
    // └─────────────────────────────────────────────────────────────────┘
    // 
    // THỨ TỰ CHẠY:
    // 1. BaseLearningService gọi convertToDTOsForLearning()
    // 2. Convert tất cả Question → DTO
    // 3. SHUFFLE thứ tự các DTO (Lớp 1)
    // 4. Loop qua từng DTO, gọi shuffleQuestionContent() (Lớp 2)
    // 5. Return danh sách đã shuffle về Frontend
    // =========================================================================

    /**
     * ✅ ENTRY POINT: Convert questions cho User học bài (CÓ SHUFFLE)
     * 
     * FLOW:
     * ┌──────────────────────────────────────────────────────────────┐
     * │ Input: List<Question> từ DB (thứ tự gốc)                     │
     * │   ↓                                                           │
     * │ Step 1: Convert tất cả → List<QuestionResponseDTO>           │
     * │   ↓                                                           │
     * │ Step 2: Collections.shuffle(dtos) → XÁO THỨ TỰ CÂU          │
     * │   ↓                                                           │
     * │ Step 3: forEach(shuffleQuestionContent) → XÁO NỘI DUNG       │
     * │   ↓                                                           │
     * │ Output: List<DTO> đã shuffle cả thứ tự và nội dung           │
     * └──────────────────────────────────────────────────────────────┘
     * 
     * Sử dụng:
     * - User GET lesson detail (làm bài mới)
     * - User retry lesson (làm lại)
     * 
     * KHÔNG dùng cho:
     * - Admin quản lý (cần thứ tự gốc)
     * - Review kết quả (cần đúng thứ tự user đã làm)
     * 
     * @param questions Danh sách câu hỏi từ DB
     * @return Danh sách DTO đã shuffle hoàn toàn
     */
    public List<QuestionResponseDTO> convertToDTOsForLearning(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }

        // ✅ BƯỚC 1: Convert tất cả Entity → DTO
        // Lúc này vẫn giữ nguyên thứ tự từ DB
        List<QuestionResponseDTO> dtos = questions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // ✅ BƯỚC 2: SHUFFLE THỨ TỰ CÁC CÂU HỎI (LỚP 1)
        // Ví dụ: [Q1, Q2, Q3, Q4, Q5] → [Q3, Q1, Q5, Q2, Q4]
        // Mỗi user sẽ thấy thứ tự khác nhau
        Collections.shuffle(dtos);
        log.debug("Shuffled {} questions order", dtos.size());

        // ✅ BƯỚC 3: SHUFFLE NỘI DUNG TỪNG CÂU (LỚP 2)
        // Gọi shuffleQuestionContent() cho từng câu
        // - Multiple Choice: shuffle options
        // - Matching: shuffle right items
        // - Fill Blank: shuffle word bank
        dtos.forEach(this::shuffleQuestionContent);

        return dtos;
    }

    /**
     * ✅ SHUFFLE NỘI DUNG BÊN TRONG MỘT CÂU HỎI (LỚP 2)
     * 
     * Phân loại theo QuestionType và gọi method tương ứng:
     * 
     * ┌─────────────────────────────────────────────────────┐
     * │ MULTIPLE_CHOICE / TRUE_FALSE / COMPLETE_CONVERSATION│
     * │ → shuffleMultipleChoiceOptions()                    │
     * │   Shuffle: [A, B, C, D] → [C, A, D, B]              │
     * ├─────────────────────────────────────────────────────┤
     * │ MATCHING                                            │
     * │ → shuffleMatchingItems()                            │
     * │   Left giữ nguyên, Right shuffle                    │
     * ├─────────────────────────────────────────────────────┤
     * │ FILL_BLANK / VERB_FORM                              │
     * │ → shuffleFillBlankWordBank()                        │
     * │   Shuffle: [go, went, gone] → [gone, go, went]     │
     * └─────────────────────────────────────────────────────┘
     * 
     * @param dto DTO cần shuffle nội dung
     */
    private void shuffleQuestionContent(QuestionResponseDTO dto) {
        if (dto.getQuestionType() == QuestionType.MULTIPLE_CHOICE ||
                dto.getQuestionType() == QuestionType.TRUE_FALSE ||
                dto.getQuestionType() == QuestionType.COMPLETE_CONVERSATION) {
            shuffleMultipleChoiceOptions(dto);
            
        } else if (dto.getQuestionType() == QuestionType.MATCHING) {
            shuffleMatchingItems(dto);
            
        } else if (dto.getQuestionType() == QuestionType.FILL_BLANK ||
                dto.getQuestionType() == QuestionType.VERB_FORM) {
            shuffleFillBlankWordBank(dto);
        }
        // Các loại khác (OPEN_ENDED, ERROR_CORRECTION, etc.) không cần shuffle
    }

    /**
     * @deprecated Backward compatibility - dùng shuffleQuestionContent() thay thế
     */
    private QuestionResponseDTO convertToDTOWithShuffle(Question question) {
        QuestionResponseDTO dto = convertToDTO(question);
        shuffleQuestionContent(dto);
        return dto;
    }

    /**
     * ✅ SHUFFLE OPTIONS CỦA MULTIPLE CHOICE
     * 
     * Cách hoạt động:
     * 1. Lấy danh sách options từ DTO
     * 2. Collections.shuffle(options) - xáo ngẫu nhiên
     * 3. Options vẫn giữ field "order" để identify khi submit
     * 
     * Ví dụ:
     * Before: [A: "Dog", B: "Cat", C: "Bird", D: "Fish"]
     * After:  [C: "Bird", A: "Dog", D: "Fish", B: "Cat"]
     * 
     * Frontend hiển thị theo thứ tự mới, nhưng khi submit vẫn
     * gửi đúng "order" để backend biết user chọn đáp án nào
     * 
     * @param dto DTO chứa Multiple Choice data
     */
    private void shuffleMultipleChoiceOptions(QuestionResponseDTO dto) {
        if (dto.getData() instanceof CreateMultipleChoiceDTO mcDto) {
            List<CreateMultipleChoiceDTO.OptionDTO> options = mcDto.getOptions();
            if (options != null && options.size() > 1) {
                Collections.shuffle(options);
                log.debug("Shuffled {} options for question {}", options.size(), dto.getId());
            }
        }
    }

    /**
     * ✅ SHUFFLE ITEMS CỦA MATCHING QUESTION
     * 
     * Chiến lược:
     * 1. Extract left/right items từ pairs (nếu chưa có)
     * 2. GIỮ NGUYÊN cột left (để user dễ theo dõi)
     * 3. SHUFFLE cột right (tăng độ khó)
     * 
     * Ví dụ:
     * Before: 
     * Left          Right
     * 1. Apple   →  Táo
     * 2. Banana  →  Chuối  
     * 3. Orange  →  Cam
     * 
     * After:
     * Left          Right
     * 1. Apple   →  Chuối
     * 2. Banana  →  Cam
     * 3. Orange  →  Táo
     * 
     * User phải match lại đúng cặp
     * 
     * @param dto DTO chứa Matching data
     */
    private void shuffleMatchingItems(QuestionResponseDTO dto) {
        if (dto.getData() instanceof CreateMatchingDTO matchDto) {
            // Bước 1: Extract left/right từ pairs nếu chưa có
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

            // Bước 2: Shuffle cột phải
            List<String> rightItems = matchDto.getRightItems();
            if (rightItems != null && !rightItems.isEmpty()) {
                Collections.shuffle(rightItems);
            }
        }
    }

    /**
     * ✅ SHUFFLE WORD BANK CỦA FILL BLANK
     * 
     * Áp dụng cho:
     * - Fill Blank với word bank (drag & drop)
     * - Verb Form với choices
     * 
     * Ví dụ:
     * Before: [go, went, gone, going]
     * After:  [gone, going, go, went]
     * 
     * User phải chọn đúng từ điền vào chỗ trống,
     * không thể nhớ theo vị trí
     * 
     * @param dto DTO chứa Fill Blank data
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
    // SCORING UTILITIES - Tính toán điểm số và thống kê
    // =========================================================================
    // Sử dụng: Sau khi chấm xong, tính tổng điểm, % đúng, etc.
    // =========================================================================

    /**
     * Tính tổng điểm từ danh sách results
     * 
     * Chỉ cộng điểm của các câu ĐÚNG
     * 
     * @param results Danh sách kết quả từ processAnswers()
     * @return Tổng điểm đạt được
     */
    public int calculateTotalScore(List<QuestionResultDTO> results) {
        return results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                .filter(r -> r.getPoints() != null)
                .mapToInt(QuestionResultDTO::getPoints)
                .sum();
    }

    /**
     * Đếm số câu trả lời đúng
     * 
     * @param results Danh sách kết quả
     * @return Số câu đúng
     */
    public int calculateCorrectCount(List<QuestionResultDTO> results) {
        return (int) results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                .count();
    }

    /**
     * Tính phần trăm điểm
     * 
     * Formula: (correctCount / totalQuestions) * 100
     * 
     * @param correctCount Số câu đúng
     * @param totalQuestions Tổng số câu
     * @return Phần trăm (0-100)
     */
    public double calculateScorePercentage(int correctCount, int totalQuestions) {
        if (totalQuestions == 0) {
            return 0;
        }
        return (double) correctCount / totalQuestions * 100;
    }

    /**
     * Tính phần trăm điểm từ results
     * 
     * Overload method cho tiện
     * 
     * @param results Danh sách kết quả
     * @return Phần trăm (0-100)
     */
    public double calculateScorePercentage(List<QuestionResultDTO> results) {
        if (results == null || results.isEmpty()) {
            return 0;
        }
        int correctCount = calculateCorrectCount(results);
        return calculateScorePercentage(correctCount, results.size());
    }
}