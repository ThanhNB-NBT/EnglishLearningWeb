package com.thanhnb.englishlearning.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thanhnb.englishlearning.dto.question.request.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * ✅ Jackson Configuration - NON-DEPRECATED VERSION
 * 
 * Key Settings:
 * - defaultViewInclusion(true): Fields without @JsonView always serialize
 * - Polymorphic type handling for QuestionData subtypes
 * - Java 8 date/time support
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        
        // =========================================================================
        // BUILD OBJECTMAPPER WITH CONFIGURATION
        // =========================================================================
        ObjectMapper mapper = builder
                .createXmlMapper(false)
                
                // ✅ @JsonView Configuration
                // TRUE = Fields without @JsonView will always be included
                // FALSE = Only fields with @JsonView are included (WRONG for our case)
                .defaultViewInclusion(true)
                
                // Exclude null values from JSON
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                
                // Pretty print (can disable in production)
                .indentOutput(true)
                
                // Java 8 date/time support
                .modules(new JavaTimeModule())
                
                // Use ISO-8601 format for dates (not timestamps)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                
                .build();
        
        // =========================================================================
        // REGISTER POLYMORPHIC SUBTYPES FOR QuestionData
        // =========================================================================
        registerQuestionSubtypes(mapper);
        
        return mapper;
    }
    
    /**
     * Register all QuestionData subtypes for polymorphic serialization/deserialization
     */
    private void registerQuestionSubtypes(ObjectMapper mapper) {
        mapper.registerSubtypes(
            // ─────────────────────────────────────────────────────────────────
            // GROUP 1: MULTIPLE CHOICE FAMILY
            // All use CreateMultipleChoiceDTO structure
            // ─────────────────────────────────────────────────────────────────
            new NamedType(CreateMultipleChoiceDTO.class, "MULTIPLE_CHOICE"),
            new NamedType(CreateMultipleChoiceDTO.class, "TRUE_FALSE"),
            new NamedType(CreateMultipleChoiceDTO.class, "COMPLETE_CONVERSATION"),
            
            // ─────────────────────────────────────────────────────────────────
            // GROUP 2: FILL BLANK FAMILY
            // All use CreateFillBlankDTO structure
            // ─────────────────────────────────────────────────────────────────
            new NamedType(CreateFillBlankDTO.class, "FILL_BLANK"),
            new NamedType(CreateFillBlankDTO.class, "TEXT_ANSWER"),
            new NamedType(CreateFillBlankDTO.class, "VERB_FORM"),

            // ─────────────────────────────────────────────────────────────────
            // GROUP 3: SPECIALIZED TYPES
            // Each has unique DTO structure
            // ─────────────────────────────────────────────────────────────────
            new NamedType(CreateErrorCorrectionDTO.class, "ERROR_CORRECTION"),
            new NamedType(CreateMatchingDTO.class, "MATCHING"),
            new NamedType(CreateSentenceBuildingDTO.class, "SENTENCE_BUILDING"),
            new NamedType(CreatePronunciationsDTO.class, "PRONUNCIATION"),
            new NamedType(CreateOpenEndedDTO.class, "OPEN_ENDED"),
            new NamedType(CreateSentenceTransformationDTO.class, "SENTENCE_TRANSFORMATION")
        );
    }
}