package com.thanhnb.englishlearning.service.listening;

import com.thanhnb.englishlearning.dto.listening.ListeningLessonDTO;
import com.thanhnb.englishlearning.dto.listening. request.CreateListeningLessonRequest;
import com.thanhnb.englishlearning.dto.listening.request.UpdateListeningLessonRequest;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning. enums.ParentType;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning. repository.listening.UserListeningProgressRepository;
import com.thanhnb. englishlearning.repository.question.QuestionRepository;
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
 * Service xử lý CRUD cho Listening Lessons
 * Tách riêng concerns: chỉ quản lý lesson entity + audio files
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

    private static final int DEFAULT_POINTS_REWARD = 25;

    // ═════════════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Lấy tất cả lessons với pagination (bao gồm inactive)
     */
    public Page<ListeningLessonDTO> getAllLessonsPaginated(Pageable pageable) {
        log.info("[ADMIN] Loading all listening lessons with pagination");

        return lessonRepository.findAll(pageable)
                .map(lesson -> {
                    ListeningLessonDTO dto = convertToDTO(lesson);
                    long questionCount = questionRepository.countByParentTypeAndParentId(
                            ParentType.LISTENING, lesson.getId());
                    dto.setQuestionCount((int) questionCount);
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

        ListeningLessonDTO dto = convertToDTO(lesson);

        long questionCount = questionRepository.countByParentTypeAndParentId(
                ParentType.LISTENING, lessonId);
        dto.setQuestionCount((int) questionCount);

        return dto;
    }

    // ═════════════════════════════════════════════════════════════════
    // CREATE OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Tạo lesson mới với audio file
     */
    public ListeningLessonDTO createLesson(CreateListeningLessonRequest request, MultipartFile audioFile) 
            throws IOException {
        log.info("[ADMIN] Creating new listening lesson:  {}", request.getTitle());

        // Validate unique title
        if (lessonRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new RuntimeException("Tiêu đề bài nghe đã tồn tại");
        }

        // Create entity WITHOUT audio URL first
        ListeningLesson lesson = new ListeningLesson();
        lesson.setTitle(request.getTitle());
        lesson.setTranscript(request.getTranscript());
        lesson.setTranscriptTranslation(request.getTranscriptTranslation());
        lesson.setDifficulty(request.getDifficulty() != null 
                ? request.getDifficulty()
                : ListeningLesson. Difficulty.BEGINNER);
        lesson.setOrderIndex(request.getOrderIndex());
        lesson.setTimeLimitSeconds(request.getTimeLimitSeconds() != null 
                ? request.getTimeLimitSeconds() 
                : 600);
        lesson.setPointsReward(request.getPointsReward() != null 
                ?  request.getPointsReward() 
                : DEFAULT_POINTS_REWARD);
        lesson.setAllowUnlimitedReplay(request.getAllowUnlimitedReplay() != null 
                ? request.getAllowUnlimitedReplay() 
                : true);
        lesson.setMaxReplayCount(request.getMaxReplayCount() != null 
                ? request.getMaxReplayCount() 
                : 3);
        lesson.setIsActive(request.getIsActive() != null 
                ? request.getIsActive() 
                : true);
        lesson.setCreatedAt(LocalDateTime.now());

        // Save to get ID first
        ListeningLesson savedLesson = lessonRepository.save(lesson);
        log.info("Created lesson entity with ID: {}", savedLesson.getId());

        // Upload audio with REAL lesson ID
        if (audioFile != null && !audioFile.isEmpty()) {
            String audioUrl = audioStorageService.uploadAudio(audioFile, savedLesson.getId());
            savedLesson.setAudioUrl(audioUrl);
            savedLesson = lessonRepository.save(savedLesson);
            log.info("Uploaded audio file: {}", audioUrl);
        }

        log.info("Created listening lesson:  id={}, title='{}'", savedLesson.getId(), savedLesson.getTitle());

        return convertToDTO(savedLesson);
    }

    // ═════════════════════════════════════════════════════════════════
    // UPDATE OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Cập nhật lesson (audio file optional)
     */
    public ListeningLessonDTO updateLesson(Long id, UpdateListeningLessonRequest request, 
            MultipartFile audioFile) throws IOException {
        log.info("[ADMIN] Updating listening lesson: id={}", id);

        ListeningLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id: " + id));

        // Check unique title (if provided and different)
        if (request.getTitle() != null &&
                !request.getTitle().equals(lesson.getTitle()) &&
                lessonRepository.existsByTitleIgnoreCaseAndIdNot(request.getTitle(), id)) {
            throw new RuntimeException("Tiêu đề bài nghe đã tồn tại");
        }

        // Update audio file if provided
        if (audioFile != null && !audioFile.isEmpty()) {
            String newAudioUrl = audioStorageService.updateAudio(audioFile, id, lesson.getAudioUrl());
            lesson.setAudioUrl(newAudioUrl);
            log.info("Updated audio file: {}", newAudioUrl);
        }

        // Update fields (only if provided)
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
        if (request.getAllowUnlimitedReplay() != null) {
            lesson.setAllowUnlimitedReplay(request.getAllowUnlimitedReplay());
        }
        if (request.getMaxReplayCount() != null) {
            lesson.setMaxReplayCount(request.getMaxReplayCount());
        }
        if (request.getIsActive() != null) {
            lesson.setIsActive(request.getIsActive());
        }

        ListeningLesson savedLesson = lessonRepository.save(lesson);
        log.info("Updated listening lesson: id={}", id);

        return convertToDTO(savedLesson);
    }

    // ═════════════════════════════════════════════════════════════════
    // DELETE OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    /**
     * [ADMIN] Set isActive = false
     */
    public void deactivateLesson(Long id) {
        log.info("[ADMIN] Soft deleting listening lesson: id={}", id);

        ListeningLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id: " + id));

        lesson.setIsActive(false);
        lessonRepository.save(lesson);

        log.info("Soft deleted listening lesson: id={}", id);
    }

    /**
     * [ADMIN] Delete lesson permanently
     * Cascade:  questions (via QuestionService) + progress + audio file
     */
    public void deleteLesson(Long id) {
        log.info("[ADMIN] Deleting listening lesson: id={}", id);

        ListeningLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id: " + id));

        Integer deletedOrderIndex = lesson.getOrderIndex();

        // 1. Delete questions
        List<Question> questions = questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(
                ParentType.LISTENING, id);

        if (!questions.isEmpty()) {
            List<Long> questionIds = questions.stream()
                    .map(Question::getId)
                    .toList();

            questionService.bulkDeleteQuestions(questionIds);
            log.info("Deleted {} questions for lesson {}", questionIds.size(), lesson.getTitle());
        }

        // 2. Delete user progress
        progressRepository.deleteByLessonId(id);
        log.info("Deleted user progress for lesson {}", id);

        // 3. Delete audio file
        if (lesson.getAudioUrl() != null) {
            audioStorageService.deleteAudio(lesson.getAudioUrl());
            log.info("Deleted audio file: {}", lesson.getAudioUrl());
        }

        // 4. Delete lesson
        lessonRepository.delete(lesson);
        log.info("Permanently deleted listening lesson: id={}", id);

        // 5. Reorder remaining lessons
        orderService.reorderLessonsAfterDelete(deletedOrderIndex);
    }

    /**
     * [ADMIN] Activate/Deactivate lesson
     */
    public void toggleLessonStatus(Long id) {
        log.info("[ADMIN] Toggling lesson status: id={}", id);

        ListeningLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id: " + id));

        lesson.setIsActive(!lesson.getIsActive());
        lessonRepository.save(lesson);

        log.info("Lesson {} is now {}", id, lesson.getIsActive() ? "active" : "inactive");
    }

    // ═════════════════════════════════════════════════════════════════
    // ORDER OPERATIONS
    // ═════════════════════════════════════════════════════════════════

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
    // CONVERSION
    // ═════════════════════════════════════════════════════════════════

    /**
     * Convert entity to DTO
     */
    private ListeningLessonDTO convertToDTO(ListeningLesson lesson) {
        ListeningLessonDTO dto = new ListeningLessonDTO();
        dto.setId(lesson.getId());
        dto.setTitle(lesson.getTitle());
        dto.setAudioUrl(lesson.getAudioUrl());
        dto.setTranscript(lesson.getTranscript());
        dto.setTranscriptTranslation(lesson. getTranscriptTranslation());
        dto.setDifficulty(lesson.getDifficulty() != null ? lesson.getDifficulty().name() : "BEGINNER");
        dto.setTimeLimitSeconds(lesson.getTimeLimitSeconds());
        dto.setOrderIndex(lesson.getOrderIndex());
        dto.setPointsReward(lesson.getPointsReward());
        dto.setAllowUnlimitedReplay(lesson.getAllowUnlimitedReplay());
        dto.setMaxReplayCount(lesson.getMaxReplayCount());
        dto.setIsActive(lesson.getIsActive());
        dto.setCreatedAt(lesson.getCreatedAt());
        return dto;
    }
}