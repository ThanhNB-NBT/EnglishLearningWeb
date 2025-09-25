import { api } from './index';

export const grammarAdminAPI = {
  getEndpoints: () => api.get('/admin/grammar/endpoints'),
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
};