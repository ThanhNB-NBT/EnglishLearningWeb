package com.thanhnb.englishlearning.repository.question;

import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

       // Hàm Update thứ tự (Dùng cho chức năng Sắp xếp lại)
       @Modifying
       @Query("UPDATE Question q SET q.orderIndex = :orderIndex WHERE q.id = :id")
       void updateOrderIndex(@Param("id") Long id, @Param("orderIndex") Integer orderIndex);

       // Hàm Lấy Max Order (Để tính số thứ tự cho câu hỏi mới tạo) - ĐÃ THÊM MỚI
       @Query("SELECT MAX(q.orderIndex) FROM Question q WHERE q.parentType = :parentType AND q.parentId = :parentId")
       Optional<Integer> findMaxOrderIndexByLessonId(@Param("parentType") ParentType parentType,
                     @Param("parentId") Long parentId);

       List<Question> findByParentTypeAndParentIdAndTaskGroupIsNullOrderByOrderIndexAsc(ParentType parentType,
                     Long parentId);

       // Lấy câu hỏi thuộc về một Task Group cụ thể
       List<Question> findByTaskGroupIdOrderByOrderIndexAsc(Long taskGroupId);

       long countByParentTypeAndParentIdAndTaskGroupIsNull(
                     ParentType parentType,
                     Long parentId);

       @Query("SELECT q FROM Question q " +
                     "LEFT JOIN FETCH q.taskGroup " +
                     "WHERE q.parentType = :parentType AND q.parentId = :parentId " +
                     "ORDER BY q.orderIndex ASC")
       List<Question> findByParentTypeAndParentIdOrderByOrderIndexAsc(
                     @Param("parentType") ParentType parentType,
                     @Param("parentId") Long parentId);

       // Đếm số câu hỏi trong một bài học
       @Query("SELECT COUNT(q) FROM Question q WHERE q.parentType = :parentType AND q.parentId = :parentId")
       Long countByParentTypeAndParentId(@Param("parentType") ParentType parentType,
                     @Param("parentId") Long parentId);

       // Đếm số câu hỏi trong một bài học thuộc nhóm task cụ thể
       @Query("SELECT COUNT(q) FROM Question q WHERE q.parentType = :parentType AND q.parentId = :parentId AND q.taskGroup = :taskGroup")
       Long countByParentTypeAndParentIdAndTaskGroup(@Param("parentType") ParentType parentType,
                     @Param("parentId") Long parentId,
                     @Param("taskGroup") String taskGroup);

       @Query("SELECT COALESCE(MAX(q.orderIndex), 0) FROM Question q WHERE q.parentType = :parentType AND q.parentId = :parentId")
       Integer findMaxOrderIndexByParent(@Param("parentType") ParentType parentType, @Param("parentId") Long parentId);
       // -------------------------------------------------------------------------
       // CÁC HÀM HỖ TRỢ TASK GROUPING (Cho giao diện Grouped View)
       // -------------------------------------------------------------------------

       // Kiểm tra lesson có task structure không
       @Query("SELECT CASE WHEN COUNT(tg) > 0 THEN true ELSE false END " +
                     "FROM TaskGroup tg " +
                     "WHERE tg.parentType = :parentType AND tg.parentId = :parentId")
       boolean hasTaskStructure(
                     @Param("parentType") ParentType parentType,
                     @Param("parentId") Long parentId);
}