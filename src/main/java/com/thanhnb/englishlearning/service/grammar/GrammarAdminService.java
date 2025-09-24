package com.thanhnb.englishlearning.service.grammar;

import com.thanhnb.englishlearning.dto.grammar.*;
import com.thanhnb.englishlearning.entity.grammar.*;
import com.thanhnb.englishlearning.repository.grammar.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrammarAdminService {

    private final GrammarTopicRepository grammarTopicRepository;
    private final GrammarLessonRepository grammarLessonRepository;
    private final GrammarQuestionRepository grammarQuestionRepository;
    private final GrammarQuestionOptionRepository grammarQuestionOptionRepository;

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
            throw new RuntimeException("Tên topic đã tồn tại");
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
            throw new RuntimeException("Không thể xoá topic vì có bài học thuộc topic này");
        }   

        grammarTopicRepository.delete(topic);
        log.info("Deleted Grammar Topic: {}", topic.getName());
    }


    // ===== LESSON MANAGEMENT =====

    // Lấy lesson theo topic
    public List<GrammarLessonDTO> getLessonsByTopic(Long topicId){
        return grammarLessonRepository.findByTopicIdOrderByOrderIndexAsc(topicId).stream()
                .map(this::convertLessonToDTO)
                .collect(Collectors.toList());
    }

    // Tạo mới lesson
    public GrammarLessonDTO createLesson(GrammarLessonDTO dto) {
        GrammarTopic topic = grammarTopicRepository.findById(dto.getTopicId())
                .orElseThrow(() -> new RuntimeException("Topic không tồn tại"));

        if (grammarLessonRepository.existsByTopicIdAndTitleIgnoreCase(dto.getTopicId(), dto.getTitle())) {
            throw new RuntimeException("Tiêu đề bài học đã tồn tại trong topic này");
        }

        if (dto.getPointsRequired() < 0) {
            throw new RuntimeException("Điểm yêu cầu phải lớn hơn hoặc bằng 0");
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
        lesson.setPointsRequired(dto.getPointsRequired());
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

        if (grammarLessonRepository.existsByTopicIdAndTitleIgnoreCaseAndIdNot(lesson.getTopic().getId(), dto.getTitle(), id)) {
            throw new RuntimeException("Tiêu đề bài học đã tồn tại trong topic này");
        }

        if (dto.getPointsRequired() < 0) {
            throw new RuntimeException("Điểm yêu cầu phải lớn hơn hoặc bằng 0");
        }
        if (dto.getPointsReward() <= 0) {
            throw new RuntimeException("Điểm thưởng phải lớn hơn 0");
        }

        lesson.setTitle(dto.getTitle());
        lesson.setLessonType(dto.getLessonType());
        lesson.setContent(dto.getContent());
        lesson.setOrderIndex(dto.getOrderIndex());
        lesson.setPointsRequired(dto.getPointsRequired());
        lesson.setPointsReward(dto.getPointsReward());
        lesson.setIsActive(dto.getIsActive());

        GrammarLesson savedLesson = grammarLessonRepository.save(lesson);
        log.info("Updated Grammar Lesson: {}", savedLesson.getTitle());

        return convertLessonToDTO(savedLesson);
    }

    // Xoá lesson
    public void deleteLesson(Long id) {
        GrammarLesson lesson = grammarLessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại"));

        // Kiểm tra nếu có câu hỏi thuộc bài học này thì không cho xoá
        long questionCount = grammarQuestionRepository.countByLessonId(id);
        if (questionCount > 0) {
            throw new RuntimeException("Không thể xoá bài học vì có câu hỏi thuộc bài học này");
        }   

        grammarLessonRepository.delete(lesson);
        log.info("Deleted Grammar Lesson: {}", lesson.getTitle());
    }

    // ===== QUESTION MANAGEMENT =====
    
    // Lấy câu hỏi theo lesson
    public List<GrammarQuestionDTO> getQuestionsByLesson(Long lessonId) {
        return grammarQuestionRepository.findByLessonIdOrderByIdAsc(lessonId).stream()
                .map(this::convertQuestionToDTO)
                .collect(Collectors.toList());
    }

    // Tạo mới câu hỏi
    public GrammarQuestionDTO createQuestion(GrammarQuestionDTO dto) {
        GrammarLesson lesson = grammarLessonRepository.findById(dto.getLessonId())
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại"));

        GrammarQuestion question = new GrammarQuestion();
        question.setLesson(lesson);
        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setExplanation(dto.getExplanation());
        question.setPoints(dto.getPoints());
        question.setCreatedAt(LocalDateTime.now());

        GrammarQuestion savedQuestion = grammarQuestionRepository.save(question);
        log.info("Created new Grammar Question: {}", savedQuestion.getQuestionText());

        // Tạo các tùy chọn
        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            List<GrammarQuestionOption> options = dto.getOptions().stream().map(optionDTO -> {
                GrammarQuestionOption option = new GrammarQuestionOption();
                option.setQuestion(savedQuestion);
                option.setOptionText(optionDTO.getOptionText());
                option.setIsCorrect(optionDTO.getIsCorrect());
                option.setOrderIndex(optionDTO.getOrderIndex());
                return option;
            }).collect(Collectors.toList());

            grammarQuestionOptionRepository.saveAll(options);
        }

        log.info("Created new grammar question: {} for lesson: {}", 
                savedQuestion.getId(), lesson.getTitle());
        
        return convertQuestionToDTO(savedQuestion);
    }

    // Cập nhật câu hỏi
    public GrammarQuestionDTO updateQuestion(Long id, GrammarQuestionDTO dto) {
        GrammarQuestion question = grammarQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại"));

        question.setQuestionText(dto.getQuestionText());
        question.setQuestionType(dto.getQuestionType());
        question.setCorrectAnswer(dto.getCorrectAnswer());
        question.setExplanation(dto.getExplanation());
        question.setPoints(dto.getPoints());

        GrammarQuestion savedQuestion = grammarQuestionRepository.save(question);
        log.info("Updated Grammar Question: {}", savedQuestion.getQuestionText());

        // Update options
        if (dto.getOptions() != null) {
            // Xóa options cũ
            grammarQuestionOptionRepository.deleteByQuestionId(id);

            // Tạo mới options
            List<GrammarQuestionOption> options = dto.getOptions().stream().map(optionDTO -> {
                GrammarQuestionOption option = new GrammarQuestionOption();
                option.setQuestion(savedQuestion);
                option.setOptionText(optionDTO.getOptionText());
                option.setIsCorrect(optionDTO.getIsCorrect());
                option.setOrderIndex(optionDTO.getOrderIndex());
                return option;
            }).collect(Collectors.toList());

            grammarQuestionOptionRepository.saveAll(options);
        }
        log.info("Updated grammar question: {} for lesson: {}", 
                savedQuestion.getId(), question.getLesson().getTitle());
        return convertQuestionToDTO(savedQuestion);
    }

    // Xoá câu hỏi
    public void deleteQuestion(Long id) {
        GrammarQuestion question = grammarQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại"));

        // Xoá options trước
        grammarQuestionOptionRepository.deleteByQuestionId(id);

        grammarQuestionRepository.delete(question);
        log.info("Deleted Grammar Question: {}", question.getQuestionText());
    }

    // ===== CONVERSION METHODS =====

    private GrammarTopicDTO convertTopicToDTO(GrammarTopic topic) {
        GrammarTopicDTO dto = new GrammarTopicDTO();
        dto.setId(topic.getId());
        dto.setName(topic.getName());
        dto.setDescription(topic.getDescription());
        dto.setLevelRequired(topic.getLevelRequired());
        dto.setOrderIndex(topic.getOrderIndex());
        dto.setIsActive(topic.getIsActive());
        dto.setCreatedAt(topic.getCreatedAt());
        return dto;
    }

    private GrammarLessonDTO convertLessonToDTO(GrammarLesson lesson) {
        GrammarLessonDTO dto = new GrammarLessonDTO();
        dto.setId(lesson.getId());
        dto.setTopicId(lesson.getTopic().getId());
        dto.setTitle(lesson.getTitle());
        dto.setLessonType(lesson.getLessonType());
        dto.setContent(lesson.getContent());
        dto.setOrderIndex(lesson.getOrderIndex());
        dto.setPointsRequired(lesson.getPointsRequired());
        dto.setPointsReward(lesson.getPointsReward());
        dto.setIsActive(lesson.getIsActive());
        dto.setCreatedAt(lesson.getCreatedAt());
        dto.setTopicName(lesson.getTopic().getName());
        
        // Count questions
        dto.setQuestionCount((int) grammarQuestionRepository.countByLessonId(lesson.getId()));
        
        return dto;
    }

    private GrammarQuestionDTO convertQuestionToDTO(GrammarQuestion question) {
        GrammarQuestionDTO dto = new GrammarQuestionDTO();
        dto.setId(question.getId());
        dto.setLessonId(question.getLesson().getId());
        dto.setQuestionText(question.getQuestionText());
        dto.setQuestionType(question.getQuestionType());
        dto.setCorrectAnswer(question.getCorrectAnswer());
        dto.setExplanation(question.getExplanation());
        dto.setPoints(question.getPoints());
        dto.setCreatedAt(question.getCreatedAt());

        // Add options if exist - KHÔNG shuffle ở admin để giữ thứ tự gốc
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            List<GrammarQuestionOptionDTO> optionDTOs = question.getOptions().stream()
                    .sorted((o1, o2) -> o1.getOrderIndex().compareTo(o2.getOrderIndex())) // Sort by order_index
                    .map(this::convertOptionToDTO)
                    .collect(Collectors.toList());
            dto.setOptions(optionDTOs);
        }

        return dto;
    }

    private GrammarQuestionOptionDTO convertOptionToDTO(GrammarQuestionOption option) {
        GrammarQuestionOptionDTO dto = new GrammarQuestionOptionDTO();
        dto.setId(option.getId());
        dto.setQuestionId(option.getQuestion().getId());
        dto.setOptionText(option.getOptionText());
        dto.setIsCorrect(option.getIsCorrect());
        dto.setOrderIndex(option.getOrderIndex());
        return dto;
    }
}
