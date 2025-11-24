package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service chuyên xử lý logic orderIndex cho Reading module
 * - Auto reorder sau khi xóa
 * - Manual reorder khi chèn giữa
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadingOrderService {

    private final ReadingLessonRepository lessonRepository;
    private final QuestionRepository questionRepository;

    // ===== REORDER LESSONS AFTER DELETE =====

    /**
     * Sắp xếp lại orderIndex của Lessons sau khi xóa
     * (Reading không có Topic, nên chỉ cần shift toàn bộ lessons)
     */
    public void reorderLessonsAfterDelete(Integer deletedPosition) {
        List<ReadingLesson> lessons = lessonRepository.findAll();
        
        List<ReadingLesson> affectedLessons = lessons.stream()
                .filter(lesson -> lesson.getOrderIndex() > deletedPosition)
                .collect(Collectors.toList());

        if (affectedLessons.isEmpty()) {
            log.info("No lessons need reordering after deletion at position {}", deletedPosition);
            return;
        }

        affectedLessons.forEach(lesson -> lesson.setOrderIndex(lesson.getOrderIndex() - 1));
        lessonRepository.saveAll(affectedLessons);

        log.info("Reordered {} reading lessons after deletion at position {}", 
                affectedLessons.size(), deletedPosition);
    }

    /**
     * Sắp xếp lại orderIndex của Questions sau khi xóa
     */
    public void reorderQuestionsAfterDelete(Long lessonId, Integer deletedPosition) {
        int affected = questionRepository.shiftOrderAfterDelete(ParentType.READING, lessonId, deletedPosition);
        log.info("Reordered {} reading questions after deletion at position {}", affected, deletedPosition);
    }

    // ===== MANUAL REORDER (INSERT) =====

    /**
     * Sắp xếp lại khi chèn lesson vào giữa
     */
    public int reorderLessons(Integer insertPosition, Long excludeLessonId) {
        if (insertPosition == null || insertPosition < 1) {
            throw new IllegalArgumentException("Vị trí chèn phải lớn hơn 0");
        }

        List<ReadingLesson> allLessons = lessonRepository.findAll();

        // Loại bỏ lesson đang edit
        if (excludeLessonId != null) {
            allLessons = allLessons.stream()
                    .filter(lesson -> !lesson.getId().equals(excludeLessonId))
                    .collect(Collectors.toList());
        }

        // Lọc các lessons cần đẩy ra sau
        List<ReadingLesson> affectedLessons = allLessons.stream()
                .filter(lesson -> lesson.getOrderIndex() >= insertPosition)
                .collect(Collectors.toList());

        if (affectedLessons.isEmpty()) {
            log.info("No lessons need reordering at position {}", insertPosition);
            return 0;
        }

        // Đẩy ra sau 1 vị trí
        affectedLessons.forEach(lesson -> lesson.setOrderIndex(lesson.getOrderIndex() + 1));
        lessonRepository.saveAll(affectedLessons);

        log.info("Reordered {} reading lessons starting from position {}",
                affectedLessons.size(), insertPosition);

        return affectedLessons.size();
    }

    /**
     * Swap 2 lessons (đổi vị trí)
     */
    @Transactional
    public void swapLessons(Long lessonId1, Long lessonId2) {
        ReadingLesson lesson1 = lessonRepository.findById(lessonId1)
                .orElseThrow(() -> new RuntimeException("Lesson 1 không tồn tại"));
        
        ReadingLesson lesson2 = lessonRepository.findById(lessonId2)
                .orElseThrow(() -> new RuntimeException("Lesson 2 không tồn tại"));

        Integer temp = lesson1.getOrderIndex();
        lesson1.setOrderIndex(lesson2.getOrderIndex());
        lesson2.setOrderIndex(temp);

        lessonRepository.save(lesson1);
        lessonRepository.save(lesson2);

        log.info("Swapped lessons: {} (order {}) ↔ {} (order {})", 
                lesson1.getTitle(), lesson2.getOrderIndex(),
                lesson2.getTitle(), lesson1.getOrderIndex());
    }

    /**
     * Di chuyển lesson từ vị trí cũ sang vị trí mới
     */
    @Transactional
    public void moveLessonToPosition(Long lessonId, Integer newPosition) {
        ReadingLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson không tồn tại"));

        Integer oldPosition = lesson.getOrderIndex();

        if (oldPosition.equals(newPosition)) {
            log.info("Lesson already at position {}", newPosition);
            return;
        }

        List<ReadingLesson> allLessons = lessonRepository.findAll();
        allLessons.sort((a, b) -> a.getOrderIndex().compareTo(b.getOrderIndex()));

        if (newPosition > oldPosition) {
            // Di chuyển xuống: shift up các lessons ở giữa
            allLessons.stream()
                    .filter(l -> l.getOrderIndex() > oldPosition && l.getOrderIndex() <= newPosition)
                    .forEach(l -> l.setOrderIndex(l.getOrderIndex() - 1));
        } else {
            // Di chuyển lên: shift down các lessons ở giữa
            allLessons.stream()
                    .filter(l -> l.getOrderIndex() >= newPosition && l.getOrderIndex() < oldPosition)
                    .forEach(l -> l.setOrderIndex(l.getOrderIndex() + 1));
        }

        lesson.setOrderIndex(newPosition);
        lessonRepository.saveAll(allLessons);

        log.info("Moved reading lesson '{}' from position {} to {}", 
                lesson.getTitle(), oldPosition, newPosition);
    }
}