package com.thanhnb.englishlearning.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.thanhnb.englishlearning.dto.question.request.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson Configuration
 * Cấu hình map các "questionType" (String) về đúng Java Class (DTO) tương ứng.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.createXmlMapper(false).build();
        
        // Register subtypes for polymorphic deserialization
        mapper.registerSubtypes(
            // 1. Nhóm Lựa chọn (Gộp hết vào CreateMultipleChoiceDTO)
            new NamedType(CreateMultipleChoiceDTO.class, "MULTIPLE_CHOICE"),
            new NamedType(CreateMultipleChoiceDTO.class, "TRUE_FALSE"),           // Map True/False -> MultipleChoice
            new NamedType(CreateMultipleChoiceDTO.class, "COMPLETE_CONVERSATION"), // Map Conversation -> MultipleChoice
            
            // 2. Nhóm Điền khuyết (Gộp hết vào CreateFillBlankDTO)
            new NamedType(CreateFillBlankDTO.class, "FILL_BLANK"),
            new NamedType(CreateFillBlankDTO.class, "TEXT_ANSWER"), // Map TextAnswer -> FillBlank
            new NamedType(CreateFillBlankDTO.class, "VERB_FORM"),   // Map VerbForm -> FillBlank

            // 3. Các nhóm giữ nguyên (DTO riêng)
            new NamedType(CreateErrorCorrectionDTO.class, "ERROR_CORRECTION"),
            new NamedType(CreateMatchingDTO.class, "MATCHING"),
            new NamedType(CreateSentenceBuildingDTO.class, "SENTENCE_BUILDING"),
            new NamedType(CreatePronunciationsDTO.class, "PRONUNCIATION"),
            new NamedType(CreateOpenEndedDTO.class, "OPEN_ENDED"),
            new NamedType(CreateListeningDTO.class, "LISTENING_COMPREHENSION"),
            new NamedType(CreateSentenceTransformationDTO.class, "SENTENCE_TRANSFORMATION")
        );
        
        return mapper;
    }
}