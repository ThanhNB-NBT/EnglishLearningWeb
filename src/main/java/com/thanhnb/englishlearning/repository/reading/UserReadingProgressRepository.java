package com.thanhnb.englishlearning.repository.reading;

import com.thanhnb.englishlearning.entity.reading.UserReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UserReadingProgressRepository extends JpaRepository<UserReadingProgress, Long> {

    Optional<UserReadingProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<UserReadingProgress> findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(Long userId);

    List<UserReadingProgress> findByUserId(Long userId);

    boolean existsByUserIdAndLessonIdAndIsCompletedTrue(Long userId, Long lessonId);

    @Query("SELECT COUNT(p) FROM UserReadingProgress p WHERE p.lesson.id = :lessonId AND p.isCompleted = true")
    long countCompletedUsersByLessonId(@Param("lessonId") Long lessonId);

    @Query("SELECT AVG(p.scorePercentage) FROM UserReadingProgress p WHERE p.lesson.id = :lessonId AND p.isCompleted = true")
    Double getAverageScoreByLessonId(@Param("lessonId") Long lessonId);

    @Query("SELECT SUM(p.attemps) FROM UserReadingProgress p WHERE p.lesson.id = :lessonId")
    Long getTotalAttemptsByLessonId(@Param("lessonId") Long lessonId);
}
