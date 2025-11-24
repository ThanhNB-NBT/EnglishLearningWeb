package com.thanhnb.englishlearning.service.question;

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
 * Factory để convert giữa CreateDTO -> Entity -> ResponseDTO
 * Xử lý polymorphic cho tất cả loại câu hỏi
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionDTOBuilder {

    // ═══════════════════════════════════════════════════════════════
    // CREATE DTO -> ENTITY
    // ═══════════════════════════════════════════════════════════════

    /**
     * Convert CreateQuestionDTO -> Question Entity
     */
    public Question createEntity(CreateQuestionDTO dto) {
        Question question = new Question();
        question.setParentType(dto.getParentType());
        question.setParentId(dto.getParentId());
        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());
        question.setExplanation(dto.getExplanation());
        question.setPoints(dto.getPoints() != null ? dto.getPoints() : 5);
        question.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : 0);
        question.setCreatedAt(LocalDateTime.now());
        
        // Build metadata based on question type
        Map<String, Object> metadata = buildMetadata(dto);
        question.setMetadata(metadata);
        
        return question;
    }

    /**
     * Build metadata từ CreateDTO theo từng loại câu hỏi
     */
    public Map<String, Object> buildMetadata(CreateQuestionDTO dto) {
        Map<String, Object> metadata = new HashMap<>();
        
        return switch (dto.getQuestionType()) {
            case MULTIPLE_CHOICE -> buildMultipleChoiceMetadata((CreateMultipleChoiceDTO) dto, metadata);
            case TRUE_FALSE -> buildTrueFalseMetadata((CreateTrueFalseDTO) dto, metadata);
            case FILL_BLANK, SHORT_ANSWER, VERB_FORM, ERROR_CORRECTION -> 
                buildTextAnswerMetadata((CreateTextAnswerDTO) dto, metadata);
            case MATCHING -> buildMatchingMetadata((CreateMatchingDTO) dto, metadata);
            case SENTENCE_BUILDING -> buildSentenceBuildingMetadata((CreateSentenceBuildingDTO) dto, metadata);
            case COMPLETE_CONVERSATION -> buildConversationMetadata((CreateConversationDTO) dto, metadata);
            case PRONUNCIATION -> buildPronunciationMetadata((CreatePronunciationsDTO) dto, metadata);
            case READING_COMPREHENSION -> buildReadingComprehensionMetadata((CreateReadingComprehensionDTO) dto, metadata);
            case OPEN_ENDED -> buildOpenEndedMetadata((CreateOpenEndedDTO) dto, metadata);
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // METADATA BUILDERS (Private)
    // ═══════════════════════════════════════════════════════════════

    private Map<String, Object> buildMultipleChoiceMetadata(CreateMultipleChoiceDTO dto, Map<String, Object> meta) {
        if (dto.getHint() != null) meta.put("hint", dto.getHint());
        
        List<Map<String, Object>> options = new ArrayList<>();

        if (dto.getOptions() == null || dto.getOptions().isEmpty()) {
        throw new IllegalArgumentException("Câu hỏi Multiple Choice phải có ít nhất một lựa chọn.");
    }

        for (CreateMultipleChoiceDTO.OptionDTO opt : dto.getOptions()) {
            Map<String, Object> option = new HashMap<>();
            option.put("text", opt.getText());
            option.put("isCorrect", opt.isCorrect());
            option.put("order", opt.getOrder());
            options.add(option);
        }
        meta.put("options", options);
        
        return meta;
    }

    private Map<String, Object> buildTrueFalseMetadata(CreateTrueFalseDTO dto, Map<String, Object> meta) {
        if (dto.getHint() != null) meta.put("hint", dto.getHint());
        
        // True/False được lưu dạng options để tương thích với checking logic
        List<Map<String, Object>> options = new ArrayList<>();
        
        Map<String, Object> trueOption = new HashMap<>();
        trueOption.put("text", "True");
        trueOption.put("isCorrect", dto.getCorrectAnswer());
        trueOption.put("order", 1);
        options.add(trueOption);
        
        Map<String, Object> falseOption = new HashMap<>();
        falseOption.put("text", "False");
        falseOption.put("isCorrect", !dto.getCorrectAnswer());
        falseOption.put("order", 2);
        options.add(falseOption);
        
        meta.put("options", options);
        return meta;
    }

    private Map<String, Object> buildTextAnswerMetadata(CreateTextAnswerDTO dto, Map<String, Object> meta) {
        if (dto.getHint() != null) meta.put("hint", dto.getHint());
        meta.put("correctAnswer", dto.getCorrectAnswer());
        meta.put("caseSensitive", dto.getCaseSensitive() != null ? dto.getCaseSensitive() : false);
        return meta;
    }

    private Map<String, Object> buildMatchingMetadata(CreateMatchingDTO dto, Map<String, Object> meta) {
        if (dto.getHint() != null) meta.put("hint", dto.getHint());
        
        List<Map<String, Object>> pairs = new ArrayList<>();
        for (CreateMatchingDTO.PairDTO pair : dto.getPairs()) {
            Map<String, Object> pairMap = new HashMap<>();
            pairMap.put("left", pair.getLeft());
            pairMap.put("right", pair.getRight());
            pairMap.put("order", pair.getOrder());
            pairs.add(pairMap);
        }
        meta.put("pairs", pairs);
        
        return meta;
    }

    private Map<String, Object> buildSentenceBuildingMetadata(CreateSentenceBuildingDTO dto, Map<String, Object> meta) {
        if (dto.getHint() != null) meta.put("hint", dto.getHint());
        meta.put("words", dto.getWords());
        meta.put("correctSentence", dto.getCorrectSentence());
        return meta;
    }

    private Map<String, Object> buildConversationMetadata(CreateConversationDTO dto, Map<String, Object> meta) {
        if (dto.getHint() != null) meta.put("hint", dto.getHint());
        meta.put("conversationContext", dto.getConversationContext());
        meta.put("options", dto.getOptions());
        meta.put("correctAnswer", dto.getCorrectAnswer());
        return meta;
    }

    private Map<String, Object> buildPronunciationMetadata(CreatePronunciationsDTO dto, Map<String, Object> meta) {
        if (dto.getHint() != null) meta.put("hint", dto.getHint());
        meta.put("words", dto.getWords());
        meta.put("categories", dto.getCategories());
        
        List<Map<String, Object>> classifications = new ArrayList<>();
        for (CreatePronunciationsDTO.ClassificationDTO cls : dto.getClassifications()) {
            Map<String, Object> clsMap = new HashMap<>();
            clsMap.put("word", cls.getWord());
            clsMap.put("category", cls.getCategory());
            classifications.add(clsMap);
        }
        meta.put("correctClassifications", classifications);
        
        return meta;
    }

    private Map<String, Object> buildReadingComprehensionMetadata(CreateReadingComprehensionDTO dto, Map<String, Object> meta) {
        if (dto.getHint() != null) meta.put("hint", dto.getHint());
        meta.put("passage", dto.getPassage());
        
        List<Map<String, Object>> blanks = new ArrayList<>();
        for (CreateReadingComprehensionDTO.BlankDTO blank : dto.getBlanks()) {
            Map<String, Object> blankMap = new HashMap<>();
            blankMap.put("position", blank.getPosition());
            blankMap.put("options", blank.getOptions());
            blankMap.put("correctAnswer", blank.getCorrectAnswer());
            blanks.add(blankMap);
        }
        meta.put("blanks", blanks);
        
        return meta;
    }

    private Map<String, Object> buildOpenEndedMetadata(CreateOpenEndedDTO dto, Map<String, Object> meta) {
        if (dto.getHint() != null) meta.put("hint", dto.getHint());
        if (dto.getSuggestedAnswer() != null) meta.put("suggestedAnswer", dto.getSuggestedAnswer());
        if (dto.getTimeLimitSeconds() != null) meta.put("timeLimitSeconds", dto.getTimeLimitSeconds());
        if (dto.getMinWord() != null) meta.put("minWords", dto.getMinWord());
        if (dto.getMaxWord() != null) meta.put("maxWords", dto.getMaxWord());
        return meta;
    }

    // ═══════════════════════════════════════════════════════════════
    // ENTITY -> RESPONSE DTO (Delegation to ConversionService)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Convert Entity -> ResponseDTO
     * (Vẫn dùng QuestionConversionService cho consistent)
     */
    public QuestionResponseDTO toResponseDTO(Question question, QuestionMapper conversionService) {
        return conversionService.convertToDTO(question);
    }

    // ═══════════════════════════════════════════════════════════════
    // RESPONSE DTO -> CREATE DTO (For AI Parsing)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Convert ResponseDTO -> CreateDTO
     * Dùng cho AI parsing khi cần convert từ generic DTO sang typed DTO
     */
    public CreateQuestionDTO responseToCreateDTO(QuestionResponseDTO responseDTO) {
        QuestionType type = responseDTO.getQuestionType();
        Map<String, Object> meta = responseDTO.getMetadata();
        
        CreateQuestionDTO createDTO = switch (type) {
            case MULTIPLE_CHOICE -> convertToMultipleChoiceDTO(responseDTO, meta);
            case TRUE_FALSE -> convertToTrueFalseDTO(responseDTO, meta);
            case FILL_BLANK, SHORT_ANSWER, VERB_FORM, ERROR_CORRECTION -> 
                convertToTextAnswerDTO(responseDTO, meta, type);
            case MATCHING -> convertToMatchingDTO(responseDTO, meta);
            case SENTENCE_BUILDING -> convertToSentenceBuildingDTO(responseDTO, meta);
            case COMPLETE_CONVERSATION -> convertToConversationDTO(responseDTO, meta);
            case PRONUNCIATION -> convertToPronunciationDTO(responseDTO, meta);
            case READING_COMPREHENSION -> convertToReadingComprehensionDTO(responseDTO, meta);
            case OPEN_ENDED -> convertToOpenEndedDTO(responseDTO, meta);
        };
        
        // Set common fields
        createDTO.setParentType(responseDTO.getParentType());
        createDTO.setParentId(responseDTO.getParentId());
        createDTO.setQuestionText(responseDTO.getQuestionText());
        createDTO.setExplanation(responseDTO.getExplanation());
        createDTO.setPoints(responseDTO.getPoints());
        createDTO.setOrderIndex(responseDTO.getOrderIndex());
        
        return createDTO;
    }

    // ═══════════════════════════════════════════════════════════════
    // CONVERSION HELPERS (Private)
    // ═══════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private CreateMultipleChoiceDTO convertToMultipleChoiceDTO(QuestionResponseDTO dto, Map<String, Object> meta) {
        CreateMultipleChoiceDTO result = new CreateMultipleChoiceDTO();
        result.setHint((String) meta.get("hint"));
        
        List<Map<String, Object>> optionMaps = (List<Map<String, Object>>) meta.get("options");
        List<CreateMultipleChoiceDTO.OptionDTO> options = new ArrayList<>();
        
        for (Map<String, Object> opt : optionMaps) {
            CreateMultipleChoiceDTO.OptionDTO option = new CreateMultipleChoiceDTO.OptionDTO();
            option.setText((String) opt.get("text"));
            option.setCorrect(Boolean.TRUE.equals(opt.get("isCorrect")));
            option.setOrder((Integer) opt.get("order"));
            options.add(option);
        }
        result.setOptions(options);
        
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreateTrueFalseDTO convertToTrueFalseDTO(QuestionResponseDTO dto, Map<String, Object> meta) {
        CreateTrueFalseDTO result = new CreateTrueFalseDTO();
        result.setHint((String) meta.get("hint"));
        
        // Extract correctAnswer từ options
        List<Map<String, Object>> options = (List<Map<String, Object>>) meta.get("options");
        boolean correctAnswer = options.stream()
            .filter(opt -> "True".equals(opt.get("text")))
            .findFirst()
            .map(opt -> Boolean.TRUE.equals(opt.get("isCorrect")))
            .orElse(false);
        
        result.setCorrectAnswer(correctAnswer);
        return result;
    }

    private CreateTextAnswerDTO convertToTextAnswerDTO(QuestionResponseDTO dto, Map<String, Object> meta, QuestionType type) {
        CreateTextAnswerDTO result = new CreateTextAnswerDTO();
        result.setHint((String) meta.get("hint"));
        result.setCorrectAnswer((String) meta.get("correctAnswer"));
        result.setCaseSensitive((Boolean) meta.getOrDefault("caseSensitive", false));
        result.setType(type);
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreateMatchingDTO convertToMatchingDTO(QuestionResponseDTO dto, Map<String, Object> meta) {
        CreateMatchingDTO result = new CreateMatchingDTO();
        result.setHint((String) meta.get("hint"));
        
        List<Map<String, Object>> pairMaps = (List<Map<String, Object>>) meta.get("pairs");
        List<CreateMatchingDTO.PairDTO> pairs = new ArrayList<>();
        
        for (Map<String, Object> pairMap : pairMaps) {
            CreateMatchingDTO.PairDTO pair = new CreateMatchingDTO.PairDTO();
            pair.setLeft((String) pairMap.get("left"));
            pair.setRight((String) pairMap.get("right"));
            pair.setOrder((Integer) pairMap.get("order"));
            pairs.add(pair);
        }
        result.setPairs(pairs);
        
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreateSentenceBuildingDTO convertToSentenceBuildingDTO(QuestionResponseDTO dto, Map<String, Object> meta) {
        CreateSentenceBuildingDTO result = new CreateSentenceBuildingDTO();
        result.setHint((String) meta.get("hint"));
        result.setWords((List<String>) meta.get("words"));
        result.setCorrectSentence((String) meta.get("correctSentence"));
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreateConversationDTO convertToConversationDTO(QuestionResponseDTO dto, Map<String, Object> meta) {
        CreateConversationDTO result = new CreateConversationDTO();
        result.setHint((String) meta.get("hint"));
        result.setConversationContext((String) meta.get("conversationContext"));
        result.setOptions((List<String>) meta.get("options"));
        result.setCorrectAnswer((String) meta.get("correctAnswer"));
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreatePronunciationsDTO convertToPronunciationDTO(QuestionResponseDTO dto, Map<String, Object> meta) {
        CreatePronunciationsDTO result = new CreatePronunciationsDTO();
        result.setHint((String) meta.get("hint"));
        result.setWords((List<String>) meta.get("words"));
        result.setCategories((List<String>) meta.get("categories"));
        
        List<Map<String, Object>> clsMaps = (List<Map<String, Object>>) meta.get("correctClassifications");
        List<CreatePronunciationsDTO.ClassificationDTO> classifications = new ArrayList<>();
        
        for (Map<String, Object> clsMap : clsMaps) {
            CreatePronunciationsDTO.ClassificationDTO cls = new CreatePronunciationsDTO.ClassificationDTO();
            cls.setWord((String) clsMap.get("word"));
            cls.setCategory((String) clsMap.get("category"));
            classifications.add(cls);
        }
        result.setClassifications(classifications);
        
        return result;
    }

    @SuppressWarnings("unchecked")
    private CreateReadingComprehensionDTO convertToReadingComprehensionDTO(QuestionResponseDTO dto, Map<String, Object> meta) {
        CreateReadingComprehensionDTO result = new CreateReadingComprehensionDTO();
        result.setHint((String) meta.get("hint"));
        result.setPassage((String) meta.get("passage"));
        
        List<Map<String, Object>> blankMaps = (List<Map<String, Object>>) meta.get("blanks");
        List<CreateReadingComprehensionDTO.BlankDTO> blanks = new ArrayList<>();
        
        for (Map<String, Object> blankMap : blankMaps) {
            CreateReadingComprehensionDTO.BlankDTO blank = new CreateReadingComprehensionDTO.BlankDTO();
            blank.setPosition((Integer) blankMap.get("position"));
            blank.setOptions((List<String>) blankMap.get("options"));
            blank.setCorrectAnswer((String) blankMap.get("correctAnswer"));
            blanks.add(blank);
        }
        result.setBlanks(blanks);
        
        return result;
    }

    private CreateOpenEndedDTO convertToOpenEndedDTO(QuestionResponseDTO dto, Map<String, Object> meta) {
        CreateOpenEndedDTO result = new CreateOpenEndedDTO();
        result.setHint((String) meta.get("hint"));
        result.setSuggestedAnswer((String) meta.get("suggestedAnswer"));
        
        Object timeLimit = meta.get("timeLimitSeconds");
        if (timeLimit instanceof Integer) {
            result.setTimeLimitSeconds((Integer) timeLimit);
        }
        
        Object minWords = meta.get("minWords");
        if (minWords instanceof Integer) {
            result.setMinWord((Integer) minWords);
        }
        
        Object maxWords = meta.get("maxWords");
        if (maxWords instanceof Integer) {
            result.setMaxWord((Integer) maxWords);
        }
        
        return result;
    }
}