// stores/aiRecommendation.js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { aiAPI } from '@/api/modules/ai.api'
import { ElMessage } from 'element-plus'

export const useAIRecommendationStore = defineStore('aiRecommendation', () => {
  // State
  const recommendations = ref([])
  const isLoading = ref(false)
  const error = ref(null)
  const metrics = ref(null)

  // Computed
  const hasRecommendations = computed(() => recommendations.value.length > 0)

  const sortedRecommendations = computed(() => {
    return [...recommendations.value].sort((a, b) => {
      // Sort by priority (high to low)
      if (b.priority !== a.priority) {
        return b.priority - a.priority
      }
      // Then by creation date (newest first)
      return new Date(b.createdAt) - new Date(a.createdAt)
    })
  })

  // Actions
  const fetchRecommendations = async () => {
    isLoading.value = true
    error.value = null

    try {
      const response = await aiAPI.getRecommendations()

      if (response.data.success) {
        recommendations.value = response.data.data || []
        console.log('✅ Recommendations fetched:', recommendations.value.length)
      } else {
        throw new Error(response.data.message || 'Failed to fetch recommendations')
      }
    } catch (err) {
      console.error('❌ Error fetching recommendations:', err)
      error.value = err.message
      recommendations.value = []
    } finally {
      isLoading.value = false
    }
  }

  const refreshRecommendations = async () => {
    isLoading.value = true
    error.value = null

    try {
      const response = await aiAPI.refreshRecommendations()

      if (response.data.success) {
        recommendations.value = response.data.data || []
        ElMessage.success(response.data.message || 'Đã làm mới gợi ý!')
        console.log('✅ Recommendations refreshed:', recommendations.value.length)
      } else {
        throw new Error(response.data.message || 'Failed to refresh recommendations')
      }
    } catch (err) {
      console.error('❌ Error refreshing recommendations:', err)
      error.value = err.message
      ElMessage.error('Không thể làm mới gợi ý')
    } finally {
      isLoading.value = false
    }
  }

  const dismissRecommendation = async (recId) => {
    try {
      await aiAPI.dismissRecommendation(recId)

      // Remove from local state
      recommendations.value = recommendations.value.filter(r => r.id !== recId)

      ElMessage.success('Đã ẩn gợi ý')
      console.log('✅ Recommendation dismissed:', recId)
    } catch (err) {
      console.error('❌ Error dismissing recommendation:', err)
      ElMessage.error('Không thể ẩn gợi ý')
      throw err
    }
  }

  const acceptRecommendation = async (recId) => {
    try {
      await aiAPI.acceptRecommendation(recId)

      // Update local state
      const rec = recommendations.value.find(r => r.id === recId)
      if (rec) {
        rec.isAccepted = true
      }

      console.log('✅ Recommendation accepted:', recId)
    } catch (err) {
      console.error('❌ Error accepting recommendation:', err)
      throw err
    }
  }

  const completeRecommendation = async (recId) => {
    try {
      await aiAPI.completeRecommendation(recId)

      // Update local state
      const rec = recommendations.value.find(r => r.id === recId)
      if (rec) {
        rec.isCompleted = true
      }

      ElMessage.success('Tuyệt vời! Bạn đã hoàn thành gợi ý này 🎉')
      console.log('✅ Recommendation completed:', recId)
    } catch (err) {
      console.error('❌ Error completing recommendation:', err)
      throw err
    }
  }

  const fetchMetrics = async () => {
    try {
      const response = await aiAPI.getMetrics()

      if (response.data.success) {
        metrics.value = response.data.data
        console.log('✅ Metrics fetched:', metrics.value)
      }
    } catch (err) {
      console.error('❌ Error fetching metrics:', err)
    }
  }

  const handleRecommendationAction = async (rec, router) => {
    try {
      // Accept recommendation
      await acceptRecommendation(rec.id)

      // Navigate based on skill type
      const routeMap = {
        GRAMMAR: `/user/grammar/lesson/${rec.targetLessonId}`,
        READING: `/user/reading/lesson/${rec.targetLessonId}`,
        LISTENING: `/user/listening/lesson/${rec.targetLessonId}`,
      }

      const targetRoute = routeMap[rec.targetSkill]

      if (targetRoute) {
        router.push(targetRoute)
      } else {
        ElMessage.warning('Không tìm thấy đường dẫn cho bài học này')
      }
    } catch (err) {
      console.error('❌ Error handling recommendation action:', err)
      ElMessage.error('Có lỗi xảy ra')
    }
  }

  return {
    // State
    recommendations,
    isLoading,
    error,
    metrics,

    // Computed
    hasRecommendations,
    sortedRecommendations,

    // Actions
    fetchRecommendations,
    refreshRecommendations,
    dismissRecommendation,
    acceptRecommendation,
    completeRecommendation,
    fetchMetrics,
    handleRecommendationAction,
  }
})
