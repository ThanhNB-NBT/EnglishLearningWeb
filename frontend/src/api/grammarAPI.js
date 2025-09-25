import { api } from './index';

export const grammarAPI = {
  getAccessibleTopics: () => api.get('/grammar/topics'),
  getTopicDetails: (topicId) => api.get(`/grammar/topics/${topicId}`),
  getLessonContent: (lessonId) => api.get(`/grammar/lessons/${lessonId}`),
  getPracticeQuestions: (lessonId, numberOfQuestions = 10) => 
    api.post(`/grammar/lesson/${lessonId}/practice?numberOfQuestions=${numberOfQuestions}`),
  submitLesson: (data) => api.post('/grammar/lessons/submit', data),
  getUserProgress: () => api.get('/grammar/progress'),
};