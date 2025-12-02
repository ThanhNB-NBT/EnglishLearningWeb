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
 * Service hợp nhất chuyển đổi dữ liệu (Converter)
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
    // 3. RESPONSE DTO -> CREATE DTO (Helper cho AI/Clone)
    // =========================================================================

    public CreateQuestionDTO toCreateDTO(QuestionResponseDTO responseDTO) {
        QuestionType type = responseDTO.getQuestionType();
        Map<String, Object> meta = responseDTO.getMetadata();
        if (meta == null) meta = new HashMap<>();

        CreateQuestionDTO createDTO = switch (type) {
            case MULTIPLE_CHOICE -> convertToMultipleChoiceDTO(meta);
            case TRUE_FALSE -> convertToTrueFalseDTO(meta);
            case SHORT_ANSWER, TEXT_ANSWER -> convertToTextAnswerDTO(meta); 
            case FILL_BLANK -> convertToFillBlankDTO(meta);
            case VERB_FORM -> convertToVerbFormDTO(meta);
            case ERROR_CORRECTION -> convertToErrorCorrectionDTO(meta);
            case MATCHING -> convertToMatchingDTO(meta);
            case SENTENCE_BUILDING -> convertToSentenceBuildingDTO(meta);
            case COMPLETE_CONVERSATION-> convertToConversationDTO(meta);
            case PRONUNCIATION -> convertToPronunciationDTO(meta);
            case READING_COMPREHENSION -> convertToReadingComprehensionDTO(meta);
            case OPEN_ENDED -> convertToOpenEndedDTO(meta);
            default -> throw new IllegalArgumentException("Unsupported type for cloning: " + type);
        };

        createDTO.setParentType(responseDTO.getParentType());
        createDTO.setParentId(responseDTO.getParentId());
        createDTO.setQuestionText(responseDTO.getQuestionText());
        createDTO.setPoints(responseDTO.getPoints());
        createDTO.setOrderIndex(responseDTO.getOrderIndex());

        return createDTO;
    }

    // =========================================================================
    // PRIVATE HELPERS - METADATA BUILDING (CREATE -> MAP)
    // =========================================================================

    public Map<String, Object> buildMetadata(CreateQuestionDTO dto) {
        Map<String, Object> metadata = new HashMap<>();

        switch (dto.getQuestionType()) {
            case MULTIPLE_CHOICE -> buildMultipleChoiceMetadata((CreateMultipleChoiceDTO) dto, metadata);
            case TRUE_FALSE -> buildTrueFalseMetadata((CreateTrueFalseDTO) dto, metadata);
            case SHORT_ANSWER, TEXT_ANSWER -> buildTextAnswerMetadata((CreateTextAnswerDTO) dto, metadata);
            case FILL_BLANK -> buildFillBlankMetadata((CreateFillBlankDTO) dto, metadata);
            case VERB_FORM -> buildVerbFormMetadata((CreateVerbFormDTO) dto, metadata);
            case ERROR_CORRECTION -> buildErrorCorrectionMetadata((CreateErrorCorrectionDTO) dto, metadata);
            case MATCHING -> buildMatchingMetadata((CreateMatchingDTO) dto, metadata);
            case SENTENCE_BUILDING -> buildSentenceBuildingMetadata((CreateSentenceBuildingDTO) dto, metadata);
            case COMPLETE_CONVERSATION -> buildConversationMetadata((CreateConversationDTO) dto, metadata);
            case PRONUNCIATION -> buildPronunciationMetadata((CreatePronunciationsDTO) dto, metadata);
            case READING_COMPREHENSION -> buildReadingComprehensionMetadata((CreateReadingComprehensionDTO) dto, metadata);
            case OPEN_ENDED -> buildOpenEndedMetadata((CreateOpenEndedDTO) dto, metadata);
        }

        return metadata;
    }

    // --- BUILDERS ---

    private void buildMultipleChoiceMetadata(CreateMultipleChoiceDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        
        List<Map<String, Object>> options = new ArrayList<>();
        for (CreateMultipleChoiceDTO.OptionDTO opt : dto.getOptions()) {
            Map<String, Object> o = new HashMap<>();
            o.put("text", opt.getText());
            o.put("isCorrect", opt.getIsCorrect() != null ? opt.getIsCorrect() : false);
            o.put("order", opt.getOrder());
            options.add(o);
        }
        meta.put("options", options);
    }

    private void buildTrueFalseMetadata(CreateTrueFalseDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        
        List<Map<String, Object>> options = new ArrayList<>();
        options.add(Map.of("text", "True", "isCorrect", dto.getCorrectAnswer(), "order", 1));
        options.add(Map.of("text", "False", "isCorrect", !dto.getCorrectAnswer(), "order", 2));
        meta.put("options", options);
    }

    // ✅ FIX: Dùng cho cả TEXT_ANSWER và SHORT_ANSWER
    private void buildTextAnswerMetadata(CreateTextAnswerDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        meta.put("correctAnswer", dto.getCorrectAnswer());
        meta.put("caseSensitive", dto.getCaseSensitive());
    }

    private void buildFillBlankMetadata(CreateFillBlankDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        List<Map<String, Object>> blanks = new ArrayList<>();
        for (CreateFillBlankDTO.BlankDTO blank : dto.getBlanks()) {
            Map<String, Object> m = new HashMap<>();
            m.put("position", blank.getPosition());
            m.put("correctAnswers", blank.getCorrectAnswers());
            blanks.add(m);
        }
        meta.put("blanks", blanks);
    }

    private void buildVerbFormMetadata(CreateVerbFormDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        List<Map<String, Object>> blanks = new ArrayList<>();
        for (CreateVerbFormDTO.VerbBlankDTO blank : dto.getBlanks()) {
            Map<String, Object> m = new HashMap<>();
            m.put("position", blank.getPosition());
            m.put("verb", blank.getVerb());
            m.put("correctAnswers", blank.getCorrectAnswers());
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
    
    private void buildConversationMetadata(CreateConversationDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        meta.put("conversationContext", dto.getConversationContext());
        meta.put("options", dto.getOptions());
        meta.put("correctAnswer", dto.getCorrectAnswer());
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

    private void buildReadingComprehensionMetadata(CreateReadingComprehensionDTO dto, Map<String, Object> meta) {
         if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
         meta.put("passage", dto.getPassage());
         List<Map<String, Object>> blanks = new ArrayList<>();
         for (CreateReadingComprehensionDTO.BlankDTO b : dto.getBlanks()) {
             Map<String, Object> m = new HashMap<>();
             m.put("position", b.getPosition());
             m.put("options", b.getOptions());
             m.put("correctAnswer", b.getCorrectAnswer());
             blanks.add(m);
         }
         meta.put("blanks", blanks);
    }

    private void buildOpenEndedMetadata(CreateOpenEndedDTO dto, Map<String, Object> meta) {
        if (dto.getExplanation() != null) meta.put("explanation", dto.getExplanation());
        if (dto.getSuggestedAnswer() != null) meta.put("suggestedAnswer", dto.getSuggestedAnswer());
        if (dto.getTimeLimitSeconds() != null) meta.put("timeLimitSeconds", dto.getTimeLimitSeconds());
        if (dto.getMinWord() != null) meta.put("minWords", dto.getMinWord());
        if (dto.getMaxWord() != null) meta.put("maxWords", dto.getMaxWord());
    }

    // =========================================================================
    // PRIVATE HELPERS - UTILS (CONVERT MAP -> DTO)
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

    // --- CONVERTERS ---

    @SuppressWarnings("unchecked")
    private CreateMultipleChoiceDTO convertToMultipleChoiceDTO(Map<String, Object> meta) {
        CreateMultipleChoiceDTO result = new CreateMultipleChoiceDTO();
        result.setExplanation((String) meta.get("explanation"));
        
        List<Map<String, Object>> optionMaps = (List<Map<String, Object>>) meta.get("options");
        List<CreateMultipleChoiceDTO.OptionDTO> options = new ArrayList<>();
        if(optionMaps != null) {
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

    @SuppressWarnings("unchecked")
    private CreateTrueFalseDTO convertToTrueFalseDTO(Map<String, Object> meta) {
        CreateTrueFalseDTO result = new CreateTrueFalseDTO();
        result.setExplanation((String) meta.get("explanation"));
        List<Map<String, Object>> options = (List<Map<String, Object>>) meta.get("options");
        if(options != null) {
            boolean correctAnswer = options.stream()
                .filter(opt -> "True".equals(opt.get("text")))
                .findFirst()
                .map(opt -> Boolean.TRUE.equals(opt.get("isCorrect")))
                .orElse(false);
            result.setCorrectAnswer(correctAnswer);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreateTextAnswerDTO convertToTextAnswerDTO(Map<String, Object> meta) {
        CreateTextAnswerDTO result = new CreateTextAnswerDTO();
        result.setExplanation((String) meta.get("explanation"));
        
        // Cả SHORT_ANSWER và TEXT_ANSWER đều dùng field correctAnswer
        if (meta.containsKey("correctAnswer")) {
             result.setCorrectAnswer((String) meta.get("correctAnswer"));
        } else if (meta.containsKey("blanks")) {
             // Fallback cho dữ liệu cũ (nếu có)
             List<Map<String,Object>> blanks = (List<Map<String,Object>>) meta.get("blanks");
             if(blanks != null && !blanks.isEmpty()) {
                  Object answers = blanks.get(0).get("correctAnswers");
                  if(answers instanceof List) result.setCorrectAnswer(String.join("|", (List<String>)answers));
             }
        }
        
        result.setCaseSensitive((Boolean) meta.getOrDefault("caseSensitive", false));
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreateFillBlankDTO convertToFillBlankDTO(Map<String, Object> meta) {
        CreateFillBlankDTO result = new CreateFillBlankDTO();
        result.setExplanation((String) meta.get("explanation"));
        
        List<Map<String, Object>> blankMaps = (List<Map<String, Object>>) meta.get("blanks");
        List<CreateFillBlankDTO.BlankDTO> blanks = new ArrayList<>();
        if(blankMaps != null) {
            for (Map<String, Object> b : blankMaps) {
                CreateFillBlankDTO.BlankDTO blank = new CreateFillBlankDTO.BlankDTO();
                blank.setPosition((Integer) b.get("position"));
                blank.setCorrectAnswers((List<String>) b.get("correctAnswers")); 
                blanks.add(blank);
            }
        }
        result.setBlanks(blanks);
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreateVerbFormDTO convertToVerbFormDTO(Map<String, Object> meta) {
        CreateVerbFormDTO result = new CreateVerbFormDTO();
        result.setExplanation((String) meta.get("explanation"));
        
        List<Map<String, Object>> blankMaps = (List<Map<String, Object>>) meta.get("blanks");
        List<CreateVerbFormDTO.VerbBlankDTO> blanks = new ArrayList<>();
        if(blankMaps != null) {
            for (Map<String, Object> b : blankMaps) {
                CreateVerbFormDTO.VerbBlankDTO blank = new CreateVerbFormDTO.VerbBlankDTO();
                blank.setPosition((Integer) b.get("position"));
                blank.setVerb((String) b.get("verb"));
                blank.setCorrectAnswers((List<String>) b.get("correctAnswers"));
                blanks.add(blank);
            }
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
        if(pairMaps != null) {
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
    private CreateConversationDTO convertToConversationDTO(Map<String, Object> meta) {
        CreateConversationDTO result = new CreateConversationDTO();
        result.setExplanation((String) meta.get("explanation"));
        result.setConversationContext((String) meta.get("conversationContext"));
        result.setOptions((List<String>) meta.get("options"));
        result.setCorrectAnswer((String) meta.get("correctAnswer"));
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
        if(clsMaps != null) {
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
    
    @SuppressWarnings("unchecked")
    private CreateReadingComprehensionDTO convertToReadingComprehensionDTO(Map<String, Object> meta) {
        CreateReadingComprehensionDTO result = new CreateReadingComprehensionDTO();
        result.setExplanation((String) meta.get("explanation"));
        result.setPassage((String) meta.get("passage"));
        List<Map<String, Object>> blankMaps = (List<Map<String, Object>>) meta.get("blanks");
        List<CreateReadingComprehensionDTO.BlankDTO> blanks = new ArrayList<>();
        if(blankMaps != null) {
            for (Map<String, Object> blankMap : blankMaps) {
                CreateReadingComprehensionDTO.BlankDTO blank = new CreateReadingComprehensionDTO.BlankDTO();
                blank.setPosition((Integer) blankMap.get("position"));
                blank.setOptions((List<String>) blankMap.get("options"));
                blank.setCorrectAnswer((String) blankMap.get("correctAnswer"));
                blanks.add(blank);
            }
        }
        result.setBlanks(blanks);
        return result;
    }
    
    private CreateOpenEndedDTO convertToOpenEndedDTO(Map<String, Object> meta) {
        CreateOpenEndedDTO result = new CreateOpenEndedDTO();
        result.setExplanation((String) meta.get("explanation"));
        result.setSuggestedAnswer((String) meta.get("suggestedAnswer"));
        
        Object timeLimit = meta.get("timeLimitSeconds");
        if (timeLimit instanceof Integer) result.setTimeLimitSeconds((Integer) timeLimit);
        
        Object minWords = meta.get("minWords");
        if (minWords instanceof Integer) result.setMinWord((Integer) minWords);
        
        Object maxWords = meta.get("maxWords");
        if (maxWords instanceof Integer) result.setMaxWord((Integer) maxWords);
        
        return result;
    }
}