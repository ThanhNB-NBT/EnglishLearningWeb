package com.thanhnb.englishlearning.service.listening;

import com.thanhnb.englishlearning.config.AudioStorageProperties;
import com.thanhnb.englishlearning.dto. listening.ListeningLessonDTO;
import com.thanhnb.englishlearning.dto.listening.request.CreateListeningLessonRequest;
import com.thanhnb.englishlearning.dto.listening.request.UpdateListeningLessonRequest;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning. enums.ParentType;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning. repository.listening.UserListeningProgressRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain. Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation. Transactional;
import org. springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java. util.List;

/**
 * Service chuyên xử lý CRUD cho Listening Lessons
 * Tương tự ReadingLessonService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListeningLessonService {

    private final ListeningLessonRepository lessonRepository;
    private final UserListeningProgressRepository progressRepository;
    private final QuestionRepository questionRepository;
    private final ListeningOrderService orderService;
    private final ListeningQuestionService questionService;
    private final AudioStorageService audioStorageService;

    // ═════════════════════════════════════════════════════════════════
    // ADMIN OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Lấy tất cả lessons với pagination (bao gồm inactive)
     */
    public Page<ListeningLessonDTO> getAllLessonsPaginated(Pageable pageable) {
        log.info("[ADMIN] Loading all listening lessons with pagination");

        return lessonRepository.findAll(pageable)
                .map(lesson -> {
                    ListeningLessonDTO dto = convertToSummaryDTO(lesson);
                    long questionCount = questionRepository.countByParentTypeAndParentId(
                            ParentType.LISTENING, lesson.getId());
                    dto. setQuestionCount((int) questionCount);
                    return dto;
                });
    }

    /**
     * [ADMIN] Lấy chi tiết 1 lesson
     */
    public ListeningLessonDTO getLessonDetail(Long lessonId) {
        log.info("[ADMIN] Loading lesson detail: lessonId={}", lessonId);

        ListeningLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id: " + lessonId));

        ListeningLessonDTO dto = convertToFullDTO(lesson);

        long questionCount = questionRepository.countByParentTypeAndParentId(
                ParentType.LISTENING, lessonId);
        dto.setQuestionCount((int) questionCount);

        return dto;
    }

    /**
     * [ADMIN] Tạo lesson mới với audio
     */
    public ListeningLessonDTO createLesson(CreateListeningLessonRequest request, MultipartFile audioFile) 
            throws IOException {
        log.info("[ADMIN] Creating new listening lesson:  {}", request.getTitle());

        // Validate
        if (lessonRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new RuntimeException("Tiêu đề bài nghe đã tồn tại");
        }

        // Create entity
        ListeningLesson lesson = new ListeningLesson();
        lesson.setTitle(request.getTitle());
        lesson.setTranscript(request.getTranscript());
        lesson.setTranscriptTranslation(request.getTranscriptTranslation());
        lesson.setDifficulty(request.getDifficulty());
        lesson.setOrderIndex(request.getOrderIndex());
        lesson.setTimeLimitSeconds(request.getTimeLimitSeconds());
        lesson.setPointsReward(request.getPointsReward());
        lesson.setAllowUnlimitedReplay(request.getAllowUnlimitedReplay());
        lesson.setMaxReplayCount(request.getMaxReplayCount());
        lesson.setIsActive(request.getIsActive());
        lesson.setCreatedAt(LocalDateTime.now());

        // Save to get ID
        ListeningLesson savedLesson = lessonRepository.save(lesson);

        // Upload audio with REAL lesson ID
        String audioUrl = audioStorageService.uploadAudio(audioFile, savedLesson.getId());
        savedLesson.setAudioUrl(audioUrl);

        // Save again with audio URL
        savedLesson = lessonRepository.save(savedLesson);

        log.info("Created listening lesson: id={}, title='{}', audioUrl='{}'", 
                savedLesson.getId(), savedLesson.getTitle(), audioUrl);

        return convertToFullDTO(savedLesson);
    }

    /**
     * [ADMIN] Cập nhật lesson
     */
    public ListeningLessonDTO updateLesson(Long id, UpdateListeningLessonRequest request, 
            MultipartFile audioFile) throws IOException {
        log.info("[ADMIN] Updating listening lesson: id={}", id);

        ListeningLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id: " + id));

        // Check title uniqueness
        if (request.getTitle() != null && 
                lessonRepository.existsByTitleIgnoreCaseAndIdNot(request.getTitle(), id)) {
            throw new RuntimeException("Tiêu đề bài nghe đã tồn tại");
        }

        // Update fields
        if (request.getTitle() != null) {
            lesson.setTitle(request.getTitle());
        }
        if (request.getTranscript() != null) {
            lesson.setTranscript(request.getTranscript());
        }
        if (request.getTranscriptTranslation() != null) {
            lesson.setTranscriptTranslation(request.getTranscriptTranslation());
        }
        if (request.getDifficulty() != null) {
            lesson.setDifficulty(request.getDifficulty());
        }
        if (request.getOrderIndex() != null) {
            lesson.setOrderIndex(request.getOrderIndex());
        }
        if (request.getTimeLimitSeconds() != null) {
            lesson.setTimeLimitSeconds(request.getTimeLimitSeconds());
        }
        if (request.getPointsReward() != null) {
            lesson.setPointsReward(request.getPointsReward());
        }
        if (request. getAllowUnlimitedReplay() != null) {
            lesson.setAllowUnlimitedReplay(request.getAllowUnlimitedReplay());
        }
        if (request.getMaxReplayCount() != null) {
            lesson.setMaxReplayCount(request.getMaxReplayCount());
        }
        if (request.getIsActive() != null) {
            lesson. setIsActive(request.getIsActive());
        }

        // Update audio if provided
        if (audioFile != null && !audioFile.isEmpty()) {
            String oldAudioUrl = lesson.getAudioUrl();
            String newAudioUrl = audioStorageService. updateAudio(audioFile, id, oldAudioUrl);
            lesson.setAudioUrl(newAudioUrl);
        }

        ListeningLesson savedLesson = lessonRepository.save(lesson);
        log.info("Updated listening lesson: id={}", id);

        return convertToFullDTO(savedLesson);
    }

    /**
     * [ADMIN] Set isActive = false
     */
    public void deactivateLesson(Long id) {
        log.info("[ADMIN] Deactivating listening lesson: id={}", id);

        ListeningLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id: " + id));

        lesson.setIsActive(false);
        lessonRepository.save(lesson);

        log.info("Deactivated listening lesson: id={}", id);
    }

    /**
     * [ADMIN] Xóa lesson vĩnh viễn (bao gồm audio file)
     */
    public void deleteLesson(Long id) {
        log.info("[ADMIN] Deleting listening lesson: id={}", id);

        ListeningLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id:  " + id));

        Integer deletedOrderIndex = lesson.getOrderIndex();

        // Delete questions
        List<Question> questions = questionRepository. findByParentTypeAndParentIdOrderByOrderIndexAsc(
                ParentType. LISTENING, id);

        if (!questions.isEmpty()) {
            List<Long> questionIds = questions. stream()
                    .map(Question::getId)
                    . toList();

            questionService.bulkDeleteQuestions(questionIds);
            log.info("Deleted {} questions for lesson {}", questionIds.size(), lesson.getTitle());
        }

        // Delete user progress
        progressRepository.deleteByLessonId(id);
        log.info("Deleted user progress for lesson {}", id);

        // Delete audio file
        if (lesson.getAudioUrl() != null && !lesson.getAudioUrl().isEmpty()) {
            audioStorageService.deleteAudio(lesson.getAudioUrl());
        }

        // Delete lesson
        lessonRepository.delete(lesson);
        log.info("Permanently deleted listening lesson: id={}", id);

        // Reorder
        orderService.reorderLessonsAfterDelete(deletedOrderIndex);
    }

    /**
     * [ADMIN] Toggle lesson status
     */
    public void toggleLessonStatus(Long id) {
        log.info("[ADMIN] Toggling lesson status:  id={}", id);

        ListeningLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id: " + id));

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

    // ═════════════════════════════════════════════════════════════════
    // CONVERSION METHODS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Convert entity to summary DTO (for list view)
     */
    private ListeningLessonDTO convertToSummaryDTO(ListeningLesson lesson) {
        return ListeningLessonDTO.summary(
                lesson.getId(),
                lesson.getTitle(),
                lesson. getDifficulty(),
                lesson. getOrderIndex(),
                lesson. getPointsReward(),
                lesson. getIsActive(),
                0); // questionCount will be set by caller
    }

    /**
     * Convert entity to full DTO (for detail view)
     */
    private ListeningLessonDTO convertToFullDTO(ListeningLesson lesson) {
        return ListeningLessonDTO.full(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getAudioUrl(),
                lesson.getTranscript(),
                lesson. getTranscriptTranslation(),
                lesson.getDifficulty(),
                lesson.getTimeLimitSeconds(),
                lesson. getOrderIndex(),
                lesson.getPointsReward(),
                lesson. getAllowUnlimitedReplay(),
                lesson.getMaxReplayCount(),
                lesson.getIsActive(),
                lesson.getCreatedAt());
    }
}