// src/api/modules/grammar.api.js
import api from '../config'

// ==================== USER APIs ====================
export const grammarUserAPI = {
  getTopics: () => api.get('/api/grammar/topics'),
  getLessonsByTopic: (topicId) => api.get(`/api/grammar/topics/${topicId}/lessons`),
  getLessonDetail: (lessonId) => api.get(`/api/grammar/lessons/${lessonId}`),
  submitLesson: (data) => api.post('/api/grammar/lessons/submit', data),
  getCompletedLessons: () => api.get('/api/grammar/progress/completed'),
  getProgressSummary: () => api.get('/api/grammar/progress/summary'),
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
    return api.get(`/api/admin/grammar/topics/${topicId}/lessons`, {
      params: { page, size, sort },
    })
  },

  getLessonById: (id) => api.get(`/api/admin/grammar/lessons/${id}`),
  createLesson: (data) => api.post('/api/admin/grammar/lessons', data),
  updateLesson: (id, data) => api.put(`/api/admin/grammar/lessons/${id}`, data),
  deleteLesson: (id) => api.delete(`/api/admin/grammar/lessons/${id}`),
  toggleLessonStatus: (id) => api.post(`/api/admin/grammar/lessons/${id}/toggle-status`),
  getNextLessonOrderIndex: (topicId) =>
    api.get(`/api/admin/grammar/topics/${topicId}/lessons/next-order`),
  fixLessonOrder: (topicId) =>
    api.post(`/api/admin/grammar/topics/${topicId}/lessons/fix-order`),

  // === Questions ===
  getQuestionsByLesson: (lessonId) =>
    api.get(`/api/admin/grammar/lessons/${lessonId}/questions`),

  getTaskStats: (lessonId) =>
    api.get(`/api/admin/grammar/lessons/${lessonId}/task-stats`),

  // Existing question CRUD
  getQuestionById: (id) => api.get(`/api/admin/grammar/questions/${id}`),
  createQuestion: (lessonId, data) =>
    api.post(`/api/admin/grammar/lessons/${lessonId}/questions`, data),
  updateQuestion: (id, data) => api.put(`/api/admin/grammar/questions/${id}`, data),
  deleteQuestion: (id) => api.delete(`/api/admin/grammar/questions/${id}`),
  bulkDeleteQuestions: (ids) =>
    api.delete('/api/admin/grammar/questions/bulk', { data: ids }),
  getNextQuestionOrderIndex: (lessonId) =>
    api.get(`/api/admin/grammar/lessons/${lessonId}/questions/next-order`),
  createQuestionsBulk: (lessonId, data) =>
    api.post(`/api/admin/grammar/lessons/${lessonId}/questions/bulk`, data),
  fixQuestionOrder: (lessonId) =>
    api.post(`/api/admin/grammar/lessons/${lessonId}/questions/fix-order`),
}
