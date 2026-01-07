// src/api/modules/ai.api.js
import apiClient from '../config'

export const aiAPI = {
  /**
   * Lấy danh sách gợi ý từ AI
   * Endpoint: GET /api/recommendations/generate
   */
  getRecommendations: () => {
    return apiClient.get('/api/recommendations/generate')
  },

  /**
   * Đánh dấu gợi ý đã hoàn thành (nếu cần)
   * Endpoint: PUT /api/recommendations/{id}/complete
   */
  completeRecommendation: (id) => {
    return apiClient.put(`/api/recommendations/${id}/complete`)
  }
}
