// src/api/modules/reading.api.js
import api from '../config'

// ==================== USER APIs ====================
export const readingUserAPI = {
  getTopics: () => api.get('/api/reading/topics'),
  getLessonsByTopic: (topicId) => api.get(`/api/reading/topics/${topicId}/lessons`),
  getLessonDetail: (lessonId) => api.get(`/api/reading/lessons/${lessonId}`),
  submitLesson: (lessonId, data) => api.post(`/api/reading/lessons/${lessonId}/submit`, data),
  getCompletedLessons: () => api.get('/api/reading/progress/completed'),
  getProgressSummary: () => api.get('/api/reading/progress/summary'),
}

// ==================== ADMIN APIs ====================
export const readingAdminAPI = {
  // === Lessons ===
  /**
   * Get lessons by topic with pagination
   * Response: { status, message, data: { content: [...], page, size, ... }, timestamp, success }
   */
  getLessonsByTopic: (topicId, params = {}) => {
    const { page = 1, size = 20, sort = 'orderIndex:asc' } = params
    return api.get(`/api/admin/reading/topics/${topicId}/lessons`, {
      params: { page, size, sort },
    })
  },

  getLessonDetail: (lessonId) => api.get(`/api/admin/reading/lessons/${lessonId}`),

  createLesson: (lessonData) => api.post('/api/admin/reading/lessons', lessonData),

  updateLesson: (id, lessonData) => api.put(`/api/admin/reading/lessons/${id}`, lessonData),

  deleteLesson: (id) => api.delete(`/api/admin/reading/lessons/${id}`),

  toggleLessonStatus: (lessonId) =>
    api.post(`/api/admin/reading/lessons/${lessonId}/toggle-status`),

  getNextLessonOrderIndex: (topicId) =>
    api.get(`/api/admin/reading/topics/${topicId}/lessons/next-order`),

  fixLessonOrder: (topicId) =>
    api.post(`/api/admin/reading/topics/${topicId}/lessons/fix-order`),

  // === Questions ===

  getQuestionsByLesson: (lessonId) =>
    api.get(`/api/admin/reading/lessons/${lessonId}/questions`),

  getTaskStats: (lessonId) =>
    api.get(`/api/admin/reading/lessons/${lessonId}/task-stats`),

  // Existing question CRUD
  getQuestionById: (questionId) => api.get(`/api/admin/reading/questions/${questionId}`),

  createQuestion: (lessonId, questionData) =>
    api.post(`/api/admin/reading/lessons/${lessonId}/questions`, questionData),

  updateQuestion: (id, questionData) => api.put(`/api/admin/reading/questions/${id}`, questionData),

  deleteQuestion: (id) => api.delete(`/api/admin/reading/questions/${id}`),

  bulkDeleteQuestions: (ids) =>
    api.delete('/api/admin/reading/questions/bulk', { data: ids }),

  getNextQuestionOrderIndex: (lessonId) =>
    api.get(`/api/admin/reading/lessons/${lessonId}/questions/next-order`),

  createQuestionsBulk: (lessonId, data) =>
    api.post(`/api/admin/reading/lessons/${lessonId}/questions/bulk`, data),

  fixQuestionOrder: (lessonId) =>
    api.post(`/api/admin/reading/lessons/${lessonId}/questions/fix-order`),
}
