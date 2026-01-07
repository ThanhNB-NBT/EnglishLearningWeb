package com.thanhnb.englishlearning.service.tracking;

import com.thanhnb.englishlearning.entity.json.QuestionTypeStats;
import com.thanhnb.englishlearning.entity.json.SkillStats;
import com.thanhnb.englishlearning.entity.json.TopicProgressStats;
import com.thanhnb.englishlearning.entity.user.UserLearningBehavior;
import com.thanhnb.englishlearning.event.LessonCompletedEvent;
import com.thanhnb.englishlearning.repository.user.UserLearningBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BehaviorTrackingListener {

    private final UserLearningBehaviorRepository behaviorRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    // ✅ FIX: Retry 3 lần, delay 500ms nếu gặp lock/concurrency error
    @Retryable(retryFor = { OptimisticLockingFailureException.class, StaleObjectStateException.class,
            CannotAcquireLockException.class }, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public void handleLessonCompleted(LessonCompletedEvent event) {
        log.info("Tracking behavior for user {} - Module {}", event.getUserId(), event.getModule());

        try {
            // ✅ FIX: Dùng Lock thay vì select thường
            UserLearningBehavior behavior = behaviorRepository.findByUserIdWithLock(event.getUserId())
                    .orElseGet(() -> {
                        // Create new (no lock needed for insert)
                        UserLearningBehavior newB = UserLearningBehavior.builder().userId(event.getUserId()).build();
                        newB.setSkillStats(new HashMap<>());
                        newB.setQuestionTypeStats(new HashMap<>());
                        newB.setTopicProgress(new HashMap<>());
                        return behaviorRepository.save(newB);
                    });

            if (behavior.getSkillStats() == null)
                behavior.setSkillStats(new HashMap<>());
            if (behavior.getQuestionTypeStats() == null)
                behavior.setQuestionTypeStats(new HashMap<>());
            if (behavior.getTopicProgress() == null)
                behavior.setTopicProgress(new HashMap<>());

            updateSkillAndQuestionStats(behavior, event);
            if (event.getTopicId() != null)
                updateTopicProgress(behavior, event);
            updateCalculatedFields(behavior);

            behavior.setLastAnalyzedAt(LocalDateTime.now());
            behaviorRepository.save(behavior);

        } catch (OptimisticLockingFailureException | CannotAcquireLockException e) {
            log.warn("Concurrency conflict for user {}. Retrying...", event.getUserId());
            throw e; // Throw để kích hoạt Retry
        } catch (Exception e) {
            log.error("Error tracking behavior: {}", e.getMessage(), e);
        }
    }

    private void updateTopicProgress(UserLearningBehavior behavior, LessonCompletedEvent event) {
        String topicKey = String.valueOf(event.getTopicId());
        TopicProgressStats topicStats = behavior.getTopicProgress().getOrDefault(topicKey, new TopicProgressStats());

        if (topicStats.getTopicId() == null) {
            topicStats.setTopicId(event.getTopicId());
            topicStats.setTopicName(event.getTopicName());
            topicStats.setTotalLessons(0L);
            topicStats.setCompletedLessons(0);
        }

        List<LessonCompletedEvent.QuestionTrackingInfo> results = event.getQuestionResults();
        if (results == null)
            results = Collections.emptyList();

        long correct = results.stream().filter(LessonCompletedEvent.QuestionTrackingInfo::isCorrect).count();
        int total = results.size();
        double lessonScore = total > 0 ? ((double) correct / total) * 100 : 100.0;
        boolean isPassed = lessonScore >= 80.0;

        topicStats.updateProgress(isPassed, lessonScore);
        behavior.getTopicProgress().put(topicKey, topicStats);
    }

    private void updateSkillAndQuestionStats(UserLearningBehavior behavior, LessonCompletedEvent event) {
        if (event.getModule() == null)
            return;
        String skillKey = event.getModule().toString();
        SkillStats skillStat = behavior.getSkillStats().getOrDefault(skillKey, new SkillStats());

        List<LessonCompletedEvent.QuestionTrackingInfo> results = event.getQuestionResults();
        if (results != null) {
            for (LessonCompletedEvent.QuestionTrackingInfo q : results) {
                boolean isCorrect = q.isCorrect();
                skillStat.addResult(isCorrect);
                if (q.getType() != null) {
                    String typeKey = q.getType().toString();
                    QuestionTypeStats typeStat = behavior.getQuestionTypeStats().getOrDefault(typeKey,
                            new QuestionTypeStats());
                    typeStat.setTypeName(typeKey);
                    typeStat.addResult(isCorrect);
                    behavior.getQuestionTypeStats().put(typeKey, typeStat);
                }
            }
        }
        behavior.getSkillStats().put(skillKey, skillStat);
    }

    private void updateCalculatedFields(UserLearningBehavior behavior) {
        String strongest = null;
        String weakest = null;
        double maxAcc = -1.0;
        double minAcc = 2.0;
        long totalAttemptsAllSkills = 0;
        long totalCorrectAllSkills = 0;

        for (Map.Entry<String, SkillStats> entry : behavior.getSkillStats().entrySet()) {
            SkillStats s = entry.getValue();
            if (s == null || s.getTotalAttempts() == null || s.getTotalAttempts() == 0)
                continue;
            double acc = s.getAccuracy();
            if (acc > maxAcc) {
                maxAcc = acc;
                strongest = entry.getKey();
            }
            if (acc < minAcc) {
                minAcc = acc;
                weakest = entry.getKey();
            }
            totalAttemptsAllSkills += s.getTotalAttempts();
            totalCorrectAllSkills += s.getCorrectAnswers();
        }
        behavior.setStrongestSkill(strongest);
        behavior.setWeakestSkill(weakest);
        if (totalAttemptsAllSkills > 0) {
            behavior.setOverallAccuracy((double) totalCorrectAllSkills / totalAttemptsAllSkills);
        }
        // Simple logic for avg attempts
        int totalLessons = behavior.getTopicProgress().values().stream()
                .mapToInt(t -> t.getCompletedLessons() != null ? t.getCompletedLessons() : 0).sum();
        if (totalLessons > 0) {
            behavior.setAvgAttemptsPerLesson((double) totalAttemptsAllSkills / 10.0 / totalLessons); // Example logic
        } else
            behavior.setAvgAttemptsPerLesson(0.0);
    }
}