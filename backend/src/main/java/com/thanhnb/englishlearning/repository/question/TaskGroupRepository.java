package com.thanhnb.englishlearning.repository.question;

import com.thanhnb.englishlearning.entity.question.TaskGroup;
import com.thanhnb.englishlearning.enums.ParentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskGroupRepository extends JpaRepository<TaskGroup, Long> {

        List<TaskGroup> findByParentTypeAndParentIdOrderByOrderIndexAsc(
                        ParentType parentType,
                        Long parentId);

        @Query("SELECT MAX(tg.orderIndex) FROM TaskGroup tg " +
                        "WHERE tg.parentType = :parentType AND tg.parentId = :parentId")
        Optional<Integer> findMaxOrderIndex(
                        @Param("parentType") ParentType parentType,
                        @Param("parentId") Long parentId);

        long countByParentTypeAndParentId(ParentType parentType, Long parentId);

        @Query("""
                        SELECT DISTINCT tg
                        FROM TaskGroup tg
                        LEFT JOIN FETCH tg.questions q
                        WHERE tg.parentType = :parentType
                        AND tg.parentId = :parentId
                        ORDER BY tg.orderIndex ASC, q.orderIndex ASC
                        """)
        List<TaskGroup> findByParentTypeAndParentIdWithQuestions(
                        @Param("parentType") ParentType parentType,
                        @Param("parentId") Long parentId);

        boolean existsByParentTypeAndParentIdAndTaskName(
                        ParentType parentType,
                        Long parentId,
                        String taskName);

        @Query("SELECT tg FROM TaskGroup tg WHERE tg.id = :id")
        Optional<TaskGroup> findByIdEagerly(@Param("id") Long id);

        @Query("SELECT tg FROM TaskGroup tg " +
                        "LEFT JOIN FETCH tg.questions " +
                        "WHERE tg.id = :id")
        Optional<TaskGroup> findByIdWithQuestions(@Param("id") Long id);
}