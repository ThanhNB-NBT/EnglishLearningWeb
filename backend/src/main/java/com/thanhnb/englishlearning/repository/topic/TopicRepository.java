package com.thanhnb.englishlearning.repository.topic;

import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.enums.EnglishLevel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

        // ==================== BASIC QUERIES ====================

        /**
         * Find all active topics
         */
        List<Topic> findByIsActiveTrueOrderByModuleTypeAscOrderIndexAsc();

        Page<Topic> findByModuleType(ModuleType moduleType, Pageable pageable);

        List<Topic> findByModuleType(ModuleType moduleType);

        Page<Topic> findByModuleTypeAndIsActiveTrue(ModuleType moduleType, Pageable pageable);

        /**
         * Find topic by ID and module type
         */
        Optional<Topic> findByIdAndModuleType(Long id, ModuleType moduleType);

        // Tìm danh sách topic theo module
        List<Topic> findByModuleTypeAndIsActiveTrueOrderByOrderIndexAsc(ModuleType moduleType);

        // Tìm cho User học: Lấy theo module + level (VD: Lấy list bài Reading level B1)
        List<Topic> findByModuleTypeAndIsActiveOrderByOrderIndexAsc(ModuleType moduleType, boolean isActive);

        // Lấy danh sách để re-index (sắp xếp lại)
        List<Topic> findByModuleTypeOrderByOrderIndexAsc(ModuleType moduleType);

        // Check trùng tên (trong phạm vi 1 module)
        boolean existsByNameAndModuleType(String name, ModuleType moduleType);

        @Query("SELECT MAX(t.orderIndex) FROM Topic t WHERE t.id = :topicId")
        Integer findMaxOrderIndexByTopicId(@Param("topicId") Long topicId);

        @Query("SELECT MAX(t.orderIndex) FROM Topic t WHERE t.moduleType = :moduleType")
        Integer findMaxOrderIndexByModuleType(@Param("moduleType") ModuleType moduleType);

        @Query("SELECT t.id FROM Topic t WHERE t.moduleType = :moduleType " +
                        "AND t.orderIndex = 0 " +
                        "AND t.isActive = false " +
                        "AND t.description = 'PLACEMENT_TEST'")
        Optional<Long> findPlacementTopicIdByModuleType(@Param("moduleType") ModuleType moduleType);

        // ==================== LEVEL-BASED QUERIES ====================

        /**
         * Find topics accessible for a specific English level
         */
        @Query("SELECT t FROM Topic t WHERE t.moduleType = :moduleType " +
                        "AND t.isActive = true " +
                        "AND t.levelRequired <= :userLevel " +
                        "ORDER BY t.orderIndex ASC")
        List<Topic> findAccessibleTopicsForLevel(
                        @Param("moduleType") ModuleType moduleType,
                        @Param("userLevel") EnglishLevel userLevel);

        // ==================== TEACHER QUERIES ====================

        /**
         * Find all topics managed by a specific teacher
         */
        @Query("SELECT DISTINCT t FROM Topic t " +
                        "JOIN TeacherTopicAssignment tta ON tta.topic.id = t.id " +
                        "WHERE tta.teacher.id = :teacherId " +
                        "AND tta.isActive = true " +
                        "AND t.isActive = true " +
                        "ORDER BY t.moduleType, t.orderIndex")
        List<Topic> findTopicsByTeacherId(@Param("teacherId") Long teacherId);

        /**
         * Find topics by teacher and module type
         */
        @Query("SELECT DISTINCT t FROM Topic t " +
                        "JOIN TeacherTopicAssignment tta ON tta.topic.id = t.id " +
                        "WHERE tta.teacher.id = :teacherId " +
                        "AND t.moduleType = :moduleType " +
                        "AND tta.isActive = true " +
                        "AND t.isActive = true " +
                        "ORDER BY t.orderIndex")
        List<Topic> findTopicsByTeacherIdAndModuleType(
                        @Param("teacherId") Long teacherId,
                        @Param("moduleType") ModuleType moduleType);

        /**
         * Check if teacher has access to a specific topic
         */
        @Query("SELECT COUNT(tta) > 0 FROM TeacherTopicAssignment tta " +
                        "WHERE tta.teacher.id = :teacherId " +
                        "AND tta.topic.id = :topicId " +
                        "AND tta.isActive = true")
        boolean isTeacherAssignedToTopic(
                        @Param("teacherId") Long teacherId,
                        @Param("topicId") Long topicId);

        // ==================== STATISTICS ====================

        /**
         * Count topics by module type
         */
        long countByModuleTypeAndIsActiveTrue(ModuleType moduleType);

        /**
         * Count all active topics
         */
        long countByIsActiveTrue();

        long countLessonsById(Long topicId);

        // ==================== ADMIN QUERIES ====================

        /**
         * Find all topics including inactive ones (for admin)
         */
        List<Topic> findAllByOrderByModuleTypeAscOrderIndexAsc();

        @Modifying
        @Query("UPDATE Topic t SET t.orderIndex = t.orderIndex - 1 " +
                        "WHERE t.moduleType = :moduleType AND t.orderIndex > :deletedOrderIndex")
        int shiftOrderAfterDelete(ModuleType moduleType, Integer deletedOrderIndex);
}