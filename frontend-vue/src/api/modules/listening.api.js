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
  getLessonDetail:  (lessonId) => api.get(`/api/listening/lessons/${lessonId}`),

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
   * Lấy danh sách bài đã hoàn thành
   */
  getCompletedLessons: () => api.get('/api/listening/progress/completed'),
}

// ==================== ADMIN APIs ====================
export const listeningAdminAPI = {
  // ==================== LESSON CRUD ====================

  /**
   * Lấy tất cả bài nghe (có pagination)
   */
  getAllLessons: (params = {}) => {
    const { page = 0, size = 10, sort = 'orderIndex,asc' } = params
    return api.get('/api/admin/listening/lessons', { params: { page, size, sort } })
  },

  /**
   * Lấy chi tiết bài nghe
   */
  getLessonById: (lessonId) => api.get(`/api/admin/listening/lessons/${lessonId}`),

  /**
   * Tạo bài nghe mới với audio
   */
  createLesson: (formData) => {
    return api.post('/api/admin/listening/lessons', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 1200000, // 20 minutes for file upload
    })
  },

  /**
   * Cập nhật bài nghe
   */
  updateLesson: (id, formData) => {
    return api.put(`/api/admin/listening/lessons/${id}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 1200000, // 20 minutes for file upload
    })
  },

  /**
   * Xóa bài nghe vĩnh viễn
   */
  deleteLesson: (id) => api.delete(`/api/admin/listening/lessons/${id}`),

  /**
   * Toggle lesson status (activate/deactivate)
   */
  toggleLessonStatus:  (lessonId) =>
    api.post(`/api/admin/listening/lessons/${lessonId}/toggle-status`),

  /**
   * Upload/Replace audio file
   */
  uploadAudio: (lessonId, audioFile) => {
    const formData = new FormData()
    formData.append('audio', audioFile)
    return api.post(`/api/admin/listening/lessons/${lessonId}/audio`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    })
  },

  // ==================== ORDER OPERATIONS ====================

  /**
   * Lấy orderIndex tiếp theo
   */
  getNextLessonOrderIndex: () => api.get('/api/admin/listening/lessons/next-order'),

  /**
   * Thay đổi thứ tự bài nghe
   */
  reorderLesson: (lessonId, newOrderIndex) =>
    api.post(`/api/admin/listening/lessons/${lessonId}/reorder`, null, {
      params: { newOrderIndex },
    }),

  /**
   * Swap 2 lessons
   */
  swapLessons: (lessonId1, lessonId2) =>
    api.post(`/api/admin/listening/lessons/${lessonId1}/swap/${lessonId2}`),

  // ==================== QUESTION CRUD ====================

  /**
   * Lấy câu hỏi theo lesson (có pagination)
   */
  getQuestionsByLesson: (lessonId, params = {}) => {
    const { page = 0, size = 1000, sort = 'orderIndex,asc' } = params
    return api.get(`/api/admin/listening/lessons/${lessonId}/questions`, {
      params: { page, size, sort },
    })
  },

  /**
   * Lấy chi tiết câu hỏi
   */
  getQuestionById: (questionId) => api.get(`/api/admin/listening/questions/${questionId}`),

  /**
   * Tạo câu hỏi mới
   */
  createQuestion: (questionData) => api.post('/api/admin/listening/questions', questionData),

  /**
   * Cập nhật câu hỏi
   */
  updateQuestion: (id, questionData) =>
    api.put(`/api/admin/listening/questions/${id}`, questionData),

  /**
   * Xóa câu hỏi
   */
  deleteQuestion: (id) => api.delete(`/api/admin/listening/questions/${id}`),

  /**
   * Tạo nhiều câu hỏi cùng lúc
   */
  createQuestionsInBulk: (lessonId, questions) =>
    api.post(`/api/admin/listening/lessons/${lessonId}/questions/bulk`, questions),

  /**
   * Xóa nhiều câu hỏi
   */
  bulkDeleteQuestions: (questionIds) =>
    api.post('/api/admin/listening/questions/bulk-delete', { questionIds }),

  /**
   * Lấy orderIndex tiếp theo cho question
   */
  getNextQuestionOrderIndex: (lessonId) =>
    api.get(`/api/admin/listening/lessons/${lessonId}/questions/next-order`),

  /**
   * Copy questions từ lesson khác
   */
  copyQuestions: (sourceLessonId, targetLessonId) =>
    api.post(`/api/admin/listening/lessons/${sourceLessonId}/copy-to/${targetLessonId}`),

  // ==================== VALIDATION OPERATIONS ====================

  /**
   * Validate và fix orderIndex của tất cả lessons
   */
  validateAllLessonsOrder: () => api.post('/api/admin/listening/lessons/validate-all-order'),

  /**
   * Validate orderIndex của questions trong 1 lesson
   */
  validateQuestionsOrder: (lessonId) =>
    api.post(`/api/admin/listening/lessons/${lessonId}/questions/validate-order`),

  /**
   * Validate tất cả questions orderIndex
   */
  validateAllQuestionsOrder: () => api.post('/api/admin/listening/questions/validate-all-order'),

  /**
   * Validate audio files
   */
  validateAudioFiles: () => api.post('/api/admin/listening/validate-audio-files'),

  /**
   * Health check toàn bộ module
   */
  healthCheck: () => api.post('/api/admin/listening/health-check'),
}
