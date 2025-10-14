import { api } from './index';

export const grammarAdminAPI = {
  getAllTopics: () => api.get('/admin/grammar/topics'),
  createTopic: (topicData) => api.post('/admin/grammar/topics', topicData),
  updateTopic: (id, topicData) => api.put(`/admin/grammar/topics/${id}`, topicData),
  deleteTopic: (id) => api.delete(`/admin/grammar/topics/${id}`),

  getLessonsByTopic: (topicId) => api.get(`/admin/grammar/topics/${topicId}/lessons`),
  createLesson: (lessonData) => api.post('/admin/grammar/lessons', lessonData),
  updateLesson: (id, lessonData) => api.put(`/admin/grammar/lessons/${id}`, lessonData),
  deleteLesson: (id) => api.delete(`/admin/grammar/lessons/${id}`),

  getQuestionsByLesson: (lessonId) => api.get(`/admin/grammar/lessons/${lessonId}/questions`),
  createQuestion: (questionData) => api.post('/admin/grammar/questions', questionData),
  updateQuestion: (id, questionData) => api.put(`/admin/grammar/questions/${id}`, questionData),
  deleteQuestion: (id) => api.delete(`/admin/grammar/questions/${id}`),

  // Parse PDF với Gemini AI
  parsePDF: (topicId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/admin/grammar/topics/${topicId}/parse-pdf`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  // Lưu trữ câu hỏi đã parse
  saveParsedLessons: (topicId, parseResult) =>
    api.post(`/admin/grammar/topics/${topicId}/save-parsed-lessons`, parseResult),

  // Lấy chi tiết lesson
  getLessonDetail: (lessonId) => api.get(`/admin/grammar/lessons/${lessonId}`),

  // Tạo nhiều question cùng lúc
  createQuestionsInBulk: (lessonId, questions) =>
    api.post(`/admin/grammar/lessons/${lessonId}/questions/bulk`, questions),

  // Copy question
  copyQuestion: (sourceLessonId, targetLessonId) =>
    api.post(`/admin/grammar/lessons/${sourceLessonId}/copy-to/${targetLessonId}`),
};