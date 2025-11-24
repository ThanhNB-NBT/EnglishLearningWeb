package com.thanhnb.englishlearning.service.reading;

import com.thanhnb.englishlearning.dto.reading.ReadingLessonDTO;
import com.thanhnb.englishlearning.entity.reading.ReadingLesson;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.reading.UserReadingProgressRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service chuyên xử lý CRUD cho Reading Lessons
 * Refactored:
 * - Remove QuestionOptionRepository (deprecated)
 * - Use QuestionService for cascade operations
 * - Consistent with Grammar/Listening modules
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadingLessonService {

    private final ReadingLessonRepository lessonRepository;
    private final UserReadingProgressRepository progressRepository;
    private final QuestionRepository questionRepository;
    private final ReadingOrderService orderService;
    private final ReadingQuestionService questionService;

    private static final int DEFAULT_POINTS_REWARD = 25;

    /**
     * [ADMIN] Lấy tất cả lessons với pagination (bao gồm inactive)
     */
    public Page<ReadingLessonDTO> getAllLessonsPaginated(Pageable pageable) {
        log.info("[ADMIN] Loading all reading lessons with pagination");

        return lessonRepository.findAll(pageable)
                .map(lesson -> {
                    ReadingLessonDTO dto = convertToDTO(lesson);
                    long questionCount = questionRepository.countByParentTypeAndParentId(
                            ParentType.READING, lesson.getId());
                    dto.setQuestionCount((int) questionCount);
                    return dto;
                });
    }

    /**
     * [ADMIN] Lấy chi tiết 1 lesson
     */
    public ReadingLessonDTO getLessonDetail(Long lessonId) {
        log.info("[ADMIN] Loading lesson detail: lessonId={}", lessonId);

        ReadingLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với id: " + lessonId));

        ReadingLessonDTO dto = convertToDTO(lesson);

        long questionCount = questionRepository.countByParentTypeAndParentId(
                ParentType.READING, lessonId);
        dto.setQuestionCount((int) questionCount);

        return dto;
    }

    /**
     * [ADMIN] Tạo lesson mới
     */
    public ReadingLessonDTO createLesson(ReadingLessonDTO dto) {
        log.info("[ADMIN] Creating new reading lesson: {}", dto.getTitle());

        validateLessonDTO(dto);

        if (lessonRepository.existsByTitleIgnoreCase(dto.getTitle())) {
            throw new RuntimeException("Tiêu đề bài đọc đã tồn tại");
        }

        ReadingLesson lesson = new ReadingLesson();
        lesson.setTitle(dto.getTitle());
        lesson.setContent(dto.getContent());
        lesson.setContentTranslation(dto.getContentTranslation());
        lesson.setOrderIndex(dto.getOrderIndex());
        lesson.setPointsReward(dto.getPointsReward() != null ? dto.getPointsReward() : DEFAULT_POINTS_REWARD);
        lesson.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        lesson.setCreatedAt(LocalDateTime.now());

        ReadingLesson savedLesson = lessonRepository.save(lesson);
        log.info("Created reading lesson: id={}, title='{}'", savedLesson.getId(), savedLesson.getTitle());

        return convertToDTO(savedLesson);
    }

    /**
     * [ADMIN] Cập nhật lesson
     */
    public ReadingLessonDTO updateLesson(Long id, ReadingLessonDTO dto) {
        log.info("[ADMIN] Updating reading lesson: id={}", id);

        ReadingLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với id: " + id));

        if (lessonRepository.existsByTitleIgnoreCaseAndIdNot(dto.getTitle(), id)) {
            throw new RuntimeException("Tiêu đề bài đọc đã tồn tại");
        }

        lesson.setTitle(dto.getTitle());
        lesson.setContent(dto.getContent());
        lesson.setContentTranslation(dto.getContentTranslation());
        lesson.setOrderIndex(dto.getOrderIndex());

        if (dto.getPointsReward() != null) {
            lesson.setPointsReward(dto.getPointsReward());
        }

        if (dto.getIsActive() != null) {
            lesson.setIsActive(dto.getIsActive());
        }

        ReadingLesson savedLesson = lessonRepository.save(lesson);
        log.info("Updated reading lesson: id={}", id);

        return convertToDTO(savedLesson);
    }

    /**
     * [ADMIN] set isActive = false
     */
    public void deactiveLesson(Long id) {
        log.info("[ADMIN] Soft deleting reading lesson: id={}", id);

        ReadingLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với id: " + id));

        lesson.setIsActive(false);
        lessonRepository.save(lesson);

        log.info("Soft deleted reading lesson: id={}", id);
    }

    /**
     * [ADMIN] delete lesson
     * Cascade: questions (via QuestionService) + progress
     */
    public void deleteLesson(Long id) {
        log.info("[ADMIN] deleting reading lesson: id={}", id);

        ReadingLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với id: " + id));

        Integer deletedOrderIndex = lesson.getOrderIndex();

        List<Question> questions = questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(
                ParentType.READING, id);

        if (!questions.isEmpty()) {
            List<Long> questionIds = questions.stream()
                    .map(Question::getId)
                    .toList();

            questionService.bulkDeleteQuestions(questionIds);
            log.info("Deleted {} questions for lesson {}", questionIds.size(), lesson.getTitle());
        }

        progressRepository.deleteByLessonId(id);
        log.info("Deleted user progress for lesson {}", id);

        lessonRepository.delete(lesson);
        log.info("Permanently deleted reading lesson: id={}", id);

        orderService.reorderLessonsAfterDelete(deletedOrderIndex);
    }

    /**
     * [ADMIN] Activate/Deactivate lesson
     */
    public void toggleLessonStatus(Long id) {
        log.info("[ADMIN] Toggling lesson status: id={}", id);

        ReadingLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài đọc không tồn tại với id: " + id));

        lesson.setIsActive(!lesson.getIsActive());
        lessonRepository.save(lesson);

        log.info("Lesson {} is now {}", id, lesson.getIsActive() ? "active" : "inactive");
    }

    /**
     * [ADMIN] Lấy orderIndex tiếp theo
     */
    public Integer getNextOrderIndex() {
        Integer maxOrder = lessonRepository.findMaxOrderIndex();

        if (maxOrder != null) {
            log.info("Max lesson orderIndex: {}", maxOrder);
            return maxOrder + 1;
        }

        log.info("No lessons found, returning 1");
        return 1;
    }

    /**
     * Validate lesson DTO
     */
    private void validateLessonDTO(ReadingLessonDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Tiêu đề không được để trống");
        }

        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new RuntimeException("Nội dung không được để trống");
        }

        if (dto.getOrderIndex() == null || dto.getOrderIndex() < 1) {
            throw new RuntimeException("OrderIndex phải lớn hơn 0");
        }
    }

    /**
     * Convert entity to DTO
     */
    private ReadingLessonDTO convertToDTO(ReadingLesson lesson) {
        return ReadingLessonDTO.full(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getContentTranslation(),
                lesson.getOrderIndex(),
                lesson.getPointsReward(),
                lesson.getIsActive(),
                lesson.getCreatedAt());
    }
}