package com.thanhnb.englishlearning.service.question;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.entity.question.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ✅ Service chuyển đổi Entity <-> DTO
 * Centralized conversion logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionMapper {

    private final ObjectMapper objectMapper;

    /**
     * ✅ Convert Question entity -> QuestionResponseDTO
     * @param question Question entity cần convert
     * @return QuestionResponseDTO
     */
    public QuestionResponseDTO convertToDTO(Question question) {
        if (question == null) {
            return null;
        }

        QuestionResponseDTO dto = new QuestionResponseDTO();
        dto.setId(question.getId());
        dto.setParentType(question.getParentType());
        dto.setParentId(question.getParentId());
        dto.setQuestionText(question.getQuestionText());
        dto.setQuestionType(question.getQuestionType());
        dto.setExplanation(question.getExplanation());
        dto.setPoints(question.getPoints());
        dto.setOrderIndex(question.getOrderIndex());
        dto.setCreatedAt(question.getCreatedAt());
        
        // Convert metadata từ Object sang Map
        dto.setMetadata(convertMetadata(question.getMetadata()));

        log.debug("✅ Converted question {} to DTO", question.getId());
        return dto;
    }

    /**
     * ✅ Convert list of questions to DTOs
     * @param questions Danh sách Question entities
     * @return Danh sách QuestionResponseDTO
     */
    public List<QuestionResponseDTO> convertToDTOs(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }

        return questions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Helper: Convert metadata từ Object sang Map<String, Object>
     * @param metadata Metadata object từ entity
     * @return Map<String, Object> hoặc null
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertMetadata(Object metadata) {
        if (metadata == null) {
            return null;
        }

        try {
            // Nếu đã là Map thì return luôn
            if (metadata instanceof Map) {
                return (Map<String, Object>) metadata;
            }

            // Nếu là String JSON thì parse
            if (metadata instanceof String) {
                return objectMapper.readValue((String) metadata, Map.class);
            }

            // Các trường hợp khác: convert qua JSON rồi parse lại
            String json = objectMapper.writeValueAsString(metadata);
            return objectMapper.readValue(json, Map.class);

        } catch (Exception e) {
            log.warn("⚠️ Cannot convert metadata to Map: {}", e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Helper: Convert Map metadata sang JSON String (để lưu vào DB)
     * @param metadata Map metadata
     * @return JSON String hoặc null
     */
    public String convertMetadataToJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.error("❌ Cannot convert metadata to JSON: {}", e.getMessage());
            return null;
        }
    }
}