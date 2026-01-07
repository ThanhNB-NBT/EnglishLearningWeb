package com.thanhnb.englishlearning.service.level;

import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.grammar.UserGrammarProgressRepository;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning.repository.listening.UserListeningProgressRepository;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.reading.UserReadingProgressRepository;
import com.thanhnb.englishlearning.repository.topic.TopicRepository;
import com.thanhnb.englishlearning.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service xử lý logic nâng cấp level cho user
 * 
 * Rules:
 * - User phải complete 100% lessons trong topic của level hiện tại
 * - Grammar: Cho phép partial upgrade (chỉ cần hoàn thành Grammar topic)
 * - Reading/Listening: Phải hoàn thành cả 3 kỹ năng mới unlock level cao hơn
 * - Level tăng theo thứ tự: A1 → A2 → B1 → B2 → C1 → C2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LevelUpgradeService {

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    
    // Progress Repositories
    private final UserGrammarProgressRepository grammarProgressRepo;
    private final UserReadingProgressRepository readingProgressRepo;
    private final UserListeningProgressRepository listeningProgressRepo;
    
    // Lesson Repositories
    private final GrammarLessonRepository grammarLessonRepo;
    private final ReadingLessonRepository readingLessonRepo;
    private final ListeningLessonRepository listeningLessonRepo;

    /**
     * Kiểm tra và nâng level cho user sau khi complete lesson
     * 
     * @param userId ID của user
     * @param moduleType Module vừa complete (GRAMMAR/READING/LISTENING)
     * @param completedTopicId ID của topic vừa complete lesson cuối
     * @return LevelUpgradeResult chứa thông tin về việc nâng level
     */
    @Transactional
    public LevelUpgradeResult checkAndUpgradeLevel(Long userId, ModuleType moduleType, Long completedTopicId) {
        log.info("Checking level upgrade for user={}, module={}, topicId={}", 
            userId, moduleType, completedTopicId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        EnglishLevel currentLevel = user.getEnglishLevel();
        
        if (currentLevel == null) {
            log.warn("User {} has no level set, cannot upgrade", userId);
            return LevelUpgradeResult.noUpgrade("Vui lòng thiết lập trình độ ban đầu");
        }

        // Kiểm tra xem đã complete hết lessons trong topic này chưa
        if (!isTopicCompleted(userId, moduleType, completedTopicId)) {
            log.debug("Topic {} not fully completed yet for user {}", completedTopicId, userId);
            return LevelUpgradeResult.noUpgrade("Hoàn thành tất cả bài học trong chủ đề này");
        }

        // ✅ Topic vừa complete xong
        log.info("User {} completed topic {} in {}", userId, completedTopicId, moduleType);

        // Kiểm tra điều kiện nâng level theo module
        if (moduleType == ModuleType.GRAMMAR) {
            // Grammar: Cho phép partial upgrade
            return checkGrammarUpgrade(user, currentLevel);
        } else {
            // Reading/Listening: Cần đủ 3 kỹ năng
            return checkFullUpgrade(user, currentLevel, moduleType);
        }
    }

    /**
     * Kiểm tra nâng level cho Grammar (partial upgrade)
     */
    private LevelUpgradeResult checkGrammarUpgrade(User user, EnglishLevel currentLevel) {
        Long userId = user.getId();
        
        // Kiểm tra xem đã hoàn thành hết topics Grammar của level hiện tại chưa
        boolean allGrammarTopicsCompleted = areAllTopicsCompleted(userId, ModuleType.GRAMMAR, currentLevel);
        
        if (!allGrammarTopicsCompleted) {
            return LevelUpgradeResult.inProgress(
                "Bạn đã hoàn thành chủ đề này! Tiếp tục hoàn thành các chủ đề Grammar còn lại."
            );
        }

        // ✅ Đã hoàn thành hết Grammar topics của level hiện tại
        // Kiểm tra xem Reading/Listening đã đủ chưa
        boolean readingCompleted = areAllTopicsCompleted(userId, ModuleType.READING, currentLevel);
        boolean listeningCompleted = areAllTopicsCompleted(userId, ModuleType.LISTENING, currentLevel);

        if (readingCompleted && listeningCompleted) {
            // ✅ Đủ cả 3 kỹ năng → Nâng level
            return upgradeUserLevel(user, currentLevel);
        } else {
            // Grammar xong nhưng 2 kỹ năng kia chưa
            String missingSkills = getMissingSkills(readingCompleted, listeningCompleted);
            return LevelUpgradeResult.partialComplete(
                String.format("Chúc mừng! Bạn đã hoàn thành tất cả chủ đề Ngữ pháp cấp %s. " +
                    "Hãy hoàn thành các chủ đề %s để nâng cấp trình độ.", 
                    currentLevel, missingSkills)
            );
        }
    }

    /**
     * Kiểm tra nâng level cho Reading/Listening (cần đủ 3 kỹ năng)
     */
    private LevelUpgradeResult checkFullUpgrade(User user, EnglishLevel currentLevel, ModuleType completedModule) {
        Long userId = user.getId();
        
        // Kiểm tra xem đã hoàn thành hết topics của module này chưa
        boolean thisModuleCompleted = areAllTopicsCompleted(userId, completedModule, currentLevel);
        
        if (!thisModuleCompleted) {
            String moduleName = getModuleName(completedModule);
            return LevelUpgradeResult.inProgress(
                String.format("Bạn đã hoàn thành chủ đề này! Tiếp tục hoàn thành các chủ đề %s còn lại.", 
                    moduleName)
            );
        }

        // ✅ Module này đã xong, check 2 module còn lại
        boolean grammarCompleted = areAllTopicsCompleted(userId, ModuleType.GRAMMAR, currentLevel);
        boolean readingCompleted = areAllTopicsCompleted(userId, ModuleType.READING, currentLevel);
        boolean listeningCompleted = areAllTopicsCompleted(userId, ModuleType.LISTENING, currentLevel);

        if (grammarCompleted && readingCompleted && listeningCompleted) {
            // ✅ Đủ cả 3 kỹ năng → Nâng level
            return upgradeUserLevel(user, currentLevel);
        } else {
            // Module này xong nhưng còn module khác chưa
            String missingSkills = getMissingSkillsForFull(
                completedModule, grammarCompleted, readingCompleted, listeningCompleted
            );
            return LevelUpgradeResult.partialComplete(
                String.format("Chúc mừng! Bạn đã hoàn thành tất cả chủ đề %s cấp %s. " +
                    "Hãy hoàn thành các chủ đề %s để nâng cấp trình độ.", 
                    getModuleName(completedModule), currentLevel, missingSkills)
            );
        }
    }

    /**
     * Nâng level cho user
     */
    private LevelUpgradeResult upgradeUserLevel(User user, EnglishLevel currentLevel) {
        EnglishLevel nextLevel = getNextLevel(currentLevel);
        
        if (nextLevel == null) {
            // Đã đạt level cao nhất
            return LevelUpgradeResult.maxLevel(
                String.format("Chúc mừng! Bạn đã hoàn thành tất cả các chủ đề cấp %s và đạt trình độ cao nhất!", 
                    currentLevel)
            );
        }

        // ✅ Nâng level
        user.setEnglishLevel(nextLevel);
        userRepository.save(user);

        log.info("User {} upgraded from {} to {}", user.getId(), currentLevel, nextLevel);

        return LevelUpgradeResult.upgraded(
            currentLevel,
            nextLevel,
            String.format("🎉 Chúc mừng! Bạn đã được nâng cấp từ %s lên %s! " +
                "Giờ bạn có thể học các bài học cấp độ cao hơn.", 
                currentLevel, nextLevel)
        );
    }

    /**
     * Kiểm tra xem topic đã được complete 100% chưa
     */
    private boolean isTopicCompleted(Long userId, ModuleType moduleType, Long topicId) {
        // Lấy tổng số lessons trong topic
        long totalLessons = getTotalLessonsInTopic(moduleType, topicId);
        
        if (totalLessons == 0) {
            return false;
        }

        // Lấy số lessons đã complete
        long completedLessons = getCompletedLessonsInTopic(userId, moduleType, topicId);

        log.debug("Topic {} completion: {}/{}", topicId, completedLessons, totalLessons);
        
        return completedLessons >= totalLessons;
    }

    /**
     * Kiểm tra xem tất cả topics của 1 module ở 1 level đã complete chưa
     */
    private boolean areAllTopicsCompleted(Long userId, ModuleType moduleType, EnglishLevel level) {
        // Lấy tất cả topic IDs của module + level này
        var topicIds = topicRepository.findByModuleTypeAndIsActiveTrueOrderByOrderIndexAsc(moduleType)
            .stream()
            .filter(t -> t.getLevelRequired() != null && t.getLevelRequired() == level)
            .map(t -> t.getId())
            .toList();

        if (topicIds.isEmpty()) {
            log.warn("No topics found for module={}, level={}", moduleType, level);
            return false;
        }

        // Kiểm tra từng topic
        for (Long topicId : topicIds) {
            if (!isTopicCompleted(userId, moduleType, topicId)) {
                log.debug("Topic {} not completed for user {}", topicId, userId);
                return false;
            }
        }

        return true;
    }

    private long getTotalLessonsInTopic(ModuleType moduleType, Long topicId) {
        return switch (moduleType) {
            case GRAMMAR -> grammarLessonRepo.countByTopicIdAndIsActiveTrue(topicId);
            case READING -> readingLessonRepo.countByTopicIdAndIsActiveTrue(topicId);
            case LISTENING -> listeningLessonRepo.countByTopicIdAndIsActiveTrue(topicId);
            default -> 0L;
        };
    }

    private long getCompletedLessonsInTopic(Long userId, ModuleType moduleType, Long topicId) {
        return switch (moduleType) {
            case GRAMMAR -> grammarProgressRepo.countCompletedLessonsByUserAndTopic(userId, topicId);
            case READING -> readingProgressRepo.countCompletedLessonsByUserAndTopic(userId, topicId);
            case LISTENING -> listeningProgressRepo.countCompletedLessonsByUserAndTopic(userId, topicId);
            default -> 0L;
        };
    }

    private EnglishLevel getNextLevel(EnglishLevel currentLevel) {
        return switch (currentLevel) {
            case A1 -> EnglishLevel.A2;
            case A2 -> EnglishLevel.B1;
            case B1 -> EnglishLevel.B2;
            case B2 -> EnglishLevel.C1;
            case C1 -> EnglishLevel.C2;
            case C2 -> null; // Đã max level
        };
    }

    private String getModuleName(ModuleType moduleType) {
        return switch (moduleType) {
            case GRAMMAR -> "Ngữ pháp";
            case READING -> "Đọc hiểu";
            case LISTENING -> "Nghe hiểu";
            default -> moduleType.name();
        };
    }

    private String getMissingSkills(boolean readingCompleted, boolean listeningCompleted) {
        if (!readingCompleted && !listeningCompleted) {
            return "Đọc hiểu và Nghe hiểu";
        } else if (!readingCompleted) {
            return "Đọc hiểu";
        } else {
            return "Nghe hiểu";
        }
    }

    private String getMissingSkillsForFull(ModuleType completed, 
            boolean grammarCompleted, boolean readingCompleted, boolean listeningCompleted) {
        StringBuilder missing = new StringBuilder();
        
        if (!grammarCompleted && completed != ModuleType.GRAMMAR) {
            missing.append("Ngữ pháp");
        }
        if (!readingCompleted && completed != ModuleType.READING) {
            if (missing.length() > 0) missing.append(" và ");
            missing.append("Đọc hiểu");
        }
        if (!listeningCompleted && completed != ModuleType.LISTENING) {
            if (missing.length() > 0) missing.append(" và ");
            missing.append("Nghe hiểu");
        }
        
        return missing.toString();
    }

    /**
     * DTO chứa kết quả kiểm tra nâng level
     */
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class LevelUpgradeResult {
        private final boolean upgraded;           // Có nâng level không
        private final boolean partialComplete;    // Hoàn thành 1 phần (ví dụ chỉ Grammar)
        private final boolean maxLevelReached;    // Đã đạt level cao nhất
        private final EnglishLevel oldLevel;      // Level cũ (nếu có upgrade)
        private final EnglishLevel newLevel;      // Level mới (nếu có upgrade)
        private final String message;             // Message hiển thị cho user

        public static LevelUpgradeResult upgraded(EnglishLevel oldLevel, EnglishLevel newLevel, String message) {
            return new LevelUpgradeResult(true, false, false, oldLevel, newLevel, message);
        }

        public static LevelUpgradeResult partialComplete(String message) {
            return new LevelUpgradeResult(false, true, false, null, null, message);
        }

        public static LevelUpgradeResult inProgress(String message) {
            return new LevelUpgradeResult(false, false, false, null, null, message);
        }

        public static LevelUpgradeResult maxLevel(String message) {
            return new LevelUpgradeResult(false, false, true, null, null, message);
        }

        public static LevelUpgradeResult noUpgrade(String message) {
            return new LevelUpgradeResult(false, false, false, null, null, message);
        }
    }
}