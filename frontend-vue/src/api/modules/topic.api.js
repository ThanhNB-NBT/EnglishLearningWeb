// src/api/modules/topic.api.js
import api from '../config'

/**
 * ✅ TOPIC MANAGEMENT APIs
 * Unified endpoint: /api/admin/topics/{moduleType}
 * Admin sees ALL, Teacher sees ASSIGNED
 */
export const topicAPI = {
  // ==================== GET TOPICS (Paginated) ====================

  /**
   * Get topics by module (with pagination)
   * Admin: See all topics
   * Teacher: See only assigned topics
   *
   * GET /api/admin/topics/{moduleType}?page=1&size=10&sort=orderIndex:asc
   */
  getTopicsByModule: (moduleType, params = {}) => {
    const { page = 1, size = 10, sort = 'orderIndex:asc' } = params
    return api.get(`/api/admin/topics/${moduleType}`, {
      params: { page, size, sort },
    })
  },

  /**
   * Shorthand methods for each module
   */
  getGrammarTopics: (params) => topicAPI.getTopicsByModule('GRAMMAR', params),
  getListeningTopics: (params) => topicAPI.getTopicsByModule('LISTENING', params),
  getReadingTopics: (params) => topicAPI.getTopicsByModule('READING', params),

  // ==================== GET TOPIC BY ID ====================

  /**
   * Get topic details by ID
   * GET /api/admin/topics/detail/{topicId}
   */
  getTopicById: (topicId) => {
    return api.get(`/api/admin/topics/detail/${topicId}`)
  },

  // ==================== CREATE TOPIC (ADMIN ONLY) ====================

  /**
   * Create new topic
   * POST /api/admin/topics/{moduleType}
   */
  createTopic: (moduleType, data) => {
    return api.post(`/api/admin/topics/${moduleType}`, data)
  },

  // ==================== UPDATE TOPIC (ADMIN ONLY) ====================

  /**
   * Update topic
   * PUT /api/admin/topics/{topicId}
   */
  updateTopic: (topicId, data) => {
    return api.put(`/api/admin/topics/${topicId}`, data)
  },

  // ==================== DELETE TOPIC (ADMIN ONLY) ====================

  /**
   * Delete topic (smart re-order)
   * DELETE /api/admin/topics/{topicId}
   */
  deleteTopic: (topicId) => {
    return api.delete(`/api/admin/topics/${topicId}`)
  },

  // ==================== TOGGLE STATUS (ADMIN ONLY) ====================

  /**
   * Toggle topic status (active/inactive)
   * PATCH /api/admin/topics/{topicId}/toggle
   */
  toggleStatus: (topicId) => {
    return api.patch(`/api/admin/topics/${topicId}/toggle`)
  },

  // ==================== FIX ORDER (ADMIN ONLY) ====================

  /**
   * Fix order indexes for all topics in module
   * POST /api/admin/topics/{moduleType}/fix-index
   */
  fixOrderIndexes: (moduleType) => {
    return api.post(`/api/admin/topics/${moduleType}/fix-index`)
  },

  // ==================== GET TOPICS FOR USER ====================
  getUserTopicsByModule: (moduleType) => {
    return api.get('/api/users/topics', { // ✅ Gọi vào UserTopicController
        params: {
            module: moduleType
        }
    })
  }
}
