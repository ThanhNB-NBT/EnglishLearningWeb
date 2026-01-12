package com.thanhnb.englishlearning.repository.grammar;

import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.enums.LessonType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrammarLessonRepository extends JpaRepository<GrammarLesson, Long> {

       // ===== ADMIN ===========
       // Tìm tất cả lessons theo topic
       Page<GrammarLesson> findByTopicIdOrderByOrderIndexAsc(Long topicId, Pageable pageable);

       Page<GrammarLesson> findByTopicId(Long topicId, Pageable pageable);

       List<GrammarLesson> findByTopicIdOrderByOrderIndexAsc(Long topicId);

       List<GrammarLesson> findByIsActiveTrueOrderByOrderIndexAsc();

       // Lấy số thứ tự lớn nhất của lesson trong topic
       @Query("SELECT MAX(gl.orderIndex) FROM GrammarLesson gl WHERE gl.topic.id = :topicId")
       Integer findMaxOrderIndexByTopicId(@Param("topicId") Long topicId);

       @Modifying
       @Query("UPDATE GrammarLesson g SET g.orderIndex = g.orderIndex - 1 " +
                     "WHERE g.topic.id = :topicId AND g.orderIndex > :deletedOrderIndex")
       int shiftOrderAfterDelete(Long topicId, Integer deletedOrderIndex);

       // Tìm lesson với câu hỏi (sử dụng bảng question)
       @Query("SELECT gl FROM GrammarLesson gl " +
                     "LEFT JOIN FETCH Question q ON q.parentId = gl.id AND q.parentType = 'GRAMMAR' " +
                     "WHERE gl.id = :lessonId" +
                     " ORDER BY q.orderIndex ASC")
       Optional<GrammarLesson> findByIdWithQuestions(@Param("lessonId") Long lessonId);

       // Kiểm tra tiêu đề trong topic (admin validation)
       boolean existsByTopicIdAndTitleIgnoreCase(Long topicId, String title);

       boolean existsByTopicIdAndTitleIgnoreCaseAndIdNot(Long topicId, String title, Long id);

       // Số lượng lesson trong topic
       long countByTopicId(Long topicId);

       int countByTopicIdAndIsActiveTrue(Long topicId);

       @Query("SELECT gl FROM GrammarLesson gl WHERE gl.topic.id = :topicId ORDER BY gl.orderIndex ASC")
       List<GrammarLesson> findAllByTopicIdOrderByOrderIndexAsc(@Param("topicId") Long topicId);

       // ===== USER ============
       // Tìm lesson theo topic, vị trí theo order_index
       List<GrammarLesson> findByTopicIdAndIsActiveTrueOrderByOrderIndexAsc(Long topicId);

       // Tìm lesson theo kiểu(type) của topic
       List<GrammarLesson> findByTopicIdAndLessonTypeAndIsActiveTrueOrderByOrderIndexAsc(Long topicId,
                     LessonType lessonType);

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
       Optional<GrammarLesson> findNextLessonInTopic(@Param("topicId") Long topicId,
                     @Param("currentOrderIndex") Integer currentOrderIndex);

       // Đếm số lesson theo lesson type trong topic
       long countByTopicIdAndLessonTypeAndIsActiveTrue(Long topicId, LessonType lessonType);
}
