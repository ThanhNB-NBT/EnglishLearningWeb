// src/composables/useLevelAccess.js
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { isTopicAccessible, isLessonAccessible, getLevelLabel, getLevelColor } from '@/utils/levelValidator'

/**
 * Composable for handling level-based access control
 * Centralized logic for Topic/Lesson level requirements
 */
export function useLevelAccess() {
  const authStore = useAuthStore()

  // User's current level
  const userLevel = computed(() => authStore.currentUser?.englishLevel)

  /**
   * Check if user can access a topic
   */
  const canAccessTopic = computed(() => (topic) => {
    return isTopicAccessible(topic, userLevel.value)
  })

  /**
   * Check if user can access a lesson
   * Returns detailed result object
   */
  const canAccessLesson = (lesson, topic, allLessons, isCompletedFn) => {
    return isLessonAccessible(
      lesson,
      topic,
      userLevel.value,
      allLessons,
      isCompletedFn
    )
  }

  /**
   * Get access status for lesson (for UI display)
   */
  const getLessonAccessStatus = (lesson, topic, allLessons, isCompletedFn) => {
    const result = canAccessLesson(lesson, topic, allLessons, isCompletedFn)

    return {
      ...result,
      canAccess: result.accessible,
      isLevelRestricted: ['TOPIC_LEVEL', 'LESSON_LEVEL'].includes(result.reason),
      isSequentialLock: result.reason === 'SEQUENTIAL',
      isInactive: result.reason === 'INACTIVE',
    }
  }

  /**
   * Show appropriate warning message
   */
  const getAccessWarningMessage = (lesson, topic) => {
    if (!lesson.levelRequired && !topic.levelRequired) return null

    const requiredLevel = lesson.levelRequired || topic.levelRequired

    if (!userLevel.value) {
      return {
        type: 'warning',
        message: `Bài học này yêu cầu trình độ ${requiredLevel}. Vui lòng cập nhật trình độ của bạn.`,
      }
    }

    if (!isTopicAccessible(topic, userLevel.value)) {
      return {
        type: 'error',
        message: `Chủ đề này yêu cầu trình độ ${topic.levelRequired}. Bạn hiện tại: ${userLevel.value}`,
      }
    }

    if (lesson.levelRequired && !isLessonAccessible(lesson, topic, userLevel.value, [], () => true).accessible) {
      return {
        type: 'warning',
        message: `Bài học này yêu cầu trình độ ${lesson.levelRequired}. Bạn hiện tại: ${userLevel.value}`,
      }
    }

    return null
  }

  return {
    userLevel,
    canAccessTopic,
    canAccessLesson,
    getLessonAccessStatus,
    getAccessWarningMessage,
    getLevelLabel,
    getLevelColor,
  }
}
