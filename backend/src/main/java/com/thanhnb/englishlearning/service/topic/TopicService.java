package com.thanhnb.englishlearning.service.topic;

import com.thanhnb.englishlearning.dto.PaginatedResponse;
import com.thanhnb.englishlearning.dto.topic.TopicDto;
import com.thanhnb.englishlearning.dto.topic.request.CreateTopicRequest;
import com.thanhnb.englishlearning.dto.topic.request.UpdateTopicRequest;
import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.topic.TopicRepository;
import com.thanhnb.englishlearning.util.PaginationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopicService {

    private final TopicRepository topicRepository;
    private final TeacherAssignmentService assignmentService;

    @Transactional(readOnly = true)
    public PaginatedResponse<TopicDto> getAllTopics(ModuleType moduleType, int page, int size, String sort) {
        Pageable pageable = PaginationHelper.createPageable(page, size, sort);

        Page<Topic> topicPage = topicRepository.findByModuleType(moduleType, pageable);

        return PaginationHelper.toPaginatedResponse(topicPage, this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<TopicDto> getTopicsByModule(ModuleType moduleType, boolean isActive) {
        List<Topic> topics;
        if (isActive) {
            topics = topicRepository.findByModuleTypeAndIsActiveOrderByOrderIndexAsc(moduleType, true);
        } else {
            topics = topicRepository.findByModuleTypeOrderByOrderIndexAsc(moduleType);
        }
        return topics.stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public TopicDto getTopicById(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with ID: " + id));
        return mapToDto(topic);
    }

    // ==================== 1. CREATE (AUTO ORDER) ====================
    @Transactional
    public TopicDto createTopic(CreateTopicRequest request, ModuleType moduleType) {
        Topic topic = new Topic();
        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        topic.setLevelRequired(request.getLevelRequired());
        topic.setModuleType(moduleType);
        topic.setIsActive(true); // Mặc định Active khi mới tạo

        // LOGIC MỚI: Tự động tính OrderIndex nếu không truyền vào
        if (request.getOrderIndex() == null) {
            topic.setOrderIndex(getNextOrderIndex(moduleType));
        } else {
            topic.setOrderIndex(request.getOrderIndex());
        }

        Topic savedTopic = topicRepository.save(topic);
        return mapToDto(savedTopic);
    }

    // ==================== 2. UPDATE ====================

    @Transactional
    public TopicDto updateTopic(Long id, UpdateTopicRequest request) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with ID: " + id));

        if (request.getName() != null) {
            topic.setName(request.getName());
        }
        if (request.getDescription() != null) {
            topic.setDescription(request.getDescription());
        }
        if (request.getLevelRequired() != null) {
            topic.setLevelRequired(request.getLevelRequired());
        }

        if (request.getOrderIndex() != null) {
            topic.setOrderIndex(request.getOrderIndex());
        }

        if (request.getIsActive() != null) {
            topic.setIsActive(request.getIsActive());
        }

        Topic savedTopic = topicRepository.save(topic);
        return mapToDto(savedTopic);
    }

    // ==================== 3. DELETE (SMART RE-ORDER) ====================
    @Transactional
    public void deleteTopic(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        ModuleType moduleType = topic.getModuleType();

        // Bước 1: Hủy tất cả phân công giáo viên (Clean up dependencies)
        assignmentService.revokeAllAssignmentsByTopic(id);

        // Bước 2: Xóa Topic
        topicRepository.delete(topic);
        log.info("Deleted topic ID: {}", id);

        // Bước 3: [QUAN TRỌNG] Sắp xếp lại thứ tự các topic còn lại để lấp chỗ trống
        fixOrderIndexes(moduleType);
    }

    // ==================== 4. TOGGLE STATUS (QUICK ACTION) ====================
    @Transactional
    public TopicDto toggleStatus(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        // Đảo ngược trạng thái
        topic.setIsActive(!topic.getIsActive());

        return mapToDto(topicRepository.save(topic));
    }

    // ==================== 5. UTILITIES: FIX ORDER & GET NEXT ====================

    /**
     * Hàm chuẩn hóa lại thứ tự (Ví dụ: 1, 5, 9 -> 1, 2, 3)
     * Gọi hàm này sau khi Xóa hoặc khi Admin bấm nút "Fix Order"
     */
    @Transactional
    public void fixOrderIndexes(ModuleType moduleType) {
        List<Topic> topics = topicRepository.findByModuleTypeOrderByOrderIndexAsc(moduleType);

        int expectedOrder = 1;
        int fixedCount = 0;

        for (Topic t : topics) {
            // Nếu thứ tự hiện tại sai lệch so với mong đợi (1, 2, 3...)
            if (t.getOrderIndex() == null || t.getOrderIndex() != expectedOrder) {
                t.setOrderIndex(expectedOrder);
                fixedCount++;
            }
            expectedOrder++;
        }

        if (fixedCount > 0) {
            topicRepository.saveAll(topics); // Lưu lại các thay đổi
            log.info("Auto-fixed order for {} topics in module {}", fixedCount, moduleType);
        }
    }

    // Lấy số thứ tự tiếp theo (Max + 1)
    public Integer getNextOrderIndex(ModuleType moduleType) {
        Integer maxOrder = topicRepository.findMaxOrderIndexByModuleType(moduleType);
        return (maxOrder == null) ? 1 : maxOrder + 1;
    }

    // ==================== PRIVATE MAPPER ====================

    private TopicDto mapToDto(Topic topic) {
        long lessonCount = topicRepository.countLessonsById(topic.getId());
        return TopicDto.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .moduleType(topic.getModuleType())
                .levelRequired(topic.getLevelRequired())
                .orderIndex(topic.getOrderIndex())
                .isActive(topic.getIsActive())
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                // lessonCount và teacherCount có thể tính sau hoặc set tạm null
                .lessonCount((int) lessonCount)
                .teacherCount(0)
                .build();
    }
}