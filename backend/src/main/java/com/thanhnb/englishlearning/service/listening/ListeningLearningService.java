package com.thanhnb.englishlearning.service.listening;

import com.thanhnb.englishlearning.dto.common.SubmitResultDTO;
import com.thanhnb.englishlearning.dto.listening.ListeningLessonDTO;
import com.thanhnb.englishlearning.dto.listening.ListeningSubmitRequest;
import com.thanhnb.englishlearning.dto.listening.ListeningSubmitResponse;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson;
import com.thanhnb.englishlearning.entity.listening.UserListeningProgress;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.exception.ResourceNotFoundException;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning.repository.listening.UserListeningProgressRepository;
import com.thanhnb.englishlearning.service.common.BaseLearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ✅ REFACTORED: Listening Learning Service
 * - Đã bổ sung lại hàm getProgressSummary
 * - Kế thừa BaseLearningService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListeningLearningService extends BaseLearningService<ListeningLesson, UserListeningProgress> {

        private final ListeningLessonRepository lessonRepository;
        private final UserListeningProgressRepository progressRepository;

        // ═══════════════════════════════════════════════════════════
        // IMPLEMENT ABSTRACT METHODS
        // ═══════════════════════════════════════════════════════════

        @Override
        protected ParentType getParentType() {
                return ParentType.LISTENING;
        }

        @Override
        protected Integer getLessonOrder(ListeningLesson l) {
                return l.getOrderIndex();
        }

        @Override
        protected Long getLessonId(ListeningLesson l) {
                return l.getId();
        }

        @Override
        protected boolean isLessonActive(ListeningLesson l) {
                return l.getIsActive();
        }

        @Override
        protected int getPointsReward(ListeningLesson l) {
                return l.getPointsReward();
        }

        @Override
        protected Long getTopicId(ListeningLesson l) {
                return l.getTopic() != null ? l.getTopic().getId() : null;
        }

        @Override
        protected String getTopicName(ListeningLesson l) {
                return l.getTopic() != null ? l.getTopic().getName() : "Unknown";
        }

        @Override
        protected EnglishLevel getLessonRequiredLevel(ListeningLesson l) {
                return l.getTopic() != null ? l.getTopic().getLevelRequired() : null;
        }

        @Override
        protected UserListeningProgress createNewProgressInstance(Long userId, ListeningLesson lesson) {
                User user = userRepository.getReferenceById(userId);
                UserListeningProgress p = new UserListeningProgress();
                p.setUser(user);
                p.setLesson(lesson);
                p.setPlayCount(0);
                p.setHasViewedTranscript(false);
                lessonProgressService.initializeProgress(p);
                return p;
        }

        @Override
        protected Optional<UserListeningProgress> findProgress(Long userId, Long lessonId) {
                return progressRepository.findByUserIdAndLessonId(userId, lessonId);
        }

        @Override
        protected void saveProgress(UserListeningProgress p) {
                progressRepository.save(p);
        }

        // ═══════════════════════════════════════════════════════════
        // PUBLIC API - SUBMIT LESSON
        // ═══════════════════════════════════════════════════════════

        public ListeningSubmitResponse submitLesson(Long userId, Long lessonId, ListeningSubmitRequest request) {
                log.info("Submitting listening lesson: userId={}, lessonId={}", userId, lessonId);

                ListeningLesson lesson = lessonRepository.findById(lessonId)
                                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                // Validate
                List<ListeningLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();
                validateLessonAccess(lesson, allLessons, userId,
                                progressRepository::existsByUserIdAndLessonIdAndIsCompletedTrue,
                                user.getEnglishLevel());

                // 🔥 GỌI HÀM CHUNG BASE
                SubmitResultDTO result = processSubmission(
                                userId,
                                lesson,
                                allLessons,
                                request.getAnswers(),
                                ModuleType.LISTENING);

                return ListeningSubmitResponse.of(
                                lesson.getId(), lesson.getTitle(),
                                result.totalQuestions(), result.correctCount(), result.totalScore(),
                                result.scorePercentage(),
                                result.isPassed(), result.pointsEarned(), result.hasUnlockedNext(),
                                result.nextLessonId(),
                                result.results(), result.levelUpgradeResult());
        }

        // ═══════════════════════════════════════════════════════════
        // PUBLIC API - GET DATA
        // ═══════════════════════════════════════════════════════════

        public List<ListeningLessonDTO> getAllLessonsForUser(Long userId, Long topicId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                EnglishLevel userLevel = user.getEnglishLevel();

                List<ListeningLesson> lessons = (topicId != null)
                                ? lessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId)
                                : lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();

                List<UserListeningProgress> progressList = progressRepository.findByUserId(userId);
                Set<Long> completedIds = progressList.stream()
                                .filter(UserListeningProgress::getIsCompleted)
                                .map(p -> p.getLesson().getId())
                                .collect(Collectors.toSet());

                return lessons.stream()
                                .filter(ListeningLesson::getIsActive)
                                .map(lesson -> {
                                        boolean isUnlocked = isLessonUnlocked(
                                                        lesson, lessons, userId,
                                                        this::getLessonOrder, this::getLessonId,
                                                        (u, lId) -> completedIds.contains(lId),
                                                        userLevel);

                                        Optional<UserListeningProgress> progressOpt = progressList.stream()
                                                        .filter(p -> p.getLesson().getId().equals(lesson.getId()))
                                                        .findFirst();

                                        ListeningLessonDTO dto = ListeningLessonDTO.builder()
                                                        .id(lesson.getId()).title(lesson.getTitle())
                                                        .topicId(lesson.getTopic().getId())
                                                        .topicName(lesson.getTopic().getName())
                                                        .orderIndex(lesson.getOrderIndex())
                                                        .pointsReward(lesson.getPointsReward())
                                                        .requiredLevel(getLessonRequiredLevel(lesson))
                                                        .isUnlocked(isUnlocked).isActive(lesson.getIsActive())
                                                        .build();

                                        progressOpt.ifPresent(p -> dto.withProgress(
                                                        p.getIsCompleted(), p.getScorePercentage(), p.getAttempts(),
                                                        p.getPlayCount(), p.getHasViewedTranscript(),
                                                        p.getCompletedAt()));
                                        return dto;
                                }).toList();
        }

        public ListeningLessonDTO getLessonDetail(Long lessonId, Long userId) {
                ListeningLesson lesson = lessonRepository.findById(lessonId)
                                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                List<ListeningLesson> allLessons = lessonRepository.findAllByIsActiveTrueOrderByOrderIndexAsc();
                validateLessonAccess(lesson, allLessons, userId,
                                progressRepository::existsByUserIdAndLessonIdAndIsCompletedTrue,
                                user.getEnglishLevel());

                ListeningLessonDTO dto = ListeningLessonDTO.builder()
                                .id(lesson.getId()).title(lesson.getTitle()).audioUrl(lesson.getAudioUrl())
                                .transcript(lesson.getTranscript())
                                .transcriptTranslation(lesson.getTranscriptTranslation())
                                .topicId(lesson.getTopic().getId()).topicName(lesson.getTopic().getName())
                                .timeLimitSeconds(lesson.getTimeLimitSeconds()).pointsReward(lesson.getPointsReward())
                                .allowUnlimitedReplay(lesson.getAllowUnlimitedReplay())
                                .maxReplayCount(lesson.getMaxReplayCount())
                                .requiredLevel(getLessonRequiredLevel(lesson)).build();

                dto.withGroupedQuestions(getGroupedQuestionsForLesson(lessonId));

                UserListeningProgress progress = findProgress(userId, lessonId).orElse(null);
                if (progress != null) {
                        dto.withProgress(
                                        progress.getIsCompleted(), progress.getScorePercentage(),
                                        progress.getAttempts(),
                                        progress.getPlayCount(), progress.getHasViewedTranscript(),
                                        progress.getCompletedAt());
                        boolean transcriptUnlocked = Boolean.TRUE.equals(progress.getHasViewedTranscript());
                        dto.withTranscriptInfo(transcriptUnlocked, calculateRemainingReplays(lesson, progress));
                        if (!transcriptUnlocked) {
                                dto.setTranscript(null);
                                dto.setTranscriptTranslation(null);
                        }
                } else {
                        dto.withTranscriptInfo(false, calculateRemainingReplays(lesson, null));
                        dto.setTranscript(null);
                        dto.setTranscriptTranslation(null);
                }
                return dto;
        }

        // ═══════════════════════════════════════════════════════════
        // PUBLIC API - LISTENING SPECIFIC FEATURES
        // ═══════════════════════════════════════════════════════════

        public PlayCountResponse trackPlayCount(Long userId, Long lessonId) {
                boolean canPlay = canPlayLesson(userId, lessonId);

                if (!canPlay) {
                        UserListeningProgress progress = findProgress(userId, lessonId).orElse(null);
                        return PlayCountResponse.builder()
                                        .currentPlayCount(progress != null ? progress.getPlayCount() : 0)
                                        .canUnlockTranscript(progress != null && canUnlockTranscript(userId, lessonId))
                                        .message("Hết lượt nghe. Hãy làm bài tập.")
                                        .build();
                }

                UserListeningProgress progress = findProgress(userId, lessonId)
                                .orElseGet(() -> createNewProgressInstance(userId,
                                                lessonRepository.findById(lessonId).orElseThrow()));

                progress.setPlayCount((progress.getPlayCount() == null ? 0 : progress.getPlayCount()) + 1);
                saveProgress(progress);

                return PlayCountResponse.builder()
                                .currentPlayCount(progress.getPlayCount())
                                .canUnlockTranscript(canUnlockTranscript(userId, lessonId))
                                .message("Đã ghi nhận lượt nghe")
                                .build();
        }

        public String unlockTranscriptForUser(Long userId, Long lessonId) {
                if (!canUnlockTranscript(userId, lessonId)) {
                        return "Bạn cần nghe thêm hoặc hoàn thành bài học để mở Transcript.";
                }

                UserListeningProgress progress = findProgress(userId, lessonId)
                                .orElseGet(() -> createNewProgressInstance(userId,
                                                lessonRepository.findById(lessonId).orElseThrow()));

                if (!Boolean.TRUE.equals(progress.getHasViewedTranscript())) {
                        progress.setHasViewedTranscript(true);
                        saveProgress(progress);
                        return "Đã mở khóa transcript";
                }
                return "Transcript đã được mở trước đó";
        }

        public boolean canPlayLesson(Long userId, Long lessonId) {
                ListeningLesson lesson = lessonRepository.findById(lessonId)
                                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
                if (Boolean.TRUE.equals(lesson.getAllowUnlimitedReplay()))
                        return true;

                Optional<UserListeningProgress> pOpt = findProgress(userId, lessonId);
                if (pOpt.isEmpty())
                        return true;

                int current = pOpt.get().getPlayCount() == null ? 0 : pOpt.get().getPlayCount();
                int max = lesson.getMaxReplayCount() == null ? 3 : lesson.getMaxReplayCount();
                return current < max;
        }

        public boolean canUnlockTranscript(Long userId, Long lessonId) {
                Optional<UserListeningProgress> pOpt = findProgress(userId, lessonId);
                if (pOpt.isEmpty())
                        return false;

                UserListeningProgress p = pOpt.get();
                if (Boolean.TRUE.equals(p.getHasViewedTranscript()))
                        return true;

                int playCount = p.getPlayCount() == null ? 0 : p.getPlayCount();
                return playCount >= 2 || Boolean.TRUE.equals(p.getIsCompleted());
        }

        // ✅ Đã thêm lại hàm này
        public ListeningProgressSummary getProgressSummary(Long userId) {
                List<UserListeningProgress> completedLessons = getCompletedLessons(userId);

                int totalCompleted = completedLessons.size();

                double avgScore = completedLessons.stream()
                                .filter(p -> p.getScorePercentage() != null && p.getScorePercentage() > 0)
                                .mapToDouble(UserListeningProgress::getScorePercentage)
                                .average()
                                .orElse(0.0);

                int totalAttempts = completedLessons.stream()
                                .filter(p -> p.getAttempts() != null)
                                .mapToInt(UserListeningProgress::getAttempts)
                                .sum();

                List<RecentCompletion> recentCompletions = completedLessons.stream()
                                .filter(p -> p.getCompletedAt() != null)
                                .sorted((p1, p2) -> p2.getCompletedAt().compareTo(p1.getCompletedAt()))
                                .limit(5)
                                .map(p -> new RecentCompletion(
                                                p.getLesson().getId(),
                                                p.getLesson().getTitle(),
                                                p.getScorePercentage() != null ? p.getScorePercentage() : 0.0,
                                                p.getCompletedAt()))
                                .toList();

                return ListeningProgressSummary.builder()
                                .userId(userId)
                                .totalCompleted(totalCompleted)
                                .averageScore(Math.round(avgScore * 100.0) / 100.0)
                                .totalAttempts(totalAttempts)
                                .recentCompletions(recentCompletions)
                                .build();
        }

        public List<UserListeningProgress> getCompletedLessons(Long userId) {
                return progressRepository.findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(userId);
        }

        // ═══════════════════════════════════════════════════════════
        // HELPERS & DTOs
        // ═══════════════════════════════════════════════════════════

        private Integer calculateRemainingReplays(ListeningLesson lesson, UserListeningProgress progress) {
                if (Boolean.TRUE.equals(lesson.getAllowUnlimitedReplay()))
                        return null;
                int max = lesson.getMaxReplayCount() != null ? lesson.getMaxReplayCount() : 3;
                int current = (progress != null && progress.getPlayCount() != null) ? progress.getPlayCount() : 0;
                return Math.max(0, max - current);
        }

        @lombok.Data
        @lombok.Builder
        public static class ListeningProgressSummary {
                private Long userId;
                private int totalCompleted;
                private double averageScore;
                private int totalAttempts;
                private List<RecentCompletion> recentCompletions;
        }

        public record RecentCompletion(Long lessonId, String lessonTitle, double score, LocalDateTime completedAt) {
        }

        @lombok.Data
        @lombok.Builder
        public static class PlayCountResponse {
                private Integer currentPlayCount;
                private Boolean canUnlockTranscript;
                private String message;
        }
}