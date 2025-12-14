// src/api/modules/listening.api.js
import api from '../config'

// ==================== USER APIs ====================
export const listeningUserAPI = {
  /**
   * Lấy tất cả bài listening với tiến độ của user
   */
  getLessons: () => api.get('/api/listening/lessons'),

  /**
   * Lấy chi tiết bài listening (audio, transcript, câu hỏi)
   */
  getLessonDetail: (lessonId) => api.get(`/api/listening/lessons/${lessonId}`),

  /**
   * Track play count (user click play audio)
   */
  trackPlay: (lessonId) => api.post(`/api/listening/lessons/${lessonId}/play`),

  /**
   * View transcript (unlock and mark as viewed)
   */
  viewTranscript: (lessonId) => api.post(`/api/listening/lessons/${lessonId}/transcript`),

  /**
   * Nộp bài làm
   */
  submitLesson: (lessonId, data) => api.post(`/api/listening/lessons/${lessonId}/submit`, data),

  /**
   * Lấy thống kê listening của user
   */
  getStatistics: () => api.get('/api/listening/statistics'),
}

// ==================== ADMIN APIs ====================
export const listeningAdminAPI = {
  // === Lessons ===
  getAllLessons: () => api.get('/api/admin/listening/lessons'),

  getLessonById: (lessonId) => api.get(`/api/admin/listening/lessons/${lessonId}`),

  createLesson: (formData) => {
    return api.post('/api/admin/listening/lessons', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 1200000, // 20 minutes for file upload
    })
  },

  updateLesson: (id, formData) => {
    return api.put(`/api/admin/listening/lessons/${id}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 1200000, // 20 minutes for file upload
    })
  },

  deleteLesson: (id) => api.delete(`/api/admin/listening/lessons/${id}`),

  uploadAudio: (lessonId, audioFile) => {
    const formData = new FormData()
    formData.append('audio', audioFile)
    return api.post(`/api/admin/listening/lessons/${lessonId}/audio`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    })
  },

  // === Questions ===
  getQuestionsByLesson: (lessonId, params = {}) => {
    const { page = 0, size = 1000, sort = 'orderIndex,asc' } = params
    return api.get(`/api/admin/listening/lessons/${lessonId}/questions`, {
      params: { page, size, sort },
    })
  },

  getQuestionById: (questionId) => api.get(`/api/admin/listening/questions/${questionId}`),

  createQuestion: (questionData) => api.post('/api/admin/listening/questions', questionData),

  updateQuestion: (id, questionData) => api.put(`/api/admin/listening/questions/${id}`, questionData),

  deleteQuestion: (id) => api.delete(`/api/admin/listening/questions/${id}`),

  bulkDeleteQuestions: (questionIds) =>
    api.post('/api/admin/listening/questions/bulk-delete', { questionIds }),

  createQuestionsInBulk: (lessonId, questions) =>
    api.post(`/api/admin/listening/lessons/${lessonId}/questions/bulk`, questions),

  getNextQuestionOrderIndex: (lessonId) =>
    api.get(`/api/admin/listening/lessons/${lessonId}/questions/next-order`),

  // === Statistics ===
  getLessonStatistics: (lessonId) => api.get(`/api/admin/listening/lessons/${lessonId}/statistics`),

  getModuleStatistics: () => api.get('/api/admin/listening/statistics'),
}
