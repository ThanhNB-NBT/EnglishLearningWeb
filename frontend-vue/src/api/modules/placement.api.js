// src/api/modules/placement.api.js
import apiClient from '../config'

export const placementAPI = {
  /**
   * GET /api/user/placement-test
   * Lấy đề thi placement test
   */
  getPlacementTest: () => apiClient.get('/api/user/placement-test'),

  /**
   * POST /api/user/placement-test/submit
   * Nộp bài placement test
   * @param {Object} data - { answers: [{ questionId, selectedOptions, textAnswer }] }
   */
  submitPlacementTest: (data) => apiClient.post('/api/user/placement-test/submit', data),
}
