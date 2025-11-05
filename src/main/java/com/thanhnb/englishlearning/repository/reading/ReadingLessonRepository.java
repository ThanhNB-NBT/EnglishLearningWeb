package com.thanhnb.englishlearning.repository.reading;

import com.thanhnb.englishlearning.entity.reading.ReadingLesson;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReadingLessonRepository extends JpaRepository<ReadingLesson, Long> {

    Page<ReadingLesson> findAllByIsActiveTrueOrderByOrderIndexAsc(Pageable pageable);

    List<ReadingLesson> findAllByIsActiveTrueOrderByOrderIndexAsc();

    @Query("SELECT MAX(l.orderIndex) FROM ReadingLesson l")
    Integer findMaxOrderIndex();

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndIdNot(String title, Long id);

}
