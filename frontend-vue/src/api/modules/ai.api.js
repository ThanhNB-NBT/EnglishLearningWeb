// src/api/modules/ai.api.js - ✅ FIXED VERSION
import api from '../config'

/**
 * ✅ AI Import API - Matches Backend Endpoints
 */
export const aiAPI = {
  // =========================================================================
  // AI IMPORT - PARSE & SAVE
  // =========================================================================

  /**
   * ✅ Parse lesson from file or instruction
   * POST /api/admin/ai-import/parse
   *
   * @param {String} moduleType - 'GRAMMAR' | 'READING' | 'LISTENING'
   * @param {Number} topicId - Required
   * @param {File} file - Optional PDF/DOCX file
   * @param {String} instruction - Optional AI instruction
   */
  parseLesson: (moduleType, topicId, file = null, instruction = null) => {
    const formData = new FormData()
    formData.append('moduleType', moduleType)
    formData.append('topicId', topicId)

    if (file) {
      formData.append('file', file)
    }

    if (instruction) {
      formData.append('instruction', instruction)
    }

    return api.post('/api/admin/ai-import/parse', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000, // 2 minutes timeout
    })
  },

  /**
   * ✅ Save parsed lesson to database
   * POST /api/admin/ai-import/save
   *
   * @param {String} moduleType - 'GRAMMAR' | 'READING' | 'LISTENING'
   * @param {Object} lessonData - Lesson DTO with questions
   * @param {File} audioFile - Optional audio file for LISTENING
   */
  saveLesson: (moduleType, lessonData, audioFile = null) => {
    const formData = new FormData()
    formData.append('moduleType', moduleType)
    formData.append('lesson', JSON.stringify(lessonData))

    if (audioFile) {
      formData.append('audioFile', audioFile)
    }

    return api.post('/api/admin/ai-import/save', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    })
  },

  // =========================================================================
  // RECOMMENDATION ENDPOINTS (unchanged)
  // =========================================================================

  getRecommendations: () => {
    return api.get('/api/recommendations')
  },

  getRecommendationById: (id) => {
    return api.get(`/api/recommendations/${id}`)
  },

  refreshRecommendations: () => {
    return api.post('/api/recommendations/refresh')
  },

  acceptRecommendation: (id) => {
    return api.post(`/api/recommendations/${id}/accept`)
  },

  completeRecommendation: (id) => {
    return api.post(`/api/recommendations/${id}/complete`)
  },

  dismissRecommendation: (id) => {
    return api.delete(`/api/recommendations/${id}`)
  },

  getMetrics: () => {
    return api.get('/api/recommendations/metrics')
  },
}

export default aiAPI
