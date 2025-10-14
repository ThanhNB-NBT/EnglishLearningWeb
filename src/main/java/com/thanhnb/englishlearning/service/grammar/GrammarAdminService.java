package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.entity.grammar.*;
import com.thanhnb.englishlearning.entity.question.*;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.repository.grammar.*;
import com.thanhnb.englishlearning.repository.question.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarAdminService {

    private final GrammarTopicRepository grammarTopicRepository;
    private final GrammarLessonRepository grammarLessonRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

    // ===== TOPIC MANAGEMENT =====

    // Lấy danh sách topic (bao gồm không hoạt động -inactive-)
    public List<GrammarTopicDTO> getAllTopics() {
        return grammarTopicRepository.findAllByOrderIndexAsc().stream()
                .map(this::convertTopicToDTO)
                .collect(Collectors.toList());
    }

    // Tạo mới topic
    public GrammarTopicDTO createTopic(GrammarTopicDTO dto) {
        if (grammarTopicRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new RuntimeException("Tên topic đã tồn tại");
        }

        GrammarTopic topic = new GrammarTopic();
        topic.setName(dto.getName());
        topic.setDescription(dto.getDescription());
        topic.setLevelRequired(dto.getLevelRequired());
        topic.setOrderIndex(dto.getOrderIndex());
        topic.setIsActive(dto.getIsActive());
        topic.setCreatedAt(LocalDateTime.now());

        GrammarTopic savedTopic = grammarTopicRepository.save(topic);
        log.info("Created new Grammar Topic: {}", savedTopic.getName());

        return convertTopicToDTO(savedTopic);
    }

    // Cập nhật topic
    public GrammarTopicDTO updateTopic(Long id, GrammarTopicDTO dto) {
        GrammarTopic topic = grammarTopicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        if (grammarTopicRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            throw new RuntimeException("Tên topic đã tồn tại: " + dto.getName());
        }

        topic.setName(dto.getName());
        topic.setDescription(dto.getDescription());
        topic.setLevelRequired(dto.getLevelRequired());
        topic.setOrderIndex(dto.getOrderIndex());
        topic.setIsActive(dto.getIsActive());

        GrammarTopic savedTopic = grammarTopicRepository.save(topic);
        log.info("Updated Grammar Topic: {}", savedTopic.getName());

        return convertTopicToDTO(savedTopic);
    }

    // Xoá topic
    public void deleteTopic(Long id) {
        GrammarTopic topic = grammarTopicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        // Kiểm tra nếu có bài học thuộc topic này thì không cho xoá
        long lessonCount = grammarLessonRepository.countByTopicId(id);
        if (lessonCount > 0) {
            throw new RuntimeException("Không thể xoá topic vì có " + lessonCount + " bài học thuộc topic này");
        }

        grammarTopicRepository.delete(topic);
        log.info("Deleted Grammar Topic: {}", topic.getName());
    }

    // ===== LESSON MANAGEMENT =====

    // Lấy lesson theo topic
    public List<GrammarLessonDTO> getLessonsByTopic(Long topicId) {
        if (!grammarTopicRepository.existsById(topicId)) {
            throw new RuntimeException("Topic không tồn tại");
        }

        return grammarLessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId).stream()
                .map(lesson -> {
                    GrammarLessonDTO dto = convertLessonToDTO(lesson);
                    // Đếm số câu hỏi theo ParentType.GRAMMAR
                    long requestCount = questionRepository.countByParentTypeAndParentId(ParentType.GRAMMAR,
                            lesson.getId());
                    dto.setQuestionCount((int) requestCount);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public GrammarLessonDTO getLessonDetail(Long lessonId) {
        GrammarLesson lesson = grammarLessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại"));

        GrammarLessonDTO dto = convertLessonToDTO(lesson);

        // Lấy câu hỏi
        List<Question> questions = questionRepository
                .findByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType.GRAMMAR, lessonId);
        dto.setQuestions(questions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList()));
        dto.setQuestionCount(questions.size());

        return dto;
    }

    // Tạo mới lesson
    public GrammarLessonDTO createLesson(GrammarLessonDTO dto) {
        GrammarTopic topic = grammarTopicRepository.findById(dto.getTopicId())
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        if (grammarLessonRepository.existsByTopicIdAndTitleIgnoreCase(dto.getTopicId(), dto.getTitle())) {
            throw new RuntimeException("Tiêu đề bài học đã tồn tại trong topic này");
        }

        if (dto.getEstimatedDuration() < 0) {
            throw new RuntimeException("Thời gian ước lượng phải lớn hơn hoặc bằng 0");
        }

        if (dto.getPointsReward() <= 0) {
            throw new RuntimeException("Điểm thưởng phải lớn hơn 0");
        }

        GrammarLesson lesson = new GrammarLesson();
        lesson.setTopic(topic);
        lesson.setTitle(dto.getTitle());
        lesson.setLessonType(dto.getLessonType());
        lesson.setContent(dto.getContent());
        lesson.setOrderIndex(dto.getOrderIndex());
        lesson.setEstimatedDuration(dto.getEstimatedDuration() != null ? dto.getEstimatedDuration() : 30);
        lesson.setPointsReward(dto.getPointsReward());
        lesson.setIsActive(dto.getIsActive());
        lesson.setCreatedAt(LocalDateTime.now());

        GrammarLesson savedLesson = grammarLessonRepository.save(lesson);
        log.info("Created new Grammar Lesson: {}", savedLesson.getTitle());

        return convertLessonToDTO(savedLesson);
    }

    // Cập nhật lesson
    public GrammarLessonDTO updateLesson(Long id, GrammarLessonDTO dto) {
        GrammarLesson lesson = grammarLessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại"));

        if (grammarLessonRepository.existsByTopicIdAndTitleIgnoreCaseAndIdNot(lesson.getTopic().getId(), dto.getTitle(),
                id)) {
            throw new RuntimeException("Tiêu đề bài học đã tồn tại trong topic này");
        }

        if (dto.getEstimatedDuration() < 0) {
            throw new RuntimeException("Điểm yêu cầu phải lớn hơn hoặc bằng 0");
        }
        if (dto.getPointsReward() <= 0) {
            throw new RuntimeException("Điểm thưởng phải lớn hơn 0");
        }

        lesson.setTitle(dto.getTitle());
        lesson.setLessonType(dto.getLessonType());
        lesson.setContent(dto.getContent());
        lesson.setOrderIndex(dto.getOrderIndex());
        if (dto.getEstimatedDuration() != null) {
            lesson.setEstimatedDuration(dto.getEstimatedDuration());
        }
        if (dto.getPointsReward() != null) {
            lesson.setPointsReward(dto.getPointsReward());
        }
        if (dto.getIsActive() != null) {
            lesson.setIsActive(dto.getIsActive());
        }

        GrammarLesson savedLesson = grammarLessonRepository.save(lesson);
        log.info("Updated Grammar Lesson: {}", savedLesson.getTitle());

        return convertLessonToDTO(savedLesson);
    }

    // Xoá lesson
    public void deleteLesson(Long id) {
        GrammarLesson lesson = grammarLessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại"));

        // Kiểm tra nếu có câu hỏi thuộc bài học này thì không cho xoá
        long questionCount = questionRepository.countByParentTypeAndParentId(ParentType.GRAMMAR, id);
        if (questionCount > 0) {
            throw new RuntimeException("Không thể xoá bài học vì có " + questionCount + " câu hỏi thuộc bài học này");
        }

        grammarLessonRepository.delete(lesson);
        log.info("Deleted Grammar Lesson: {}", lesson.getTitle());
    }

    // ===== QUESTION MANAGEMENT =====

    // Lấy câu hỏi theo lesson
    public List<GrammarQuestionDTO> getQuestionsByLesson(Long lessonId) {
        if (!grammarLessonRepository.existsById(lessonId)) {
            throw new RuntimeException("Bài học không tồn tại");
        }
        return questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(ParentType.GRAMMAR, lessonId)
                .stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    // Tạo mới câu hỏi
    public GrammarQuestionDTO createQuestion(GrammarQuestionDTO dto) {
        GrammarLesson lesson = grammarLessonRepository.findById(dto.getLessonId())
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại"));

        // Tạo mới câu hỏi với parentType = GRAMMAR
        Question question = new Question();
        question.setParentType(ParentType.GRAMMAR);
        question.setParentId(dto.getLessonId());
        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setExplanation(dto.getExplanation());
        question.setPoints(dto.getPoints() != null ? dto.getPoints() : 5);

        if (dto.getOrderIndex() == null) {
            long currentCount = questionRepository.countByParentTypeAndParentId(ParentType.GRAMMAR, dto.getLessonId());
            question.setOrderIndex((int) currentCount + 1);
        } else {
            question.setOrderIndex(dto.getOrderIndex());
        }

        question.setCreatedAt(LocalDateTime.now());

        Question savedQuestion = questionRepository.save(question);
        log.info("Created new Grammar Question: {}", savedQuestion.getQuestionText());

        // Tạo các tùy chọn (cho câu trắc nghiệm)
        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            List<QuestionOption> options = dto.getOptions().stream()
                    .map(optionDTO -> {
                        QuestionOption option = new QuestionOption();
                        option.setQuestion(savedQuestion);
                        option.setOptionText(optionDTO.getOptionText());
                        option.setIsCorrect(optionDTO.getIsCorrect() != null ? optionDTO.getIsCorrect() : false);
                        option.setOrderIndex(optionDTO.getOrderIndex());
                        return option;
                    }).collect(Collectors.toList());

            questionOptionRepository.saveAll(options);
            log.info("✅ Created {} options for question id={}", options.size(), savedQuestion.getId());
        }

        log.info("Created new grammar question: {} for lesson: {}",
                savedQuestion.getId(), lesson.getTitle());

        return convertQuestionToDTO(savedQuestion);
    }

    // Cập nhật câu hỏi
    public GrammarQuestionDTO updateQuestion(Long id, GrammarQuestionDTO dto) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại"));

        if (!question.getParentType().equals(ParentType.GRAMMAR)) {
            throw new RuntimeException("Câu hỏi không thuộc bài học ngữ pháp");
        }

        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setExplanation(dto.getExplanation());

        if (dto.getPoints() != null) {
            question.setPoints(dto.getPoints());
        }

        if (dto.getOrderIndex() != null) {
            question.setOrderIndex(dto.getOrderIndex());
        }

        Question savedQuestion = questionRepository.save(question);

        // Update options
        if (dto.getOptions() != null) {
            // Xóa options cũ
            questionOptionRepository.deleteByQuestionId(id);

            // Tạo mới options
            List<QuestionOption> options = dto.getOptions().stream()
                    .map(optionDTO -> {
                        QuestionOption option = new QuestionOption();
                        option.setQuestion(savedQuestion);
                        option.setOptionText(optionDTO.getOptionText());
                        option.setIsCorrect(optionDTO.getIsCorrect() != null ? optionDTO.getIsCorrect() : false);
                        option.setOrderIndex(optionDTO.getOrderIndex());
                        return option;
                    }).collect(Collectors.toList());

            questionOptionRepository.saveAll(options);
            log.info("📝 Updated {} options for question id={}", options.size(), id);
        }
        log.info("Updated grammar questionID: {}", id);
        return convertQuestionToDTO(savedQuestion);
    }

    // Xoá câu hỏi
    public void deleteQuestion(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại"));

        if (!question.getParentType().equals(ParentType.GRAMMAR)) {
            throw new RuntimeException("Câu hỏi không thuộc bài học ngữ pháp");
        }

        // Xoá options trước
        questionOptionRepository.deleteByQuestionId(id);

        questionRepository.delete(question);
        log.info("Deleted Grammar Question: {}", question.getQuestionText());
    }

    // Tạo nhiều question cùng lúc
    @Transactional
    public List<GrammarQuestionDTO> createQuestionsInBulk(Long lessonId, List<GrammarQuestionDTO> questionDTOs) {
        GrammarLesson lesson = grammarLessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại"));

        Long currentCount = questionRepository.countByParentTypeAndParentId(ParentType.GRAMMAR, lessonId);
        final int[] orderIndex = { currentCount.intValue() };

        List<Question> questions = questionDTOs.stream()
                .map(dto -> {
                    Question question = new Question();
                    question.setParentType(ParentType.GRAMMAR);
                    question.setParentId(lessonId);
                    question.setQuestionText(dto.getQuestionText());
                    question.setQuestionType(dto.getQuestionType());
                    question.setCorrectAnswer(dto.getCorrectAnswer());
                    question.setExplanation(dto.getExplanation());
                    question.setPoints(dto.getPoints() != null ? dto.getPoints() : 5);
                    question.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : ++orderIndex[0]);
                    question.setCreatedAt(LocalDateTime.now());
                    return question;
                })
                .collect(Collectors.toList());

        List<Question> savedQuestions = questionRepository.saveAll(questions);

        // Tạo các tùy chọn (cho câu trắc nghiệm)
        for (int i = 0; i < savedQuestions.size(); i++) {
            Question savedQuestion = savedQuestions.get(i);
            GrammarQuestionDTO dto = questionDTOs.get(i);

            if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
                List<QuestionOption> options = dto.getOptions().stream()
                        .map(optionDTO -> {
                            QuestionOption option = new QuestionOption();
                            option.setQuestion(savedQuestion);
                            option.setOptionText(optionDTO.getOptionText());
                            option.setIsCorrect(optionDTO.getIsCorrect() != null ? optionDTO.getIsCorrect() : false);
                            option.setOrderIndex(optionDTO.getOrderIndex());
                            return option;
                        })
                        .collect(Collectors.toList());

                questionOptionRepository.saveAll(options);
                log.info("✅ Created {} options for question id={}", options.size(), savedQuestion.getId());
            }
        }
        log.info("Created {} new questions for lesson: {}", questions.size(), lesson.getTitle());
        return questions.stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Import lessons từ PDF parsing result (lessons + questions)
     * Dùng cho save-parsed-lessons endpoint
     */
    @Transactional
    public List<GrammarLessonDTO> importLessonsFromPDF(Long topicId, List<GrammarLessonDTO> lessonDTOs) {
        GrammarTopic topic = grammarTopicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại với id: " + topicId));

        List<GrammarLessonDTO> savedLessons = new ArrayList<>();

        for (GrammarLessonDTO lessonDTO : lessonDTOs) {
            try {
                // Set topicId
                lessonDTO.setTopicId(topicId);

                // Tạo lesson
                GrammarLesson lesson = new GrammarLesson();
                lesson.setTopic(topic);
                lesson.setTitle(lessonDTO.getTitle());
                lesson.setLessonType(lessonDTO.getLessonType());
                lesson.setContent(lessonDTO.getContent());
                lesson.setOrderIndex(lessonDTO.getOrderIndex());
                lesson.setEstimatedDuration(
                        lessonDTO.getEstimatedDuration() != null ? lessonDTO.getEstimatedDuration() : 30);
                lesson.setPointsReward(lessonDTO.getPointsReward() != null ? lessonDTO.getPointsReward() : 10);
                lesson.setIsActive(lessonDTO.getIsActive() != null ? lessonDTO.getIsActive() : true);
                lesson.setCreatedAt(LocalDateTime.now());

                GrammarLesson savedLesson = grammarLessonRepository.save(lesson);

                // Đếm số questions
                int questionCount = 0;

                // Tạo questions nếu có (cho PRACTICE lessons)
                if (lessonDTO.getQuestions() != null && !lessonDTO.getQuestions().isEmpty()) {
                    for (GrammarQuestionDTO questionDTO : lessonDTO.getQuestions()) {
                        // Tạo Question với ParentType.GRAMMAR
                        Question question = new Question();
                        question.setParentType(ParentType.GRAMMAR);
                        question.setParentId(savedLesson.getId());
                        question.setQuestionText(questionDTO.getQuestionText());
                        question.setQuestionType(questionDTO.getQuestionType());
                        question.setCorrectAnswer(questionDTO.getCorrectAnswer());
                        question.setExplanation(questionDTO.getExplanation());
                        question.setPoints(questionDTO.getPoints() != null ? questionDTO.getPoints() : 5);
                        question.setOrderIndex(
                                questionDTO.getOrderIndex() != null ? questionDTO.getOrderIndex() : questionCount + 1);
                        question.setCreatedAt(LocalDateTime.now());

                        Question savedQuestion = questionRepository.save(question);
                        questionCount++;

                        // Tạo options nếu có (cho MULTIPLE_CHOICE)
                        if (questionDTO.getOptions() != null && !questionDTO.getOptions().isEmpty()) {
                            List<QuestionOption> options = questionDTO.getOptions().stream()
                                    .map(optionDTO -> {
                                        QuestionOption option = new QuestionOption();
                                        option.setQuestion(savedQuestion);
                                        option.setOptionText(optionDTO.getOptionText());
                                        option.setIsCorrect(
                                                optionDTO.getIsCorrect() != null ? optionDTO.getIsCorrect() : false);
                                        option.setOrderIndex(optionDTO.getOrderIndex());
                                        return option;
                                    })
                                    .collect(Collectors.toList());

                            questionOptionRepository.saveAll(options);
                        }
                    }

                    log.info("✅ Created {} questions for lesson: {}", questionCount, savedLesson.getTitle());
                }

                // Convert sang DTO
                GrammarLessonDTO savedDTO = convertLessonToDTO(savedLesson);
                savedDTO.setQuestionCount(questionCount);
                savedLessons.add(savedDTO);

                log.info("✅ Imported lesson: {} (type: {}, {} questions)",
                        savedLesson.getTitle(), savedLesson.getLessonType(), questionCount);

            } catch (Exception e) {
                log.error("❌ Failed to import lesson: {}", lessonDTO.getTitle(), e);
                // Continue với các lessons khác
            }
        }

        log.info(" Successfully imported {} lessons for topic: {}", savedLessons.size(), topic.getName());

        return savedLessons;
    }

    // Sao chép câu hỏi từ bài học khác
    @Transactional
    public void copyQuestionsToLesson(Long sourceLessonId, Long targetLessonId) {
        if (!grammarLessonRepository.existsById(sourceLessonId)) {
            throw new RuntimeException("Source lesson không tồn tại");
        }
        if (!grammarLessonRepository.existsById(targetLessonId)) {
            throw new RuntimeException("Target lesson không tồn tại");
        }

        List<Question> sourceQuestions = questionRepository.findByParentTypeAndParentIdOrderByOrderIndexAsc(
                ParentType.GRAMMAR, sourceLessonId);

        long currentCount = questionRepository.countByParentTypeAndParentId(ParentType.GRAMMAR, targetLessonId);

        for (Question source : sourceQuestions) {
            Question newQuestion = new Question();
            newQuestion.setParentType(ParentType.GRAMMAR);
            newQuestion.setParentId(targetLessonId);
            newQuestion.setQuestionText(source.getQuestionText());
            newQuestion.setQuestionType(source.getQuestionType());
            newQuestion.setCorrectAnswer(source.getCorrectAnswer());
            newQuestion.setExplanation(source.getExplanation());
            newQuestion.setPoints(source.getPoints());
            newQuestion.setOrderIndex((int) ++currentCount);
            newQuestion.setCreatedAt(LocalDateTime.now());

            Question savedQuestion = questionRepository.save(newQuestion);

            // Copy options
            List<QuestionOption> sourceOptions = questionOptionRepository
                    .findByQuestionIdOrderByOrderIndexAsc(source.getId());
            if (!sourceOptions.isEmpty()) {
                List<QuestionOption> newOptions = sourceOptions.stream()
                        .map(sourceOption -> {
                            QuestionOption newOption = new QuestionOption();
                            newOption.setQuestion(savedQuestion);
                            newOption.setOptionText(sourceOption.getOptionText());
                            newOption.setIsCorrect(sourceOption.getIsCorrect());
                            newOption.setOrderIndex(sourceOption.getOrderIndex());
                            return newOption;
                        })
                        .collect(Collectors.toList());

                questionOptionRepository.saveAll(newOptions);
            }
        }

        log.info("✅ Copied {} questions from lesson {} to lesson {}",
                sourceQuestions.size(), sourceLessonId, targetLessonId);
    }

    // ===== CONVERSION METHODS =====

    private GrammarTopicDTO convertTopicToDTO(GrammarTopic topic) {
        return new GrammarTopicDTO(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getLevelRequired(),
                topic.getOrderIndex(),
                topic.getIsActive(),
                topic.getCreatedAt(),
                null, // lessons
                null, // completedLessons
                null, // totalLessons
                null // isAccessible
        );
    }

    private GrammarLessonDTO convertLessonToDTO(GrammarLesson lesson) {
        return GrammarLessonDTO.full(
                lesson.getId(),
                lesson.getTopic().getId(),
                lesson.getTitle(),
                lesson.getLessonType(),
                lesson.getContent(),
                lesson.getOrderIndex(),
                lesson.getPointsReward(),
                lesson.getEstimatedDuration(),
                lesson.getIsActive(),
                lesson.getCreatedAt(),
                lesson.getTopic().getName());
    }

    private GrammarQuestionDTO convertQuestionToDTO(Question question) {
        // Load options
        List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(question.getId());

        List<GrammarQuestionOptionDTO> optionDTOs = options.stream()
                .map(option -> new GrammarQuestionOptionDTO(
                        option.getId(),
                        question.getId(),
                        option.getOptionText(),
                        option.getIsCorrect(),
                        option.getOrderIndex()))
                .collect(Collectors.toList());

        GrammarQuestionDTO dto = new GrammarQuestionDTO(
                question.getId(),
                question.getParentId(), // lessonId
                question.getQuestionText(),
                question.getQuestionType(),
                question.getCorrectAnswer(),
                question.getExplanation(),
                question.getPoints(),
                question.getOrderIndex(),
                question.getCreatedAt(),
                optionDTOs.isEmpty() ? null : optionDTOs,
                true // showCorrectAnswer - admin luôn thấy
        );

        dto.setOrderIndex(question.getOrderIndex());

        return dto;
    }
}
