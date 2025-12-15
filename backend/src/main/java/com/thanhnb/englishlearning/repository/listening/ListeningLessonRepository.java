package com.thanhnb.englishlearning.repository.listening;

import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListeningLessonRepository extends JpaRepository<ListeningLesson, Long> {

        // ==================== ACTIVE LESSONS ====================

        /**
         * Tìm tất cả lessons active với pagination
         */
        Page<ListeningLesson> findAllByIsActiveTrueOrderByOrderIndexAsc(Pageable pageable);

        /**
         * Tìm tất cả lessons active, sắp xếp theo orderIndex
         */
        List<ListeningLesson> findAllByIsActiveTrueOrderByOrderIndexAsc();

        /**
         * Tìm lesson active theo ID
         */
        Optional<ListeningLesson> findByIdAndIsActiveTrue(Long id);

        /**
         * Đếm số lessons active
         */
        long countByIsActiveTrue();

        /**
         * Đếm tổng số lessons
         */
        @Override
        long count();

        // ==================== EXISTENCE CHECKS ====================

        /**
         * Kiểm tra lesson có tồn tại với title không (case-insensitive)
         */
        boolean existsByTitleIgnoreCase(String title);

        /**
         * Kiểm tra lesson có tồn tại với title nhưng khác ID
         * (dùng cho update để check unique title)
         */
        boolean existsByTitleIgnoreCaseAndIdNot(String title, Long id);

        /**
         * Kiểm tra có lesson nào với orderIndex không
         */
        boolean existsByOrderIndex(Integer orderIndex);

        // ==================== ORDER INDEX QUERIES ====================

        /**
         * Lấy orderIndex lớn nhất hiện có
         */
        @Query("SELECT MAX(l.orderIndex) FROM ListeningLesson l")
        Integer findMaxOrderIndex();

        /**
         * Lấy lesson đầu tiên (orderIndex nhỏ nhất)
         */
        Optional<ListeningLesson> findFirstByOrderByOrderIndexAsc();

        /**
         * Lấy lesson cuối cùng (orderIndex lớn nhất)
         */
        Optional<ListeningLesson> findFirstByOrderByOrderIndexDesc();

        /**
         * Tìm lessons có orderIndex >= giá trị cho trước
         */
        List<ListeningLesson> findByOrderIndexGreaterThanEqualOrderByOrderIndexAsc(Integer orderIndex);

        /**
         * Tìm lessons có orderIndex > giá trị cho trước
         */
        List<ListeningLesson> findByOrderIndexGreaterThanOrderByOrderIndexAsc(Integer orderIndex);

        /**
         * Lấy tất cả lessons sắp xếp theo orderIndex
         */
        List<ListeningLesson> findAllByOrderByOrderIndexAsc();

        // ==================== SEARCH QUERIES ====================

        /**
         * Tìm kiếm lessons theo title (case-insensitive, partial match)
         */
        @Query("SELECT l FROM ListeningLesson l WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', : keyword, '%'))")
        List<ListeningLesson> searchByTitle(@Param("keyword") String keyword);

        /**
         * Tìm kiếm lessons theo title với filter active
         */
        @Query("SELECT l FROM ListeningLesson l WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', : keyword, '%')) " +
                        "AND l.isActive = : isActive")
        List<ListeningLesson> searchByTitleAndActive(@Param("keyword") String keyword,
                        @Param("isActive") Boolean isActive);

        // ==================== VALIDATION QUERIES ====================

        /**
         * Tìm các lessons có orderIndex trùng lặp
         */
        @Query("SELECT l. orderIndex, COUNT(l) FROM ListeningLesson l " +
                        "GROUP BY l. orderIndex HAVING COUNT(l) > 1")
        List<Object[]> findDuplicateOrderIndexes();

        /**
         * Kiểm tra có gaps trong orderIndex không
         * Trả về list các orderIndex bị thiếu
         * CHỈ DÙNG CHO POSTGRESQL
         */
        @Query(value = "SELECT t.seq FROM generate_series(1, " +
                        "COALESCE((SELECT MAX(order_index) FROM listening_lessons), 0)) AS t(seq) " +
                        "WHERE NOT EXISTS (SELECT 1 FROM listening_lessons WHERE order_index = t.seq)", nativeQuery = true)
        List<Integer> findMissingOrderIndexes();

        // ==================== BULK OPERATIONS ====================

        /**
         * Xóa tất cả lessons không active (soft deleted)
         */
        void deleteByIsActiveFalse();
}