import { api } from "../index";

// ==================== USER APIs ====================
export const grammarUserAPI = {
  getAccessibleTopics: () => api.get("/grammar/topics"),
  getTopicDetails: (topicId) => api.get(`/grammar/topics/${topicId}`),
  getLessonContent: (lessonId) => api.get(`/grammar/lessons/${lessonId}`),
  submitLesson: (data) => api.post("/grammar/lessons/submit", data),
  getUserProgress: () => api.get("/grammar/progress"),
};

// ==================== ADMIN APIs ====================
export const grammarAdminAPI = {

  // Topics
  getAllTopics: (params = {}) => {
    const { page = 0, size = 6, sort = "orderIndex,asc" } = params;
    return api.get("/admin/grammar/topics", { params: { page, size, sort } });
  },
  createTopic: (topicData) => api.post("/admin/grammar/topics", topicData),
  updateTopic: (id, topicData) =>
    api.put(`/admin/grammar/topics/${id}`, topicData),
  deleteTopic: (id) => api.delete(`/admin/grammar/topics/${id}`),
  getNextTopicOrderIndex: () => api.get("/admin/grammar/topics/next-order"),

  // Lessons
  getLessonsByTopic: (topicId, params = {}) => {
    const { page = 0, size = 6, sort = "orderIndex,asc" } = params;
    return api.get(`/admin/grammar/topics/${topicId}/lessons`, {
      params: { page, size, sort },
    });
  },
  getLessonDetail: (lessonId) => api.get(`/admin/grammar/lessons/${lessonId}`),
  createLesson: (lessonData) => api.post("/admin/grammar/lessons", lessonData),
  updateLesson: (id, lessonData) =>
    api.put(`/admin/grammar/lessons/${id}`, lessonData),
  deleteLesson: (id, params = {}) => api.delete(`/admin/grammar/lessons/${id}`, { params }),
  getNextLessonOrderIndex: (topicId) =>
    api.get(`/admin/grammar/topics/${topicId}/lessons/next-order`),

  // Reorder lessons
  reorderLessons: (topicId, reorderData) =>
    api.post(`/admin/grammar/topics/${topicId}/lessons/reorder`, reorderData),

  // Questions
  getQuestionsByLesson: (lessonId, params = {}) => {
    const { page = 0, size = 10, sort = "orderIndex,asc" } = params;
    return api.get(`/admin/grammar/lessons/${lessonId}/questions`, {
      params: { page, size, sort },
    });
  },
  createQuestion: (questionData) =>
    api.post("/admin/grammar/questions", questionData),
  updateQuestion: (id, questionData) =>
    api.put(`/admin/grammar/questions/${id}`, questionData),
  deleteQuestion: (id) => api.delete(`/admin/grammar/questions/${id}`),
  bulkDeleteQuestions: (questionIds) => api.post(`/admin/grammar/questions/bulk-delete`, { questionIds }),
  getNextQuestionOrderIndex: (lessonId) =>
    api.get(`/admin/grammar/lessons/${lessonId}/questions/next-order`),
  createQuestionsInBulk: (lessonId, questions) =>
    api.post(`/admin/grammar/lessons/${lessonId}/questions/bulk`, questions),
  copyQuestion: (sourceLessonId, targetLessonId) =>
    api.post(
      `/admin/grammar/lessons/${sourceLessonId}/copy-to/${targetLessonId}`
    ),

  // AI Features
  parseFile: (topicId, file, pages = null) => {
    const formData = new FormData();
    formData.append("file", file);

    if (pages && pages.length > 0) {
      // Backend expects List<Integer> as query param
      pages.forEach((page) => formData.append("pages", page));
    }

    return api.post(`/admin/grammar/topics/${topicId}/parse-file`, formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
      timeout: 600000, // 10 minutes timeout
    });
  },
  parsePDF: (topicId, file) => {
    return grammarAdminAPI.parseFile(topicId, file, null);
  },
  saveParsedLessons: (topicId, parseResult) =>
    api.post(
      `/admin/grammar/topics/${topicId}/save-parsed-lessons`,
      parseResult
    ),
};

// Export default for convenience
export default {
  user: grammarUserAPI,
  admin: grammarAdminAPI,
};
