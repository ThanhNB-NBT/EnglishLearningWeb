package com.thanhnb.englishlearning.repository.question;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;

import com.thanhnb.englishlearning.entity.question.QuestionOption;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    // Find options by question (ordered)
    List<QuestionOption> findByQuestionIdOrderByOrderIndexAsc(Long questionId);

    // Find correct option for question
    Optional<QuestionOption> findByQuestionIdAndIsCorrectTrue(Long questionId);

    // Find all correct options for question (support multiple correct answers)
    List<QuestionOption> findByQuestionIdAndIsCorrectTrueOrderByOrderIndexAsc(Long questionId);

    // Count options for question
    long countByQuestionId(Long questionId);

    // Count correct options for question
    long countByQuestionIdAndIsCorrectTrue(Long questionId);

    // Check if question has any options
    boolean existsByQuestionId(Long questionId);

    // Delete all options for question (for admin cleanup)
    void deleteByQuestionId(Long questionId);

    // Find options by question with specific order range
    @Query("SELECT qo FROM QuestionOption qo WHERE qo.question.id = :questionId " +
            "AND qo.orderIndex BETWEEN :startIndex AND :endIndex " +
            "ORDER BY qo.orderIndex ASC")
    List<QuestionOption> findByQuestionIdAndOrderRange(
            @Param("questionId") Long questionId,
            @Param("startIndex") Integer startIndex,
            @Param("endIndex") Integer endIndex);

    // Get random options for shuffle (admin preview/test)
    @Query(value = "SELECT * FROM question_options WHERE question_id = :questionId " +
            "ORDER BY RANDOM()", nativeQuery = true)
    List<QuestionOption> findRandomOptionsByQuestionId(@Param("questionId") Long questionId);

    @Modifying
    @Query("delete from QuestionOption o where o.question.id in :questionIds")
    void deleteByQuestionIdIn(@Param("questionIds") List<Long> questionIds);
}