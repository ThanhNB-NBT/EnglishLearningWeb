package com.thanhnb.englishlearning.service.common;

import com.thanhnb.englishlearning.dto.common.SubmitResultDTO;
import com.thanhnb.englishlearning.dto.question.helper.QuestionResultDTO;
import com.thanhnb.englishlearning.dto.question.request.SubmitAnswerRequest;
import com.thanhnb.englishlearning.dto.question.response.TaskGroupedQuestionsDTO;
import com.thanhnb.englishlearning.entity.question.Question;
import com.thanhnb.englishlearning.entity.question.TaskGroup;
import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.event.LessonCompletedEvent;
import com.thanhnb.englishlearning.repository.question.QuestionRepository;
import com.thanhnb.englishlearning.repository.question.TaskGroupRepository;
import com.thanhnb.englishlearning.repository.user.UserRepository;
import com.thanhnb.englishlearning.service.common.LessonProgressService.ProgressUpdateResult;
import com.thanhnb.englishlearning.service.level.LevelUpgradeService;
import com.thanhnb.englishlearning.service.question.AnswerValidationService;
import com.thanhnb.englishlearning.service.question.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseLearningService<TLesson, TProgress extends LessonProgressService.LessonProgress> {

        @Autowired
        protected QuestionService questionService;
        @Autowired
        protected LessonProgressService lessonProgressService;
        @Autowired
        protected UserRepository userRepository;
        @Autowired
        protected AnswerValidationService answerValidationService;
        @Autowired
        protected ApplicationEventPublisher eventPublisher;
        @Autowired
        protected LevelUpgradeService levelUpgradeService;
        @Autowired
        protected TaskGroupRepository taskGroupRepository;
        @Autowired
        protected QuestionRepository questionRepository;

        // --- Abstract Methods ---
        protected abstract ParentType getParentType();

        protected abstract Integer getLessonOrder(TLesson lesson);

        protected abstract Long getLessonId(TLesson lesson);

        protected abstract boolean isLessonActive(TLesson lesson);

        protected abstract int getPointsReward(TLesson lesson);

        protected abstract TProgress createNewProgressInstance(Long userId, TLesson lesson);

        protected abstract Optional<TProgress> findProgress(Long userId, Long lessonId);

        protected abstract void saveProgress(TProgress progress);

        protected abstract Long getTopicId(TLesson lesson);

        protected abstract String getTopicName(TLesson lesson);

        protected abstract EnglishLevel getLessonRequiredLevel(TLesson lesson);

        /**
         * 🔥 CORE LOGIC: Xử lý nộp bài chung cho mọi module
         */
        @Transactional
        public SubmitResultDTO processSubmission(
                        Long userId,
                        TLesson lesson,
                        List<TLesson> allLessons,
                        List<SubmitAnswerRequest> answers,
                        ModuleType moduleType) {

                Long lessonId = getLessonId(lesson);
                log.info("Processing submission: userId={}, lessonId={}, module={}", userId, lessonId, moduleType);

                // 1. Lấy và chấm điểm câu hỏi
                List<Question> questions = loadQuestionsForGrading(lessonId);
                List<QuestionResultDTO> results = new ArrayList<>();
                List<LessonCompletedEvent.QuestionTrackingInfo> trackingInfos = new ArrayList<>();

                int correctCount = 0;
                int totalScore = 0;
                int totalQuestions = questions.size();

                Map<Long, SubmitAnswerRequest> answerMap = answers.stream()
                                .collect(Collectors.toMap(SubmitAnswerRequest::getQuestionId, a -> a, (a1, a2) -> a1));

                for (Question q : questions) {
                        SubmitAnswerRequest ans = answerMap.get(q.getId());
                        QuestionResultDTO result = answerValidationService.validateAnswer(
                                        q,
                                        ans != null ? ans.getSelectedOptions() : null,
                                        ans != null ? ans.getTextAnswer() : null);

                        results.add(result);
                        boolean isCorrect = Boolean.TRUE.equals(result.getIsCorrect());

                        if (isCorrect) {
                                correctCount++;
                                totalScore += result.getPoints(); // Dùng điểm thực tế của câu hỏi
                        }

                        trackingInfos.add(
                                        new LessonCompletedEvent.QuestionTrackingInfo(q.getQuestionType(), isCorrect));
                }

                // 2. Tính toán kết quả
                int maxPossibleScore = questions.stream().mapToInt(Question::getPoints).sum();
                double scorePercentage = maxPossibleScore > 0 ? ((double) totalScore / maxPossibleScore) * 100.0 : 0.0;
                boolean isPassed = scorePercentage >= 80.0; // Config ngưỡng pass (ví dụ 80%)

                // 3. Lưu tiến độ (Progress)
                TProgress progress = findProgress(userId, lessonId)
                                .orElseGet(() -> createNewProgressInstance(userId, lesson));

                User user = userRepository.getReferenceById(userId);
                ProgressUpdateResult updateResult = lessonProgressService.updateProgress(
                                progress, user, scorePercentage, isPassed, getPointsReward(lesson), getParentType());

                saveProgress(progress);

                // 4. Tìm bài tiếp theo & Check mở khóa
                Long nextLessonId = null;
                boolean hasUnlockedNext = false;

                TLesson nextLesson = findNextLesson(lesson, allLessons);
                if (nextLesson != null) {
                        nextLessonId = getLessonId(nextLesson);
                        // Nếu đây là lần đầu hoàn thành và đậu -> Đánh dấu là mới mở khóa
                        if (updateResult.isFirstCompletion() && isPassed) {
                                hasUnlockedNext = true;
                        }
                }

                // 5. Check nâng cấp trình độ (Chỉ khi hoàn thành lần đầu)
                LevelUpgradeService.LevelUpgradeResult levelResult = null;
                if (updateResult.isFirstCompletion() && isPassed) {
                        try {
                                levelResult = levelUpgradeService.checkAndUpgradeLevel(userId, moduleType,
                                                getTopicId(lesson));
                        } catch (Exception e) {
                                log.error("Level upgrade check failed", e);
                        }
                }

                // 6. Gửi sự kiện Tracking (Async)
                publishTrackingEvent(userId, lesson, moduleType, trackingInfos);

                return SubmitResultDTO.builder()
                                .isPassed(isPassed)
                                .scorePercentage(scorePercentage)
                                .totalScore(totalScore)
                                .correctCount(correctCount)
                                .totalQuestions(totalQuestions)
                                .pointsEarned(updateResult.getPointsEarned())
                                .results(results)
                                .hasUnlockedNext(hasUnlockedNext)
                                .nextLessonId(nextLessonId) // ✅ Luôn trả về ID nếu có
                                .levelUpgradeResult(levelResult)
                                .build();
        }

        // --- Helpers ---

        private List<Question> loadQuestionsForGrading(Long lessonId) {
                List<Question> allQuestions = new ArrayList<>();
                // Lấy câu hỏi lẻ
                allQuestions.addAll(questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(getParentType(),
                                lessonId));
                // Lấy câu hỏi trong nhóm
                List<TaskGroup> groups = taskGroupRepository
                                .findByParentTypeAndParentIdOrderByOrderIndexAsc(getParentType(), lessonId);
                for (TaskGroup g : groups) {
                        allQuestions.addAll(questionRepository.findByTaskGroupIdOrderByOrderIndexAsc(g.getId()));
                }
                return allQuestions;
        }

        protected TLesson findNextLesson(TLesson currentLesson, List<TLesson> allLessons) {
                if (allLessons == null || allLessons.isEmpty())
                        return null;

                // Sắp xếp lại cho chắc chắn
                List<TLesson> sorted = allLessons.stream()
                                .sorted(Comparator.comparing(this::getLessonOrder))
                                .toList();

                Long currentId = getLessonId(currentLesson);
                for (int i = 0; i < sorted.size() - 1; i++) {
                        if (getLessonId(sorted.get(i)).equals(currentId)) {
                                TLesson next = sorted.get(i + 1);
                                if (isLessonActive(next))
                                        return next; // Chỉ trả về nếu bài tiếp theo Active
                        }
                }
                return null;
        }

        private void publishTrackingEvent(Long userId, TLesson lesson, ModuleType module,
                        List<LessonCompletedEvent.QuestionTrackingInfo> infos) {
                try {
                        eventPublisher.publishEvent(new LessonCompletedEvent(
                                        this, userId, module, getTopicId(lesson), getTopicName(lesson), infos));
                } catch (Exception e) {
                        log.error("Failed to publish tracking event", e);
                }
        }

        // Logic Unlock & Access Check (Giữ nguyên hoặc tinh chỉnh nhẹ)
        protected boolean isLessonUnlocked(TLesson lesson, List<TLesson> allLessons, Long userId,
                        Function<TLesson, Integer> orderGetter, Function<TLesson, Long> idGetter,
                        BiPredicate<Long, Long> progressChecker, EnglishLevel userLevel) {
                if (!isLessonActive(lesson))
                        return false;

                // Check Level
                EnglishLevel required = getLessonRequiredLevel(lesson);
                if (required != null && userLevel != null && userLevel.ordinal() < required.ordinal())
                        return false;

                Integer order = orderGetter.apply(lesson);
                if (order == 1)
                        return true;

                // Tìm bài trước
                return allLessons.stream()
                                .filter(this::isLessonActive)
                                .filter(l -> orderGetter.apply(l) == order - 1)
                                .findFirst()
                                .map(prev -> progressChecker.test(userId, idGetter.apply(prev)))
                                .orElse(true); // Fallback: nếu không tìm thấy bài trước thì mở
        }

        protected void validateLessonAccess(TLesson lesson, List<TLesson> allLessons, Long userId,
                        BiPredicate<Long, Long> progressChecker, EnglishLevel userLevel) {
                if (!isLessonActive(lesson))
                        throw new IllegalStateException("Bài học không khả dụng");
                if (!isLessonUnlocked(lesson, allLessons, userId, this::getLessonOrder, this::getLessonId,
                                progressChecker, userLevel)) {
                        throw new IllegalStateException("Bạn chưa đủ điều kiện mở bài học này");
                }
        }

        // Abstract method hỗ trợ lấy Grouped Questions cho Frontend
        protected TaskGroupedQuestionsDTO getGroupedQuestionsForLesson(Long lessonId) {
                List<Question> questions = questionService.loadQuestionsByParent(getParentType(), lessonId);
                if (questions.isEmpty())
                        return TaskGroupedQuestionsDTO.builder().hasTaskStructure(false).standaloneQuestions(List.of())
                                        .build();

                Map<Long, List<Question>> groupedMap = new LinkedHashMap<>();
                List<Question> standalone = new ArrayList<>();

                for (Question q : questions) {
                        if (q.getTaskGroup() != null)
                                groupedMap.computeIfAbsent(q.getTaskGroup().getId(), k -> new ArrayList<>()).add(q);
                        else
                                standalone.add(q);
                }

                List<TaskGroupedQuestionsDTO.TaskGroup> tasks = groupedMap.entrySet().stream()
                                .map(entry -> {
                                        TaskGroup tg = entry.getValue().get(0).getTaskGroup();
                                        return TaskGroupedQuestionsDTO.TaskGroup.builder()
                                                        .taskGroupId(tg.getId())
                                                        .taskName(tg.getTaskName())
                                                        .taskInstruction(tg.getInstruction())
                                                        .taskOrder(tg.getOrderIndex())
                                                        .questions(questionService
                                                                        .convertToDTOsForLearning(entry.getValue())) // Shuffle
                                                                                                                     // options
                                                        .build();
                                })
                                .sorted(Comparator.comparing(TaskGroupedQuestionsDTO.TaskGroup::getTaskOrder))
                                .toList();

                return TaskGroupedQuestionsDTO.builder()
                                .hasTaskStructure(!tasks.isEmpty())
                                .tasks(tasks)
                                .standaloneQuestions(questionService.convertToDTOsForLearning(standalone))
                                .build();
        }
}