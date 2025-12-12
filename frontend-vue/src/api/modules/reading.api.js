// src/api/modules/reading.api.js
import api from '../config'

// ==================== USER APIs (từ ReadingController) ====================
export const readingUserAPI = {
  /**
   * Lấy tất cả bài đọc (active) với tiến độ của user.
   * Controller backend không có phân trang cho API này.
   */
  getLessons: () => api.get('/reading/lessons'),

  /**
   * Lấy chi tiết bài đọc (nội dung, câu hỏi)
   */
  getLessonDetail: (lessonId) => api.get(`/reading/lessons/${lessonId}`),

  /**
   * Nộp bài làm
   * @param {number} lessonId ID bài đọc
   * @param {object} data Payload chứa các câu trả lời
   */
  submitLesson: (lessonId, data) => api.post(`/reading/lessons/${lessonId}/submit`, data),

  /**
   * Lấy danh sách các bài đã hoàn thành
   */
  getCompletedLessons: () => api.get('/reading/progress/completed'),

  /**
   * Lấy tóm tắt tiến độ (tổng số bài, điểm TB, v.v.)
   */
  getProgressSummary: () => api.get('/reading/progress/summary'),
}

// ==================== ADMIN APIs (từ ReadingAdminController) ====================
export const readingAdminAPI = {
  // === AI Parsing ===
  parseFile: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/api/admin/reading/lessons/parse-file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600000, // 10 phút
    })
  },
  saveParsedLesson: (parsedLesson) =>
    api.post('/api/admin/reading/lessons/save-parsed-lesson', parsedLesson),

  // === Lessons ===
  getAllLessons: (params = {}) => {
    const { page = 0, size = 10, sort = 'orderIndex,asc' } = params
    return api.get('/api/admin/reading/lessons', { params: { page, size, sort } })
  },
  getLessonDetail: (lessonId) => api.get(`/api/admin/reading/lessons/${lessonId}`),
  createLesson: (lessonData) => api.post('/api/admin/reading/lessons', lessonData),
  updateLesson: (id, lessonData) => api.put(`/api/admin/reading/lessons/${id}`, lessonData),
  deleteLesson: (id) => api.delete(`/api/admin/reading/lessons/${id}`),
  toggleLessonStatus: (lessonId) => api.post(`/api/admin/reading/lessons/${lessonId}/toggle-status`),
  // === Order ===
  getNextLessonOrderIndex: () => api.get('/api/admin/reading/lessons/next-order'),
  reorderLesson: (lessonId, newOrderIndex) =>
    api.post(`/api/admin/reading/lessons/${lessonId}/reorder`, null, {
      params: { newOrderIndex },
    }),
  swapLessons: (lessonId1, lessonId2) =>
    api.post(`/api/admin/reading/lessons/${lessonId1}/swap/${lessonId2}`),
  // === Questions ===
  getQuestionsByLesson: (lessonId, params = {}) => {
    const { page = 0, size = 10, sort = 'orderIndex,asc' } = params
    return api.get(`/api/admin/reading/lessons/${lessonId}/questions`, {
      params: { page, size, sort },
    })
  },
  getQuestionById: (questionId) => api.get(`/api/admin/reading/questions/${questionId}`),
  createQuestion: (questionData) => api.post('/api/admin/reading/questions', questionData),
  updateQuestion: (id, questionData) => api.put(`/api/admin/reading/questions/${id}`, questionData),
  deleteQuestion: (id) => api.delete(`/api/admin/reading/questions/${id}`),
  bulkDeleteQuestions: (questionIds) =>
    api.post('/api/admin/reading/questions/bulk-delete', { questionIds }),
  getNextQuestionOrderIndex: (lessonId) =>
    api.get(`/api/admin/reading/lessons/${lessonId}/questions/next-order`),
  createQuestionsInBulk: (lessonId, questions) =>
    api.post(`/api/admin/reading/lessons/${lessonId}/questions/bulk`, questions),
  copyQuestions: (sourceLessonId, targetLessonId) =>
    api.post(`/api/admin/reading/lessons/${sourceLessonId}/copy-to/${targetLessonId}`),

  // === Statistics ===
  getLessonStatistics: (lessonId) => api.get(`/api/admin/reading/lessons/${lessonId}/statistics`),
  getModuleStatistics: () => api.get('/api/admin/reading/statistics'),

  // === Validation ===
  validateAllLessonsOrder: () => api.post('/api/admin/reading/lessons/validate-all-order'),
  validateQuestionsOrder: (lessonId) =>
    api.post(`/api/admin/reading/lessons/${lessonId}/questions/validate-order`),
  validateAllQuestionsOrder: () => api.post('/api/admin/reading/questions/validate-all-order'),
  healthCheck: () => api.post('/api/admin/reading/health-check'),
}
