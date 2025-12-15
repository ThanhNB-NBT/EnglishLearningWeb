package com.thanhnb.englishlearning.repository.listening;

import com.thanhnb.englishlearning.entity.listening.UserListeningProgress;
import org.springframework.data.jpa.repository.JpaRepository;
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

        // Lấy danh sách bài đã hoàn thành (Dùng cho API completed lessons)
        List<UserListeningProgress> findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(Long userId);

        Integer countByLessonIdAndIsCompletedTrue(Long lessonId);

        // Xóa progress theo lessonId (Dùng khi xóa bài học)
        void deleteByLessonId(Long lessonId);

        // Check nhanh xem user đã hoàn thành bài này chưa (Dùng cho logic unlock)
        boolean existsByUserIdAndLessonIdAndIsCompletedTrue(Long userId, Long lessonId);
}