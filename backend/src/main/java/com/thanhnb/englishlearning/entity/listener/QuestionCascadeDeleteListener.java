package com.thanhnb.englishlearning.entity.listener;

import com.thanhnb.englishlearning.entity.grammar.GrammarLesson;
import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import jakarta.persistence.PreRemove;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 🗑️ Entity Listener for Cascade Deleting Questions
 * 
 * ✅ FIXED: Single @PreRemove method handling all lesson types
 */
@Component
@Slf4j
public class QuestionCascadeDeleteListener {
    
    private static QuestionRepository questionRepository;
    
    @Autowired
    public void setQuestionRepository(QuestionRepository questionRepository) {
        QuestionCascadeDeleteListener.questionRepository = questionRepository;
        log.info("✅ QuestionCascadeDeleteListener initialized");
    }
    
    // =========================================================================
    // ✅ SINGLE @PreRemove METHOD - Handles all lesson types
    // =========================================================================
    
    /**
     * Single callback for all lesson deletions
     * JPA will call this for any entity using this listener
     */
    @PreRemove
    public void beforeDelete(Object entity) {
        log.debug("@PreRemove triggered for entity: {}", entity.getClass().getSimpleName());
        
        // Determine lesson type and handle accordingly
        if (entity instanceof GrammarLesson) {
            handleGrammarLesson((GrammarLesson) entity);
        } else if (entity instanceof ReadingLesson) {
            handleReadingLesson((ReadingLesson) entity);
        } else if (entity instanceof ListeningLesson) {
            handleListeningLesson((ListeningLesson) entity);
        } else {
            log.warn("⚠️ Unknown entity type: {}", entity.getClass().getName());
        }
    }
    
    // =========================================================================
    // LESSON TYPE HANDLERS
    // =========================================================================
    
    private void handleGrammarLesson(GrammarLesson lesson) {
        cascadeDeleteQuestions(ParentType.GRAMMAR, lesson.getId(), "GrammarLesson");
    }
    
    private void handleReadingLesson(ReadingLesson lesson) {
        cascadeDeleteQuestions(ParentType.READING, lesson.getId(), "ReadingLesson");
    }
    
    private void handleListeningLesson(ListeningLesson lesson) {
        cascadeDeleteQuestions(ParentType.LISTENING, lesson.getId(), "ListeningLesson");
    }
    
    // =========================================================================
    // CORE DELETION LOGIC
    // =========================================================================
    
    private void cascadeDeleteQuestions(ParentType parentType, Long parentId, String lessonTypeName) {
        if (questionRepository == null) {
            log.error("❌ QuestionRepository not initialized!");
            return;
        }
        
        if (parentId == null) {
            log.warn("⚠️ Parent ID is null - skipping cascade delete");
            return;
        }
        
        try {
            List<Question> questions = questionRepository
                .findByParentTypeAndParentIdOrderByOrderIndexAsc(parentType, parentId);
            
            if (!questions.isEmpty()) {
                int count = questions.size();
                questionRepository.deleteAll(questions);
                
                log.info("✅ Cascade deleted {} questions for {} id={}", 
                    count, lessonTypeName, parentId);
                
                if (log.isDebugEnabled()) {
                    String questionIds = questions.stream()
                        .map(q -> q.getId().toString())
                        .collect(java.util.stream.Collectors.joining(", "));
                    log.debug("   Deleted question IDs: [{}]", questionIds);
                }
            } else {
                log.debug("ℹ️ No questions found for {} id={}", lessonTypeName, parentId);
            }
            
        } catch (Exception e) {
            log.error("❌ Error cascade deleting questions for {} id={}: {}", 
                lessonTypeName, parentId, e.getMessage(), e);
            log.warn("⚠️ Continuing with lesson deletion despite question deletion failure");
        }
    }
    
    // =========================================================================
    // UTILITY METHODS
    // =========================================================================
    
    public static boolean isInitialized() {
        return questionRepository != null;
    }
    
    public static long countQuestionsForLesson(ParentType parentType, Long parentId) {
        if (questionRepository == null) {
            log.warn("QuestionRepository not initialized");
            return 0;
        }
        return questionRepository.countByParentTypeAndParentId(parentType, parentId);
    }
}