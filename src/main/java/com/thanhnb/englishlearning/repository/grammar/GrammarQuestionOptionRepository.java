package com.thanhnb.englishlearning.repository.grammar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thanhnb.englishlearning.entity.grammar.GrammarQuestionOption;

import java.util.List;

@Repository
public interface GrammarQuestionOptionRepository extends JpaRepository<GrammarQuestionOption, Long> {
    
    // Find options by question (ordered)
    List<GrammarQuestionOption> findByQuestionIdOrderByOrderIndexAsc(Long questionId);
    
    // Find correct option for question
    GrammarQuestionOption findByQuestionIdAndIsCorrectTrue(Long questionId);
    
    // Count options for question
    long countByQuestionId(Long questionId);
    
    // Delete all options for question (for admin cleanup)
    void deleteByQuestionId(Long questionId);
}

