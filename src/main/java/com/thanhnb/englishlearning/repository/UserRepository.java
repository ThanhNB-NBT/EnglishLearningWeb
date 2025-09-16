package com.thanhnb.englishlearning.repository;

import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRole(UserRole role);
    List<User> findByEnglishLevel(EnglishLevel englishLevel);
    Optional<User> findByEmail(String email);
    List<User> findByIsActiveTrue();
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.totalPoints >= :minPoints ORDER BY u.totalPoints DESC")
    List<User> findTopUsersByPoints(@Param("minPoints") Integer minPoints);

    @Query("SELECT u FROM User u WHERE u.streakDays >= :minStreak ORDER BY u.streakDays DESC")
    List<User> findTopUsersByStreak(@Param("minStreak") Integer minStreak);
}
