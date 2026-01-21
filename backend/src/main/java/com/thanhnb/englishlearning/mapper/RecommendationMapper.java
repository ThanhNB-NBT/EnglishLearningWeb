package com.thanhnb.englishlearning.mapper;

import com.thanhnb.englishlearning.dto.ai.AIRecommendationDto;
import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.entity.recommendation.AIRecommendation;
import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.entity.user.UserLearningBehavior;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.topic.TopicRepository;
import com.thanhnb.englishlearning.repository.user.UserLearningBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationMapper {

    private final GrammarLessonRepository grammarRepo;
    private final ReadingLessonRepository readingRepo;
    private final ListeningLessonRepository listeningRepo;
    private final TopicRepository topicRepository;
    private final UserLearningBehaviorRepository behaviorRepository;
    private final QuestionRepository questionRepository;

    /**
     * ✅ Convert entity to full DTO with all enrichments
     */
    public AIRecommendationDto toDto(AIRecommendation entity) {
        if (entity == null) {
            log.warn("Received null recommendation entity");
            return null;
        }

        log.debug("Mapping recommendation: id={}, type={}, title={}",
                entity.getId(), entity.getType(), entity.getTitle());

        AIRecommendationDto dto = AIRecommendationDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .reasoning(entity.getReasoning())
                .priority(entity.getPriority())
                .targetSkill(entity.getTargetSkill())
                .targetLessonId(entity.getTargetLessonId())
                .targetTopicId(entity.getTargetTopicId())
                .isShown(entity.isShown())
                .isAccepted(entity.getIsAccepted())
                .isCompleted(entity.isCompleted())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .shownAt(entity.getShownAt())
                .build();

        // Enrich with lesson details
        enrichLessonDetails(dto, entity);

        // Enrich with topic details
        enrichTopicDetails(dto, entity);

        // Enrich with user progress
        enrichUserProgress(dto, entity);

        // Add smart badges
        addSmartBadges(dto);

        // Add time-based tags
        addTimeTags(dto);

        // Add motivation
        addMotivation(dto);

        // Set rewards
        dto.setPointsReward(calculatePointsReward(entity));

        log.debug("✅ Mapped recommendation {} successfully: title={}, badges={}",
                dto.getId(), dto.getTitle(), dto.getBadges());

        return dto;
    }

    /**
     * ✅ FIXED: Enrich with lesson details
     */
    private void enrichLessonDetails(AIRecommendationDto dto, AIRecommendation entity) {
        if (entity.getTargetLessonId() == null || entity.getTargetSkill() == null) {
            log.debug("No lesson target for recommendation {}", entity.getId());
            return;
        }

        try {
            switch (entity.getTargetSkill()) {
                case GRAMMAR -> {
                    Optional<GrammarLesson> lesson = grammarRepo.findById(entity.getTargetLessonId());
                    if (lesson.isPresent()) {
                        GrammarLesson l = lesson.get();
                        dto.setTargetLessonTitle(l.getTitle());

                        long questionCount = questionRepository.countByParentTypeAndParentId(
                                ParentType.GRAMMAR, entity.getTargetLessonId());
                        dto.setTotalQuestions((int) questionCount);
                        dto.setEstimatedMinutes(estimateTime(dto.getTotalQuestions()));

                        // ✅ FIX: Null-safe Topic access
                        if (l.getTopic() != null) {
                            try {
                                // ✅ Try to access topic fields safely
                                dto.setLessonDifficulty(l.getTopic().getLevelRequired());
                            } catch (org.hibernate.LazyInitializationException e) {
                                // ✅ If lazy loading fails, query topic separately
                                log.warn("Topic lazy load failed for lesson {}, fetching separately", l.getId());
                                topicRepository.findById(l.getTopic().getId()).ifPresent(topic -> {
                                    dto.setLessonDifficulty(topic.getLevelRequired());
                                });
                            }
                        }

                        log.debug("Enriched GRAMMAR lesson: {}, {} questions",
                                l.getTitle(), questionCount);
                    } else {
                        log.warn("Grammar lesson {} not found", entity.getTargetLessonId());
                    }
                }

                case READING -> {
                    Optional<ReadingLesson> lesson = readingRepo.findById(entity.getTargetLessonId());
                    if (lesson.isPresent()) {
                        ReadingLesson l = lesson.get();
                        dto.setTargetLessonTitle(l.getTitle());

                        long questionCount = questionRepository.countByParentTypeAndParentId(
                                ParentType.READING, entity.getTargetLessonId());
                        dto.setTotalQuestions((int) questionCount);
                        dto.setEstimatedMinutes(estimateTime(dto.getTotalQuestions()));

                        // ✅ FIX: Null-safe Topic access
                        if (l.getTopic() != null) {
                            try {
                                dto.setLessonDifficulty(l.getTopic().getLevelRequired());
                            } catch (org.hibernate.LazyInitializationException e) {
                                log.warn("Topic lazy load failed for lesson {}, fetching separately", l.getId());
                                topicRepository.findById(l.getTopic().getId()).ifPresent(topic -> {
                                    dto.setLessonDifficulty(topic.getLevelRequired());
                                });
                            }
                        }

                        log.debug("Enriched READING lesson: {}, {} questions",
                                l.getTitle(), questionCount);
                    } else {
                        log.warn("Reading lesson {} not found", entity.getTargetLessonId());
                    }
                }

                case LISTENING -> {
                    Optional<ListeningLesson> lesson = listeningRepo.findById(entity.getTargetLessonId());
                    if (lesson.isPresent()) {
                        ListeningLesson l = lesson.get();
                        dto.setTargetLessonTitle(l.getTitle());

                        long questionCount = questionRepository.countByParentTypeAndParentId(
                                ParentType.LISTENING, entity.getTargetLessonId());
                        dto.setTotalQuestions((int) questionCount);
                        dto.setEstimatedMinutes(estimateTime(dto.getTotalQuestions()));

                        // ✅ FIX: Null-safe Topic access
                        if (l.getTopic() != null) {
                            try {
                                dto.setLessonDifficulty(l.getTopic().getLevelRequired());
                            } catch (org.hibernate.LazyInitializationException e) {
                                log.warn("Topic lazy load failed for lesson {}, fetching separately", l.getId());
                                topicRepository.findById(l.getTopic().getId()).ifPresent(topic -> {
                                    dto.setLessonDifficulty(topic.getLevelRequired());
                                });
                            }
                        }

                        log.debug("Enriched LISTENING lesson: {}, {} questions",
                                l.getTitle(), questionCount);
                    } else {
                        log.warn("Listening lesson {} not found", entity.getTargetLessonId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to enrich lesson details for recommendation {}: {}",
                    entity.getId(), e.getMessage());
            // ✅ Don't throw - just log and continue with partial data
        }
    }

    /**
     * Enrich with topic details
     */
    private void enrichTopicDetails(AIRecommendationDto dto, AIRecommendation entity) {
        if (entity.getTargetTopicId() == null) {
            log.debug("No topic target for recommendation {}", entity.getId());
            return;
        }

        try {
            Optional<Topic> topic = topicRepository.findById(entity.getTargetTopicId());
            if (topic.isPresent()) {
                Topic t = topic.get();
                dto.setTargetTopicName(t.getName());

                // Get topic completion from user behavior
                UserLearningBehavior behavior = behaviorRepository
                        .findByUserId(entity.getUser().getId())
                        .orElse(null);

                if (behavior != null && behavior.getTopicProgress() != null) {
                    var progress = behavior.getTopicProgress().get(entity.getTargetTopicId().toString());
                    if (progress != null) {
                        dto.setTopicCompletionPercentage(progress.getCompletionPercentage());
                        log.debug("Topic {} completion: {}%", t.getName(), progress.getCompletionPercentage());
                    }
                }
            } else {
                log.warn("Topic {} not found", entity.getTargetTopicId());
            }
        } catch (Exception e) {
            log.error("Failed to enrich topic details: {}", e.getMessage(), e);
        }
    }

    /**
     * Enrich with user progress data
     */
    private void enrichUserProgress(AIRecommendationDto dto, AIRecommendation entity) {
        // TODO: Implement when UserProgress table is available
        // For now, set defaults
        dto.setIsPreviouslyAttempted(false);
        dto.setAttemptsCount(0);
        dto.setBestScore(0.0);
        dto.setIsInLearningPath(true);
    }

    /**
     * ✅ Add smart badges based on recommendation characteristics
     */
    private void addSmartBadges(AIRecommendationDto dto) {
        List<String> badges = new ArrayList<>();

        // Priority badges
        if (dto.getPriority() != null) {
            if (dto.getPriority() >= 6) {
                badges.add("🔥 KHẨN CẤP");
            } else if (dto.getPriority() >= 5) {
                badges.add("⚡ ƯU TIÊN");
            }
        }

        // Quick win badge
        if (dto.getEstimatedMinutes() != null && dto.getEstimatedMinutes() <= 10) {
            badges.add("⚡ NHANH");
        }

        // Progress badges
        if (dto.getTopicCompletionPercentage() != null) {
            if (dto.getTopicCompletionPercentage() > 80) {
                badges.add("🎯 SẮP XONG");
            } else if (dto.getTopicCompletionPercentage() > 50) {
                badges.add("📈 ĐANG HỌC");
            } else if (dto.getTopicCompletionPercentage() < 20) {
                badges.add("🆕 MỚI");
            }
        }

        // Type-specific badges
        switch (dto.getType()) {
            case "STREAK_SAVER" -> badges.add("🔥 CỨU STREAK");
            case "PRACTICE_WEAK_SKILL" -> badges.add("💪 CẢI THIỆN");
            case "REVIEW_LESSON" -> badges.add("📄 ÔN TẬP");
            case "MISTAKE_PATTERN" -> badges.add("🎯 KHẮC PHỤC");
            case "TIME_OPTIMAL" -> badges.add("⏰ THỜI ĐIỂM TỐT");
            case "CONTINUE_TOPIC" -> badges.add("📚 TIẾP TỤC");
        }

        // New content badge
        if (dto.getIsPreviouslyAttempted() != null && !dto.getIsPreviouslyAttempted()) {
            badges.add("✨ MỚI");
        }

        dto.setBadges(badges);
        log.debug("Added {} badges to recommendation {}", badges.size(), dto.getId());
    }

    /**
     * Add time-based recommendation tags
     */
    private void addTimeTags(AIRecommendationDto dto) {
        LocalTime now = LocalTime.now();

        if (dto.getTargetSkill() == null)
            return;

        // Morning (6:00 - 12:00): Best for Reading & Grammar
        if (now.isAfter(LocalTime.of(6, 0)) && now.isBefore(LocalTime.of(12, 0))) {
            if (dto.getTargetSkill() == ModuleType.READING) {
                dto.setTimeTag("☀️ BUỔI SÁNG TỐT");
            } else if (dto.getTargetSkill() == ModuleType.GRAMMAR) {
                dto.setTimeTag("☀️ TẬP TRUNG");
            }
        }

        // Afternoon (12:00 - 18:00): Balanced
        else if (now.isAfter(LocalTime.of(12, 0)) && now.isBefore(LocalTime.of(18, 0))) {
            dto.setTimeTag("☀️ BUỔI CHIỀU");
        }

        // Evening (18:00 - 22:00): Best for Listening & Review
        else if (now.isAfter(LocalTime.of(18, 0)) && now.isBefore(LocalTime.of(22, 0))) {
            if (dto.getTargetSkill() == ModuleType.LISTENING) {
                dto.setTimeTag("🌙 BUỔI TỐI TỐT");
            } else {
                dto.setTimeTag("🌙 ÔN TẬP");
            }
        }

        // Late night (22:00+): Light content only
        else {
            if (dto.getEstimatedMinutes() != null && dto.getEstimatedMinutes() <= 10) {
                dto.setTimeTag("🌃 NHANH");
            }
        }
    }

    /**
     * ✅ Add personalized motivation message
     */
    private void addMotivation(AIRecommendationDto dto) {
        String message = switch (dto.getType()) {
            case "STREAK_SAVER" -> "Đừng để streak đứt! Bạn làm được! 💪";
            case "REVIEW_LESSON" -> "Ôn tập giúp nhớ lâu hơn! 🧠";
            case "MISTAKE_PATTERN" -> "Khắc phục điểm yếu = Tiến bộ vượt bậc! 🚀";
            case "PRACTICE_WEAK_SKILL" -> "Luyện tập chăm chỉ sẽ thấy kết quả! 💯";
            case "TIME_OPTIMAL" -> "Đây là thời điểm học hiệu quả nhất! ⏰";
            case "CONTINUE_TOPIC" -> {
                if (dto.getTopicCompletionPercentage() != null && dto.getTopicCompletionPercentage() > 80) {
                    yield "Gần hoàn thành rồi, cố lên! 🎯";
                } else {
                    yield "Tiếp tục nào, bạn đang làm tốt lắm! 📚";
                }
            }
            default -> "Bắt đầu ngay để đạt mục tiêu! 🌟";
        };

        dto.setMotivationMessage(message);
    }

    /**
     * Calculate points reward based on difficulty and type
     */
    private Integer calculatePointsReward(AIRecommendation entity) {
        int basePoints = 30;

        // Priority bonus
        if (entity.getPriority() != null && entity.getPriority() >= 4) {
            basePoints += 20;
        }

        // Type bonus
        if ("PRACTICE_WEAK_SKILL".equals(entity.getType())) {
            basePoints += 30; // Extra reward for practicing weak skills
        } else if ("STREAK_SAVER".equals(entity.getType())) {
            basePoints += 40; // Highest reward for saving streak
        }

        return basePoints;
    }

    /**
     * Estimate completion time based on question count
     */
    private Integer estimateTime(Integer questionCount) {
        if (questionCount == null || questionCount == 0) {
            return 10; // Default
        }

        // Assume 1.5 minutes per question on average
        return (int) Math.ceil(questionCount * 1.5);
    }
}