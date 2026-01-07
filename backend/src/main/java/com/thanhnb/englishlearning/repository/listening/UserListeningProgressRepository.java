package com.thanhnb.englishlearning.repository.listening;

import com.thanhnb.englishlearning.entity.listening.UserListeningProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserListeningProgressRepository extends JpaRepository<UserListeningProgress, Long> {

        List<UserListeningProgress> findByLessonId(Long lessonId);

        // Tìm progress theo user và lesson
        Optional<UserListeningProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

        // Lấy tất cả progress của user (để map vào list lesson)
        List<UserListeningProgress> findByUserId(Long userId);

        @Query("""
                            SELECT p FROM UserListeningProgress p
                            JOIN FETCH p.lesson l
                            WHERE p.user.id = :userId
                            AND p.isCompleted = true
                            ORDER BY p.completedAt DESC
                        """)
        List<UserListeningProgress> findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(@Param("userId") Long userId);

        Integer countByLessonIdAndIsCompletedTrue(Long lessonId);

        @Query("SELECT COUNT(p) FROM UserListeningProgress p " +
                        "WHERE p.user.id = :userId " +
                        "AND p.lesson.topic.id = :topicId " +
                        "AND p.isCompleted = true")
        long countCompletedLessonsByUserAndTopic(
                        @Param("userId") Long userId,
                        @Param("topicId") Long topicId);

        // Xóa progress theo lessonId (Dùng khi xóa bài học)
        void deleteByLessonId(Long lessonId);

        // Check nhanh xem user đã hoàn thành bài này chưa (Dùng cho logic unlock)
        boolean existsByUserIdAndLessonIdAndIsCompletedTrue(Long userId, Long lessonId);
}