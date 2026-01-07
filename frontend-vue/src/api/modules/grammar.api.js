// src/api/modules/grammar.api.js
import apiClient from '../config'

// ==================== USER APIs ====================
export const grammarUserAPI = {
  getTopics: () => apiClient.get('/api/grammar/topics'),
  getLessonsByTopic: (topicId) => apiClient.get(`/api/grammar/topics/${topicId}/lessons`),
  getLessonDetail: (lessonId) => apiClient.get(`/api/grammar/lessons/${lessonId}`),
  submitLesson: (data) => apiClient.post('/api/grammar/lessons/submit', data),
  getCompletedLessons: () => apiClient.get('/api/grammar/progress/completed'),
  getProgressSummary: () => apiClient.get('/api/grammar/progress/summary'),
}

// ==================== ADMIN APIs ====================
export const grammarAdminAPI = {
  // === Lessons ===
  /**
   * Get lessons by topic with pagination
   * Response: { status, message, data: { content: [...], page, size, ... }, timestamp, success }
   */
  getLessonsByTopic: (topicId, params = {}) => {
    const { page = 1, size = 20, sort = 'orderIndex:asc' } = params
    return apiClient.get(`/api/admin/grammar/topics/${topicId}/lessons`, {
      params: { page, size, sort },
    })
  },

  getLessonById: (id) => apiClient.get(`/api/admin/grammar/lessons/${id}`),
  createLesson: (data) => apiClient.post('/api/admin/grammar/lessons', data),
  updateLesson: (id, data) => apiClient.put(`/api/admin/grammar/lessons/${id}`, data),
  deleteLesson: (id) => apiClient.delete(`/api/admin/grammar/lessons/${id}`),
  toggleLessonStatus: (id) => apiClient.post(`/api/admin/grammar/lessons/${id}/toggle-status`),
  getNextLessonOrderIndex: (topicId) =>
    apiClient.get(`/api/admin/grammar/topics/${topicId}/lessons/next-order`),
  fixLessonOrder: (topicId) =>
    apiClient.post(`/api/admin/grammar/topics/${topicId}/lessons/fix-order`),

  // === Questions ===
  getQuestionsByLesson: (lessonId) =>
    apiClient.get(`/api/admin/grammar/lessons/${lessonId}/questions`),

  getTaskStats: (lessonId) =>
    apiClient.get(`/api/admin/grammar/lessons/${lessonId}/task-stats`),

  // Existing question CRUD
  getQuestionById: (id) => apiClient.get(`/api/admin/grammar/questions/${id}`),
  createQuestion: (lessonId, data) =>
    apiClient.post(`/api/admin/grammar/lessons/${lessonId}/questions`, data),
  updateQuestion: (id, data) => apiClient.put(`/api/admin/grammar/questions/${id}`, data),
  deleteQuestion: (id) => apiClient.delete(`/api/admin/grammar/questions/${id}`),
  bulkDeleteQuestions: (ids) =>
    apiClient.delete('/api/admin/grammar/questions/bulk', { data: ids }),
  getNextQuestionOrderIndex: (lessonId) =>
    apiClient.get(`/api/admin/grammar/lessons/${lessonId}/questions/next-order`),
  createQuestionsBulk: (lessonId, data) =>
    apiClient.post(`/api/admin/grammar/lessons/${lessonId}/questions/bulk`, data),
  fixQuestionOrder: (lessonId) =>
    apiClient.post(`/api/admin/grammar/lessons/${lessonId}/questions/fix-order`),
}
