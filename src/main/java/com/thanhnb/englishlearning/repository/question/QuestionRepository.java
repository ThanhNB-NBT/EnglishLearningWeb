package com.thanhnb.englishlearning.repository.question;

import com.thanhnb.englishlearning.enums.QuestionType;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

       // ===== COMMON QUERIES (for all parent types) =====

       // Tìm tất cả questions theo parent (Grammar/Reading/Speaking)
       Page<Question> findByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType parentType, Long parentId,
                     Pageable pageable);

       List<Question> findByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType parentType, Long parentId);

       // Tìm questions theo parent và type
       List<Question> findByParentTypeAndParentIdAndQuestionTypeOrderByOrderIndexAsc(
                     ParentType parentType, Long parentId, QuestionType questionType);

       // Lấy số thứ tự lớn nhất của question trong parent
       @Query("SELECT MAX(q.orderIndex) FROM Question q WHERE q.parentType = :parentType AND q.parentId = :parentId")
       Integer findMaxOrderIndexByParentTypeAndParentId(
                     @Param("parentType") ParentType parentType,
                     @Param("parentId") Long parentId);

       // Tìm question với options
       @Query("SELECT q FROM Question q LEFT JOIN FETCH q.options qo " +
                     "WHERE q.id = :questionId ORDER BY qo.orderIndex ASC")
       Optional<Question> findByIdWithOptions(@Param("questionId") Long questionId);

       // Count questions by parent
       long countByParentTypeAndParentId(ParentType parentType, Long parentId);

       // Count questions by parent and type
       long countByParentTypeAndParentIdAndQuestionType(ParentType parentType, Long parentId,
                     QuestionType questionType);

       // ===== GRAMMAR SPECIFIC =====

       // Tìm grammar questions theo lesson
       @Query("SELECT q FROM Question q WHERE q.parentType = 'GRAMMAR' " +
                     "AND q.parentId = :lessonId ORDER BY q.orderIndex ASC")
       List<Question> findGrammarQuestionsByLessonId(@Param("lessonId") Long lessonId);

       // Search grammar questions by content
       @Query("SELECT q FROM Question q WHERE q.parentType = 'GRAMMAR' " +
                     "AND q.parentId = :lessonId " +
                     "AND (q.questionText LIKE %:keyword% OR q.correctAnswer LIKE %:keyword%) " +
                     "ORDER BY q.orderIndex ASC")
       List<Question> findGrammarQuestionsByContentContaining(@Param("lessonId") Long lessonId,
                     @Param("keyword") String keyword);

       // Get random grammar questions (for mini quiz)
       @Query(value = "SELECT * FROM questions q WHERE q.parent_type = 'GRAMMAR' " +
                     "AND q.parent_id = :lessonId ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
       List<Question> findRandomGrammarQuestions(@Param("lessonId") Long lessonId, @Param("count") Integer count);

       // ===== READING SPECIFIC =====

       // Tìm reading questions theo lesson
       @Query("SELECT q FROM Question q WHERE q.parentType = 'READING' " +
                     "AND q.parentId = :lessonId ORDER BY q.orderIndex ASC")
       List<Question> findReadingQuestionsByLessonId(@Param("lessonId") Long lessonId);

       // Get random reading questions
       @Query(value = "SELECT * FROM questions q WHERE q.parent_type = 'READING' " +
                     "AND q.parent_id = :lessonId ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
       List<Question> findRandomReadingQuestions(@Param("lessonId") Long lessonId, @Param("count") Integer count);

       // ===== SPEAKING SPECIFIC =====

       // Tìm speaking questions theo topic
       @Query("SELECT q FROM Question q WHERE q.parentType = 'SPEAKING' " +
                     "AND q.parentId = :topicId ORDER BY q.orderIndex ASC")
       List<Question> findSpeakingQuestionsByTopicId(@Param("topicId") Long topicId);

       // Tìm speaking questions có time limit
       @Query("SELECT q FROM Question q WHERE q.parentType = 'SPEAKING' " +
                     "AND q.parentId = :topicId AND q.timeLimitSeconds IS NOT NULL " +
                     "ORDER BY q.orderIndex ASC")
       List<Question> findSpeakingQuestionsWithTimeLimit(@Param("topicId") Long topicId);

       // Get random speaking questions
       @Query(value = "SELECT * FROM questions q WHERE q.parent_type = 'SPEAKING' " +
                     "AND q.parent_id = :topicId ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
       List<Question> findRandomSpeakingQuestions(@Param("topicId") Long topicId, @Param("count") Integer count);

       // ===== ADMIN QUERIES =====

       // Xóa tất cả questions theo parent (admin cleanup)
       void deleteByParentTypeAndParentId(ParentType parentType, Long parentId);

       // Tìm questions chưa có explanation (admin review)
       @Query("SELECT q FROM Question q WHERE q.parentType = :parentType " +
                     "AND (q.explanation IS NULL OR q.explanation = '') " +
                     "ORDER BY q.createdAt DESC")
       List<Question> findQuestionsWithoutExplanation(@Param("parentType") ParentType parentType);

       // Statistics: Count questions by type for a parent
       @Query("SELECT q.questionType, COUNT(q) FROM Question q " +
                     "WHERE q.parentType = :parentType AND q.parentId = :parentId " +
                     "GROUP BY q.questionType")
       List<Object[]> countQuestionsByType(@Param("parentType") ParentType parentType,
                     @Param("parentId") Long parentId);

       @Modifying(clearAutomatically = true, flushAutomatically = true)
       @Query("update Question q set q.orderIndex = q.orderIndex - 1 " +
                     "where q.parentType = :parentType and q.parentId = :parentId and q.orderIndex > :deletedPosition")
       int shiftOrderAfterDelete(@Param("parentType") ParentType parentType,
                     @Param("parentId") Long parentId,
                     @Param("deletedPosition") Integer deletedPosition);

       List<Question> findByIdIn(List<Long> ids);
}