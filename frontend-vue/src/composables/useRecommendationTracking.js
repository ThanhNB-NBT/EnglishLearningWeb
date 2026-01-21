// src/composables/useRecommendationTracking.js

import { useAIRecommendationStore } from '@/stores/aiRecommendation'

/**
 * 🎯 Composable để track recommendation completion
 *
 * Use case: Khi user hoàn thành lesson, tự động check xem lesson đó
 * có phải là recommended lesson không. Nếu đúng → mark as completed.
 *
 * @example
 * // Trong LessonView.vue hoặc khi user complete lesson
 * const { trackLessonCompletion } = useRecommendationTracking()
 *
 * await trackLessonCompletion(lessonId, 'GRAMMAR')
 */
export function useRecommendationTracking() {
  const aiStore = useAIRecommendationStore()

  /**
   * Track lesson completion
   * Tự động tìm recommendation matching và mark as completed
   */
  const trackLessonCompletion = async (lessonId, moduleType) => {
    try {
      const matchingRec = aiStore.recommendations.find(rec =>
        rec.targetLessonId === lessonId &&
        rec.targetSkill === moduleType.toUpperCase()
      )

      if (matchingRec) {
        console.log('✅ Completed recommended lesson:', matchingRec.id)
        await aiStore.completeRecommendation(matchingRec.id)
      }
    } catch (error) {
      console.error('Error tracking recommendation:', error)
      // Không throw - tracking failure không block user
    }
  }

  /**
   * Track topic completion
   */
  const trackTopicCompletion = async (topicId, moduleType) => {
    try {
      const matchingRec = aiStore.recommendations.find(rec =>
        rec.targetTopicId === topicId &&
        rec.targetSkill === moduleType.toUpperCase()
      )

      if (matchingRec) {
        await aiStore.completeRecommendation(matchingRec.id)
      }
    } catch (error) {
      console.error('Error tracking topic completion:', error)
    }
  }

  /**
   * Check nếu lesson này được recommend
   * Dùng để hiển thị badge "AI Recommended"
   */
  const getRecommendationForLesson = (lessonId, moduleType) => {
    return aiStore.recommendations.find(rec =>
      rec.targetLessonId === lessonId &&
      rec.targetSkill === moduleType.toUpperCase()
    )
  }

  /**
   * Refresh recommendations sau mỗi 3 lessons
   */
  const maybeRefreshRecommendations = async (completedLessonCount) => {
    if (completedLessonCount % 3 === 0) {
      await aiStore.fetchRecommendations(true)
    }
  }

  return {
    trackLessonCompletion,
    trackTopicCompletion,
    getRecommendationForLesson,
    maybeRefreshRecommendations
  }
}
