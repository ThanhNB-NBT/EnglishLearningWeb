package com.thanhnb.englishlearning.service.ai.recommendation;

import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.grammar.UserGrammarProgress;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.entity.listening.UserListeningProgress;
import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.entity.reading.UserReadingProgress;
import com.thanhnb.englishlearning.entity.recommendation.AIRecommendation;
import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.entity.user.UserLearningBehavior;
import com.thanhnb.englishlearning.entity.user.UserStats;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.grammar.UserGrammarProgressRepository;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning.repository.listening.UserListeningProgressRepository;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.reading.UserReadingProgressRepository;
import com.thanhnb.englishlearning.repository.recommendation.AIRecommendationRepository;
import com.thanhnb.englishlearning.repository.topic.TopicRepository;
import com.thanhnb.englishlearning.repository.user.UserLearningBehaviorRepository;
import com.thanhnb.englishlearning.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIRecommendationService {

    private final UserLearningBehaviorRepository behaviorRepository;
    private final AIRecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final GrammarLessonRepository grammarRepo;
    private final ReadingLessonRepository readingRepo;
    private final ListeningLessonRepository listeningRepo;
    private final UserGrammarProgressRepository grammarProgressRepo;
    private final UserReadingProgressRepository readingProgressRepo;
    private final UserListeningProgressRepository listeningProgressRepo;
    private static final int RECOMMENDATION_CACHE_HOURS = 6;
    private static final int MAX_RECOMMENDATIONS = 5;
    private static final int REVIEW_DAYS_THRESHOLD = 7;
    private static final double REVIEW_SCORE_THRESHOLD = 80.0; // 80%
    private static final double MISTAKE_SCORE_THRESHOLD = 75.0; // Topics with avg < 75%

    // =========================================================================
    // MAIN ENTRY POINT
    // =========================================================================

    @Transactional
    public List<AIRecommendation> generateRecommendations(Long userId) {
        log.info("🎯 Generating AI recommendations for user {}", userId);

        try {
            // ✅ FIX 1: Validate cached recommendations
            List<AIRecommendation> cached = getValidCachedRecommendations(userId);

            if (!cached.isEmpty()) {
                log.info("📦 Found {} valid cached recommendations", cached.size());
                return cached;
            }

            // ✅ FIX 2: Generate new recommendations in separate transaction
            return generateNewRecommendations(userId);

        } catch (Exception e) {
            log.error("❌ Failed to generate recommendations: {}", e.getMessage(), e);

            // ✅ FIX 3: Fallback without throwing exception
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    return generateDefaultRecommendations(user);
                }
            } catch (Exception fallbackError) {
                log.error("❌ Even fallback failed: {}", fallbackError.getMessage());
            }

            return Collections.emptyList();
        }
    }

    private List<AIRecommendation> getValidCachedRecommendations(Long userId) {
        try {
            List<AIRecommendation> cached = recommendationRepository
                    .findByUserIdAndExpiresAtAfterAndIsCompletedFalseOrderByPriorityDesc(
                            userId, LocalDateTime.now());

            if (cached.isEmpty()) {
                return Collections.emptyList();
            }

            // ✅ Validate that referenced entities still exist and are active
            List<AIRecommendation> validRecs = new ArrayList<>();

            for (AIRecommendation rec : cached) {
                if (isRecommendationValid(rec)) {
                    validRecs.add(rec);
                } else {
                    log.warn("⚠️ Removing invalid cached recommendation: {}", rec.getId());
                    // Mark as invalid by setting it completed
                    try {
                        rec.setCompleted(true);
                        recommendationRepository.save(rec);
                    } catch (Exception e) {
                        log.error("Failed to invalidate recommendation {}: {}",
                                rec.getId(), e.getMessage());
                    }
                }
            }

            return validRecs;

        } catch (Exception e) {
            log.error("Error validating cached recommendations: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // =========================================================================
    // Check if recommendation is still valid
    // =========================================================================

    private boolean isRecommendationValid(AIRecommendation rec) {
        try {
            // Check if topic exists and is active
            if (rec.getTargetTopicId() != null) {
                Optional<Topic> topic = topicRepository.findById(rec.getTargetTopicId());
                if (topic.isEmpty() || topic.get().getIsActive() == null ||
                        !topic.get().getIsActive()) {
                    log.debug("Topic {} is inactive or missing", rec.getTargetTopicId());
                    return false;
                }
            }

            // Check if lesson exists and is active
            if (rec.getTargetLessonId() != null && rec.getTargetSkill() != null) {
                boolean lessonExists = checkLessonExists(
                        rec.getTargetLessonId(),
                        rec.getTargetSkill());

                if (!lessonExists) {
                    log.debug("Lesson {} ({}) is inactive or missing",
                            rec.getTargetLessonId(), rec.getTargetSkill());
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Error validating recommendation {}: {}", rec.getId(), e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // Check if lesson exists and is active
    // =========================================================================

    private boolean checkLessonExists(Long lessonId, ModuleType skill) {
        try {
            switch (skill) {
                case GRAMMAR -> {
                    Optional<GrammarLesson> lesson = grammarRepo.findById(lessonId);
                    return lesson.isPresent() &&
                            lesson.get().getIsActive() != null &&
                            lesson.get().getIsActive();
                }
                case READING -> {
                    Optional<ReadingLesson> lesson = readingRepo.findById(lessonId);
                    return lesson.isPresent() &&
                            lesson.get().getIsActive() != null &&
                            lesson.get().getIsActive();
                }
                case LISTENING -> {
                    Optional<ListeningLesson> lesson = listeningRepo.findById(lessonId);
                    return lesson.isPresent() &&
                            lesson.get().getIsActive() != null &&
                            lesson.get().getIsActive();
                }
                default -> {
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error checking lesson existence: {}", e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // Generate new recommendations in separate transaction
    // =========================================================================

    @Transactional
    public List<AIRecommendation> generateNewRecommendations(Long userId) {
        log.info("🔄 Generating NEW recommendations for user {}", userId);

        try {
            // Get user data
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            UserLearningBehavior behavior = behaviorRepository.findByUserId(userId)
                    .orElse(null);

            if (behavior == null) {
                log.warn("⚠️ No learning behavior data for user {}", userId);
                return generateDefaultRecommendations(user);
            }

            // Generate recommendations
            RecommendationContext context = buildRecommendationContext(user, behavior);
            List<AIRecommendation> recommendations = generateSmartRecommendations(context);

            // ✅ Save and return
            List<AIRecommendation> saved = recommendationRepository.saveAll(recommendations);
            log.info("✅ Generated {} NEW recommendations for user {}", saved.size(), userId);

            return saved;

        } catch (Exception e) {
            log.error("❌ Error in generateNewRecommendations: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate recommendations: " + e.getMessage(), e);
        }
    }

    @CacheEvict(value = "recommendations", key = "#userId")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateRecommendations(Long userId) {
        log.info("🗑️ Invalidating recommendations for user {}", userId);

        try {
            List<AIRecommendation> allRecs = recommendationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId);

            if (!allRecs.isEmpty()) {
                recommendationRepository.deleteAll(allRecs);
                recommendationRepository.flush();
                log.info("✅ Deleted {} recommendations", allRecs.size());
            }
        } catch (Exception e) {
            log.error("❌ Error invalidating recommendations: {}", e.getMessage(), e);
            // Don't throw - just log
        }
    }

    @Transactional(readOnly = true)
    public AIRecommendation getRecommendationById(Long recommendationId, Long userId) {
        AIRecommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));

        if (!rec.getUser().getId().equals(userId)) {
            throw new SecurityException("Not authorized");
        }

        // ✅ Update shown status in separate transaction if needed
        if (!rec.isShown()) {
            updateRecommendationShown(recommendationId);
        }

        return rec;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRecommendationShown(Long recommendationId) {
        try {
            AIRecommendation rec = recommendationRepository.findById(recommendationId)
                    .orElse(null);

            if (rec != null && !rec.isShown()) {
                rec.setShown(true);
                rec.setShownAt(LocalDateTime.now());
                recommendationRepository.save(rec);
            }
        } catch (Exception e) {
            log.error("Error updating recommendation shown status: {}", e.getMessage());
        }
    }

    // =========================================================================
    // SMART RECOMMENDATION GENERATION
    // =========================================================================

    private List<AIRecommendation> generateSmartRecommendations(
            RecommendationContext context) throws Exception {

        List<AIRecommendation> recommendations = new ArrayList<>();

        // ==================== STREAK SAVER ====================
        if (!context.getUserState().hasStreakToday()) {
            AIRecommendation streakRec = generateStreakSaverRecommendation(context);
            if (streakRec != null) {
                recommendations.add(streakRec);
                log.info("🔥 Added STREAK_SAVER recommendation");
            }
        }
        // ==================== PRIORITY 1: REVIEW LESSONS ====================
        List<AIRecommendation> reviewRecs = generateReviewRecommendations(context);
        if (!reviewRecs.isEmpty()) {
            recommendations.addAll(reviewRecs);
            log.info("🔄 Added {} review recommendations", reviewRecs.size());
        }

        // ==================== PRIORITY 2: MISTAKE PATTERN ====================
        AIRecommendation mistakeRec = generateMistakePatternRecommendation(context);
        if (mistakeRec != null) {
            recommendations.add(mistakeRec);
            log.info("🎯 Added mistake pattern recommendation");
        }

        // ==================== PRIORITY 3: WEAK SKILL ====================
        if (context.getUserState().hasWeakSkill()) {
            AIRecommendation weakRec = generateWeaknessRecommendation(context);
            if (weakRec != null) {
                recommendations.add(weakRec);
            }
        }

        // ==================== PRIORITY 4: CONTINUE PROGRESS ====================
        if (context.getUserState().hasInProgressLessons()) {
            List<AIRecommendation> progressRecs = generateProgressRecommendations(context);
            recommendations.addAll(progressRecs);
            log.info("📚 Added {} CONTINUE_TOPIC recommendations", progressRecs.size());
        }

        // ==================== PRIORITY 5: TIME OPTIMAL ====================
        if (context.getTimeContext().isMorning()) {
            AIRecommendation morningRec = generateMorningRecommendation(context);
            if (morningRec != null)
                recommendations.add(morningRec);
        } else if (context.getTimeContext().isEvening()) {
            AIRecommendation eveningRec = generateEveningRecommendation(context);
            if (eveningRec != null)
                recommendations.add(eveningRec);
        }

        // ==================== FALLBACK: NEXT LESSON ====================
        int neededNextLessons = Math.max(0, MAX_RECOMMENDATIONS - recommendations.size());
        if (neededNextLessons > 0) {
            List<AIRecommendation> nextLessons = generateNextLessonRecommendations(context);
            recommendations.addAll(nextLessons.stream().limit(neededNextLessons).toList());
            log.info("➡️ Added {} NEXT_LESSON recommendations",
                    Math.min(nextLessons.size(), neededNextLessons));
        }

        // Ensure diversity
        recommendations = ensureDiversity(recommendations, context.getUser());

        // Sort by priority and limit
        List<AIRecommendation> result = recommendations.stream()
                .sorted(Comparator.comparing(AIRecommendation::getPriority).reversed())
                .limit(MAX_RECOMMENDATIONS)
                .collect(Collectors.toList());

        log.info("✅ Generated {} total recommendations (types: {})",
                result.size(),
                result.stream().map(AIRecommendation::getType).distinct().collect(Collectors.joining(", ")));

        return result;
    }

    // =========================================================================
    // REVIEW LESSON RECOMMENDATIONS
    // =========================================================================

    private List<AIRecommendation> generateReviewRecommendations(RecommendationContext ctx) {
        List<AIRecommendation> recs = new ArrayList<>();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(REVIEW_DAYS_THRESHOLD);

        try {
            // Check Grammar lessons needing review
            List<UserGrammarProgress> grammarReviews = grammarProgressRepo
                    .findByUserIdAndIsCompletedTrueAndScorePercentageLessThanAndCompletedAtBefore(
                            ctx.getUser().getId(), REVIEW_SCORE_THRESHOLD, cutoffDate)
                    .stream()
                    .limit(1)
                    .collect(Collectors.toList());

            for (UserGrammarProgress progress : grammarReviews) {
                GrammarLesson lesson = progress.getLesson();
                long daysSince = ChronoUnit.DAYS.between(progress.getCompletedAt(), LocalDateTime.now());

                AIRecommendation rec = AIRecommendation.builder()
                        .user(ctx.getUser())
                        .type("REVIEW_LESSON")
                        .title("🔄 Ôn lại: " + lesson.getTitle())
                        .description(String.format(
                                "Lần trước bạn đạt %.0f%%. Làm lại để cải thiện!\n" +
                                        "⏰ Đã %d ngày chưa ôn bài này",
                                progress.getScorePercentage(),
                                daysSince))
                        .reasoning(String.format(
                                "User scored %.0f%% on this lesson %d days ago. Needs review.",
                                progress.getScorePercentage(), daysSince))
                        .targetSkill(ModuleType.GRAMMAR)
                        .targetLessonId(lesson.getId())
                        .targetTopicId(lesson.getTopic().getId())
                        .priority(5)
                        .isShown(false)
                        .isCompleted(false)
                        .expiresAt(LocalDateTime.now().plusHours(RECOMMENDATION_CACHE_HOURS))
                        .build();

                recs.add(rec);
            }

            // Check Reading lessons needing review
            List<UserReadingProgress> readingReviews = readingProgressRepo
                    .findByUserIdAndIsCompletedTrueAndScorePercentageLessThanAndCompletedAtBefore(
                            ctx.getUser().getId(), REVIEW_SCORE_THRESHOLD, cutoffDate)
                    .stream()
                    .limit(1)
                    .collect(Collectors.toList());

            for (UserReadingProgress progress : readingReviews) {
                ReadingLesson lesson = progress.getLesson();
                long daysSince = ChronoUnit.DAYS.between(progress.getCompletedAt(), LocalDateTime.now());

                AIRecommendation rec = AIRecommendation.builder()
                        .user(ctx.getUser())
                        .type("REVIEW_LESSON")
                        .title("🔄 Ôn lại: " + lesson.getTitle())
                        .description(String.format(
                                "Lần trước bạn đạt %.0f%%. Làm lại để cải thiện!\n" +
                                        "⏰ Đã %d ngày chưa ôn bài này",
                                progress.getScorePercentage(),
                                daysSince))
                        .reasoning(String.format(
                                "User scored %.0f%% on this lesson %d days ago. Needs review.",
                                progress.getScorePercentage(), daysSince))
                        .targetSkill(ModuleType.READING)
                        .targetLessonId(lesson.getId())
                        .targetTopicId(lesson.getTopic().getId())
                        .priority(5)
                        .isShown(false)
                        .isCompleted(false)
                        .expiresAt(LocalDateTime.now().plusHours(RECOMMENDATION_CACHE_HOURS))
                        .build();

                recs.add(rec);
            }

            // Check Listening lessons needing review
            List<UserListeningProgress> listeningReviews = listeningProgressRepo
                    .findByUserIdAndIsCompletedTrueAndScorePercentageLessThanAndCompletedAtBefore(
                            ctx.getUser().getId(), REVIEW_SCORE_THRESHOLD, cutoffDate)
                    .stream()
                    .limit(1)
                    .collect(Collectors.toList());

            for (UserListeningProgress progress : listeningReviews) {
                ListeningLesson lesson = progress.getLesson();
                long daysSince = ChronoUnit.DAYS.between(progress.getCompletedAt(), LocalDateTime.now());

                AIRecommendation rec = AIRecommendation.builder()
                        .user(ctx.getUser())
                        .type("REVIEW_LESSON")
                        .title("🔄 Ôn lại: " + lesson.getTitle())
                        .description(String.format(
                                "Lần trước bạn đạt %.0f%%. Làm lại để cải thiện!\n" +
                                        "⏰ Đã %d ngày chưa ôn bài này",
                                progress.getScorePercentage(),
                                daysSince))
                        .reasoning(String.format(
                                "User scored %.0f%% on this lesson %d days ago. Needs review.",
                                progress.getScorePercentage(), daysSince))
                        .targetSkill(ModuleType.LISTENING)
                        .targetLessonId(lesson.getId())
                        .targetTopicId(lesson.getTopic().getId())
                        .priority(5)
                        .isShown(false)
                        .isCompleted(false)
                        .expiresAt(LocalDateTime.now().plusHours(RECOMMENDATION_CACHE_HOURS))
                        .build();

                recs.add(rec);
            }

            log.debug("Generated {} review recommendations", recs.size());

        } catch (Exception e) {
            log.error("Error generating review recommendations: {}", e.getMessage());
        }

        return recs.stream().limit(1).collect(Collectors.toList());
    }

    // =========================================================================
    // MISTAKE PATTERN RECOMMENDATION
    // =========================================================================

    private AIRecommendation generateMistakePatternRecommendation(RecommendationContext ctx) {
        try {
            // Find topics where user has low average score
            Map<Long, TopicScore> topicScores = new HashMap<>();

            // Aggregate Grammar scores by topic
            List<UserGrammarProgress> grammarProgress = grammarProgressRepo
                    .findByUserIdAndIsCompletedTrue(ctx.getUser().getId());

            for (UserGrammarProgress progress : grammarProgress) {
                Long topicId = progress.getLesson().getTopic().getId();
                topicScores.computeIfAbsent(topicId, k -> new TopicScore(topicId, ModuleType.GRAMMAR));
                topicScores.get(topicId).addScore(progress.getScorePercentage());
            }

            // Aggregate Reading scores by topic
            List<UserReadingProgress> readingProgress = readingProgressRepo
                    .findByUserIdAndIsCompletedTrue(ctx.getUser().getId());

            for (UserReadingProgress progress : readingProgress) {
                Long topicId = progress.getLesson().getTopic().getId();
                topicScores.computeIfAbsent(topicId, k -> new TopicScore(topicId, ModuleType.READING));
                topicScores.get(topicId).addScore(progress.getScorePercentage());
            }

            // Aggregate Listening scores by topic
            List<UserListeningProgress> listeningProgress = listeningProgressRepo
                    .findByUserIdAndIsCompletedTrue(ctx.getUser().getId());

            for (UserListeningProgress progress : listeningProgress) {
                Long topicId = progress.getLesson().getTopic().getId();
                topicScores.computeIfAbsent(topicId, k -> new TopicScore(topicId, ModuleType.LISTENING));
                topicScores.get(topicId).addScore(progress.getScorePercentage());
            }

            // Find weakest topic (lowest avg score)
            Optional<TopicScore> weakestTopic = topicScores.values().stream()
                    .filter(ts -> ts.getAverageScore() < MISTAKE_SCORE_THRESHOLD)
                    .filter(ts -> ts.getAttemptCount() >= 1)
                    .min(Comparator.comparing(TopicScore::getAverageScore));

            if (weakestTopic.isEmpty()) {
                log.debug("No weak topics found (all scores > 75% or < 1 attempt)");
                return null;
            }

            TopicScore weakest = weakestTopic.get();
            Optional<Topic> topic = topicRepository.findById(weakest.getTopicId());

            if (topic.isEmpty()) {
                return null;
            }

            // Find next lesson in this weak topic that user hasn't completed or scored low
            List<LessonInfo> availableLessons = getAvailableLessonsForTopic(
                    ctx.getUser(), weakest.getTopicId(), weakest.getModuleType());

            if (availableLessons.isEmpty()) {
                return null;
            }

            LessonInfo targetLesson = availableLessons.get(0);

            return AIRecommendation.builder()
                    .user(ctx.getUser())
                    .type("MISTAKE_PATTERN")
                    .title("🎯 Khắc phục điểm yếu: " + topic.get().getName())
                    .description(String.format(
                            "Bạn hay gặp khó khăn với chủ đề này (%.0f%% trung bình).\n" +
                                    "💡 Bài này sẽ giúp bạn cải thiện!\n" +
                                    "📊 Đã làm %d bài trong topic này",
                            weakest.getAverageScore(),
                            weakest.getAttemptCount()))
                    .reasoning(String.format(
                            "User has low average score (%.0f%%) on this topic with %d attempts. Needs focused practice.",
                            weakest.getAverageScore(),
                            weakest.getAttemptCount()))
                    .targetSkill(weakest.getModuleType())
                    .targetLessonId(targetLesson.getLessonId())
                    .targetTopicId(weakest.getTopicId())
                    .priority(5)
                    .isShown(false)
                    .isCompleted(false)
                    .expiresAt(LocalDateTime.now().plusHours(RECOMMENDATION_CACHE_HOURS))
                    .build();

        } catch (Exception e) {
            log.error("Error generating mistake pattern recommendation: {}", e.getMessage());
        }

        return null;
    }

    // =========================================================================
    // STREAK SAVER RECOMMENDATION
    // =========================================================================

    private AIRecommendation generateStreakSaverRecommendation(RecommendationContext ctx) {
        try {
            // Find quickest lesson across all modules
            Map<ModuleType, List<LessonInfo>> available = ctx.getAvailableLessons();

            for (ModuleType module : ModuleType.values()) {
                List<LessonInfo> lessons = available.get(module);
                if (lessons != null && !lessons.isEmpty()) {
                    LessonInfo quickLesson = lessons.get(0);

                    return AIRecommendation.builder()
                            .user(ctx.getUser())
                            .type("STREAK_SAVER")
                            .title("🔥 Cứu Streak - Học ngay!")
                            .description(String.format(
                                    "Bạn chưa học hôm nay! Streak hiện tại: %d ngày.\n" +
                                            "Hoàn thành bài này để giữ streak! (~10-15 phút)",
                                    ctx.getStats().getCurrentStreak()))
                            .reasoning("User hasn't studied today - urgent streak protection needed")
                            .targetSkill(module)
                            .targetLessonId(quickLesson.getLessonId())
                            .targetTopicId(quickLesson.getTopicId())
                            .priority(6)
                            .isShown(false)
                            .isCompleted(false)
                            .expiresAt(LocalDateTime.now().plusHours(RECOMMENDATION_CACHE_HOURS))
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("Error generating streak saver: {}", e.getMessage());
        }

        return null;
    }

    // =========================================================================
    // HELPER: Get available lessons for a specific topic
    // =========================================================================

    private List<LessonInfo> getAvailableLessonsForTopic(User user, Long topicId, ModuleType moduleType) {
        List<LessonInfo> lessons = new ArrayList<>();

        try {
            // ✅ FIX: Check if topic is active FIRST
            Optional<Topic> topicOpt = topicRepository.findById(topicId);
            if (topicOpt.isEmpty()) {
                log.warn("Topic {} not found", topicId);
                return lessons;
            }

            Topic topic = topicOpt.get();
            if (topic.getIsActive() == null || !topic.getIsActive()) {
                log.warn("Topic {} is inactive, skipping recommendations", topicId);
                return lessons;
            }

            switch (moduleType) {
                case GRAMMAR -> {
                    // Get completed lesson IDs
                    Set<Long> completedIds = grammarProgressRepo
                            .findByUserIdAndIsCompletedTrue(user.getId())
                            .stream()
                            .map(p -> p.getLesson().getId())
                            .collect(Collectors.toSet());

                    // Get all ACTIVE lessons in this topic
                    List<GrammarLesson> allLessons = grammarRepo
                            .findByTopicIdOrderByOrderIndexAsc(topicId)
                            .stream()
                            // ✅ FIX: Only active lessons
                            .filter(l -> l.getIsActive() != null && l.getIsActive())
                            .toList();

                    // Filter uncompleted or low-score lessons
                    for (GrammarLesson lesson : allLessons) {
                        if (!completedIds.contains(lesson.getId())) {
                            lessons.add(new LessonInfo(
                                    lesson.getId(),
                                    topicId,
                                    lesson.getTitle(),
                                    ModuleType.GRAMMAR));
                        } else {
                            // Check if scored low - should retry
                            Optional<UserGrammarProgress> progress = grammarProgressRepo
                                    .findByUserIdAndLessonId(user.getId(), lesson.getId());

                            if (progress.isPresent() &&
                                    progress.get().getScorePercentage() < MISTAKE_SCORE_THRESHOLD) {
                                lessons.add(new LessonInfo(
                                        lesson.getId(),
                                        topicId,
                                        lesson.getTitle(),
                                        ModuleType.GRAMMAR));
                            }
                        }
                    }
                }

                case READING -> {
                    Set<Long> completedIds = readingProgressRepo
                            .findByUserIdAndIsCompletedTrue(user.getId())
                            .stream()
                            .map(p -> p.getLesson().getId())
                            .collect(Collectors.toSet());

                    List<ReadingLesson> allLessons = readingRepo
                            .findByTopicIdOrderByOrderIndexAsc(topicId)
                            .stream()
                            // ✅ FIX: Only active lessons
                            .filter(l -> l.getIsActive() != null && l.getIsActive())
                            .toList();

                    for (ReadingLesson lesson : allLessons) {
                        if (!completedIds.contains(lesson.getId())) {
                            lessons.add(new LessonInfo(
                                    lesson.getId(),
                                    topicId,
                                    lesson.getTitle(),
                                    ModuleType.READING));
                        } else {
                            Optional<UserReadingProgress> progress = readingProgressRepo
                                    .findByUserIdAndLessonId(user.getId(), lesson.getId());

                            if (progress.isPresent() &&
                                    progress.get().getScorePercentage() < MISTAKE_SCORE_THRESHOLD) {
                                lessons.add(new LessonInfo(
                                        lesson.getId(),
                                        topicId,
                                        lesson.getTitle(),
                                        ModuleType.READING));
                            }
                        }
                    }
                }

                case LISTENING -> {
                    Set<Long> completedIds = listeningProgressRepo
                            .findByUserIdAndIsCompletedTrue(user.getId())
                            .stream()
                            .map(p -> p.getLesson().getId())
                            .collect(Collectors.toSet());

                    List<ListeningLesson> allLessons = listeningRepo
                            .findByTopicIdOrderByOrderIndexAsc(topicId)
                            .stream()
                            // ✅ FIX: Only active lessons
                            .filter(l -> l.getIsActive() != null && l.getIsActive())
                            .toList();

                    for (ListeningLesson lesson : allLessons) {
                        if (!completedIds.contains(lesson.getId())) {
                            lessons.add(new LessonInfo(
                                    lesson.getId(),
                                    topicId,
                                    lesson.getTitle(),
                                    ModuleType.LISTENING));
                        } else {
                            Optional<UserListeningProgress> progress = listeningProgressRepo
                                    .findByUserIdAndLessonId(user.getId(), lesson.getId());

                            if (progress.isPresent() &&
                                    progress.get().getScorePercentage() < MISTAKE_SCORE_THRESHOLD) {
                                lessons.add(new LessonInfo(
                                        lesson.getId(),
                                        topicId,
                                        lesson.getTitle(),
                                        ModuleType.LISTENING));
                            }
                        }
                    }
                }
            }

            log.debug("Found {} available lessons in topic {} ({})",
                    lessons.size(), topicId, moduleType);

        } catch (Exception e) {
            log.error("Error getting available lessons for topic {}: {}", topicId, e.getMessage());
        }

        return lessons;
    }

    // =========================================================================
    // EXISTING METHODS (from original service)
    // =========================================================================

    private RecommendationContext buildRecommendationContext(User user, UserLearningBehavior behavior) {
        UserStats stats = user.getStats();
        LearningPattern pattern = analyzeLearningPattern(user, behavior);
        Map<ModuleType, List<LessonInfo>> availableLessons = getAvailableLessons(user);
        Set<Long> completedLessonIds = getCompletedLessonIds(user.getId());
        Map<String, Integer> recentMistakes = getRecentCommonMistakes(user.getId());
        UserState userState = determineUserState(user, behavior);
        TimeContext timeContext = getTimeContext();

        return RecommendationContext.builder()
                .user(user)
                .behavior(behavior)
                .stats(stats)
                .pattern(pattern)
                .availableLessons(availableLessons)
                .completedLessonIds(completedLessonIds)
                .recentMistakes(recentMistakes)
                .userState(userState)
                .timeContext(timeContext)
                .build();
    }

    private AIRecommendation generateWeaknessRecommendation(RecommendationContext ctx) {
        String weakSkill = ctx.getBehavior().getWeakestSkill();
        if (weakSkill == null)
            return null;

        ModuleType module = ModuleType.valueOf(weakSkill);
        List<LessonInfo> lessons = ctx.getAvailableLessons().get(module);
        if (lessons == null || lessons.isEmpty())
            return null;

        LessonInfo target = lessons.get(0);
        Double accuracy = ctx.getBehavior().getSkillStats().get(weakSkill) != null
                ? ctx.getBehavior().getSkillStats().get(weakSkill).getAccuracy()
                : 0.0;

        return AIRecommendation.builder()
                .user(ctx.getUser())
                .type("PRACTICE_WEAK_SKILL")
                .title("💪 Cải thiện " + weakSkill)
                .description(String.format(
                        "Bạn đang gặp khó khăn với %s (%.1f%% chính xác). " +
                                "Hãy luyện tập thêm để cải thiện!",
                        weakSkill, accuracy * 100))
                .reasoning("AI phát hiện kỹ năng yếu nhất của bạn cần cải thiện")
                .targetSkill(module)
                .targetLessonId(target.getLessonId())
                .targetTopicId(target.getTopicId())
                .priority(5)
                .isShown(false)
                .isCompleted(false)
                .expiresAt(LocalDateTime.now().plusHours(RECOMMENDATION_CACHE_HOURS))
                .build();
    }

    private List<AIRecommendation> generateProgressRecommendations(RecommendationContext ctx) {
        List<AIRecommendation> recs = new ArrayList<>();

        if (ctx.getBehavior().getTopicProgress() == null) {
            log.debug("No topic progress data available");
            return recs;
        }

        log.debug("Checking in-progress topics...");

        ctx.getBehavior().getTopicProgress().forEach((topicId, progress) -> {
            // ✅ FIX: Null-safe progress check
            if (progress == null || progress.getCompletionPercentage() == null) {
                log.debug("Skipping topic {} due to null progress data", topicId);
                return;
            }

            if (progress.getCompletionPercentage() > 0 &&
                    progress.getCompletionPercentage() < 100) {

                try {
                    Long tid = Long.parseLong(topicId);
                    Optional<Topic> topic = topicRepository.findById(tid);

                    if (topic.isPresent() &&
                            topic.get().getIsActive() != null &&
                            topic.get().getIsActive()) {

                        log.info("✅ Found CONTINUE_TOPIC: topic={}, completion={}%",
                                topic.get().getName(), progress.getCompletionPercentage());

                        AIRecommendation rec = AIRecommendation.builder()
                                .user(ctx.getUser())
                                .type("CONTINUE_TOPIC")
                                .title("📚 Tiếp tục: " + topic.get().getName())
                                .description(String.format(
                                        "Bạn đã hoàn thành %.0f%% chủ đề này. " +
                                                "Hãy tiếp tục để đạt 100%%!",
                                        progress.getCompletionPercentage()))
                                .reasoning("Người dùng đang học dở chủ đề này")
                                .targetSkill(topic.get().getModuleType())
                                .targetTopicId(tid)
                                .priority(4)
                                .isShown(false)
                                .isCompleted(false)
                                .expiresAt(LocalDateTime.now().plusHours(RECOMMENDATION_CACHE_HOURS))
                                .build();

                        recs.add(rec);
                    }
                } catch (NumberFormatException e) {
                    log.error("Invalid topic ID format: {}", topicId);
                } catch (Exception e) {
                    log.error("Error processing topic {}: {}", topicId, e.getMessage());
                }
            }
        });

        log.debug("Found {} in-progress topics", recs.size());
        return recs.stream().limit(2).collect(Collectors.toList());
    }

    private AIRecommendation generateMorningRecommendation(RecommendationContext ctx) {
        List<LessonInfo> readingLessons = ctx.getAvailableLessons().get(ModuleType.READING);

        if (readingLessons != null && !readingLessons.isEmpty()) {
            LessonInfo target = readingLessons.get(0);

            return AIRecommendation.builder()
                    .user(ctx.getUser())
                    .type("TIME_OPTIMAL")
                    .title("☀️ Buổi sáng - Luyện đọc hiểu")
                    .description(
                            "Buổi sáng là thời điểm tốt nhất để luyện đọc hiểu. " +
                                    "Tập trung cao, ghi nhớ tốt hơn!")
                    .reasoning("Research shows reading comprehension is better in morning")
                    .targetSkill(ModuleType.READING)
                    .targetLessonId(target.getLessonId())
                    .targetTopicId(target.getTopicId())
                    .priority(3)
                    .isShown(false)
                    .isCompleted(false)
                    .expiresAt(LocalDateTime.now().plusHours(RECOMMENDATION_CACHE_HOURS))
                    .build();
        }

        return null;
    }

    private AIRecommendation generateEveningRecommendation(RecommendationContext ctx) {
        List<LessonInfo> listeningLessons = ctx.getAvailableLessons().get(ModuleType.LISTENING);

        if (listeningLessons != null && !listeningLessons.isEmpty()) {
            LessonInfo target = listeningLessons.get(0);

            return AIRecommendation.builder()
                    .user(ctx.getUser())
                    .type("TIME_OPTIMAL")
                    .title("🌙 Buổi tối - Luyện nghe")
                    .description(
                            "Buổi tối thích hợp cho luyện nghe và ôn tập. " +
                                    "Học thoải mái, không căng thẳng!")
                    .reasoning("Evening is better for passive learning like listening")
                    .targetSkill(ModuleType.LISTENING)
                    .targetLessonId(target.getLessonId())
                    .targetTopicId(target.getTopicId())
                    .priority(3)
                    .isShown(false)
                    .isCompleted(false)
                    .expiresAt(LocalDateTime.now().plusHours(RECOMMENDATION_CACHE_HOURS))
                    .build();
        }

        return null;
    }

    private List<AIRecommendation> generateNextLessonRecommendations(RecommendationContext ctx) {
        List<AIRecommendation> recs = new ArrayList<>();

        for (ModuleType module : ModuleType.values()) {
            List<LessonInfo> lessons = ctx.getAvailableLessons().get(module);
            if (lessons != null && !lessons.isEmpty()) {
                LessonInfo next = lessons.get(0);

                AIRecommendation rec = AIRecommendation.builder()
                        .user(ctx.getUser())
                        .type("NEXT_LESSON")
                        .title("🎯 Bài tiếp theo: " + next.getTitle())
                        .description("Tiếp tục hành trình học của bạn!")
                        .reasoning("Bài học tiếp theo trong lộ trình")
                        .targetSkill(module)
                        .targetLessonId(next.getLessonId())
                        .targetTopicId(next.getTopicId())
                        .priority(2)
                        .isShown(false)
                        .isCompleted(false)
                        .expiresAt(LocalDateTime.now().plusHours(RECOMMENDATION_CACHE_HOURS))
                        .build();

                recs.add(rec);
            }
        }

        return recs.stream().limit(3).collect(Collectors.toList());
    }

    private List<AIRecommendation> generateDefaultRecommendations(User user) {
        log.info("Generating default recommendations for user {}", user.getId());

        List<AIRecommendation> recs = new ArrayList<>();
        Map<ModuleType, List<LessonInfo>> available = getAvailableLessons(user);

        for (ModuleType module : ModuleType.values()) {
            List<LessonInfo> lessons = available.get(module);
            if (lessons != null && !lessons.isEmpty()) {
                LessonInfo lesson = lessons.get(0);

                AIRecommendation rec = AIRecommendation.builder()
                        .user(user)
                        .type("NEXT_LESSON")
                        .title("🎯 Bắt đầu với " + module)
                        .description("Bắt đầu hành trình học của bạn!")
                        .reasoning("Default recommendation - no behavior data yet")
                        .targetSkill(module)
                        .targetLessonId(lesson.getLessonId())
                        .targetTopicId(lesson.getTopicId())
                        .priority(2)
                        .isShown(false)
                        .isCompleted(false)
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .build();

                recs.add(rec);
            }
        }

        return recommendationRepository.saveAll(recs);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private LearningPattern analyzeLearningPattern(User user, UserLearningBehavior behavior) {
        boolean isConsistent = user.getStats() != null && user.getStats().getCurrentStreak() >= 3;
        double avgAttempts = behavior.getAvgAttemptsPerLesson() != null ? behavior.getAvgAttemptsPerLesson() : 0.0;
        boolean struggles = avgAttempts > 2.0;
        double accuracy = behavior.getOverallAccuracy() != null ? behavior.getOverallAccuracy() : 0.0;
        boolean needsReview = accuracy < 0.7;

        String description;
        if (isConsistent && !struggles && !needsReview) {
            description = "Excellent learner: Consistent, high accuracy, minimal struggles";
        } else if (struggles) {
            description = "Needs support: Multiple attempts needed per lesson";
        } else if (needsReview) {
            description = "Needs review: Accuracy below 70%";
        } else {
            description = "Developing learner: Shows potential, needs encouragement";
        }

        return new LearningPattern(isConsistent, struggles, needsReview, description);
    }

    private UserState determineUserState(User user, UserLearningBehavior behavior) {
        // Check weak skill
        boolean hasWeakSkill = behavior.getWeakestSkill() != null &&
                behavior.getSkillStats() != null &&
                behavior.getSkillStats().get(behavior.getWeakestSkill()) != null &&
                behavior.getSkillStats().get(behavior.getWeakestSkill()).getAccuracy() != null &&
                behavior.getSkillStats().get(behavior.getWeakestSkill()).getAccuracy() < 0.6;

        // ✅ FIX: Null-safe check for topic progress
        boolean hasInProgress = false;
        if (behavior.getTopicProgress() != null && !behavior.getTopicProgress().isEmpty()) {
            hasInProgress = behavior.getTopicProgress().values().stream()
                    .filter(p -> p != null) // ✅ Filter out null progress
                    .filter(p -> p.getCompletionPercentage() != null) // ✅ Filter out null percentage
                    .anyMatch(p -> p.getCompletionPercentage() > 0 && p.getCompletionPercentage() < 100);
        }

        // Check struggling
        boolean isStruggling = behavior.getAvgAttemptsPerLesson() != null &&
                behavior.getAvgAttemptsPerLesson() > 2.5;

        // ✅ Check if user has studied today
        boolean hasStreakToday = user.getStats() != null &&
                user.getStats().hasStreakToday();

        log.debug("UserState: hasWeakSkill={}, hasInProgress={}, isStruggling={}, hasStreakToday={}",
                hasWeakSkill, hasInProgress, isStruggling, hasStreakToday);

        return new UserState(hasWeakSkill, hasInProgress, isStruggling, hasStreakToday);
    }

    private TimeContext getTimeContext() {
        LocalTime now = LocalTime.now();
        DayOfWeek day = LocalDateTime.now().getDayOfWeek();

        String timeOfDay;
        if (now.isBefore(LocalTime.of(12, 0))) {
            timeOfDay = "Morning";
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            timeOfDay = "Afternoon";
        } else {
            timeOfDay = "Evening";
        }

        return new TimeContext(timeOfDay, day.toString(), now);
    }

    private Map<ModuleType, List<LessonInfo>> getAvailableLessons(User user) {
        Map<ModuleType, List<LessonInfo>> result = new HashMap<>();

        try {
            Set<Long> completedLessonIds = getCompletedLessonIds(user.getId());

            // ==================== GRAMMAR ====================
            List<GrammarLesson> grammarLessons = grammarRepo.findAll().stream()
                    // ✅ FIX 1: Check lesson active
                    .filter(l -> l.getIsActive() != null && l.getIsActive())
                    // ✅ FIX 2: Check topic active
                    .filter(l -> l.getTopic() != null &&
                            l.getTopic().getIsActive() != null &&
                            l.getTopic().getIsActive())
                    // Check not completed
                    .filter(l -> !completedLessonIds.contains(l.getId()))
                    // Check user level
                    .filter(l -> isLessonMatchUserLevel(l.getTopic(), user.getEnglishLevel()))
                    // Sort by order
                    .sorted(Comparator.comparing(GrammarLesson::getOrderIndex))
                    .limit(5)
                    .toList();

            result.put(ModuleType.GRAMMAR, grammarLessons.stream()
                    .map(l -> new LessonInfo(
                            l.getId(),
                            l.getTopic().getId(),
                            l.getTitle(),
                            ModuleType.GRAMMAR))
                    .collect(Collectors.toList()));

            log.debug("Found {} available GRAMMAR lessons from active topics", grammarLessons.size());

            // ==================== READING ====================
            List<ReadingLesson> readingLessons = readingRepo.findAll().stream()
                    // ✅ FIX 1: Check lesson active
                    .filter(l -> l.getIsActive() != null && l.getIsActive())
                    // ✅ FIX 2: Check topic active
                    .filter(l -> l.getTopic() != null &&
                            l.getTopic().getIsActive() != null &&
                            l.getTopic().getIsActive())
                    // Check not completed
                    .filter(l -> !completedLessonIds.contains(l.getId()))
                    // Check user level
                    .filter(l -> isLessonMatchUserLevel(l.getTopic(), user.getEnglishLevel()))
                    // Sort by order
                    .sorted(Comparator.comparing(ReadingLesson::getOrderIndex))
                    .limit(5)
                    .toList();

            result.put(ModuleType.READING, readingLessons.stream()
                    .map(l -> new LessonInfo(
                            l.getId(),
                            l.getTopic().getId(),
                            l.getTitle(),
                            ModuleType.READING))
                    .collect(Collectors.toList()));

            log.debug("Found {} available READING lessons from active topics", readingLessons.size());

            // ==================== LISTENING ====================
            List<ListeningLesson> listeningLessons = listeningRepo.findAll().stream()
                    // ✅ FIX 1: Check lesson active
                    .filter(l -> l.getIsActive() != null && l.getIsActive())
                    // ✅ FIX 2: Check topic active
                    .filter(l -> l.getTopic() != null &&
                            l.getTopic().getIsActive() != null &&
                            l.getTopic().getIsActive())
                    // Check not completed
                    .filter(l -> !completedLessonIds.contains(l.getId()))
                    // Check user level
                    .filter(l -> isLessonMatchUserLevel(l.getTopic(), user.getEnglishLevel()))
                    // Sort by order
                    .sorted(Comparator.comparing(ListeningLesson::getOrderIndex))
                    .limit(5)
                    .toList();

            result.put(ModuleType.LISTENING, listeningLessons.stream()
                    .map(l -> new LessonInfo(
                            l.getId(),
                            l.getTopic().getId(),
                            l.getTitle(),
                            ModuleType.LISTENING))
                    .collect(Collectors.toList()));

            log.debug("Found {} available LISTENING lessons from active topics", listeningLessons.size());

        } catch (Exception e) {
            log.error("Error loading available lessons: {}", e.getMessage(), e);
        }

        return result;
    }

    private boolean isLessonMatchUserLevel(Topic topic, com.thanhnb.englishlearning.enums.EnglishLevel userLevel) {
        if (topic == null || topic.getLevelRequired() == null || userLevel == null) {
            return true;
        }

        int topicLevel = topic.getLevelRequired().ordinal();
        int userLevelOrdinal = userLevel.ordinal();

        return topicLevel <= userLevelOrdinal + 1;
    }

    private Set<Long> getCompletedLessonIds(Long userId) {
        Set<Long> completed = new HashSet<>();

        completed.addAll(grammarProgressRepo.findByUserIdAndIsCompletedTrue(userId)
                .stream().map(p -> p.getLesson().getId()).collect(Collectors.toSet()));

        completed.addAll(readingProgressRepo.findByUserIdAndIsCompletedTrue(userId)
                .stream().map(p -> p.getLesson().getId()).collect(Collectors.toSet()));

        completed.addAll(listeningProgressRepo.findByUserIdAndIsCompletedTrue(userId)
                .stream().map(p -> p.getLesson().getId()).collect(Collectors.toSet()));

        return completed;
    }

    private Map<String, Integer> getRecentCommonMistakes(Long userId) {
        // TODO: Implement based on answer history if needed
        return new HashMap<>();
    }

    private List<AIRecommendation> ensureDiversity(List<AIRecommendation> recs, User user) {
        if (recs.isEmpty())
            return recs;

        Map<ModuleType, Long> countBySkill = recs.stream()
                .filter(r -> r.getTargetSkill() != null)
                .collect(Collectors.groupingBy(AIRecommendation::getTargetSkill, Collectors.counting()));

        if (countBySkill.size() == 1 && recs.size() >= 3) {
            log.debug("🎨 Adding diversity to recommendations");

            ModuleType dominantSkill = countBySkill.keySet().iterator().next();

            for (ModuleType skill : ModuleType.values()) {
                if (skill != dominantSkill) {
                    AIRecommendation diverseRec = createSimpleRecommendation(user, skill);
                    if (diverseRec != null && recs.size() < MAX_RECOMMENDATIONS) {
                        recs.add(diverseRec);
                        break;
                    }
                }
            }
        }

        return recs;
    }

    private AIRecommendation createSimpleRecommendation(User user, ModuleType skill) {
        try {
            Map<ModuleType, List<LessonInfo>> available = getAvailableLessons(user);
            List<LessonInfo> lessons = available.get(skill);

            if (lessons == null || lessons.isEmpty()) {
                return null;
            }

            LessonInfo lesson = lessons.get(0);

            return AIRecommendation.builder()
                    .user(user)
                    .type("VARIETY")
                    .title("🎯 Thử thách: " + skill.toString())
                    .description("Đa dạng hóa kỹ năng của bạn!")
                    .reasoning("Added for skill variety")
                    .targetSkill(skill)
                    .targetLessonId(lesson.getLessonId())
                    .targetTopicId(lesson.getTopicId())
                    .priority(2)
                    .isShown(false)
                    .isCompleted(false)
                    .expiresAt(LocalDateTime.now().plusHours(RECOMMENDATION_CACHE_HOURS))
                    .build();

        } catch (Exception e) {
            log.error("Error creating diverse recommendation for {}: {}", skill, e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    @lombok.Data
    @lombok.Builder
    private static class RecommendationContext {
        private User user;
        private UserLearningBehavior behavior;
        private UserStats stats;
        private LearningPattern pattern;
        private Map<ModuleType, List<LessonInfo>> availableLessons;
        private Set<Long> completedLessonIds;
        private Map<String, Integer> recentMistakes;
        private UserState userState;
        private TimeContext timeContext;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class LearningPattern {
        private boolean consistent;
        private boolean struggles;
        private boolean needsReview;
        private String description;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class UserState {
        private boolean weakSkill;
        private boolean inProgressLessons;
        private boolean struggling;
        private boolean streakToday;

        public boolean hasWeakSkill() {
            return weakSkill;
        }

        public boolean hasInProgressLessons() {
            return inProgressLessons;
        }

        public boolean isStruggling() {
            return struggling;
        }

        public boolean hasStreakToday() {
            return streakToday;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class TimeContext {
        private String timeOfDay;
        private String dayOfWeek;
        private LocalTime currentTime;

        public boolean isMorning() {
            return currentTime.isBefore(LocalTime.of(12, 0));
        }

        public boolean isEvening() {
            return currentTime.isAfter(LocalTime.of(18, 0));
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class LessonInfo {
        private Long lessonId;
        private Long topicId;
        private String title;
        private ModuleType module;
    }

    /**
     * Helper class to track topic scores
     */
    @lombok.Data
    private static class TopicScore {
        private Long topicId;
        private ModuleType moduleType;
        private List<Double> scores = new ArrayList<>();

        public TopicScore(Long topicId, ModuleType moduleType) {
            this.topicId = topicId;
            this.moduleType = moduleType;
        }

        public void addScore(Double score) {
            if (score != null) {
                scores.add(score);
            }
        }

        public Double getAverageScore() {
            if (scores.isEmpty())
                return 0.0;
            return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        public int getAttemptCount() {
            return scores.size();
        }
    }
}