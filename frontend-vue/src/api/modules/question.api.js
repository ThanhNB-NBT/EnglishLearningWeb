// src/api/modules/question.api.js - ✅ FIXED VERSION
import api from '@/api/config'

/**
 * ✅ Excel Import API - Matches Backend
 */
export const questionImportApi = {
  /**
   * Download Excel template
   * GET /api/admin/questions/import/template
   */
  downloadTemplate: () => {
    return api.get('/api/admin/questions/import/template', {
      responseType: 'blob'
    })
  },

  /**
   * ✅ Parse Excel file
   * POST /api/admin/questions/import/parse-excel
   *
   * @param {File} file - Excel file
   * @param {String} parentType - 'GRAMMAR' | 'READING' | 'LISTENING'
   * @param {Number} lessonId - Lesson ID
   */
  parseExcel: (file, parentType, lessonId) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('parentType', parentType)
    formData.append('lessonId', lessonId)

    return api.post('/api/admin/questions/import/parse-excel', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    })
  },

  /**
   * ✅ Save batch questions after review
   * POST /api/admin/questions/import/save-batch
   *
   * @param {Array} questions - Array of CreateQuestionDTO
   */
  saveBatchQuestions: (questions) => {
    return api.post('/api/admin/questions/import/save-batch', questions)
  },

  exportQuestions: (parentType, lessonId, lessonTitle) => {
    return api.get('/api/admin/questions/import/export', {
      params: {
        parentType,
        lessonId,
        lessonTitle: lessonTitle || `Lesson ${lessonId}`
      },
      responseType: 'blob'
    })
  },
}

export default questionImportApi
