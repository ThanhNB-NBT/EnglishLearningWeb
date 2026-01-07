// src/utils/levelValidator.js

/**
 * English Level Hierarchy
 * Matches backend enum order
 */
export const ENGLISH_LEVELS = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']

/**
 * Check if user level is sufficient for required level
 * @param {string} userLevel - User's current level (e.g., 'B1')
 * @param {string} requiredLevel - Required level for resource (e.g., 'A2')
 * @returns {boolean} - True if user level >= required level
 */
export function isLevelSufficient(userLevel, requiredLevel) {
  if (!requiredLevel) return true // No requirement = accessible
  if (!userLevel) return false // No user level = cannot access

  const userIndex = ENGLISH_LEVELS.indexOf(userLevel)
  const requiredIndex = ENGLISH_LEVELS.indexOf(requiredLevel)

  // Invalid level = deny access
  if (userIndex === -1 || requiredIndex === -1) return false

  return userIndex >= requiredIndex
}

/**
 * Check if topic is accessible (Topic-level requirement)
 * @param {Object} topic - Topic object with levelRequired
 * @param {string} userLevel - User's current level
 * @returns {boolean}
 */
export function isTopicAccessible(topic, userLevel) {
  if (!topic?.levelRequired) return true
  return isLevelSufficient(userLevel, topic.levelRequired)
}

/**
 * Check if lesson is accessible (Combined Topic + Lesson + Sequential check)
 * @param {Object} lesson - Lesson object
 * @param {Object} topic - Parent topic object
 * @param {string} userLevel - User's current level
 * @param {Array} allLessons - All lessons in topic (for sequential check)
 * @param {Function} isCompletedFn - Function to check if previous lesson is completed
 * @returns {Object} - { accessible: boolean, reason: string }
 */
export function isLessonAccessible(lesson, topic, userLevel, allLessons, isCompletedFn) {
  // 1. Check if lesson is active
  if (!lesson.isActive) {
    return {
      accessible: false,
      reason: 'INACTIVE',
      message: 'Bài học hiện không khả dụng',
    }
  }

  // 2. Check TOPIC level requirement first
  if (!isTopicAccessible(topic, userLevel)) {
    return {
      accessible: false,
      reason: 'TOPIC_LEVEL',
      message: `Chủ đề này yêu cầu trình độ ${topic.levelRequired}. Bạn hiện tại: ${userLevel || 'Chưa xác định'}`,
      requiredLevel: topic.levelRequired,
      currentLevel: userLevel,
    }
  }

  // 3. Check LESSON level requirement (if different from topic)
  if (lesson.levelRequired && lesson.levelRequired !== topic.levelRequired) {
    if (!isLevelSufficient(userLevel, lesson.levelRequired)) {
      return {
        accessible: false,
        reason: 'LESSON_LEVEL',
        message: `Bài học này yêu cầu trình độ ${lesson.levelRequired}. Bạn hiện tại: ${userLevel || 'Chưa xác định'}`,
        requiredLevel: lesson.levelRequired,
        currentLevel: userLevel,
      }
    }
  }

  // 4. Check sequential unlock (if not first lesson)
  if (lesson.orderIndex > 1) {
    const previousLesson = allLessons?.find((l) => l.orderIndex === lesson.orderIndex - 1)

    if (previousLesson && !isCompletedFn(previousLesson.id)) {
      return {
        accessible: false,
        reason: 'SEQUENTIAL',
        message: 'Hoàn thành bài trước để mở khóa bài này',
        previousLessonId: previousLesson.id,
        previousLessonTitle: previousLesson.title,
      }
    }
  }

  // All checks passed
  return { accessible: true, reason: 'OK' }
}

/**
 * Get user-friendly level label
 */
export function getLevelLabel(level) {
  const labels = {
    A1: 'Beginner',
    A2: 'Elementary',
    B1: 'Intermediate',
    B2: 'Upper Intermediate',
    C1: 'Advanced',
    C2: 'Proficiency',
  }
  return labels[level] || level
}

/**
 * Get level color for UI
 */
export function getLevelColor(level) {
  const colors = {
    A1: 'success',
    A2: 'success',
    B1: 'primary',
    B2: 'primary',
    C1: 'warning',
    C2: 'danger',
  }
  return colors[level] || 'info'
}
