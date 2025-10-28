package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.GrammarTopicDTO;
import com.thanhnb.englishlearning.entity.grammar.GrammarTopic;
import com.thanhnb.englishlearning.repository.grammar.GrammarTopicRepository;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarTopicService {

    private final GrammarTopicRepository topicRepository;
    private final GrammarLessonRepository lessonRepository;
    private final GrammarOrderService orderService;

    // ===== READ OPERATIONS =====

    public Page<GrammarTopicDTO> getAllTopicsPaginated(Pageable pageable) {
        return topicRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    public GrammarTopicDTO getTopicById(Long id) {
        GrammarTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));
        return convertToDTO(topic);
    }

    // ===== CREATE =====

    public GrammarTopicDTO createTopic(GrammarTopicDTO dto) {
        // Validate
        if (topicRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new RuntimeException("Tên topic đã tồn tại");
        }

        // Create entity
        GrammarTopic topic = new GrammarTopic();
        topic.setName(dto.getName());
        topic.setDescription(dto.getDescription());
        topic.setLevelRequired(dto.getLevelRequired());
        topic.setOrderIndex(dto.getOrderIndex());
        topic.setIsActive(dto.getIsActive());
        topic.setCreatedAt(LocalDateTime.now());

        GrammarTopic savedTopic = topicRepository.save(topic);
        log.info("✅ Created new Grammar Topic: {}", savedTopic.getName());

        return convertToDTO(savedTopic);
    }

    // ===== UPDATE =====

    public GrammarTopicDTO updateTopic(Long id, GrammarTopicDTO dto) {
        GrammarTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        // Validate unique name
        if (topicRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            throw new RuntimeException("Tên topic đã tồn tại: " + dto.getName());
        }

        // Update fields
        topic.setName(dto.getName());
        topic.setDescription(dto.getDescription());
        topic.setLevelRequired(dto.getLevelRequired());
        topic.setOrderIndex(dto.getOrderIndex());
        topic.setIsActive(dto.getIsActive());

        GrammarTopic savedTopic = topicRepository.save(topic);
        log.info("✅ Updated Grammar Topic: {}", savedTopic.getName());

        return convertToDTO(savedTopic);
    }

    // ===== DELETE =====

    public void deleteTopic(Long id) {
        GrammarTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        // Check if has lessons
        long lessonCount = lessonRepository.countByTopicId(id);
        if (lessonCount > 0) {
            throw new RuntimeException("Không thể xóa topic vì có " + lessonCount + " bài học thuộc topic này");
        }

        Integer deletedOrderIndex = topic.getOrderIndex();
        topicRepository.delete(topic);
        log.info("✅ Deleted Grammar Topic: {} (orderIndex: {})", topic.getName(), deletedOrderIndex);

        // Auto reorder
        orderService.reorderTopicsAfterDelete(deletedOrderIndex);
    }

    // ===== GET NEXT ORDER INDEX =====

    public Integer getNextOrderIndex() {
        Integer maxOrder = topicRepository.findMaxOrderIndex();
        if (maxOrder != null) {
            log.info("✅ Max topic orderIndex: {}", maxOrder);
            return maxOrder + 1;
        }
        log.info("⚠️ No topics found, returning 1");
        return 1;
    }

    // ===== CONVERSION =====

    private GrammarTopicDTO convertToDTO(GrammarTopic topic) {
        return new GrammarTopicDTO(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getLevelRequired(),
                topic.getOrderIndex(),
                topic.getIsActive(),
                topic.getCreatedAt(),
                null, null, null, null
        );
    }
}