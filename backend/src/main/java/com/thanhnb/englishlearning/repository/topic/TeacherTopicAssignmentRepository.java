package com.thanhnb.englishlearning.repository.topic;

import com.thanhnb.englishlearning.entity.topic.TeacherTopicAssignment;
import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.ModuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherTopicAssignmentRepository extends JpaRepository<TeacherTopicAssignment, Long> {

        // ==================== EXISTING METHODS ====================

        Optional<TeacherTopicAssignment> findByTeacherAndTopic(User teacher, Topic topic);

        Optional<TeacherTopicAssignment> findByTeacherIdAndTopicId(Long teacherId, Long topicId);

        List<TeacherTopicAssignment> findByTeacherIdAndIsActiveTrueOrderByModuleTypeAscTopicOrderIndexAsc(
                        Long teacherId);

        List<TeacherTopicAssignment> findByTopicIdAndIsActiveTrue(Long topicId);

        List<TeacherTopicAssignment> findByIsActiveTrueOrderByAssignedAtDesc();

        boolean existsByTeacherIdAndTopicIdAndIsActiveTrue(Long teacherId, Long topicId);

        long countByTeacherIdAndIsActiveTrue(Long teacherId);

        long countByTopicIdAndIsActiveTrue(Long topicId);

        /**
         * Get active assignment
         */
        Optional<TeacherTopicAssignment> findByTeacherIdAndTopicIdAndIsActiveTrue(Long teacherId, Long topicId);

        // ==================== NEW METHODS FOR TEACHER PORTAL ====================

        /**
         * ✅ NEW: Find assignments by teacher and module type
         */
        @Query("SELECT a FROM TeacherTopicAssignment a " +
                        "WHERE a.teacher.id = :teacherId " +
                        "AND a.topic.moduleType = :moduleType " +
                        "AND a.isActive = true " +
                        "ORDER BY a.topic.orderIndex ASC")
        List<TeacherTopicAssignment> findByTeacherIdAndModuleType(
                        @Param("teacherId") Long teacherId,
                        @Param("moduleType") ModuleType moduleType);

        /**
         * ✅ NEW: Find topics by teacher and module type (with pagination)
         */
        @Query("SELECT a.topic FROM TeacherTopicAssignment a " +
                        "WHERE a.teacher.id = :teacherId " +
                        "AND a.topic.moduleType = :moduleType " +
                        "AND a.isActive = true")
        Page<Topic> findTopicsByTeacherAndModule(
                        @Param("teacherId") Long teacherId,
                        @Param("moduleType") ModuleType moduleType,
                        Pageable pageable);

        /**
         * ✅ NEW: Get assignment with full details (fetch join)
         */
        @Query("SELECT a FROM TeacherTopicAssignment a " +
                        "JOIN FETCH a.teacher " +
                        "JOIN FETCH a.topic " +
                        "LEFT JOIN FETCH a.assignedBy " +
                        "WHERE a.id = :assignmentId")
        Optional<TeacherTopicAssignment> findByIdWithDetails(@Param("assignmentId") Long assignmentId);

        /**
         * ✅ NEW: Find teacher IDs by topic
         */
        @Query("SELECT a.teacher.id FROM TeacherTopicAssignment a " +
                        "WHERE a.topic.id = :topicId " +
                        "AND a.isActive = true")
        List<Long> findTeacherIdsByTopicId(@Param("topicId") Long topicId);

        /**
         * ✅ NEW: Find topic IDs by teacher
         */
        @Query("SELECT a.topic.id FROM TeacherTopicAssignment a " +
                        "WHERE a.teacher.id = :teacherId " +
                        "AND a.isActive = true")
        List<Long> findTopicIdsByTeacherId(@Param("teacherId") Long teacherId);

        /**
         * ✅ NEW: Deactivate all assignments by teacher
         */
        @Modifying
        @Query("UPDATE TeacherTopicAssignment a " +
                        "SET a.isActive = false " +
                        "WHERE a.teacher.id = :teacherId")
        int deactivateAllByTeacherId(@Param("teacherId") Long teacherId);

        /**
         * ✅ NEW: Deactivate all assignments by topic
         */
        @Modifying
        @Query("UPDATE TeacherTopicAssignment a " +
                        "SET a.isActive = false " +
                        "WHERE a.topic.id = :topicId")
        int deactivateAllByTopicId(@Param("topicId") Long topicId);
}