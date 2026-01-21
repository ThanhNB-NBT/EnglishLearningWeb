// src/api/modules/teacher_topic.api.js
import api from '@/api/config'

/**
 * ✅ Teacher Topic API - For managing assigned topics
 * These endpoints allow teachers to view and manage ONLY their assigned topics
 */
export const teacherTopicAPI = {
  /**
   * Get topics assigned to current teacher (paginated)
   * GET /api/teacher/my-topics/paginated?moduleType=GRAMMAR&page=0&size=10
   */
  getMyTopicsPaginated: (moduleType, page = 0, size = 10, sortBy = 'orderIndex', sortDir = 'asc') => {
    return api.get('/api/teacher/my-topics/paginated', {
      params: {
        moduleType,
        page,
        size,
        sort: `${sortBy}:${sortDir}`
      }
    })
  },

  /**
   * Get all topics assigned to current teacher by module
   * GET /api/teacher/my-topics?moduleType=GRAMMAR
   */
  getMyTopicsByModule: (moduleType) => {
    return api.get('/api/teacher/my-topics', {
      params: { moduleType }
    })
  },

  /**
   * Check if teacher is assigned to a specific topic
   * GET /api/teacher/check-assignment/{topicId}
   */
  checkAssignment: (topicId) => {
    return api.get(`/api/teacher/check-assignment/${topicId}`)
  }
}

export default teacherTopicAPI
