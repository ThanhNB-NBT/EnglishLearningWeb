package com.thanhnb.englishlearning.repository.grammar;

import com.thanhnb.englishlearning.entity.grammar.GrammarQuestion;
import com.thanhnb.englishlearning.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrammarQuestionRepository extends JpaRepository<GrammarQuestion, Long> {

    // ===== ADMIN CRUD =====
    // Find all questions by lesson (for admin)
    List<GrammarQuestion> findByLessonIdOrderByIdAsc(Long lessonId);
    
    // Find questions by lesson and type (for admin filter)
    List<GrammarQuestion> findByLessonIdAndQuestionTypeOrderByIdAsc(Long lessonId, QuestionType questionType);
    
    // Search questions by content (for admin search)
    @Query("SELECT gq FROM GrammarQuestion gq WHERE gq.lesson.id = :lessonId " +
           "AND (gq.questionText LIKE %:keyword% OR gq.correctAnswer LIKE %:keyword%) " +
           "ORDER BY gq.id ASC")
    List<GrammarQuestion> findByLessonIdAndContentContaining(@Param("lessonId") Long lessonId, @Param("keyword") String keyword);
    
    // Count questions by lesson and type
    long countByLessonIdAndQuestionType(Long lessonId, QuestionType questionType);

    // ===== USER VIEW =====
    // Find question with options
    @Query("SELECT gq FROM GrammarQuestion gq LEFT JOIN FETCH gq.options gqo WHERE gq.id = :questionId ORDER BY gqo.orderIndex ASC")
    Optional<GrammarQuestion> findByIdWithOptions(@Param("questionId") Long questionId);

    // Count questions in a lesson
    long countByLessonId(Long lessonId);

    // Get random questions for mini quiz (unlock quiz)
    @Query("SELECT gq FROM GrammarQuestion gq WHERE gq.lesson.id = :lessonId ORDER BY RANDOM() LIMIT :count")
    List<GrammarQuestion> findRandomQuestionsByLessonId(@Param("lessonId") Long lessonId, @Param("count") Integer count);
}