package com.thanhnb.englishlearning.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.thanhnb.englishlearning.dto.question.request.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * ✅ Jackson Configuration để hỗ trợ polymorphic deserialization cho CreateQuestionDTO
 * 
 * Khi client gửi JSON với field "questionType", Jackson sẽ tự động deserialize
 * đúng subclass tương ứng (CreateMultipleChoiceDTO, CreateMatchingDTO, ...)
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.createXmlMapper(false).build();
        
        // Register subtypes for polymorphic deserialization
        mapper.registerSubtypes(
            new NamedType(CreateMultipleChoiceDTO.class, "MULTIPLE_CHOICE"),
            new NamedType(CreateTrueFalseDTO.class, "TRUE_FALSE"),
            new NamedType(CreateFillBlankDTO.class, "FILL_BLANK"),
            new NamedType(CreateTextAnswerDTO.class, "TEXT_ANSWER"),
            new NamedType(CreateVerbFormDTO.class, "VERB_FORM"),
            new NamedType(CreateErrorCorrectionDTO.class, "ERROR_CORRECTION"),
            new NamedType(CreateMatchingDTO.class, "MATCHING"),
            new NamedType(CreateSentenceBuildingDTO.class, "SENTENCE_BUILDING"),
            new NamedType(CreateConversationDTO.class, "COMPLETE_CONVERSATION"),
            new NamedType(CreatePronunciationsDTO.class, "PRONUNCIATION"),
            new NamedType(CreateReadingComprehensionDTO.class, "READING_COMPREHENSION"),
            new NamedType(CreateOpenEndedDTO.class, "OPEN_ENDED")
        );
        
        return mapper;
    }
}