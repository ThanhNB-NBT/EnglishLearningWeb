import { defineStore } from 'pinia'
import { ref } from 'vue'
import { aiAPI } from '@/api' // ✅ Import đúng kiểu named export

export const useAIRecommendationStore = defineStore('aiRecommendation', () => {
  const recommendations = ref([])
  const isLoading = ref(false)
  const error = ref(null)

  const fetchRecommendations = async () => {
    if (isLoading.value) return

    isLoading.value = true
    error.value = null

    try {
      // Backend trả về: { status: 200, message: "...", data: [...] }
      const response = await aiAPI.getRecommendations()

      // Check response theo cấu trúc CustomApiResponse của bạn
      if (response.data && response.data.success) {
        recommendations.value = response.data.data || []
      }
    } catch (err) {
      console.error('Lỗi lấy gợi ý AI:', err)
      error.value = 'Không thể kết nối với AI Mentor.'
    } finally {
      isLoading.value = false
    }
  }

  const markAsCompleted = async (id) => {
    try {
      await aiAPI.completeRecommendation(id)
      // Cập nhật UI ngay lập tức
      recommendations.value = recommendations.value.filter(r => r.id !== id)
    } catch (err) {
      console.error(err)
    }
  }

  return {
    recommendations,
    isLoading,
    error,
    fetchRecommendations,
    markAsCompleted
  }
})
