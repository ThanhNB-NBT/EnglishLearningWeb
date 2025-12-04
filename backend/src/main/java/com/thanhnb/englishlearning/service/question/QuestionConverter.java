package com.thanhnb.englishlearning.service.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.dto.question.request.*;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.QuestionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service chuyển đổi dữ liệu câu hỏi.
 * Đã được tối ưu để hỗ trợ gộp các loại câu hỏi (Consolidation).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionConverter {

    private final ObjectMapper objectMapper;

    // =========================================================================
    // 1. ENTITY -> RESPONSE DTO
    // =========================================================================

    public QuestionResponseDTO toResponseDTO(Question question) {
        if (question == null) return null;

        QuestionResponseDTO dto = new QuestionResponseDTO();
        dto.setId(question.getId());
        dto.setParentType(question.getParentType());
        dto.setParentId(question.getParentId());
        dto.setQuestionText(question.getQuestionText());
        dto.setQuestionType(question.getQuestionType());
        dto.setPoints(question.getPoints());
        dto.setOrderIndex(question.getOrderIndex());
        dto.setCreatedAt(question.getCreatedAt());

        Map<String, Object> metadata = convertMetadataToMap(question.getMetadata());
        dto.setMetadata(metadata);

        if (metadata != null && metadata.containsKey("explanation")) {
            dto.setExplanation((String) metadata.get("explanation"));
        }

        return dto;
    }

    public List<QuestionResponseDTO> toResponseDTOs(List<Question> questions) {
        if (questions == null) return List.of();
        return questions.stream().map(this::toResponseDTO).toList();
    }

    // =========================================================================
    // 2. CREATE DTO -> ENTITY
    // =========================================================================

    public Question toEntity(CreateQuestionDTO dto) {
        Question question = new Question();
        question.setParentType(dto.getParentType());
        question.setParentId(dto.getParentId());
        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());
        question.setPoints(dto.getPoints() != null ? dto.getPoints() : 1);
        question.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : 0);
        question.setCreatedAt(LocalDateTime.now());

        Map<String, Object> metadata = buildMetadata(dto);
        question.setMetadata(metadata);

        return question;
    }

    // =========================================================================
    // 3. RESPONSE DTO -> CREATE DTO (Clone / Edit / AI Parse)
    // =========================================================================

    public CreateQuestionDTO toCreateDTO(QuestionResponseDTO responseDTO) {
        QuestionType type = responseDTO.getQuestionType();
        Map<String, Object> meta = responseDTO.getMetadata();
        if (meta == null) meta = new HashMap<>();

        CreateQuestionDTO createDTO = switch (type) {
            // --- NHÓM LỰA CHỌN (Gộp True/False, Conversation vào MultipleChoice) ---
            case MULTIPLE_CHOICE -> convertToMultipleChoiceDTO(meta);
            case TRUE_FALSE -> convertToTrueFalseAsMultipleChoice(meta);
            case COMPLETE_CONVERSATION -> convertToConversationAsMultipleChoice(meta);

            // --- NHÓM ĐIỀN KHUYẾT (Gộp TextAnswer, VerbForm vào FillBlank) ---
            case FILL_BLANK, TEXT_ANSWER, VERB_FORM -> convertToFillBlankDTO(meta);

            // --- CÁC LOẠI KHÁC ---
            case ERROR_CORRECTION -> convertToErrorCorrectionDTO(meta);
            case MATCHING -> convertToMatchingDTO(meta);
            case SENTENCE_BUILDING -> convertToSentenceBuildingDTO(meta);
            case PRONUNCIATION -> convertToPronunciationDTO(meta);
            case OPEN_ENDED -> convertToOpenEndedDTO(meta);
            case SENTENCE_TRANSFORMATION -> convertToSentenceTransformationDTO(meta);
            
            // Mặc định fallback
            default -> throw new IllegalArgumentException("Chưa hỗ trợ clone loại câu hỏi: " + type);
        };

        // Map các trường chung
        createDTO.setParentType(responseDTO.getParentType());
        createDTO.setParentId(responseDTO.getParentId());
        createDTO.setQuestionText(responseDTO.getQuestionText());
        createDTO.setPoints(responseDTO.getPoints());
        createDTO.setOrderIndex(responseDTO.getOrderIndex());

        return createDTO;
    }

    // =========================================================================
    // 4. METADATA BUILDERS (DTO -> MAP)
    // =========================================================================

    public Map<String, Object> buildMetadata(CreateQuestionDTO dto) {
        Map<String, Object> metadata = new HashMap<>();

        // Sử dụng Pattern Matching for switch (Java 17+) hoặc instanceof truyền thống
        if (dto instanceof CreateMultipleChoiceDTO mcDto) {
            buildMultipleChoiceMetadata(mcDto, metadata);
        } 
        else if (dto instanceof CreateFillBlankDTO fbDto) {
            buildFillBlankMetadata(fbDto, metadata);
        }
        else if (dto instanceof CreateMatchingDTO mDto) {
            buildMatchingMetadata(mDto, metadata);
        }
        else if (dto instanceof CreateSentenceBuildingDTO sbDto) {
            buildSentenceBuildingMetadata(sbDto, metadata);
        }
        else if (dto instanceof CreateErrorCorrectionDTO ecDto) {
            buildErrorCorrectionMetadata(ecDto, metadata);
        }
        else if (dto instanceof CreatePronunciationsDTO pDto) {
            buildPronunciationMetadata(pDto, metadata);
        }
        else if (dto instanceof CreateOpenEndedDTO oeDto) {
            buildOpenEndedMetadata(oeDto, metadata);
        }
        else if (dto instanceof CreateSentenceTransformationDTO stDto) {
            buildSentenceTransformationMetadata(stDto, metadata);
        }
        // Listening...

        return metadata;
    }

    // --- Specific Builders ---

    private void buildMultipleChoiceMetadata(CreateMultipleChoiceDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        
        // Nếu DTO có trường conversationContext (nếu bạn đã thêm vào), map nó
        // meta.put("conversationContext", dto.getConversationContext()); 

        List<Map<String, Object>> options = new ArrayList<>();
        for (CreateMultipleChoiceDTO.OptionDTO opt : dto.getOptions()) {
            Map<String, Object> o = new HashMap<>();
            o.put("text", opt.getText());
            o.put("isCorrect", Boolean.TRUE.equals(opt.getIsCorrect()));
            o.put("order", opt.getOrder());
            options.add(o);
        }
        meta.put("options", options);
    }

    private void buildFillBlankMetadata(CreateFillBlankDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        
        // Map Word Bank
        if (dto.getWordBank() != null && !dto.getWordBank().isEmpty()) {
            meta.put("wordBank", dto.getWordBank());
        }

        List<Map<String, Object>> blanks = new ArrayList<>();
        for (CreateFillBlankDTO.BlankDTO blank : dto.getBlanks()) {
            Map<String, Object> m = new HashMap<>();
            m.put("position", blank.getPosition());
            m.put("correctAnswers", blank.getCorrectAnswers());
            // Hint cho Verb Form
            if (blank.getHint() != null) {
                m.put("hint", blank.getHint());
                m.put("verb", blank.getHint()); // Backward compatibility
            }
            blanks.add(m);
        }
        meta.put("blanks", blanks);
    }

    private void buildErrorCorrectionMetadata(CreateErrorCorrectionDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        meta.put("errorText", dto.getErrorText());
        meta.put("correction", dto.getCorrection());
    }

    private void buildMatchingMetadata(CreateMatchingDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        List<Map<String, Object>> pairs = new ArrayList<>();
        for (CreateMatchingDTO.PairDTO pair : dto.getPairs()) {
            Map<String, Object> m = new HashMap<>();
            m.put("left", pair.getLeft());
            m.put("right", pair.getRight());
            m.put("order", pair.getOrder());
            pairs.add(m);
        }
        meta.put("pairs", pairs);
    }

    private void buildSentenceBuildingMetadata(CreateSentenceBuildingDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        meta.put("words", dto.getWords());
        meta.put("correctSentence", dto.getCorrectSentence());
    }

    private void buildPronunciationMetadata(CreatePronunciationsDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        meta.put("words", dto.getWords());
        meta.put("categories", dto.getCategories());
        
        List<Map<String, Object>> classifications = new ArrayList<>();
        for (CreatePronunciationsDTO.ClassificationDTO cls : dto.getClassifications()) {
            Map<String, Object> m = new HashMap<>();
            m.put("word", cls.getWord());
            m.put("category", cls.getCategory());
            classifications.add(m);
        }
        meta.put("correctClassifications", classifications);
    }

    private void buildOpenEndedMetadata(CreateOpenEndedDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        if (dto.getSuggestedAnswer() != null) meta.put("suggestedAnswer", dto.getSuggestedAnswer());
        if (dto.getTimeLimitSeconds() != null) meta.put("timeLimitSeconds", dto.getTimeLimitSeconds());
        if (dto.getMinWord() != null) meta.put("minWords", dto.getMinWord());
        if (dto.getMaxWord() != null) meta.put("maxWords", dto.getMaxWord());
    }

    private void buildSentenceTransformationMetadata(CreateSentenceTransformationDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        meta.put("originalSentence", dto.getOriginalSentence());
        if (dto.getBeginningPhrase() != null) meta.put("beginningPhrase", dto.getBeginningPhrase());
        meta.put("correctAnswers", dto.getCorrectAnswers());
    }

    // =========================================================================
    // 5. CONVERTERS (MAP -> DTO) - XỬ LÝ TƯƠNG THÍCH NGƯỢC
    // =========================================================================

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertMetadataToMap(Object metadata) {
        if (metadata == null) return null;
        try {
            if (metadata instanceof Map) return (Map<String, Object>) metadata;
            if (metadata instanceof String) return objectMapper.readValue((String) metadata, Map.class);
            return objectMapper.convertValue(metadata, Map.class);
        } catch (Exception e) {
            log.warn("Cannot convert metadata to Map: {}", e.getMessage());
            return null;
        }
    }

    // --- Multiple Choice (Standard) ---
    @SuppressWarnings("unchecked")
    private CreateMultipleChoiceDTO convertToMultipleChoiceDTO(Map<String, Object> meta) {
        CreateMultipleChoiceDTO result = new CreateMultipleChoiceDTO();
        result.setExplanation((String) meta.get("explanation"));

        List<Map<String, Object>> optionMaps = (List<Map<String, Object>>) meta.get("options");
        List<CreateMultipleChoiceDTO.OptionDTO> options = new ArrayList<>();
        if (optionMaps != null) {
            for (Map<String, Object> opt : optionMaps) {
                CreateMultipleChoiceDTO.OptionDTO option = new CreateMultipleChoiceDTO.OptionDTO();
                option.setText((String) opt.get("text"));
                option.setIsCorrect(Boolean.TRUE.equals(opt.get("isCorrect")));
                option.setOrder((Integer) opt.get("order"));
                options.add(option);
            }
        }
        result.setOptions(options);
        return result;
    }

    // --- True/False (Convert to Multiple Choice) ---
    private CreateMultipleChoiceDTO convertToTrueFalseAsMultipleChoice(Map<String, Object> meta) {
        CreateMultipleChoiceDTO result = new CreateMultipleChoiceDTO();
        result.setExplanation((String) meta.get("explanation"));
        
        boolean isTrueCorrect = false;
        // Logic cũ: correctAnswer = true/false
        if (meta.containsKey("correctAnswer") && meta.get("correctAnswer") instanceof Boolean) {
            isTrueCorrect = (Boolean) meta.get("correctAnswer");
        } 
        // Logic cũ hơn: options list
        else if (meta.containsKey("options")) {
             // ... extract from options ...
        }

        List<CreateMultipleChoiceDTO.OptionDTO> options = new ArrayList<>();
        options.add(new CreateMultipleChoiceDTO.OptionDTO("True", isTrueCorrect, 1));
        options.add(new CreateMultipleChoiceDTO.OptionDTO("False", !isTrueCorrect, 2));
        
        result.setOptions(options);
        return result;
    }

    // --- Conversation (Convert to Multiple Choice) ---
    @SuppressWarnings("unchecked")
    private CreateMultipleChoiceDTO convertToConversationAsMultipleChoice(Map<String, Object> meta) {
        CreateMultipleChoiceDTO result = new CreateMultipleChoiceDTO();
        result.setExplanation((String) meta.get("explanation"));
        
        // Lưu ý: context bị mất nếu CreateMultipleChoiceDTO không có trường chứa nó
        // Nếu cần, bạn có thể append vào explanation hoặc questionText
        
        List<String> textOptions = (List<String>) meta.get("options");
        String correctAns = (String) meta.get("correctAnswer");
        
        List<CreateMultipleChoiceDTO.OptionDTO> options = new ArrayList<>();
        if (textOptions != null) {
            for (int i = 0; i < textOptions.size(); i++) {
                String text = textOptions.get(i);
                boolean isCorrect = text.equals(correctAns);
                options.add(new CreateMultipleChoiceDTO.OptionDTO(text, isCorrect, i + 1));
            }
        }
        result.setOptions(options);
        return result;
    }

    // --- Fill Blank (Gộp Text Answer & Verb Form) ---
    @SuppressWarnings("unchecked")
    private CreateFillBlankDTO convertToFillBlankDTO(Map<String, Object> meta) {
        CreateFillBlankDTO result = new CreateFillBlankDTO();
        result.setExplanation((String) meta.get("explanation"));

        // 1. Map Word Bank (New)
        if (meta.containsKey("wordBank")) {
            result.setWordBank((List<String>) meta.get("wordBank"));
        }

        List<CreateFillBlankDTO.BlankDTO> blanks = new ArrayList<>();

        // 2. Case 1: Standard FillBlank / VerbForm (có 'blanks' list)
        if (meta.containsKey("blanks")) {
            List<Map<String, Object>> blankMaps = (List<Map<String, Object>>) meta.get("blanks");
            if (blankMaps != null) {
                for (Map<String, Object> b : blankMaps) {
                    CreateFillBlankDTO.BlankDTO blank = new CreateFillBlankDTO.BlankDTO();
                    blank.setPosition((Integer) b.get("position"));
                    
                    // Correct Answers
                    Object ansObj = b.get("correctAnswers");
                    if (ansObj instanceof List) {
                        blank.setCorrectAnswers((List<String>) ansObj);
                    } else if (ansObj instanceof String) {
                        blank.setCorrectAnswers(List.of((String) ansObj));
                    }
                    
                    // Hint / Verb
                    if (b.containsKey("hint")) blank.setHint((String) b.get("hint"));
                    else if (b.containsKey("verb")) blank.setHint((String) b.get("verb")); // Legacy verb form

                    blanks.add(blank);
                }
            }
        } 
        // 3. Case 2: Legacy TextAnswer (có 'correctAnswer' string đơn)
        else if (meta.containsKey("correctAnswer")) {
            String answerStr = (String) meta.get("correctAnswer");
            // Tách dấu | nếu có (VD: "color|colour")
            List<String> answers = new ArrayList<>();
            if (answerStr != null) {
                answers = Arrays.asList(answerStr.split("\\|"));
            }
            
            CreateFillBlankDTO.BlankDTO blank = new CreateFillBlankDTO.BlankDTO();
            blank.setPosition(1);
            blank.setCorrectAnswers(answers);
            blanks.add(blank);
        }

        result.setBlanks(blanks);
        return result;
    }

    private CreateErrorCorrectionDTO convertToErrorCorrectionDTO(Map<String, Object> meta) {
        CreateErrorCorrectionDTO result = new CreateErrorCorrectionDTO();
        result.setExplanation((String) meta.get("explanation"));
        result.setErrorText((String) meta.get("errorText"));
        result.setCorrection((String) meta.get("correction"));
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreateMatchingDTO convertToMatchingDTO(Map<String, Object> meta) {
        CreateMatchingDTO result = new CreateMatchingDTO();
        result.setExplanation((String) meta.get("explanation"));
        List<Map<String, Object>> pairMaps = (List<Map<String, Object>>) meta.get("pairs");
        List<CreateMatchingDTO.PairDTO> pairs = new ArrayList<>();
        if (pairMaps != null) {
            for (Map<String, Object> pairMap : pairMaps) {
                CreateMatchingDTO.PairDTO pair = new CreateMatchingDTO.PairDTO();
                pair.setLeft((String) pairMap.get("left"));
                pair.setRight((String) pairMap.get("right"));
                pair.setOrder((Integer) pairMap.get("order"));
                pairs.add(pair);
            }
        }
        result.setPairs(pairs);
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreateSentenceBuildingDTO convertToSentenceBuildingDTO(Map<String, Object> meta) {
        CreateSentenceBuildingDTO result = new CreateSentenceBuildingDTO();
        result.setExplanation((String) meta.get("explanation"));
        result.setWords((List<String>) meta.get("words"));
        result.setCorrectSentence((String) meta.get("correctSentence"));
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreatePronunciationsDTO convertToPronunciationDTO(Map<String, Object> meta) {
        CreatePronunciationsDTO result = new CreatePronunciationsDTO();
        result.setExplanation((String) meta.get("explanation"));
        result.setWords((List<String>) meta.get("words"));
        result.setCategories((List<String>) meta.get("categories"));
        List<Map<String, Object>> clsMaps = (List<Map<String, Object>>) meta.get("correctClassifications");
        List<CreatePronunciationsDTO.ClassificationDTO> classifications = new ArrayList<>();
        if (clsMaps != null) {
            for (Map<String, Object> clsMap : clsMaps) {
                CreatePronunciationsDTO.ClassificationDTO cls = new CreatePronunciationsDTO.ClassificationDTO();
                cls.setWord((String) clsMap.get("word"));
                cls.setCategory((String) clsMap.get("category"));
                classifications.add(cls);
            }
        }
        result.setClassifications(classifications);
        return result;
    }

    private CreateOpenEndedDTO convertToOpenEndedDTO(Map<String, Object> meta) {
        CreateOpenEndedDTO result = new CreateOpenEndedDTO();
        result.setExplanation((String) meta.get("explanation"));
        result.setSuggestedAnswer((String) meta.get("suggestedAnswer"));
        if (meta.get("timeLimitSeconds") instanceof Integer) result.setTimeLimitSeconds((Integer) meta.get("timeLimitSeconds"));
        if (meta.get("minWords") instanceof Integer) result.setMinWord((Integer) meta.get("minWords"));
        if (meta.get("maxWords") instanceof Integer) result.setMaxWord((Integer) meta.get("maxWords"));
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreateSentenceTransformationDTO convertToSentenceTransformationDTO(Map<String, Object> meta) {
        CreateSentenceTransformationDTO dto = new CreateSentenceTransformationDTO();
        dto.setExplanation((String) meta.get("explanation"));
        dto.setOriginalSentence((String) meta.get("originalSentence"));
        dto.setBeginningPhrase((String) meta.get("beginningPhrase"));
        Object answersObj = meta.get("correctAnswers");
        if (answersObj instanceof List) {
            dto.setCorrectAnswers((List<String>) answersObj);
        }
        return dto;
    }
}