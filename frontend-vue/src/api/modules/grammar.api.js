// src/api/modules/grammar.api.js
import apiClient from '../config'

// ==================== USER APIs (từ GrammarController) ====================
export const grammarUserAPI = {
  getAccessibleTopics: () => apiClient.get('/api/grammar/topics'),
  getTopicDetails: (topicId) => apiClient.get(`/api/grammar/topics/${topicId}`),
  getLessonContent: (lessonId) => apiClient.get(`/api/grammar/lessons/${lessonId}`),
  submitLesson: (data) => apiClient.post('/api/grammar/lessons/submit', data),
  getUserProgress: () => apiClient.get('/api/grammar/progress'),
}

// ==================== ADMIN APIs (từ GrammarAdminController) ====================
export const grammarAdminAPI = {
  // === AI Parsing ===
  parseFile: (topicId, file, pages = null, parsingContext = null) => {
    const formData = new FormData()
    formData.append('file', file)

    // Build query params for pages and parsingContext
    const params = new URLSearchParams()

    // Add pages as repeated params (backend expects List<Integer>)
    if (pages && pages.length > 0) {
      pages.forEach((page) => params.append('pages', page))
    }

    // Add parsing context if provided
    if (parsingContext && parsingContext.trim()) {
      params.append('parsingContext', parsingContext.trim())
    }

    return apiClient.post(
      `/api/admin/grammar/topics/${topicId}/parse-file`,
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 600000, // 10 minutes
        params: params
      }
    )
  },

  /**
   * Save parsed lessons to database
   */
  saveParsedLessons: (topicId, parseResult) =>
    apiClient.post(`/api/admin/grammar/topics/${topicId}/save-parsed-lessons`, parseResult),

  // === Topics ===
  getAllTopics: (params = {}) => {
    const { page = 0, size = 10, sort = 'orderIndex,asc' } = params
    return apiClient.get('/api/admin/grammar/topics', { params: { page, size, sort } })
  },
  getTopicById: (id) => apiClient.get(`/api/admin/grammar/topics/${id}`),
  createTopic: (topicData) => apiClient.post('/api/admin/grammar/topics', topicData),
  updateTopic: (id, topicData) => apiClient.put(`/api/admin/grammar/topics/${id}`, topicData),
  deleteTopic: (id) => apiClient.delete(`/api/admin/grammar/topics/${id}`),
  activateTopic: (id) => apiClient.patch(`/api/admin/grammar/topics/${id}/activate`),
  deactivateTopic: (id) => apiClient.patch(`/api/admin/grammar/topics/${id}/deactivate`),
  getNextTopicOrderIndex: () => apiClient.get('/api/admin/grammar/topics/next-order'),

  // === Lessons ===
  getLessonsByTopic: (topicId, params = {}) => {
    const { page = 0, size = 10, sort = 'orderIndex,asc' } = params
    return apiClient.get(`/api/admin/grammar/topics/${topicId}/lessons`, {
      params: { page, size, sort },
    })
  },
  getLessonDetail: (lessonId) => apiClient.get(`/api/admin/grammar/lessons/${lessonId}`),
  createLesson: (lessonData) => apiClient.post('/api/admin/grammar/lessons', lessonData),
  updateLesson: (id, lessonData) => apiClient.put(`/api/admin/grammar/lessons/${id}`, lessonData),
  deleteLesson: (id, cascade = false) => {
    return apiClient.delete(`/api/admin/grammar/lessons/${id}`, { params: { cascade } })
  },
  activateLesson: (id) => apiClient.patch(`/api/admin/grammar/lessons/${id}/activate`),
  deactivateLesson: (id) => apiClient.patch(`/api/admin/grammar/lessons/${id}/deactivate`),
  getNextLessonOrderIndex: (topicId) =>
    apiClient.get(`/api/admin/grammar/topics/${topicId}/lessons/next-order`),
  reorderLessons: (
    topicId,
    reorderData, // reorderData = { insertPosition, excludeLessonId }
  ) => apiClient.post(`/api/admin/grammar/topics/${topicId}/lessons/reorder`, reorderData),

  // === Questions ===
  getQuestionsByLesson: (lessonId, params = {}) => {
    const { page = 0, size = 10, sort = 'orderIndex,asc' } = params
    return apiClient.get(`/api/admin/grammar/lessons/${lessonId}/questions`, {
      params: { page, size, sort },
    })
  },
  getQuestionById: (id) => apiClient.get(`/api/admin/grammar/questions/${id}`),
  createQuestion: (questionData) => apiClient.post('/api/admin/grammar/questions', questionData),
  updateQuestion: (id, questionData) => apiClient.put(`/api/admin/grammar/questions/${id}`, questionData),
  deleteQuestion: (id) => apiClient.delete(`/api/admin/grammar/questions/${id}`),
  bulkDeleteQuestions: (questionIds) =>
    apiClient.post('/api/admin/grammar/questions/bulk-delete', { questionIds }),
  getNextQuestionOrderIndex: (lessonId) =>
    apiClient.get(`/api/admin/grammar/lessons/${lessonId}/questions/next-order`),
  createQuestionsInBulk: (lessonId, questions) =>
    apiClient.post(`/api/admin/grammar/lessons/${lessonId}/questions/bulk`, questions),
  copyQuestions: (sourceLessonId, targetLessonId) =>
    apiClient.post(`/api/admin/grammar/lessons/${sourceLessonId}/copy-to/${targetLessonId}`),

  // === Validation Endpoints ===
  validateAllTopicsOrder: () => apiClient.post('/api/admin/grammar/topics/validate-all-order'),
  validateLessonOrder: (topicId) =>
    apiClient.post(`/api/admin/grammar/topics/${topicId}/lessons/validate-order`),
  validateAllLessonsOrder: () => apiClient.post('/admin/grammar/lessons/validate-all-order'),
  validateQuestionOrder: (lessonId) =>
    apiClient.post(`/api/admin/grammar/lessons/${lessonId}/questions/validate-order`),
  validateAllQuestionsInTopic: (topicId) =>
    apiClient.post(`/api/admin/grammar/topics/${topicId}/questions/validate-all-order`),
  validateAllQuestionsOrder: () => apiClient.post('/api/admin/grammar/questions/validate-all-order'),
}
