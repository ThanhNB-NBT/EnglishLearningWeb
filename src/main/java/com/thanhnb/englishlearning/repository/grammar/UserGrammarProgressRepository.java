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

    //Tìm tiến độ cụ thể của người dùng
    Optional<UserGrammarProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    // Tìm tiến độ của tất cả người dùng trong topic
    @Query("SELECT ugp FROM UserGrammarProgress ugp JOIN ugp.lesson gl" +
            " WHERE ugp.user.id = :userId AND gl.topic.id = :topicId" +
            " ORDER BY gl.orderIndex ASC")
    List<UserGrammarProgress> findByUserIdAndTopicId(@Param("userId") Long userId, @Param("topicId") Long topicId);

    // Tìm người dùng hoàn thành lessons
    List<UserGrammarProgress> findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(Long userId);

    // Kiểm tra nếu tiến độ người dùng hoàn thành
    boolean existsByUserIdAndLessonIdAndIsCompletedTrue(Long userId, Long lessonId);

    // Số lượng hoàn thành lesson trong topic
    @Query("SELECT COUNT(ugp) FROM UserGrammarProgress ugp JOIN ugp.lesson gl " +
            "WHERE ugp.user.id = :userId AND gl.topic.id = :topicId AND ugp.isCompleted = true")
    Long countCompletedLessonsInTopic(@Param("userId") Long userId, @Param("topicId") Long topicId);

    // Lấy tổng điểm ngữ pháp của người dùng
    @Query("SELECT COALESCE(SUM(ugp.score), 0) FROM UserGrammarProgress ugp WHERE ugp.user.id = :userId")
    Integer getTotalScoreByUserId(@Param("userId") Long userId);

    // Tìm tiến độ người dùng với thông tin lesson
    @Query("SELECT ugp FROM UserGrammarProgress ugp " +
            " JOIN FETCH ugp.lesson gl " +
            " JOIN FETCH gl.topic gt " +
            " WHERE ugp.user.id = :userId " +
            " ORDER BY gt.orderIndex ASC, gl.orderIndex ASC ")
    List<UserGrammarProgress> findUserProgressWithLessonDetails(@Param("userId") Long userId);

}
