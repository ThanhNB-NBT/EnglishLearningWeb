package com.thanhnb.englishlearning.repository.grammar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thanhnb.englishlearning.entity.grammar.UserGrammarProgress;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserGrammarProgressRepository extends JpaRepository<UserGrammarProgress, Long> {

    // Tìm tiến độ cụ thể của người dùng
    Optional<UserGrammarProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    // Tìm tiến độ của tất cả người dùng trong topic
    @Query("SELECT ugp FROM UserGrammarProgress ugp JOIN ugp.lesson gl " +
            "WHERE ugp.user.id = :userId AND gl.topic.id = :topicId " +
            "ORDER BY gl.orderIndex ASC")
    List<UserGrammarProgress> findByUserIdAndTopicId(@Param("userId") Long userId, @Param("topicId") Long topicId);

    // Tìm người dùng hoàn thành lessons
    List<UserGrammarProgress> findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(Long userId);

    // Kiểm tra nếu tiến độ người dùng hoàn thành
    boolean existsByUserIdAndLessonIdAndIsCompletedTrue(Long userId, Long lessonId);

    // Số lượng hoàn thành lesson trong topic
    @Query("SELECT COUNT(ugp) FROM UserGrammarProgress ugp JOIN ugp.lesson gl " +
            "WHERE ugp.user.id = :userId AND gl.topic.id = :topicId AND ugp.isCompleted = true")
    Long countCompletedLessonsInTopic(@Param("userId") Long userId, @Param("topicId") Long topicId);

    // Lấy điểm trung bình grammar của người dùng (từ scorePercentage)
    @Query("SELECT COALESCE(AVG(ugp.scorePercentage), 0) FROM UserGrammarProgress ugp " +
           "WHERE ugp.user.id = :userId AND ugp.isCompleted = true")
    Double getAverageScoreByUserId(@Param("userId") Long userId);

    // Tìm tiến độ người dùng với thông tin lesson
    @Query("SELECT ugp FROM UserGrammarProgress ugp " +
            "JOIN FETCH ugp.lesson gl " +
            "JOIN FETCH gl.topic gt " +
            "WHERE ugp.user.id = :userId " +
            "ORDER BY gt.orderIndex ASC, gl.orderIndex ASC")
    List<UserGrammarProgress> findUserProgressWithLessonDetails(@Param("userId") Long userId);

    // ===== NEW QUERIES FOR READING PROGRESS TRACKING =====
    
    // Tìm lessons mà user đã đọc nhưng chưa hoàn thành
    @Query("SELECT ugp FROM UserGrammarProgress ugp " +
           "WHERE ugp.user.id = :userId " +
           "AND ugp.readingTime > 0 " +
           "AND ugp.isCompleted = false " +
           "ORDER BY ugp.updatedAt DESC")
    List<UserGrammarProgress> findInProgressLessons(@Param("userId") Long userId);
    
    // Tìm lessons người dùng đã scroll đến cuối
    @Query("SELECT ugp FROM UserGrammarProgress ugp " +
           "WHERE ugp.user.id = :userId " +
           "AND ugp.hasScrolledToEnd = true " +
           "ORDER BY ugp.updatedAt DESC")
    List<UserGrammarProgress> findLessonsScrolledToEnd(@Param("userId") Long userId);
    
    // Tính tổng thời gian đọc của user (giây)
    @Query("SELECT COALESCE(SUM(ugp.readingTime), 0) FROM UserGrammarProgress ugp " +
           "WHERE ugp.user.id = :userId")
    Long getTotalReadingTimeByUserId(@Param("userId") Long userId);
    
    // Tìm bài học có điểm cao nhất của user
    @Query("SELECT ugp FROM UserGrammarProgress ugp " +
           "WHERE ugp.user.id = :userId " +
           "ORDER BY ugp.scorePercentage DESC " +
           "LIMIT 1")
    Optional<UserGrammarProgress> findBestScoreLesson(@Param("userId") Long userId);
    
    // Statistics: Tính completion rate của user trong topic
    @Query("SELECT " +
           "CAST(COUNT(CASE WHEN ugp.isCompleted = true THEN 1 END) AS double) * 100.0 / COUNT(ugp) " +
           "FROM UserGrammarProgress ugp JOIN ugp.lesson gl " +
           "WHERE ugp.user.id = :userId AND gl.topic.id = :topicId")
    Double getCompletionRateInTopic(@Param("userId") Long userId, @Param("topicId") Long topicId);
}