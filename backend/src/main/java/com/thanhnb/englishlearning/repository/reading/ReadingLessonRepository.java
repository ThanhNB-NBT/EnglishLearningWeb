package com.thanhnb.englishlearning.repository.reading;

import com.thanhnb.englishlearning.entity.reading.ReadingLesson;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReadingLessonRepository extends JpaRepository<ReadingLesson, Long> {

        Page<ReadingLesson> findAllByIsActiveTrueOrderByOrderIndexAsc(Pageable pageable);

        /**
         * Tìm tất cả lessons active, sắp xếp theo orderIndex
         */
        List<ReadingLesson> findAllByIsActiveTrueOrderByOrderIndexAsc();

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

        // ==================== COUNT QUERIES ====================

        /**
         * Đếm số lessons active
         */
        long countByIsActiveTrue();

        /**
         * Đếm tổng số lessons
         */
        @Override
        long count();

        /**
         * Lấy orderIndex lớn nhất hiện có
         */
        @Query("SELECT MAX(l.orderIndex) FROM ReadingLesson l")
        Integer findMaxOrderIndex();

        /**
         * Lấy lesson đầu tiên (orderIndex nhỏ nhất)
         */
        Optional<ReadingLesson> findFirstByOrderByOrderIndexAsc();

        /**
         * Lấy lesson cuối cùng (orderIndex lớn nhất)
         */
        Optional<ReadingLesson> findFirstByOrderByOrderIndexDesc();

        /**
         * Tìm lessons có orderIndex >= giá trị cho trước
         */
        List<ReadingLesson> findByOrderIndexGreaterThanEqualOrderByOrderIndexAsc(Integer orderIndex);

        /**
         * Tìm lessons có orderIndex > giá trị cho trước
         */
        List<ReadingLesson> findByOrderIndexGreaterThanOrderByOrderIndexAsc(Integer orderIndex);

        // ==================== SEARCH QUERIES ====================

        /**
         * Tìm kiếm lessons theo title (case-insensitive, partial match)
         */
        @Query("SELECT l FROM ReadingLesson l WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<ReadingLesson> searchByTitle(@Param("keyword") String keyword);

        /**
         * Tìm kiếm lessons theo title với filter active
         */
        @Query("SELECT l FROM ReadingLesson l WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "AND l.isActive = :isActive")
        List<ReadingLesson> searchByTitleAndActive(@Param("keyword") String keyword,
                        @Param("isActive") Boolean isActive);

        // ==================== VALIDATION QUERIES ====================

        /**
         * Tìm các lessons có orderIndex trùng lặp
         */
        @Query("SELECT l.orderIndex, COUNT(l) FROM ReadingLesson l " +
                        "GROUP BY l.orderIndex HAVING COUNT(l) > 1")
        List<Object[]> findDuplicateOrderIndexes();

        /**
         * Kiểm tra có gaps trong orderIndex không
         * Trả về list các orderIndex bị thiếu
         */
        @Query(value = "SELECT t.seq FROM generate_series(1, " +
                        "COALESCE((SELECT MAX(order_index) FROM reading_lessons), 0)) AS t(seq) " +
                        "WHERE NOT EXISTS (SELECT 1 FROM reading_lessons WHERE order_index = t.seq)", nativeQuery = true)
        List<Integer> findMissingOrderIndexes();

        // ==================== BULK OPERATIONS ====================

        /**
         * Xóa tất cả lessons không active (soft deleted)
         */
        void deleteByIsActiveFalse();

}
