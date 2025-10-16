import { api } from '../index';

// ==================== USER APIs ====================
export const grammarUserAPI = {
  getAccessibleTopics: () => api.get('/grammar/topics'),
  getTopicDetails: (topicId) => api.get(`/grammar/topics/${topicId}`),
  getLessonContent: (lessonId) => api.get(`/grammar/lessons/${lessonId}`),
  getPracticeQuestions: (lessonId, numberOfQuestions = 10) => 
    api.post(`/grammar/lesson/${lessonId}/practice?numberOfQuestions=${numberOfQuestions}`),
  submitLesson: (data) => api.post('/grammar/lessons/submit', data),
  getUserProgress: () => api.get('/grammar/progress'),
};

// ==================== ADMIN APIs ====================
export const grammarAdminAPI = {
  // Topics
  getAllTopics: () => api.get('/admin/grammar/topics'),
  createTopic: (topicData) => api.post('/admin/grammar/topics', topicData),
  updateTopic: (id, topicData) => api.put(`/admin/grammar/topics/${id}`, topicData),
  deleteTopic: (id) => api.delete(`/admin/grammar/topics/${id}`),

  // Lessons
  getLessonsByTopic: (topicId) => api.get(`/admin/grammar/topics/${topicId}/lessons`),
  getLessonDetail: (lessonId) => api.get(`/admin/grammar/lessons/${lessonId}`),
  createLesson: (lessonData) => api.post('/admin/grammar/lessons', lessonData),
  updateLesson: (id, lessonData) => api.put(`/admin/grammar/lessons/${id}`, lessonData),
  deleteLesson: (id) => api.delete(`/admin/grammar/lessons/${id}`),

  // Questions
  getQuestionsByLesson: (lessonId) => api.get(`/admin/grammar/lessons/${lessonId}/questions`),
  createQuestion: (questionData) => api.post('/admin/grammar/questions', questionData),
  updateQuestion: (id, questionData) => api.put(`/admin/grammar/questions/${id}`, questionData),
  deleteQuestion: (id) => api.delete(`/admin/grammar/questions/${id}`),
  createQuestionsInBulk: (lessonId, questions) =>
    api.post(`/admin/grammar/lessons/${lessonId}/questions/bulk`, questions),
  copyQuestion: (sourceLessonId, targetLessonId) =>
    api.post(`/admin/grammar/lessons/${sourceLessonId}/copy-to/${targetLessonId}`),

  // AI Features
  parsePDF: (topicId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/admin/grammar/topics/${topicId}/parse-pdf`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  saveParsedLessons: (topicId, parseResult) =>
    api.post(`/admin/grammar/topics/${topicId}/save-parsed-lessons`, parseResult),
};

// Export default for convenience
export default {
  user: grammarUserAPI,
  admin: grammarAdminAPI,
};