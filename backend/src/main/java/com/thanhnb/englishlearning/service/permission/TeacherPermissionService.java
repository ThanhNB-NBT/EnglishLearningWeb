package com.thanhnb.englishlearning.service.permission;

import com.thanhnb.englishlearning.entity.user.User;
import com.thanhnb.englishlearning.enums.UserRole;
import com.thanhnb.englishlearning.repository.topic.TeacherTopicAssignmentRepository;
import com.thanhnb.englishlearning.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * ✅ Centralized permission check service
 * Used by: Grammar/Reading/Listening Lesson & Question Services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherPermissionService {

    private final TeacherTopicAssignmentRepository assignmentRepository;
    private final UserService userService;

    /**
     * ✅ Check if current user can manage a topic
     * - Admin: Always allowed
     * - Teacher: Must have active assignment
     * - Others: Denied
     * 
     * @param topicId Topic to check
     * @throws AccessDeniedException if user has no permission
     */
    public void checkTopicPermission(Long topicId) {
        User currentUser = userService.getCurrentUser();
        
        // Admin always has permission
        if (currentUser.getRole() == UserRole.ADMIN) {
            log.debug("Admin user {} has permission for topic {}", 
                currentUser.getUsername(), topicId);
            return;
        }
        
        // Teacher must have active assignment
        if (currentUser.getRole() == UserRole.TEACHER) {
            boolean hasAssignment = assignmentRepository
                .existsByTeacherIdAndTopicIdAndIsActiveTrue(
                    currentUser.getId(), 
                    topicId
                );
            
            if (hasAssignment) {
                log.debug("Teacher {} has assignment for topic {}", 
                    currentUser.getUsername(), topicId);
                return;
            }
            
            log.warn("Teacher {} tried to access topic {} without assignment", 
                currentUser.getUsername(), topicId);
            throw new AccessDeniedException(
                "Bạn không có quyền quản lý chủ đề này. Vui lòng liên hệ Admin để được phân công."
            );
        }
        
        // Regular users shouldn't reach here (handled by Spring Security)
        log.error("User {} with role {} tried to access admin/teacher endpoint", 
            currentUser.getUsername(), currentUser.getRole());
        throw new AccessDeniedException("Bạn không có quyền truy cập chức năng này");
    }

    /**
     * ✅ Check permission without throwing exception
     * Useful for UI logic
     * 
     * @param topicId Topic to check
     * @return true if user has permission, false otherwise
     */
    public boolean hasTopicPermission(Long topicId) {
        try {
            checkTopicPermission(topicId);
            return true;
        } catch (AccessDeniedException e) {
            return false;
        }
    }

    /**
     * ✅ Check if current user is admin
     * 
     * @return true if admin, false otherwise
     */
    public boolean isAdmin() {
        try {
            User currentUser = userService.getCurrentUser();
            return currentUser.getRole() == UserRole.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * ✅ Check if current user is teacher
     * 
     * @return true if teacher, false otherwise
     */
    public boolean isTeacher() {
        try {
            User currentUser = userService.getCurrentUser();
            return currentUser.getRole() == UserRole.TEACHER;
        } catch (Exception e) {
            return false;
        }
    }
}