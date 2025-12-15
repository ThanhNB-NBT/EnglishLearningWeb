package com.thanhnb.englishlearning.service.listening;

import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util. List;

/**
 * Service quản lý orderIndex của Listening Lessons
 * Tách riêng concerns: chỉ xử lý logic liên quan đến thứ tự
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListeningOrderService {

    private final ListeningLessonRepository lessonRepository;

    // ═════════════════════════════════════════════════════════════════
    // REORDER OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Di chuyển lesson đến vị trí mới
     */
    public void moveLessonToPosition(Long lessonId, Integer newOrderIndex) {
        log.info("Moving listening lesson {} to position {}", lessonId, newOrderIndex);

        ListeningLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id: " + lessonId));

        Integer oldOrderIndex = lesson.getOrderIndex();

        if (oldOrderIndex.equals(newOrderIndex)) {
            log.info("Lesson {} already at position {}, no action needed", lessonId, newOrderIndex);
            return;
        }

        List<ListeningLesson> allLessons = lessonRepository.findAllByOrderByOrderIndexAsc();

        if (newOrderIndex < 1 || newOrderIndex > allLessons.size()) {
            throw new RuntimeException("Vị trí mới không hợp lệ:  " + newOrderIndex);
        }

        // Remove lesson from old position
        allLessons.remove(lesson);

        // Insert at new position (0-indexed)
        allLessons.add(newOrderIndex - 1, lesson);

        // Reindex all lessons
        for (int i = 0; i < allLessons.size(); i++) {
            allLessons.get(i).setOrderIndex(i + 1);
        }

        lessonRepository.saveAll(allLessons);

        log.info("Moved lesson {} from position {} to {}", lessonId, oldOrderIndex, newOrderIndex);
    }

    /**
     * Swap 2 lessons
     */
    public void swapLessons(Long lessonId1, Long lessonId2) {
        log.info("Swapping listening lessons {} and {}", lessonId1, lessonId2);

        ListeningLesson lesson1 = lessonRepository.findById(lessonId1)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id: " + lessonId1));

        ListeningLesson lesson2 = lessonRepository.findById(lessonId2)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id:  " + lessonId2));

        Integer tempOrder = lesson1.getOrderIndex();
        lesson1.setOrderIndex(lesson2.getOrderIndex());
        lesson2.setOrderIndex(tempOrder);

        lessonRepository.save(lesson1);
        lessonRepository.save(lesson2);

        log.info("Swapped lessons:  {} (order: {}) <-> {} (order: {})",
                lessonId1, lesson2.getOrderIndex(), lessonId2, lesson1.getOrderIndex());
    }

    // ═════════════════════════════════════════════════════════════════
    // AUTO-FIX OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Reindex tất cả lessons sau khi delete
     */
    public void reorderLessonsAfterDelete(Integer deletedOrderIndex) {
        log.info("Reordering listening lessons after deleting position {}", deletedOrderIndex);

        List<ListeningLesson> affectedLessons = lessonRepository
                .findByOrderIndexGreaterThanOrderByOrderIndexAsc(deletedOrderIndex);

        if (affectedLessons.isEmpty()) {
            log.info("No lessons to reorder");
            return;
        }

        // Shift down by 1
        for (ListeningLesson lesson : affectedLessons) {
            lesson.setOrderIndex(lesson.getOrderIndex() - 1);
        }

        lessonRepository.saveAll(affectedLessons);

        log.info("Reordered {} lessons after deletion", affectedLessons.size());
    }

    /**
     * Fix duplicate orderIndexes
     */
    public int fixDuplicateOrderIndexes() {
        log.info("Fixing duplicate orderIndexes for listening lessons");

        List<Object[]> duplicates = lessonRepository.findDuplicateOrderIndexes();

        if (duplicates.isEmpty()) {
            log.info("No duplicate orderIndexes found");
            return 0;
        }

        log.warn("Found {} duplicate orderIndex values", duplicates.size());

        // Reindex all lessons
        List<ListeningLesson> allLessons = lessonRepository.findAllByOrderByOrderIndexAsc();

        for (int i = 0; i < allLessons.size(); i++) {
            allLessons.get(i).setOrderIndex(i + 1);
        }

        lessonRepository.saveAll(allLessons);

        log.info("Fixed duplicate orderIndexes, reindexed {} lessons", allLessons.size());
        return duplicates.size();
    }

    /**
     * Fix gaps in orderIndex (1, 2, 5, 7 -> 1, 2, 3, 4)
     */
    public int fixGapsInOrderIndexes() {
        log.info("Fixing gaps in orderIndexes for listening lessons");

        List<ListeningLesson> allLessons = lessonRepository.findAllByOrderByOrderIndexAsc();

        if (allLessons.isEmpty()) {
            return 0;
        }

        int fixedCount = 0;

        for (int i = 0; i < allLessons.size(); i++) {
            int expectedOrder = i + 1;
            ListeningLesson lesson = allLessons.get(i);

            if (! lesson.getOrderIndex().equals(expectedOrder)) {
                lesson.setOrderIndex(expectedOrder);
                fixedCount++;
            }
        }

        if (fixedCount > 0) {
            lessonRepository.saveAll(allLessons);
            log.info("Fixed {} gaps in orderIndexes", fixedCount);
        } else {
            log.info("No gaps found in orderIndexes");
        }

        return fixedCount;
    }

    /**
     * Validate và fix tất cả issues về orderIndex
     */
    public int validateAndFixAllOrderIssues() {
        log.info("Validating and fixing all order issues for listening lessons");

        int totalFixed = 0;

        // Fix duplicates first
        totalFixed += fixDuplicateOrderIndexes();

        // Then fix gaps
        totalFixed += fixGapsInOrderIndexes();

        log.info("Total order issues fixed: {}", totalFixed);

        return totalFixed;
    }
}