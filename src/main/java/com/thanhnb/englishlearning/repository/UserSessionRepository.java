package com.thanhnb.englishlearning.repository;

import com.thanhnb.englishlearning.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByTokenHashAndIsActiveTrue(String tokenHash);
    
    List<UserSession> findByUserIdAndIsActiveTrueOrderByLastActivityDesc(Long userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.tokenHash = :tokenHash")
    void deactivateSession(@Param("tokenHash") String tokenHash);
    
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userId = :userId")
    void deactivateAllUserSessions(@Param("userId") Long userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.lastActivity = :now WHERE s.tokenHash = :tokenHash")
    void updateLastActivity(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now OR s.isActive = false")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);
    
    long countByUserIdAndIsActiveTrue(Long userId);
}
