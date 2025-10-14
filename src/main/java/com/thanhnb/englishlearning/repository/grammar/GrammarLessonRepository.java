package com.thanhnb.englishlearning.repository.grammar;

import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.enums.LessonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrammarLessonRepository extends JpaRepository<GrammarLesson, Long> {

    // ===== ADMIN ===========
    // Tìm tất cả lessons theo topic
    List<GrammarLesson> findByTopicIdOrderByOrderIndexAsc(Long topicId);

    // Tìm lesson theo topic và trạng thái active
    List<GrammarLesson> findByTopicIdAndIsActiveOrderByOrderIndexAsc(Long topicId, Boolean isActive);

    // Tìm lesson với câu hỏi (sử dụng bảng question)
    @Query("SELECT gl FROM GrammarLesson gl " +
           "LEFT JOIN FETCH Question q ON q.parentId = gl.id AND q.parentType = 'GRAMMAR' " +
           "WHERE gl.id = :lessonId" +
           " ORDER BY q.orderIndex ASC")
    Optional<GrammarLesson> findByIdWithQuestions(@Param("lessonId") Long lessonId);

    // Kiểm tra tiêu đề trong topic (admin validation)
    boolean existsByTopicIdAndTitleIgnoreCase(Long topicId, String title);
    boolean existsByTopicIdAndTitleIgnoreCaseAndIdNot(Long topicId, String title, Long id);

    // Kiểm tra lesson tồn tại và active
    Boolean existsByIdAndIsActiveTrue(Long lessonId);

    // Tìm lesson theo title
    List<GrammarLesson> findByTitleContainingIgnoreCaseOrderByTopicIdAscOrderIndexAsc(String title);

    // Số lượng lesson trong topic
    long countByTopicId(Long topicId);
    long countByTopicIdAndIsActive(Long topicId, Boolean isActive);


    // ===== USER ============
    // Tìm lesson theo topic, vị trí theo order_index
    List<GrammarLesson> findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(Long topicId);

    // Tìm lesson theo kiểu(type) của topic
    List<GrammarLesson> findByTopicIdAndLessonTypeAndIsActiveTrueOrderByOrderIndexAsc(Long topicId, LessonType lessonType);

    // Tìm lessons với questions (sử dụng bảng unified)
    @Query("SELECT gl FROM GrammarLesson gl " +
           "LEFT JOIN FETCH Question q ON q.parentId = gl.id AND q.parentType = 'GRAMMAR' " +
           "WHERE gl.id = :lessonId AND gl.isActive = true " +
           "ORDER BY q.orderIndex ASC")
    Optional<GrammarLesson> findByIdWithLessons(@Param("lessonId") Long lessonId);

    // Tìm lesson tiếp theo trong topic
    @Query("SELECT gl FROM GrammarLesson gl WHERE gl.topic.id = :topicId " +
           "AND gl.orderIndex > :currentOrderIndex AND gl.isActive = true " +
           "ORDER BY gl.orderIndex ASC LIMIT 1")
    Optional<GrammarLesson> findNextLessonInTopic(@Param("topicId") Long topicId, @Param("currentOrderIndex") Integer currentOrderIndex);

    // Đếm số lesson theo lesson type trong topic
    long countByTopicIdAndLessonTypeAndIsActiveTrue(Long topicId, LessonType lessonType);

    // Tìm lesson theo estimated duration (ví dụ: cho quick practice)
    @Query("SELECT gl FROM GrammarLesson gl " +
           "WHERE gl.topic.id = :topicId " +
           "AND gl.estimatedDuration <= :maxDuration " +
           "AND gl.isActive = true " +
           "ORDER BY gl.orderIndex ASC")
    List<GrammarLesson> findShortLessonsInTopic(@Param("topicId") Long topicId, @Param("maxDuration") Integer maxDuration);

}
