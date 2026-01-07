package com.thanhnb.englishlearning.repository.recommendation;

import com.thanhnb.englishlearning.entity.recommendation.AIRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {
    
    // Lấy gợi ý còn hạn, chưa hoàn thành, sắp xếp theo độ ưu tiên
    List<AIRecommendation> findByUserIdAndExpiresAtAfterAndIsCompletedFalseOrderByPriorityDesc(
        Long userId, LocalDateTime now
    );

    // Xóa gợi ý hết hạn (cho Scheduler)
    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime now);
}