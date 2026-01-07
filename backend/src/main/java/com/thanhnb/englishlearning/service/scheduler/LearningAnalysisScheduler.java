package com.thanhnb.englishlearning.service.scheduler;

import com.thanhnb.englishlearning.entity.topic.Topic;
import com.thanhnb.englishlearning.entity.user.UserLearningBehavior;
import com.thanhnb.englishlearning.repository.grammar.GrammarLessonRepository;
import com.thanhnb.englishlearning.repository.listening.ListeningLessonRepository;
import com.thanhnb.englishlearning.repository.reading.ReadingLessonRepository;
import com.thanhnb.englishlearning.repository.topic.TopicRepository;
import com.thanhnb.englishlearning.repository.user.UserLearningBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningAnalysisScheduler {

    private final UserLearningBehaviorRepository behaviorRepository;
    
    // ✅ Inject đủ các Repository cần thiết
    private final TopicRepository topicRepository;
    private final GrammarLessonRepository grammarRepo;
    private final ReadingLessonRepository readingRepo;
    private final ListeningLessonRepository listeningRepo;

    // Chạy vào 2:00 sáng mỗi ngày
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void analyzeDailyBehaviors() {
        log.info("Starting daily learning behavior analysis...");

        List<UserLearningBehavior> behaviors = behaviorRepository.findAll();

        for (UserLearningBehavior behavior : behaviors) {
            try {
                // Duyệt qua map topic_progress để update số liệu
                behavior.getTopicProgress().forEach((topicIdStr, stats) -> {
                    try {
                        Long topicId = Long.parseLong(topicIdStr);
                        
                        // 1. Tìm Topic để biết nó thuộc Module nào
                        Topic topic = topicRepository.findById(topicId).orElse(null);
                        
                        if (topic != null) {
                            long realTotal = 0;
                            
                            // 2. Switch case theo Module để đếm đúng bảng
                            switch (topic.getModuleType()) { // Giả sử Topic có field getModule() trả về ModuleType
                                case GRAMMAR -> realTotal = grammarRepo.countByTopicIdAndIsActiveTrue(topicId);
                                case READING -> realTotal = readingRepo.countByTopicIdAndIsActiveTrue(topicId);
                                case LISTENING -> realTotal = listeningRepo.countByTopicIdAndIsActiveTrue(topicId);
                                default -> log.warn("Unknown module for topic {}", topicId);
                            }
                            
                            // 3. Update lại stats
                            if (realTotal > 0) {
                                stats.setTotalLessons(realTotal);
                                
                                // Recalculate % completion
                                int completed = stats.getCompletedLessons() != null ? stats.getCompletedLessons() : 0;
                                double percent = (double) completed / realTotal * 100;
                                stats.setCompletionPercentage(Math.min(100.0, percent));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error updating topic stats for topicId: {}", topicIdStr, e);
                    }
                });
                
                behavior.setLastAnalyzedAt(LocalDateTime.now());
                behaviorRepository.save(behavior);
                
            } catch (Exception e) {
                log.error("Error analyzing behavior for user {}", behavior.getUserId(), e);
            }
        }
        
        log.info("Daily analysis completed.");
    }
}