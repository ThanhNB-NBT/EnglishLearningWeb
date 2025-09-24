package com.thanhnb.englishlearning.service.user;

import com.thanhnb.englishlearning.dto.user.UserDto;
import com.thanhnb.englishlearning.entity.User;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.UserRole;
import com.thanhnb.englishlearning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(UserDto userDto) {
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new RuntimeException("Tên tài khoản đã tồn tại");
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setFullName(userDto.getFullName());
        user.setRole(userDto.getRole() != null ? userDto.getRole() : UserRole.USER);
        user.setEnglishLevel(userDto.getEnglishLevel() != null ? userDto.getEnglishLevel() : EnglishLevel.BEGINNER);
        user.setTotalPoints(0);
        user.setStreakDays(0);
        user.setIsActive(true);

        return userRepository.save(user);
    }

    public User updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!user.getUsername().equals(userDto.getUsername()) && userRepository.existsByUsername(userDto.getUsername())) {
            throw new RuntimeException("Tên tài khoản đã tồn tại");
        }
        if (!user.getEmail().equals(userDto.getEmail()) && userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        user.setFullName(userDto.getFullName());
        user.setRole(userDto.getRole() != null ? userDto.getRole() : user.getRole());
        user.setEnglishLevel(userDto.getEnglishLevel() != null ? userDto.getEnglishLevel() : user.getEnglishLevel());
        user.setIsActive(userDto.getIsActive() != null ? userDto.getIsActive() : user.getIsActive());

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Người dùng không tồn tại");
        }
        userRepository.deleteById(id);
    }

    public void blockUser(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        user.setIsActive(false);
        userRepository.save(user);
    }

    public void unblockUser(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        user.setIsActive(true);
        userRepository.save(user);
    }

    public void updateLastLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
    }

    public void addPoints(Long userId, int points) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        user.setTotalPoints(user.getTotalPoints() + points);
        userRepository.save(user);
    }

    public void updateStreakDays(Long userId, int streakDays) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        user.setStreakDays(streakDays);
        userRepository.save(user);
    }

    public List<User> getTopUsersByPoints(int minPoints) {
        return userRepository.findTopUsersByPoints(minPoints);
    }
    
    public List<User> getTopUsersByStreakDays(int minStreakDays) {
        return userRepository.findTopUsersByStreak(minStreakDays);
    }

    public List<User> getActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    

}
