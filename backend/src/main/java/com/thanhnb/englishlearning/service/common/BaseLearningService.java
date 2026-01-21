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

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * BASE LEARNING SERVICE - Template Pattern cho tất cả Module Learning
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * MỤC ĐÍCH:
 * - Tránh duplicate code giữa Grammar, Reading, Listening, Vocabulary
 * - Cung cấp core logic chung: Submit, Unlock, Progress, Level Up
 * - Các module con chỉ cần implement abstract methods
 * 
 * PATTERN: Template Method Pattern
 * - Base class định nghĩa SKELETON của algorithm
 * - Child classes (GrammarLearningService, ReadingLearningService...)
 *   implement CHI TIẾT cụ thể
 * 
 * LUỒNG CHÍNH:
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ 1. User GET Lesson → Check Unlock → Return Questions (SHUFFLED)    │
 * │ 2. User SUBMIT → Process Answers → Update Progress → Level Up?     │
 * │ 3. System → Unlock Next Lesson → Track Analytics                   │
 * └─────────────────────────────────────────────────────────────────────┘
 * 
 * SERVICES ĐƯỢC INJECT:
 * - QuestionService: Load & shuffle questions
 * - AnswerValidationService: Chấm điểm
 * - LessonProgressService: Update progress (points, completed, etc.)
 * - LevelUpgradeService: Check nâng cấp level
 * - UserRepository, TaskGroupRepository, QuestionRepository
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */
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

    // =========================================================================
    // ABSTRACT METHODS - Child classes PHẢI implement
    // =========================================================================
    // Mỗi module (Grammar, Reading, Listening) có đặc thù riêng:
    // - Cấu trúc Lesson khác nhau
    // - Cách tính điểm khác nhau
    // - Repository khác nhau
    // 
    // → Base class KHÔNG THỂ biết chi tiết → Dùng Abstract Methods
    // =========================================================================

    /** Loại parent (GRAMMAR, READING, LISTENING, VOCABULARY) */
    protected abstract ParentType getParentType();

    /** Thứ tự của lesson (dùng để unlock lesson tiếp theo) */
    protected abstract Integer getLessonOrder(TLesson lesson);

    /** ID của lesson */
    protected abstract Long getLessonId(TLesson lesson);

    /** Kiểm tra lesson có active không */
    protected abstract boolean isLessonActive(TLesson lesson);

    /** Điểm thưởng khi hoàn thành lesson */
    protected abstract int getPointsReward(TLesson lesson);

    /** Tạo progress object mới (mỗi module có Progress khác nhau) */
    protected abstract TProgress createNewProgressInstance(Long userId, TLesson lesson);

    /** Tìm progress của user cho lesson này */
    protected abstract Optional<TProgress> findProgress(Long userId, Long lessonId);

    /** Lưu progress vào DB */
    protected abstract void saveProgress(TProgress progress);

    /** ID của topic chứa lesson (dùng cho level upgrade) */
    protected abstract Long getTopicId(TLesson lesson);

    /** Tên topic (dùng cho tracking) */
    protected abstract String getTopicName(TLesson lesson);

    /** Level tối thiểu để mở lesson */
    protected abstract EnglishLevel getLessonRequiredLevel(TLesson lesson);

    /**
     * ═══════════════════════════════════════════════════════════════════════
     * 🔥 CORE LOGIC: XỬ LÝ NỘP BÀI - CHẤM ĐIỂM - CẬP NHẬT PROGRESS
     * ═══════════════════════════════════════════════════════════════════════
     * 
     * THỨ TỰ THỰC HIỆN (6 BƯỚC):
     * 
     * ┌─────────────────────────────────────────────────────────────────────┐
     * │ BƯỚC 1: LOAD & CHẤM ĐIỂM CÂU HỎI                                    │
     * │   ├─ Load tất cả questions của lesson (standalone + trong tasks)   │
     * │   ├─ Loop qua từng câu, gọi AnswerValidationService.validate()     │
     * │   ├─ Tính: correctCount, totalScore, maxPossibleScore              │
     * │   └─ Tạo List<QuestionResultDTO> (isCorrect, points, feedback)     │
     * ├─────────────────────────────────────────────────────────────────────┤
     * │ BƯỚC 2: TÍNH TOÁN KẾT QUẢ                                           │
     * │   ├─ scorePercentage = (totalScore / maxPossibleScore) * 100       │
     * │   └─ isPassed = scorePercentage >= 80% (configurable)              │
     * ├─────────────────────────────────────────────────────────────────────┤
     * │ BƯỚC 3: CẬP NHẬT PROGRESS (QUAN TRỌNG!)                             │
     * │   ├─ Tìm hoặc tạo Progress cho user                                │
     * │   ├─ Gọi LessonProgressService.updateProgress()                    │
     * │   │   → Cập nhật: scorePercentage, isCompleted, attempts           │
     * │   │   → Cộng điểm nếu lần đầu pass                                 │
     * │   │   → Tăng số bài học hoàn thành                                 │
     * │   └─ Lưu Progress vào DB                                           │
     * ├─────────────────────────────────────────────────────────────────────┤
     * │ BƯỚC 4: TÌM & MỞ KHÓA BÀI TIẾP THEO                                 │
     * │   ├─ Tìm lesson tiếp theo trong cùng topic                         │
     * │   ├─ Nếu pass lần đầu → hasUnlockedNext = true                     │
     * │   └─ Return nextLessonId để frontend redirect                      │
     * ├─────────────────────────────────────────────────────────────────────┤
     * │ BƯỚC 5: CHECK NÂNG CẤP LEVEL (Nếu pass lần đầu)                    │
     * │   ├─ Gọi LevelUpgradeService.checkAndUpgradeLevel()                │
     * │   ├─ Check: User đã hoàn thành đủ % lessons trong module?          │
     * │   └─ Nếu đủ → Nâng level: A1 → A2 → B1 → B2 → C1 → C2             │
     * ├─────────────────────────────────────────────────────────────────────┤
     * │ BƯỚC 6: GỬI SỰ KIỆN TRACKING (Async)                                │
     * │   ├─ Publish LessonCompletedEvent                                  │
     * │   ├─ Data: questionTypes, isCorrect, timestamps                    │
     * │   └─ Dùng cho: Analytics, Reports, AI recommendations              │
     * └─────────────────────────────────────────────────────────────────────┘
     * 
     * RETURN:
     * SubmitResultDTO {
     *   isPassed,           // User có pass không (>= 80%)
     *   scorePercentage,    // Điểm % (0-100)
     *   totalScore,         // Tổng điểm đạt được
     *   correctCount,       // Số câu đúng
     *   totalQuestions,     // Tổng số câu
     *   pointsEarned,       // Điểm thưởng nhận được (0 nếu đã làm rồi)
     *   results,            // Chi tiết từng câu (đúng/sai, feedback)
     *   hasUnlockedNext,    // Có mở khóa bài mới không
     *   nextLessonId,       // ID bài tiếp theo (nếu có)
     *   levelUpgradeResult  // Kết quả nâng level (nếu có)
     * }
     * 
     * @param userId ID của user
     * @param lesson Lesson đang submit
     * @param allLessons Tất cả lessons trong topic (để tìm next)
     * @param answers Danh sách câu trả lời từ user
     * @param moduleType GRAMMAR, READING, LISTENING, VOCABULARY
     * @return SubmitResultDTO chứa đầy đủ thông tin kết quả
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
                totalScore += result.getPoints();
            }

            trackingInfos.add(
                    new LessonCompletedEvent.QuestionTrackingInfo(q.getQuestionType(), isCorrect));
        }

        // 2. Tính toán kết quả
        int maxPossibleScore = questions.stream().mapToInt(Question::getPoints).sum();
        double scorePercentage = maxPossibleScore > 0 ? ((double) totalScore / maxPossibleScore) * 100.0 : 0.0;
        scorePercentage = Math.round(scorePercentage);
        boolean isPassed = scorePercentage >= 80.0;

        // 3. Lưu tiến độ
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
            if (updateResult.isFirstCompletion() && isPassed) {
                hasUnlockedNext = true;
            }
        }

        // 5. Check nâng cấp trình độ
        LevelUpgradeService.LevelUpgradeResult levelResult = null;
        if (updateResult.isFirstCompletion() && isPassed) {
            try {
                levelResult = levelUpgradeService.checkAndUpgradeLevel(userId, moduleType, getTopicId(lesson));
            } catch (Exception e) {
                log.error("Level upgrade check failed", e);
            }
        }

        // 6. Gửi sự kiện Tracking
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
                .nextLessonId(nextLessonId)
                .levelUpgradeResult(levelResult)
                .build();
    }

    // =========================================================================
    // HELPER METHODS - Các phương thức hỗ trợ
    // =========================================================================

    /**
     * Load tất cả câu hỏi của lesson để chấm điểm
     * 
     * QUAN TRỌNG: Phải load CẢ 2 LOẠI:
     * 1. Câu hỏi standalone (không thuộc task nào)
     * 2. Câu hỏi trong các TaskGroups
     * 
     * FLOW:
     * ┌────────────────────────────────────────────────────────┐
     * │ 1. Load câu hỏi standalone (parentType + parentId)    │
     * │ 2. Load tất cả TaskGroups của lesson                   │
     * │ 3. Loop qua từng TaskGroup, load questions             │
     * │ 4. Remove duplicates (case: câu hỏi bị query 2 lần)   │
     * │ 5. Return danh sách unique questions                   │
     * └────────────────────────────────────────────────────────┘
     * 
     * @param lessonId ID của lesson
     * @return List<Question> không trùng lặp
     */
    private List<Question> loadQuestionsForGrading(Long lessonId) {
        List<Question> allQuestions = new ArrayList<>();

        // Lấy câu hỏi lẻ (standalone)
        allQuestions.addAll(questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(getParentType(), lessonId));

        // Lấy câu hỏi trong nhóm (TaskGroups)
        List<TaskGroup> groups = taskGroupRepository
                .findByParentTypeAndParentIdOrderByOrderIndexAsc(getParentType(), lessonId);
        for (TaskGroup g : groups) {
            allQuestions.addAll(questionRepository.findByTaskGroupIdOrderByOrderIndexAsc(g.getId()));
        }

        // ✅ FIX: Remove duplicates bằng Set dựa trên Question ID
        // (Tránh trường hợp câu hỏi bị query nhiều lần)
        Map<Long, Question> uniqueMap = new LinkedHashMap<>();
        for (Question q : allQuestions) {
            uniqueMap.putIfAbsent(q.getId(), q);
        }

        List<Question> result = new ArrayList<>(uniqueMap.values());
        log.debug("Loaded {} unique questions for lesson {}", result.size(), lessonId);

        return result;
    }

    /**
     * Tìm lesson tiếp theo trong cùng topic
     * 
     * Logic:
     * 1. Sort tất cả lessons theo orderIndex
     * 2. Tìm vị trí của currentLesson
     * 3. Return lesson ở vị trí tiếp theo (nếu có và active)
     * 
     * @param currentLesson Lesson hiện tại
     * @param allLessons Tất cả lessons trong topic
     * @return Lesson tiếp theo hoặc null
     */
    protected TLesson findNextLesson(TLesson currentLesson, List<TLesson> allLessons) {
        if (allLessons == null || allLessons.isEmpty()) return null;

        // Sắp xếp lại cho chắc chắn
        List<TLesson> sorted = allLessons.stream()
                .sorted(Comparator.comparing(this::getLessonOrder))
                .toList();

        Long currentId = getLessonId(currentLesson);
        for (int i = 0; i < sorted.size() - 1; i++) {
            if (getLessonId(sorted.get(i)).equals(currentId)) {
                TLesson next = sorted.get(i + 1);
                if (isLessonActive(next)) return next; // Chỉ trả về nếu bài tiếp theo Active
            }
        }
        return null;
    }

    /**
     * Gửi sự kiện tracking (Async)
     * 
     * Event sẽ được listener bắt để:
     * - Lưu analytics
     * - Tạo reports
     * - AI recommendations
     * 
     * Error không ảnh hưởng submission
     */
    private void publishTrackingEvent(Long userId, TLesson lesson, ModuleType module,
            List<LessonCompletedEvent.QuestionTrackingInfo> infos) {
        try {
            eventPublisher.publishEvent(new LessonCompletedEvent(
                    this, userId, module, getTopicId(lesson), getTopicName(lesson), infos));
        } catch (Exception e) {
            log.error("Failed to publish tracking event", e);
        }
    }

    // =========================================================================
    // UNLOCK & ACCESS CONTROL - Kiểm soát quyền truy cập lesson
    // =========================================================================

    /**
     * Kiểm tra lesson có được mở khóa cho user không
     * 
     * ĐIỀU KIỆN MỞ KHÓA:
     * ┌──────────────────────────────────────────────────────────┐
     * │ 1. Lesson phải ACTIVE                                    │
     * │ 2. User level >= lesson required level                   │
     * │ 3. Lesson đầu tiên (order = 1) → Luôn mở                │
     * │ 4. Lesson khác → Phải hoàn thành lesson trước đó        │
     * └──────────────────────────────────────────────────────────┘
     * 
     * @param lesson Lesson cần check
     * @param allLessons Tất cả lessons trong topic
     * @param userId ID user
     * @param orderGetter Function lấy orderIndex
     * @param idGetter Function lấy lessonId
     * @param progressChecker BiPredicate check đã hoàn thành chưa
     * @param userLevel Level hiện tại của user
     * @return true nếu unlock, false nếu còn khóa
     */
    protected boolean isLessonUnlocked(TLesson lesson, List<TLesson> allLessons, Long userId,
            Function<TLesson, Integer> orderGetter, Function<TLesson, Long> idGetter,
            BiPredicate<Long, Long> progressChecker, EnglishLevel userLevel) {
        
        // Check 1: Lesson phải active
        if (!isLessonActive(lesson)) return false;

        // Check 2: User level >= required level
        EnglishLevel required = getLessonRequiredLevel(lesson);
        if (required != null && userLevel != null && userLevel.ordinal() < required.ordinal())
            return false;

        // Check 3: Lesson đầu tiên luôn mở
        Integer order = orderGetter.apply(lesson);
        if (order == 1) return true;

        // Check 4: Phải hoàn thành lesson trước đó
        return allLessons.stream()
                .filter(this::isLessonActive)
                .filter(l -> orderGetter.apply(l) == order - 1)
                .findFirst()
                .map(prev -> progressChecker.test(userId, idGetter.apply(prev)))
                .orElse(true); // Fallback: nếu không tìm thấy bài trước thì mở
    }

    /**
     * Validate quyền truy cập lesson
     * 
     * Throw exception nếu:
     * - Lesson không active
     * - User chưa đủ điều kiện mở
     * 
     * @throws IllegalStateException nếu không có quyền truy cập
     */
    protected void validateLessonAccess(TLesson lesson, List<TLesson> allLessons, Long userId,
            BiPredicate<Long, Long> progressChecker, EnglishLevel userLevel) {
        if (!isLessonActive(lesson))
            throw new IllegalStateException("Bài học không khả dụng");
        if (!isLessonUnlocked(lesson, allLessons, userId, this::getLessonOrder, this::getLessonId,
                progressChecker, userLevel)) {
            throw new IllegalStateException("Bạn chưa đủ điều kiện mở bài học này");
        }
    }

    /**
     * ✅ UPDATED: Get grouped questions với SHUFFLE
     * - Xáo thứ tự câu hỏi TRONG mỗi task
     * - Xáo các options/items trong từng câu hỏi
     */
    protected TaskGroupedQuestionsDTO getGroupedQuestionsForLesson(Long lessonId) {
        List<Question> questions = questionService.loadQuestionsByParent(getParentType(), lessonId);
        if (questions.isEmpty())
            return TaskGroupedQuestionsDTO.builder()
                    .hasTaskStructure(false)
                    .standaloneQuestions(List.of())
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
                    
                    // ✅ SHUFFLE: Xáo thứ tự câu hỏi trong task
                    List<Question> taskQuestions = new ArrayList<>(entry.getValue());
                    Collections.shuffle(taskQuestions);
                    
                    return TaskGroupedQuestionsDTO.TaskGroup.builder()
                            .taskGroupId(tg.getId())
                            .taskName(tg.getTaskName())
                            .taskInstruction(tg.getInstruction())
                            .taskOrder(tg.getOrderIndex())
                            .questions(questionService.convertToDTOsForLearning(taskQuestions)) // Already shuffles options
                            .build();
                })
                .sorted(Comparator.comparing(TaskGroupedQuestionsDTO.TaskGroup::getTaskOrder))
                .toList();

        // ✅ SHUFFLE: Xáo thứ tự câu hỏi standalone
        Collections.shuffle(standalone);

        return TaskGroupedQuestionsDTO.builder()
                .hasTaskStructure(!tasks.isEmpty())
                .tasks(tasks)
                .standaloneQuestions(questionService.convertToDTOsForLearning(standalone))
                .build();
    }
}