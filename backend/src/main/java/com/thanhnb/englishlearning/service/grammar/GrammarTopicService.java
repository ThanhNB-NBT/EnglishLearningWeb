package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.GrammarTopicDTO;
import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarTopicService {

    private final GrammarTopicRepository topicRepository;
    private final GrammarLessonRepository lessonRepository;
    private final GrammarLessonService lessonService;
    private final GrammarOrderService orderService;

    public Page<GrammarTopicDTO> getAllTopicsPaginated(Pageable pageable) {
        return topicRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    public GrammarTopicDTO getTopicById(Long id) {
        GrammarTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại với id: " + id));
        return convertToDTO(topic);
    }

    public GrammarTopicDTO createTopic(GrammarTopicDTO dto) {
        if (topicRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new RuntimeException("Tên topic đã tồn tại: " + dto.getName());
        }

        GrammarTopic topic = new GrammarTopic();
        topic.setName(dto.getName());
        topic.setDescription(dto.getDescription());
        topic.setLevelRequired(dto.getLevelRequired());
        topic.setOrderIndex(dto.getOrderIndex());
        topic.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        topic.setCreatedAt(LocalDateTime.now());

        GrammarTopic savedTopic = topicRepository.save(topic);
        log.info("Created new Grammar Topic: {}", savedTopic.getName());

        return convertToDTO(savedTopic);
    }

    public GrammarTopicDTO updateTopic(Long id, GrammarTopicDTO dto) {
        GrammarTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại với id: " + id));

        if (topicRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            throw new RuntimeException("Tên topic đã tồn tại: " + dto.getName());
        }

        topic.setName(dto.getName());
        topic.setDescription(dto.getDescription());
        topic.setLevelRequired(dto.getLevelRequired());
        topic.setOrderIndex(dto.getOrderIndex());
        
        if (dto.getIsActive() != null) {
            topic.setIsActive(dto.getIsActive());
        }

        GrammarTopic savedTopic = topicRepository.save(topic);
        log.info("Updated Grammar Topic: {}", savedTopic.getName());

        return convertToDTO(savedTopic);
    }

    public void deleteTopic(Long id) {
        GrammarTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại với id: " + id));

        // --- CASCADE DELETE LOGIC ---
        // 1. Lấy tất cả lessons thuộc topic này
        List<GrammarLesson> lessons = lessonRepository.findByTopicIdOrderByOrderIndexAsc(id);
        
        if (!lessons.isEmpty()) {
            log.info("Deleting {} lessons inside topic {}", lessons.size(), topic.getName());
            // 2. Xóa từng lesson (sử dụng cascade=true để xóa Question và Progress)
            for (GrammarLesson lesson : lessons) {
                // Gọi service lesson để đảm bảo logic xóa question/progress được thực thi đúng
                lessonService.deleteLesson(lesson.getId(), true);
            }
        }

        Integer deletedOrderIndex = topic.getOrderIndex();
        topicRepository.delete(topic);
        log.info("Deleted Grammar Topic: {} (orderIndex: {})", topic.getName(), deletedOrderIndex);

        orderService.reorderTopicsAfterDelete(deletedOrderIndex);
    }

    public void deactivateTopic(Long id) {
        GrammarTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại với id: " + id));

        topic.setIsActive(false);
        topicRepository.save(topic);
        log.info("Deactivated Grammar Topic: {}", topic.getName());
    }

    public void activateTopic(Long id) {
        GrammarTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại với id: " + id));

        topic.setIsActive(true);
        topicRepository.save(topic);
        log.info("Activated Grammar Topic: {}", topic.getName());
    }

    public Integer getNextOrderIndex() {
        Integer maxOrder = topicRepository.findMaxOrderIndex();
        if (maxOrder != null) {
            log.info("Max topic orderIndex: {}", maxOrder);
            return maxOrder + 1;
        }
        log.info("No topics found, returning 1");
        return 1;
    }

    private GrammarTopicDTO convertToDTO(GrammarTopic topic) {
        long totalLessons = lessonRepository.countByTopicId(topic.getId());
        return new GrammarTopicDTO(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getLevelRequired(),
                topic.getOrderIndex(),
                topic.getIsActive(),
                topic.getCreatedAt(),
                null, null, (int) totalLessons, null
        );
    }
}