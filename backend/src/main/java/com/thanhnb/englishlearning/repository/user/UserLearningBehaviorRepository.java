package com.thanhnb.englishlearning.repository.user;

import com.thanhnb.englishlearning.entity.user.UserLearningBehavior;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLearningBehaviorRepository extends JpaRepository<UserLearningBehavior, Long> {
    
    // Tìm behavior của user, nếu chưa có thì trả về empty
    Optional<UserLearningBehavior> findByUserId(Long userId);

    // Query tìm những user cần phân tích lại (ví dụ: chưa phân tích trong 24h qua)
    // Dùng cho Scheduled Job sau này
    @Query("SELECT b FROM UserLearningBehavior b WHERE b.lastAnalyzedAt < :cutoffDate OR b.lastAnalyzedAt IS NULL")
    List<UserLearningBehavior> findUsersNeedAnalysis(LocalDateTime cutoffDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM UserLearningBehavior b WHERE b.userId = :userId")
    Optional<UserLearningBehavior> findByUserIdWithLock(@Param("userId") Long userId);
}