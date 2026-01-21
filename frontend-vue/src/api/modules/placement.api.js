// src/api/modules/placement.api.js
import api from '../config'

export const placementAPI = {
  /**
   * GET /api/user/placement-test
   * Lấy đề thi placement test
   */
  getPlacementTest: () => api.get('/api/user/placement-test'),

  /**
   * POST /api/user/placement-test/submit
   * Nộp bài placement test
   * @param {Object} data - { answers: [{ questionId, selectedOptions, textAnswer }] }
   */
  submitPlacementTest: (data) => api.post('/api/user/placement-test/submit', data),
}
