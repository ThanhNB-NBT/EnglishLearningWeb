package com.thanhnb.englishlearning.repository.question;

import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.QuestionType;
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
public interface QuestionRepository extends JpaRepository<Question, Long> {

       // ==================== TÌM THEO PARENT ====================

       /**
        * Tìm tất cả câu hỏi theo parentType và parentId, sắp xếp theo orderIndex
        */
       List<Question> findByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType parentType, Long parentId);

       /**
        * Tìm tất cả câu hỏi theo parentType và parentId với phân trang
        */
       Page<Question> findByParentTypeAndParentId(ParentType parentType, Long parentId, Pageable pageable);

       /**
        * Tìm câu hỏi theo parentType
        */
       Page<Question> findByParentType(ParentType parentType, Pageable pageable);

       /**
        * Tìm câu hỏi theo parentId
        */
       Page<Question> findByParentId(Long parentId, Pageable pageable);

       // ==================== TÌM THEO QUESTION TYPE ====================

       /**
        * Tìm câu hỏi theo questionType
        */
       Page<Question> findByQuestionType(QuestionType questionType, Pageable pageable);

       /**
        * Tìm câu hỏi theo parentType, parentId và questionType
        */
       List<Question> findByParentTypeAndParentIdAndQuestionType(
                     ParentType parentType,
                     Long parentId,
                     QuestionType questionType);

       /**
        * Tìm câu hỏi theo parentType, parentId và questionType với phân trang
        */
       Page<Question> findByParentTypeAndParentIdAndQuestionType(
                     ParentType parentType,
                     Long parentId,
                     QuestionType questionType,
                     Pageable pageable);

       // ==================== ĐẾM SỐ LƯỢNG ====================

       /**
        * Đếm số câu hỏi theo parentType
        */
       Long countByParentType(ParentType parentType);

       /**
        * Đếm số câu hỏi theo parentType và parentId
        */
       Long countByParentTypeAndParentId(ParentType parentType, Long parentId);

       /**
        * Đếm số câu hỏi theo questionType
        */
       Long countByQuestionType(QuestionType questionType);

       /**
        * Đếm số câu hỏi theo parentType, parentId và questionType
        */
       Long countByParentTypeAndParentIdAndQuestionType(
                     ParentType parentType,
                     Long parentId,
                     QuestionType questionType);

       // ==================== XÓA ====================

       /**
        * Xóa tất cả câu hỏi theo parentType và parentId
        */
       void deleteByParentTypeAndParentId(ParentType parentType, Long parentId);

       /**
        * Xóa câu hỏi theo questionType
        */
       void deleteByQuestionType(QuestionType questionType);

       // ==================== TÌM KIẾM NÂNG CAO ====================

       /**
        * Tìm kiếm câu hỏi theo keyword trong questionText
        */
       @Query("SELECT q FROM Question q WHERE LOWER(q.questionText) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       Page<Question> searchByQuestionText(@Param("keyword") String keyword, Pageable pageable);

       /**
        * Tìm kiếm câu hỏi theo keyword trong questionText và parentType
        */
       @Query("SELECT q FROM Question q WHERE q.parentType = :parentType " +
                     "AND LOWER(q.questionText) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       Page<Question> searchByQuestionTextAndParentType(
                     @Param("keyword") String keyword,
                     @Param("parentType") ParentType parentType,
                     Pageable pageable);

       /**
        * Tìm kiếm câu hỏi theo nhiều điều kiện
        */
       @Query("SELECT q FROM Question q WHERE " +
                     "(:parentType IS NULL OR q.parentType = :parentType) AND " +
                     "(:parentId IS NULL OR q.parentId = :parentId) AND " +
                     "(:questionType IS NULL OR q.questionType = :questionType) AND " +
                     "(:keyword IS NULL OR LOWER(q.questionText) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       Page<Question> searchWithFilters(
                     @Param("parentType") ParentType parentType,
                     @Param("parentId") Long parentId,
                     @Param("questionType") QuestionType questionType,
                     @Param("keyword") String keyword,
                     Pageable pageable);

       // ==================== THỐNG KÊ ====================

       /**
        * Thống kê số lượng câu hỏi theo từng QuestionType
        */
       @Query("SELECT q.questionType, COUNT(q) FROM Question q GROUP BY q.questionType")
       List<Object[]> countByQuestionTypeGrouped();

       /**
        * Thống kê số lượng câu hỏi theo từng ParentType
        */
       @Query("SELECT q.parentType, COUNT(q) FROM Question q GROUP BY q.parentType")
       List<Object[]> countByParentTypeGrouped();

       /**
        * Lấy tổng điểm của tất cả câu hỏi theo parentType và parentId
        */
       @Query("SELECT COALESCE(SUM(q.points), 0) FROM Question q WHERE q.parentType = :parentType AND q.parentId = :parentId")
       Integer getTotalPointsByParent(@Param("parentType") ParentType parentType, @Param("parentId") Long parentId);

       // ==================== KIỂM TRA TỒN TẠI ====================

       /**
        * Kiểm tra có câu hỏi nào với parentType và parentId không
        */
       boolean existsByParentTypeAndParentId(ParentType parentType, Long parentId);

       /**
        * Kiểm tra có câu hỏi nào với questionType không
        */
       boolean existsByQuestionType(QuestionType questionType);

       // ==================== LẤY THEO ORDER INDEX ====================

       /**
        * Lấy câu hỏi đầu tiên (orderIndex nhỏ nhất) theo parent
        */
       Optional<Question> findFirstByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType parentType, Long parentId);

       /**
        * Lấy câu hỏi cuối cùng (orderIndex lớn nhất) theo parent
        */
       Optional<Question> findFirstByParentTypeAndParentIdOrderByOrderIndexDesc(ParentType parentType, Long parentId);

       /**
        * Lấy câu hỏi theo orderIndex cụ thể
        */
       Optional<Question> findByParentTypeAndParentIdAndOrderIndex(
                     ParentType parentType,
                     Long parentId,
                     Integer orderIndex);

       // ==================== LẤY THEO ĐIỂM ====================

       /**
        * Lấy câu hỏi có điểm >= minPoints
        */
       Page<Question> findByPointsGreaterThanEqual(Integer minPoints, Pageable pageable);

       /**
        * Lấy câu hỏi theo parentType, parentId và điểm >= minPoints
        */
       List<Question> findByParentTypeAndParentIdAndPointsGreaterThanEqual(
                     ParentType parentType,
                     Long parentId,
                     Integer minPoints);

       @Modifying
       @Query("UPDATE Question q SET q.orderIndex = q.orderIndex - 1 " +
                     "WHERE q.parentType = :parentType AND q.parentId = :parentId " +
                     "AND q.orderIndex > :deletedPosition")
       int shiftOrderAfterDelete(@Param("parentType") ParentType parentType,
                     @Param("parentId") Long parentId,
                     @Param("deletedPosition") Integer deletedPosition);
}