package com.thanhnb.englishlearning.service.topic;

import com.thanhnb.englishlearning.dto.topic.TopicUserDto;
import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.topic.TopicRepository;
import com.thanhnb.englishlearning.repository.user.UserRepository;
import com.thanhnb.englishlearning.service.grammar.GrammarLearningService;
import com.thanhnb.englishlearning.service.listening.ListeningLearningService;
import com.thanhnb.englishlearning.service.reading.ReadingLearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserTopicService {

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    
    // Inject các Service học tập (Đã refactor ở bước trước)
    private final ListeningLearningService listeningService;
    private final ReadingLearningService readingService;
    private final GrammarLearningService grammarService;

    /**
     * Lấy danh sách Topic theo Module dành cho User (Kèm bài học & tiến độ)
     */
    @Transactional(readOnly = true)
    public List<TopicUserDto> getTopicsForUser(ModuleType moduleType, Long userId) {
        // 1. Lấy user để check level
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        EnglishLevel userLevel = user.getEnglishLevel();
        
        // 2. Lấy topics (bao gồm CẢ topics cao hơn level của user)
        List<Topic> topics = topicRepository
            .findByModuleTypeAndIsActiveTrueOrderByOrderIndexAsc(moduleType);

        if (topics.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Map sang DTO với lock status
        return topics.stream().map(topic -> {
            TopicUserDto dto = mapToDto(topic);
            
            // ✅ CHECK LOCK STATUS
            boolean isLocked = isTopicLocked(topic, userLevel, moduleType);
            dto.setIsLocked(isLocked);
            
            if (isLocked) {
                // Locked → Không load lessons, chỉ show message
                dto.setLockMessage(getLockMessage(topic, userLevel));
                dto.setLessons(Collections.emptyList());
                dto.setTotalLessons(0);
                dto.setCompletedLessons(0);
            } else {
                // Unlocked → Load lessons bình thường
                List<?> lessons = switch (moduleType) {
                    case LISTENING -> listeningService.getAllLessonsForUser(userId, topic.getId());
                    case READING -> readingService.getAllLessonsForUser(userId, topic.getId());
                    case GRAMMAR -> grammarService.getAllLessonsForUser(userId, topic.getId());
                    default -> Collections.emptyList();
                };
                
                dto.setLessons(lessons);
                dto.setTotalLessons(lessons.size());
                
                long completedCount = countCompleted(lessons);
                dto.setCompletedLessons((int) completedCount);
            }

            return dto;
        }).collect(Collectors.toList());
    }

    private boolean isTopicLocked(Topic topic, EnglishLevel userLevel, ModuleType moduleType) {
        EnglishLevel topicLevel = topic.getLevelRequired();
        
        // Nếu topic không có level requirement → không lock
        if (topicLevel == null) {
            return false;
        }
        
        // Nếu user chưa set level → lock tất cả
        if (userLevel == null) {
            log.warn("User has no level set, locking topic {}", topic.getId());
            return true;
        }
        
        // ✅ SPECIAL: Grammar cho phép partial upgrade
        // User A1 đã hoàn thành Grammar A1 → có thể học Grammar A2
        if (moduleType == ModuleType.GRAMMAR) {
            // Cho phép xem topic cao hơn MỘT level
            int userLevelOrder = getLevelOrder(userLevel);
            int topicLevelOrder = getLevelOrder(topicLevel);
            
            // Allow: same level hoặc +1 level
            return topicLevelOrder > (userLevelOrder + 1);
        }
        
        // Reading/Listening: Chỉ cho xem topics <= user level
        return !isLevelSufficient(userLevel, topicLevel);
    }

    private String getLockMessage(Topic topic, EnglishLevel userLevel) {
        EnglishLevel requiredLevel = topic.getLevelRequired();
        
        if (userLevel == null) {
            return "Vui lòng thiết lập trình độ tiếng Anh trong hồ sơ cá nhân để mở khóa chủ đề này.";
        }
        
        return String.format(
            "Chủ đề này yêu cầu trình độ %s. Trình độ hiện tại của bạn: %s. " +
            "Hãy hoàn thành các chủ đề cấp thấp hơn để nâng cấp trình độ.",
            requiredLevel, userLevel
        );
    }

    private TopicUserDto mapToDto(Topic topic) {
        return TopicUserDto.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .moduleType(topic.getModuleType())
                .orderIndex(topic.getOrderIndex())
                .levelRequired(topic.getLevelRequired()) // ✅ Include level
                .build();
    }

    private boolean isLevelSufficient(EnglishLevel userLevel, EnglishLevel requiredLevel) {
        return getLevelOrder(userLevel) >= getLevelOrder(requiredLevel);
    }

    private int getLevelOrder(EnglishLevel level) {
        return switch (level) {
            case A1 -> 1;
            case A2 -> 2;
            case B1 -> 3;
            case B2 -> 4;
            case C1 -> 5;
            case C2 -> 6;
        };
    }

    // Helper: Đếm số bài đã hoàn thành trong list (Dùng Reflection cho tiện vì các DTO khác nhau)
    private long countCompleted(List<?> lessons) {
        if (lessons == null || lessons.isEmpty()) return 0;
        
        return lessons.stream().filter(lesson -> {
            try {
                var method = lesson.getClass().getMethod("getIsCompleted");
                Object result = method.invoke(lesson);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                return false;
            }
        }).count();
    }
}