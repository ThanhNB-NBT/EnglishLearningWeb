package com.thanhnb.englishlearning.service.listening;

import com.thanhnb.englishlearning.dto.listening.ListeningLessonDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.request.CreateTaskGroupDTO;
import com.thanhnb.englishlearning.dto.question.response.TaskGroupResponseDTO;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning.repository.listening.UserListeningProgressRepository;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.topic.TopicRepository;
import com.thanhnb.englishlearning.service.permission.TeacherPermissionService;
import com.thanhnb.englishlearning.service.question.TaskGroupService;
import com.thanhnb.englishlearning.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Includes: CRUD + Audio Upload + Pagination + Order + Status Toggle
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListeningLessonService {

    private final ListeningLessonRepository lessonRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final UserListeningProgressRepository progressRepository;
    private final AudioStorageService audioStorage;
    private final TeacherPermissionService teacherPermissionService;
    private final UserService userService;
    private final ListeningQuestionService questionService;
    private final TaskGroupService taskGroupService;

    // ═════════════════════════════════════════════════════════════════
    // READ OPERATIONS
    // ═════════════════════════════════════════════════════════════════

    public Page<ListeningLessonDTO> getLessonsByTopic(Long topicId, Pageable pageable) {
        teacherPermissionService.checkTopicPermission(topicId);
        return lessonRepository.findByTopicId(topicId, pageable)
                .map(this::toDTO);
    }

    public ListeningLessonDTO getLessonById(Long id) {
        ListeningLesson lesson = findLessonById(id);
        teacherPermissionService.checkTopicPermission(lesson.getTopic().getId());
        return toDTO(lesson);
    }

    // ═════════════════════════════════════════════════════════════════
    // CREATE
    // ═════════════════════════════════════════════════════════════════

    public ListeningLessonDTO createLesson(ListeningLessonDTO dto, MultipartFile audioFile)
            throws IOException {
        validateForCreate(dto);
        teacherPermissionService.checkTopicPermission(dto.getTopicId());

        if (lessonRepository.existsByTitleIgnoreCase(dto.getTitle())) {
            throw new IllegalArgumentException("Title already exists");
        }

        Topic topic = topicRepository.findById(dto.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        User currentUser = userService.getCurrentUser();

        // Create WITHOUT audio URL first
        ListeningLesson lesson = ListeningLesson.builder()
                .topic(topic)
                .title(dto.getTitle())
                .transcript(dto.getTranscript())
                .transcriptTranslation(dto.getTranscriptTranslation())
                .orderIndex(dto.getOrderIndex())
                .timeLimitSeconds(dto.getTimeLimitSeconds() != null ? dto.getTimeLimitSeconds() : 600)
                .pointsReward(dto.getPointsReward() != null ? dto.getPointsReward() : 25)
                .allowUnlimitedReplay(dto.getAllowUnlimitedReplay() != null ? dto.getAllowUnlimitedReplay() : true)
                .maxReplayCount(dto.getMaxReplayCount() != null ? dto.getMaxReplayCount() : 3)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .modifiedBy(currentUser)
                .build();

        // Save to get ID
        ListeningLesson saved = lessonRepository.save(lesson);

        // Upload audio with real lesson ID
        if (audioFile != null && !audioFile.isEmpty()) {
            String audioUrl = audioStorage.uploadAudio(audioFile, saved.getId());
            saved.setAudioUrl(audioUrl);
            saved = lessonRepository.save(saved);
            log.info("Uploaded audio: {}", audioUrl);
        }

        log.info("Created listening lesson: id={}, title={}", saved.getId(), saved.getTitle());

        return toDTO(saved);
    }

    // ═════════════════════════════════════════════════════════════════
    // UPDATE
    // ═════════════════════════════════════════════════════════════════

    public ListeningLessonDTO updateLesson(Long id, ListeningLessonDTO dto, MultipartFile audioFile)
            throws IOException {
        ListeningLesson lesson = findLessonById(id);
        teacherPermissionService.checkTopicPermission(lesson.getTopic().getId());

        if (dto.getTitle() != null && !dto.getTitle().equals(lesson.getTitle())) {
            if (lessonRepository.existsByTitleIgnoreCaseAndIdNot(dto.getTitle(), id)) {
                throw new IllegalArgumentException("Title already exists");
            }
        }

        User currentUser = userService.getCurrentUser();
        lesson.setModifiedBy(currentUser);

        // Update audio if provided
        if (audioFile != null && !audioFile.isEmpty()) {
            String newAudioUrl = audioStorage.updateAudio(audioFile, id, lesson.getAudioUrl());
            lesson.setAudioUrl(newAudioUrl);
            log.info("Updated audio: {}", newAudioUrl);
        }

        updateFields(lesson, dto);

        ListeningLesson saved = lessonRepository.save(lesson);
        log.info("Updated listening lesson: id={}", id);

        return toDTO(saved);
    }

    // ═════════════════════════════════════════════════════════════════
    // DELETE
    // ═════════════════════════════════════════════════════════════════

    public void deleteLesson(Long id) {
        ListeningLesson lesson = findLessonById(id);
        teacherPermissionService.checkTopicPermission(lesson.getTopic().getId());
        Long topicId = lesson.getTopic().getId();
        Integer orderIndex = lesson.getOrderIndex();

        // Delete progress
        progressRepository.deleteByLessonId(id);

        // Delete audio
        if (lesson.getAudioUrl() != null) {
            audioStorage.deleteAudio(lesson.getAudioUrl());
        }

        // Delete lesson (questions cascade via listener)
        lessonRepository.delete(lesson);
        log.info("Deleted listening lesson: id={}", id);

        // Reorder
        reorderAfterDelete(topicId, orderIndex);
    }

    // ═════════════════════════════════════════════════════════════════
    // STATUS TOGGLE
    // ═════════════════════════════════════════════════════════════════

    public void toggleStatus(Long id) {
        ListeningLesson lesson = findLessonById(id);
        teacherPermissionService.checkTopicPermission(lesson.getTopic().getId());

        lesson.setIsActive(!lesson.getIsActive());
        lessonRepository.save(lesson);

        log.info("Toggled lesson {} status to: {}", id, lesson.getIsActive());
    }

    // ═════════════════════════════════════════════════════════════════
    // ORDER MANAGEMENT
    // ═════════════════════════════════════════════════════════════════

    public void fixOrderIndexes(Long topicId) {
        // Lấy danh sách
        List<ListeningLesson> lessons = lessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId);

        // ✅ Sort lại trong memory để đảm bảo ổn định (ID tăng dần nếu trùng Order)
        lessons.sort(Comparator.comparingInt(ListeningLesson::getOrderIndex)
                .thenComparingLong(ListeningLesson::getId));

        boolean changed = false;
        for (int i = 0; i < lessons.size(); i++) {
            ListeningLesson lesson = lessons.get(i);
            int expected = i + 1;

            if (!Integer.valueOf(expected).equals(lesson.getOrderIndex())) {
                lesson.setOrderIndex(expected);
                changed = true;
            }
        }

        if (changed) {
            lessonRepository.saveAll(lessons);
            log.info("Fixed order indexes for {} lessons in topic {}", lessons.size(), topicId);
        }
    }

    public Integer getNextOrderIndex(Long topicId) {
        Integer max = lessonRepository.findMaxOrderIndexByTopicId(topicId);
        return (max != null ? max : 0) + 1;
    }

    /**
     * CREATE LESSON WITH QUESTIONS AND TASK GROUPS
     * Note: Audio file upload is separate (can be null initially)
     */
    @Transactional
    public ListeningLessonDTO createLessonWithQuestionsAndTasks(
            ListeningLessonDTO dto,
            MultipartFile audioFile) throws IOException {

        // 1. Validate
        validateForCreate(dto);
        teacherPermissionService.checkTopicPermission(dto.getTopicId());

        if (lessonRepository.existsByTitleIgnoreCase(dto.getTitle())) {
            throw new IllegalArgumentException("Title already exists");
        }

        // 2. Get topic
        Topic topic = topicRepository.findById(dto.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        User currentUser = userService.getCurrentUser();

        // 3. Create lesson WITHOUT audio first
        ListeningLesson lesson = ListeningLesson.builder()
                .topic(topic)
                .title(dto.getTitle())
                .transcript(dto.getTranscript())
                .transcriptTranslation(dto.getTranscriptTranslation())
                .orderIndex(dto.getOrderIndex())
                .timeLimitSeconds(dto.getTimeLimitSeconds() != null ? dto.getTimeLimitSeconds() : 600)
                .pointsReward(dto.getPointsReward() != null ? dto.getPointsReward() : 25)
                .allowUnlimitedReplay(dto.getAllowUnlimitedReplay() != null ? dto.getAllowUnlimitedReplay() : true)
                .maxReplayCount(dto.getMaxReplayCount() != null ? dto.getMaxReplayCount() : 3)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .modifiedBy(currentUser)
                .build();

        lesson = lessonRepository.save(lesson);
        log.info("✅ Created listening lesson: id={}, title={}", lesson.getId(), lesson.getTitle());

        // 4. Upload audio if provided
        if (audioFile != null && !audioFile.isEmpty()) {
            String audioUrl = audioStorage.uploadAudio(audioFile, lesson.getId());
            lesson.setAudioUrl(audioUrl);
            lesson = lessonRepository.save(lesson);
            log.info("✅ Uploaded audio: {}", audioUrl);
        } else if (dto.getAudioUrl() != null) {
            // Use existing URL if provided (for AI-generated placeholder)
            lesson.setAudioUrl(dto.getAudioUrl());
            lesson = lessonRepository.save(lesson);
        }

        // 5. Create TaskGroups and Questions (same logic as above)
        if (dto.getTaskGroups() != null && !dto.getTaskGroups().isEmpty()) {
            for (ListeningLessonDTO.TaskGroupImportData taskGroupData : dto.getTaskGroups()) {
                CreateTaskGroupDTO taskGroupDTO = CreateTaskGroupDTO.builder()
                        .taskName(taskGroupData.getTaskName())
                        .instruction(taskGroupData.getInstruction())
                        .orderIndex(taskGroupData.getOrderIndex())
                        .build();

                TaskGroupResponseDTO taskGroup = taskGroupService.createTaskGroup(
                        ParentType.LISTENING,
                        lesson.getId(),
                        taskGroupDTO);

                if (taskGroupData.getQuestions() != null && !taskGroupData.getQuestions().isEmpty()) {
                    for (CreateQuestionDTO questionDTO : taskGroupData.getQuestions()) {
                        questionDTO.setTaskGroupId(taskGroup.getId());
                        questionDTO.setParentType(ParentType.LISTENING);
                        questionDTO.setParentId(lesson.getId());

                        if (questionDTO.getOrderIndex() == null || questionDTO.getOrderIndex() == 0) {
                            questionDTO.setOrderIndex(
                                    questionRepository.findMaxOrderIndexByLessonId(ParentType.LISTENING, lesson.getId())
                                            .orElse(0) + 1);
                        }

                        questionService.createQuestion(lesson.getId(), questionDTO);
                    }
                }
            }
        }

        // 6. Standalone questions
        if (dto.getStandaloneQuestions() != null && !dto.getStandaloneQuestions().isEmpty()) {
            for (CreateQuestionDTO questionDTO : dto.getStandaloneQuestions()) {
                questionDTO.setTaskGroupId(null);
                questionDTO.setParentType(ParentType.LISTENING);
                questionDTO.setParentId(lesson.getId());

                if (questionDTO.getOrderIndex() == null || questionDTO.getOrderIndex() == 0) {
                    questionDTO.setOrderIndex(
                            questionRepository.findMaxOrderIndexByLessonId(ParentType.LISTENING, lesson.getId())
                                    .orElse(0) + 1);
                }

                questionService.createQuestion(lesson.getId(), questionDTO);
            }
        }

        // 7. Return with questions
        ListeningLessonDTO result = toDTO(lesson);
        result.setGroupedQuestions(questionService.getGroupedQuestions(lesson.getId()));

        return result;
    }

    // ═════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════

    private ListeningLesson findLessonById(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + id));
    }

    private void validateForCreate(ListeningLessonDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (dto.getTranscript() == null || dto.getTranscript().isBlank()) {
            throw new IllegalArgumentException("Transcript is required");
        }
        if (dto.getOrderIndex() == null || dto.getOrderIndex() < 1) {
            throw new IllegalArgumentException("Order index must be >= 1");
        }
    }

    private void updateFields(ListeningLesson lesson, ListeningLessonDTO dto) {
        if (dto.getTitle() != null)
            lesson.setTitle(dto.getTitle());
        if (dto.getTranscript() != null)
            lesson.setTranscript(dto.getTranscript());
        if (dto.getTranscriptTranslation() != null)
            lesson.setTranscriptTranslation(dto.getTranscriptTranslation());
        if (dto.getOrderIndex() != null)
            lesson.setOrderIndex(dto.getOrderIndex());
        if (dto.getTimeLimitSeconds() != null)
            lesson.setTimeLimitSeconds(dto.getTimeLimitSeconds());
        if (dto.getPointsReward() != null)
            lesson.setPointsReward(dto.getPointsReward());
        if (dto.getAllowUnlimitedReplay() != null)
            lesson.setAllowUnlimitedReplay(dto.getAllowUnlimitedReplay());
        if (dto.getMaxReplayCount() != null)
            lesson.setMaxReplayCount(dto.getMaxReplayCount());
        if (dto.getIsActive() != null)
            lesson.setIsActive(dto.getIsActive());
    }

    private void reorderAfterDelete(Long topicId, Integer deletedPosition) {
        List<ListeningLesson> lessons = lessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId);

        lessons.stream()
                .filter(l -> l.getOrderIndex() > deletedPosition)
                .forEach(l -> l.setOrderIndex(l.getOrderIndex() - 1));

        lessonRepository.saveAll(lessons);
    }

    private ListeningLessonDTO toDTO(ListeningLesson lesson) {
        long questionCount = questionRepository.countByParentTypeAndParentId(
                ParentType.LISTENING, lesson.getId());

        return ListeningLessonDTO.builder()
                .id(lesson.getId())
                .topicId(lesson.getTopic().getId())
                .topicName(lesson.getTopic().getName())
                .title(lesson.getTitle())
                .audioUrl(lesson.getAudioUrl())
                .transcript(lesson.getTranscript())
                .transcriptTranslation(lesson.getTranscriptTranslation())
                .orderIndex(lesson.getOrderIndex())
                .timeLimitSeconds(lesson.getTimeLimitSeconds())
                .pointsReward(lesson.getPointsReward())
                .allowUnlimitedReplay(lesson.getAllowUnlimitedReplay())
                .maxReplayCount(lesson.getMaxReplayCount())
                .isActive(lesson.getIsActive())
                .createdAt(lesson.getCreatedAt())
                .questionCount((int) questionCount)
                .modifiedAt(lesson.getModifiedAt())
                .modifiedBy(lesson.getModifiedBy() != null ? lesson.getModifiedBy().getId() : null)
                .build();
    }
}