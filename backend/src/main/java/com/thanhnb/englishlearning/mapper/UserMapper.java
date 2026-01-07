package com.thanhnb.englishlearning.mapper;

import com.thanhnb.englishlearning.dto.user.response.*;
import com.thanhnb.englishlearning.entity.user.*;

public class UserMapper {

    public static UserStatsDto toStatsDto(UserStats stats) {
        if (stats == null) return null;

        return UserStatsDto.builder()
                .userId(stats.getUserId())
                .totalPoints(stats.getTotalPoints())
                .currentStreak(stats.getCurrentStreak())
                .longestStreak(stats.getLongestStreak())
                .lastStreakDate(stats.getLastStreakDate())
                .hasStreakToday(stats.hasStreakToday())
                .totalLessonsCompleted(stats.getTotalLessonsCompleted())
                .grammarCompleted(stats.getGrammarCompleted())
                .readingCompleted(stats.getReadingCompleted())
                .listeningCompleted(stats.getListeningCompleted())
                .totalStudyTimeMinutes(stats.getTotalStudyTimeMinutes())
                .averageSessionMinutes(stats.getAverageSessionMinutes())
                .lastUpdated(stats.getLastUpdated())
                .createdAt(stats.getCreatedAt())
                .build();
    }

    public static UserActivityDto toActivityDto(UserActivity activity) {
        if (activity == null) return null;

        return UserActivityDto.builder()
                .userId(activity.getUserId())
                .lastLoginDate(activity.getLastLoginDate())
                .lastActivityDate(activity.getLastActivityDate())
                .loginCount(activity.getLoginCount())
                .lastLoginIp(activity.getLastLoginIp())
                .lastUserAgent(activity.getLastUserAgent())
                .secondsSinceLastActivity(activity.getSecondsSinceLastActivity())
                .secondsSinceLastLogin(activity.getSecondsSinceLastLogin())
                .isCurrentlyActive(activity.isRecentlyActive(300))
                .updatedAt(activity.getUpdatedAt())
                .createdAt(activity.getCreatedAt())
                .build();
    }

    public static UserDetailDto toDetailDto(User user) {
        if (user == null) return null;

        return UserDetailDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .englishLevel(user.getEnglishLevel())
                .isActive(user.getIsActive())
                .isVerified(user.getIsVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .stats(toStatsDto(user.getStats()))
                .activity(toActivityDto(user.getActivity()))
                .build();
    }
}