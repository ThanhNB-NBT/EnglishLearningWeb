// src/api/modules/listening.api.js
import api from '../config'

// ==================== USER APIs ====================
export const listeningUserAPI = {
  getTopics: () => api.get('/api/listening/topics'),
  getLessonsByTopic: (topicId) => api.get(`/api/listening/topics/${topicId}/lessons`),
  getLessonDetail: (lessonId) => api.get(`/api/listening/lessons/${lessonId}`),
  trackPlay: (lessonId) => api.post(`/api/listening/lessons/${lessonId}/play`),
  viewTranscript: (lessonId) => api.post(`/api/listening/lessons/${lessonId}/transcript`),
  submitLesson: (lessonId, data) => api.post(`/api/listening/lessons/${lessonId}/submit`, data),
  getCompletedLessons: () => api.get('/api/listening/progress/completed'),
  getProgressSummary: () => api.get('/api/listening/progress/summary'),
}

// ==================== ADMIN APIs ====================
export const listeningAdminAPI = {
  // === Lessons ===
  /**
   * Get lessons by topic with pagination
   * Response: { status, message, data: { content: [...], page, size, ... }, timestamp, success }
   */
  getLessonsByTopic: (topicId, params = {}) => {
    const { page = 1, size = 20, sort = 'orderIndex:asc' } = params
    return api.get(`/api/admin/listening/topics/${topicId}/lessons`, {
      params: { page, size, sort },
    })
  },

  getLessonById: (lessonId) => api.get(`/api/admin/listening/lessons/${lessonId}`),

  createLesson: (formData) => {
    return api.post('/api/admin/listening/lessons', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 1200000,
    })
  },

  updateLesson: (id, formData) => {
    return api.post(`/api/admin/listening/lessons/${id}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 1200000,
    })
  },

  deleteLesson: (id) => api.delete(`/api/admin/listening/lessons/${id}`),

  toggleLessonStatus: (lessonId) =>
    api.post(`/api/admin/listening/lessons/${lessonId}/toggle-status`),

  getNextLessonOrderIndex: (topicId) =>
    api.get(`/api/admin/listening/topics/${topicId}/lessons/next-order`),

  fixLessonOrder: (topicId) =>
    api.post(`/api/admin/listening/topics/${topicId}/lessons/fix-order`),

  // === Questions ===
  getQuestionsByLesson: (lessonId) =>
    api.get(`/api/admin/listening/lessons/${lessonId}/questions`),

  getTaskStats: (lessonId) =>
    api.get(`/api/admin/listening/lessons/${lessonId}/task-stats`),

  // Existing question CRUD
  getQuestionById: (questionId) => api.get(`/api/admin/listening/questions/${questionId}`),

  createQuestion: (lessonId, questionData) =>
    api.post(`/api/admin/listening/lessons/${lessonId}/questions`, questionData),

  updateQuestion: (id, questionData) =>
    api.put(`/api/admin/listening/questions/${id}`, questionData),

  deleteQuestion: (id) => api.delete(`/api/admin/listening/questions/${id}`),

  createQuestionsBulk: (lessonId, data) =>
    api.post(`/api/admin/listening/lessons/${lessonId}/questions/bulk`, data),

  bulkDeleteQuestions: (ids) =>
    api.delete('/api/admin/listening/questions/bulk', { data: ids }),

  getNextQuestionOrderIndex: (lessonId) =>
    api.get(`/api/admin/listening/lessons/${lessonId}/questions/next-order`),

  fixQuestionOrder: (lessonId) =>
    api.post(`/api/admin/listening/lessons/${lessonId}/questions/fix-order`),
}
