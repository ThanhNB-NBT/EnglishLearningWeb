package com.thanhnb.englishlearning.service.listening;

import com.thanhnb.englishlearning.dto.listening.request.SubmitListeningRequest;
import com.thanhnb.englishlearning.dto.listening.response.ListeningLessonDetailResponse;
import com.thanhnb.englishlearning.dto.listening.response.ListeningLessonListResponse;
import com.thanhnb.englishlearning.dto.listening.response.ListeningSubmitResponse;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.entity.listening.UserListeningProgress;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning.repository.listening.UserListeningProgressRepository;
import com.thanhnb.englishlearning.repository.UserRepository;
import com.thanhnb.englishlearning.service.common.BaseLearningService;
import com.thanhnb.englishlearning.service.common.LessonProgressService;
import com.thanhnb.englishlearning.service.question.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Listening service extends BaseLearningService
 * - Tích hợp LessonProgressService để xử lý progress và unlock
 * - Nhất quán với Reading/Grammar modules
 * - Xử lý replay count và transcript unlock
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListeningService extends BaseLearningService<ListeningLesson, UserListeningProgress> {

    private final ListeningLessonRepository lessonRepository;
    private final UserListeningProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final QuestionService questionService;
    private final LessonProgressService lessonProgressService;

    private static final double PASS_THRESHOLD = 80.0;

    @Override
    protected ParentType getParentType() {
        return ParentType.LISTENING;
    }

    // ═════════════════════════════════════════════════════════════════
    // USER OPERATIONS - LESSON LIST
    // ═════════════════════════════════════════════════════════════════

    /**
     * [USER] Lấy tất cả bài nghe với progress và unlock status
     * Không phân trang, load tất cả active lessons
     */
    public List<ListeningLessonListResponse> getAllLessonsForUser(Long userId) {
        log.info("Loading listening lessons for user {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Người dùng không tồn tại với id: " + userId);
        }

        List<ListeningLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();
        List<UserListeningProgress> allProgress = progressRepository.findByUserId(userId);

        List<ListeningLessonListResponse> result = allLessons.stream()
                .map(lesson -> buildUserLessonListResponse(lesson, allLessons, allProgress, userId))
                .collect(Collectors.toList());

        log.info("Loaded {} listening lessons for user {}", result.size(), userId);
        return result;
    }

    // ═════════════════════════════════════════════════════════════════
    // USER OPERATIONS - LESSON DETAIL
    // ═════════════════════════════════════════════════════════════════

    /**
     * [USER] Lấy chi tiết lesson với questions và check unlock
     */
    public ListeningLessonDetailResponse getLessonDetail(Long lessonId, Long userId) {
        log.info("Loading lesson detail:  lessonId={}, userId={}", lessonId, userId);

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Người dùng không tồn tại với id:  " + userId);
        }

        ListeningLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id: " + lessonId));

        if (!lesson.getIsActive()) {
            throw new RuntimeException("Bài nghe này hiện không khả dụng");
        }

        List<ListeningLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();
        boolean isUnlocked = checkUnlockStatus(lesson, allLessons, userId);

        if (!isUnlocked) {
            throw new RuntimeException("Bạn cần hoàn thành bài nghe trước đó để mở khóa bài này");
        }

        // Get or create progress
        UserListeningProgress progress = progressRepository
                .findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElseThrow();
                    UserListeningProgress newProgress = createNewProgress(user, lesson);
                    lessonProgressService.initializeProgress(newProgress);
                    progressRepository.save(newProgress);
                    return newProgress;
                });

        // Load questions
        List<QuestionResponseDTO> questionDTOs = questionService.convertToDTOs(
                questionService.loadQuestionsByParent(getParentType(), lessonId),
                false);

        // Calculate remaining replays
        Integer remainingReplays = lesson.getAllowUnlimitedReplay()
                ? null
                : lesson.getRemainingReplays(progress.getPlayCount() != null ? progress.getPlayCount() : 0);

        // Check if transcript is unlocked (after completion or if already viewed)
        boolean transcriptUnlocked = progress.getIsCompleted() ||
                (progress.getHasViewedTranscript() != null && progress.getHasViewedTranscript());

        // Check navigation
        boolean hasNext = hasNextLesson(lesson, allLessons);
        boolean hasPrevious = hasPreviousLesson(lesson, allLessons);

        ListeningLessonDetailResponse response = ListeningLessonDetailResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .audioUrl(lesson.getAudioUrl())
                .difficulty(lesson.getDifficulty())
                .timeLimitSeconds(lesson.getTimeLimitSeconds())
                .pointsReward(lesson.getPointsReward())
                .allowUnlimitedReplay(lesson.getAllowUnlimitedReplay())
                .maxReplayCount(lesson.getMaxReplayCount())
                .remainingReplays(remainingReplays)
                .transcript(transcriptUnlocked ? lesson.getTranscript() : null)
                .transcriptTranslation(transcriptUnlocked ? lesson.getTranscriptTranslation() : null)
                .transcriptUnlocked(transcriptUnlocked)
                .isCompleted(progress.getIsCompleted())
                .scorePercentage(progress.getScorePercentage())
                .attempts(progress.getAttempts())
                .playCount(progress.getPlayCount())
                .hasViewedTranscript(progress.getHasViewedTranscript())
                .questions(questionDTOs)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();

        log.info("Loaded lesson with {} questions, unlocked={}, transcriptUnlocked={}",
                questionDTOs.size(), isUnlocked, transcriptUnlocked);

        return response;
    }

    // ═════════════════════════════════════════════════════════════════
    // USER OPERATIONS - SUBMIT
    // ═════════════════════════════════════════════════════════════════

    /**
     * [USER] Submit lesson với LessonProgressService
     */
    @Transactional
    public ListeningSubmitResponse submitLesson(Long userId, Long lessonId, SubmitListeningRequest request) {
        log.info("Submit listening lesson: userId={}, lessonId={}", userId, lessonId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại với id: " + userId));

        ListeningLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại với id: " + lessonId));

        if (!lesson.getIsActive()) {
            throw new RuntimeException("Bài nghe này hiện không khả dụng");
        }

        List<ListeningLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();

        if (!checkUnlockStatus(lesson, allLessons, userId)) {
            throw new RuntimeException("Bạn cần hoàn thành bài nghe trước đó để làm bài này");
        }

        long expectedQuestions = questionService.countQuestionsByParent(getParentType(), lessonId);
        if (request.getAnswers().size() < expectedQuestions) {
            log.warn("User {} submitted lesson {} incomplete:  {}/{} answers",
                    userId, lessonId, request.getAnswers().size(), expectedQuestions);
        }

        UserListeningProgress progress = progressRepository
                .findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    UserListeningProgress newProgress = createNewProgress(user, lesson);
                    lessonProgressService.initializeProgress(newProgress);
                    return newProgress;
                });

        // Process answers - SỬ DỤNG processAnswers từ QuestionService
        List<QuestionResultDTO> results = questionService.processAnswers(request.getAnswers(), getParentType());

        int totalQuestions = results.size();
        int correctCount = questionService.calculateCorrectCount(results);
        double scorePercentage = questionService.calculateScorePercentage(correctCount, totalQuestions);

        boolean isPassed = isPassed(scorePercentage, PASS_THRESHOLD);

        // Update progress
        LessonProgressService.ProgressUpdateResult progressResult = lessonProgressService.updateProgress(
                progress,
                user,
                scorePercentage,
                isPassed,
                lesson.getPointsReward());

        progressRepository.save(progress);

        // Check if next lesson is unlocked
        Long nextLessonId = null;
        boolean hasUnlockedNext = false;

        if (progressResult.isFirstCompletion() && progressResult.isPassed()) {
            ListeningLesson nextLesson = lessonProgressService.findNextLesson(
                    lesson,
                    allLessons,
                    ListeningLesson::getOrderIndex);

            if (nextLesson != null) {
                nextLessonId = nextLesson.getId();
                hasUnlockedNext = true;
                log.info("Unlocked next lesson: {}", nextLessonId);
            }
        }

        log.info("Submit result: correct={}/{}, scorePercentage={}%, passed={}",
                correctCount, totalQuestions, scorePercentage, isPassed);

        // Calculate total score from results
        int totalScore = results.stream()
                .filter(QuestionResultDTO::getIsCorrect)
                .mapToInt(QuestionResultDTO::getPoints)
                .sum();

        return ListeningSubmitResponse.builder()
                .lessonId(lessonId)
                .isPassed(isPassed)
                .scorePercentage(scorePercentage)
                .totalScore(totalScore) 
                .correctCount(correctCount) 
                .totalQuestions(totalQuestions) 
                .pointsEarned(progressResult.getPointsEarned()) 
                .results(results)
                .hasNextLesson(hasUnlockedNext)
                .nextLessonId(nextLessonId)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════
    // USER OPERATIONS - REPLAY & TRANSCRIPT
    // ═════════════════════════════════════════════════════════════════

    /**
     * [USER] Increment play count when user plays audio
     */
    @Transactional
    public void incrementPlayCount(Long userId, Long lessonId) {
        log.info("Incrementing play count:  userId={}, lessonId={}", userId, lessonId);

        ListeningLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài nghe không tồn tại"));

        UserListeningProgress progress = progressRepository
                .findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElseThrow();
                    UserListeningProgress newProgress = createNewProgress(user, lesson);
                    lessonProgressService.initializeProgress(newProgress);
                    return newProgress;
                });

        int currentPlayCount = progress.getPlayCount() != null ? progress.getPlayCount() : 0;

        // Check if can replay
        if (!lesson.canReplay(currentPlayCount)) {
            throw new RuntimeException("Bạn đã hết lượt nghe lại");
        }

        progress.setPlayCount(currentPlayCount + 1);
        progressRepository.save(progress);

        log.info("Play count incremented to {} for user {} on lesson {}",
                progress.getPlayCount(), userId, lessonId);
    }

    /**
     * [USER] Unlock transcript (mark as viewed)
     */
    @Transactional
    public void unlockTranscript(Long userId, Long lessonId) {
        log.info("Unlocking transcript: userId={}, lessonId={}", userId, lessonId);

        UserListeningProgress progress = progressRepository
                .findByUserIdAndLessonId(userId, lessonId)
                .orElseThrow(() -> new RuntimeException("Progress không tồn tại"));

        // Only allow if completed
        if (!progress.getIsCompleted()) {
            throw new RuntimeException("Bạn cần hoàn thành bài nghe trước khi xem transcript");
        }

        progress.setHasViewedTranscript(true);
        progressRepository.save(progress);

        log.info("Transcript unlocked for user {} on lesson {}", userId, lessonId);
    }

    // ═════════════════════════════════════════════════════════════════
    // USER OPERATIONS - HISTORY
    // ═════════════════════════════════════════════════════════════════

    /**
     * [USER] Lấy danh sách bài đã hoàn thành
     */
    public List<UserListeningProgress> getCompletedLessons(Long userId) {
        log.info("Loading completed listening lessons for user {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Người dùng không tồn tại với id:  " + userId);
        }

        return progressRepository.findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(userId);
    }

    // ═════════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Build user lesson list response với progress và unlock status
     */
    private ListeningLessonListResponse buildUserLessonListResponse(
            ListeningLesson lesson,
            List<ListeningLesson> allLessons,
            List<UserListeningProgress> allProgress,
            Long userId) {

        Optional<UserListeningProgress> progress = allProgress.stream()
                .filter(p -> p.getLesson().getId().equals(lesson.getId()))
                .findFirst();

        boolean isUnlocked = checkUnlockStatus(lesson, allLessons, userId);

        long questionCount = questionService.countQuestionsByParent(getParentType(), lesson.getId());

        return ListeningLessonListResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .difficulty(lesson.getDifficulty())
                .orderIndex(lesson.getOrderIndex())
                .pointsReward(lesson.getPointsReward())
                .isActive(lesson.getIsActive())
                .isCompleted(progress.map(UserListeningProgress::getIsCompleted).orElse(false))
                .scorePercentage(progress.map(UserListeningProgress::getScorePercentage).orElse(null))
                .attempts(progress.map(UserListeningProgress::getAttempts).orElse(0))
                .isUnlocked(isUnlocked)
                .questionCount(questionCount)
                .build();
    }

    /**
     * Check unlock status
     */
    private boolean checkUnlockStatus(ListeningLesson lesson, List<ListeningLesson> allLessons, Long userId) {
        return lessonProgressService.isLessonUnlocked(
                lesson,
                allLessons,
                userId,
                ListeningLesson::getOrderIndex,
                ListeningLesson::getId,
                progressRepository::existsByUserIdAndLessonIdAndIsCompletedTrue);
    }

    /**
     * Tạo progress mới
     */
    private UserListeningProgress createNewProgress(User user, ListeningLesson lesson) {
        log.info("Creating new listening progress for user {} and lesson {}", user.getId(), lesson.getId());

        UserListeningProgress progress = new UserListeningProgress();
        progress.setUser(user);
        progress.setLesson(lesson);
        progress.setCreatedAt(LocalDateTime.now());

        return progress;
    }

    /**
     * Check if has next lesson
     */
    private boolean hasNextLesson(ListeningLesson currentLesson, List<ListeningLesson> allLessons) {
        return allLessons.stream()
                .anyMatch(l -> l.getOrderIndex() > currentLesson.getOrderIndex());
    }

    /**
     * Check if has previous lesson
     */
    private boolean hasPreviousLesson(ListeningLesson currentLesson, List<ListeningLesson> allLessons) {
        return allLessons.stream()
                .anyMatch(l -> l.getOrderIndex() < currentLesson.getOrderIndex());
    }
}