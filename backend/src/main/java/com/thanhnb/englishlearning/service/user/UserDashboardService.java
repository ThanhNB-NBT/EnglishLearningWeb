package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.dto.user.response.UserDashboardDto;
import com.thanhnb.englishlearning.dto.user.response.UserDashboardDto.QuickStatsDto;
import com.thanhnb.englishlearning.dto.user.response.UserDashboardDto.SkillProgressDto;
import com.thanhnb.englishlearning.dto.user.response.UserDashboardDto.StreakDto;
import com.thanhnb.englishlearning.entity.json.SkillStats;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.entity.user.UserLearningBehavior;
import com.thanhnb.englishlearning.entity.user.UserStats;
import com.thanhnb.englishlearning.mapper.UserMapper;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.user.UserLearningBehaviorRepository;
import com.thanhnb.englishlearning.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserDashboardService {

    private final UserRepository userRepository;
    private final UserLearningBehaviorRepository behaviorRepository;
    private final GrammarLessonRepository grammarLessonRepo;
    private final ReadingLessonRepository readingLessonRepo;
    private final ListeningLessonRepository listeningLessonRepo;

    public UserDashboardDto getDashboardData(Long userId) {
        log.info("📊 Getting dashboard data for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        UserStats stats = user.getStats();
        if (stats == null) {
            throw new RuntimeException("UserStats not found for user: " + userId);
        }

        // ✅ Load learning behavior for accuracy data
        UserLearningBehavior behavior = behaviorRepository.findByUserId(userId).orElse(null);

        return UserDashboardDto.builder()
                .user(UserMapper.toDetailDto(user))
                .quickStats(buildQuickStats(stats))
                .skillProgress(buildSkillProgress(stats, behavior))
                .streak(buildStreak(stats))
                .build();
    }

    private QuickStatsDto buildQuickStats(UserStats stats) {
        int streakGoal = 7;
        double weeklyGoalProgress = Math.min(
                ((double) stats.getCurrentStreak() / streakGoal) * 100.0,
                100.0);

        return QuickStatsDto.builder()
                .currentStreak(stats.getCurrentStreak())
                .totalPoints(stats.getTotalPoints())
                .totalLessonsCompleted(stats.getTotalLessonsCompleted())
                .studyTimeToday(0) // TODO: Implement proper time tracking
                .weeklyGoalProgress(weeklyGoalProgress)
                .build();
    }

    /**
     * ✅ FIXED: Build skill progress WITH accuracy from UserLearningBehavior
     */
    private Map<String, SkillProgressDto> buildSkillProgress(UserStats stats, UserLearningBehavior behavior) {
        Map<String, SkillProgressDto> skillProgress = new HashMap<>();

        // Get skill stats for accuracy
        Map<String, SkillStats> skillStats = behavior != null && behavior.getSkillStats() != null
                ? behavior.getSkillStats()
                : new HashMap<>();

        // Grammar Progress
        int grammarCompleted = stats.getGrammarCompleted();
        long grammarTotal = grammarLessonRepo.countByIsActiveTrue();
        Double grammarAccuracy = getSkillAccuracy(skillStats, "GRAMMAR");

        skillProgress.put("grammar", SkillProgressDto.builder()
                .completed(grammarCompleted)
                .total((int) grammarTotal)
                .level(determineLevel(grammarCompleted, (int) grammarTotal))
                .accuracy(grammarAccuracy)
                .build());

        // Reading Progress
        int readingCompleted = stats.getReadingCompleted();
        long readingTotal = readingLessonRepo.countByIsActiveTrue();
        Double readingAccuracy = getSkillAccuracy(skillStats, "READING");

        skillProgress.put("reading", SkillProgressDto.builder()
                .completed(readingCompleted)
                .total((int) readingTotal)
                .level(determineLevel(readingCompleted, (int) readingTotal))
                .accuracy(readingAccuracy)
                .build());

        // Listening Progress
        int listeningCompleted = stats.getListeningCompleted();
        long listeningTotal = listeningLessonRepo.countByIsActiveTrue();
        Double listeningAccuracy = getSkillAccuracy(skillStats, "LISTENING");

        skillProgress.put("listening", SkillProgressDto.builder()
                .completed(listeningCompleted)
                .total((int) listeningTotal)
                .level(determineLevel(listeningCompleted, (int) listeningTotal))
                .accuracy(listeningAccuracy)
                .build());

        return skillProgress;
    }

    /**
     * ✅ Get accuracy for specific skill from SkillStats
     */
    private Double getSkillAccuracy(Map<String, SkillStats> skillStats, String skillName) {
        if (skillStats == null || !skillStats.containsKey(skillName)) {
            return 0.0;
        }

        SkillStats stats = skillStats.get(skillName);
        if (stats == null || stats.getAccuracy() == null) {
            return 0.0;
        }

        // SkillStats.accuracy is 0.0-1.0, convert to percentage
        return stats.getAccuracy() * 100.0;
    }

    private String determineLevel(int completed, int total) {
        if (total == 0)
            return "BEGINNER";

        double completionPercentage = ((double) completed / total) * 100.0;

        if (completionPercentage == 0) {
            return "BEGINNER";
        } else if (completionPercentage < 25) {
            return "ELEMENTARY";
        } else if (completionPercentage < 50) {
            return "INTERMEDIATE";
        } else if (completionPercentage < 75) {
            return "UPPER_INTERMEDIATE";
        } else {
            return "ADVANCED";
        }
    }

    private StreakDto buildStreak(UserStats stats) {
        LocalDate today = LocalDate.now();
        LocalDate lastStreakDate = stats.getLastStreakDate();

        boolean hasStreakToday = lastStreakDate != null && lastStreakDate.equals(today);

        int streakGoal = 7;
        double streakGoalProgress = Math.min(
                ((double) stats.getCurrentStreak() / streakGoal) * 100.0,
                100.0);

        return StreakDto.builder()
                .currentStreak(stats.getCurrentStreak())
                .longestStreak(stats.getLongestStreak())
                .lastStreakDate(lastStreakDate != null ? lastStreakDate.toString() : null)
                .hasStreakToday(hasStreakToday)
                .streakGoal(streakGoal)
                .streakGoalProgress(streakGoalProgress)
                .build();
    }
}