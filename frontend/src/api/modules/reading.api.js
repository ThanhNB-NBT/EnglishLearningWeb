import { api } from "../index";

// ==================== USER APIs ====================
export const readingUserAPI = {
  getLessons: (params = {}) => {
    const { page = 0, size = 10, sort = "orderIndex,asc" } = params;
    return api.get("/reading/lessons", { params: { page, size, sort } });
  },

  getLessonDetail: (lessonId) => api.get(`/reading/lessons/${lessonId}`),

  submitLesson: (lessonId, data) =>
    api.post(`/reading/lessons/${lessonId}/submit`, data),

  getCompletedLessons: () => api.get("/reading/progress/completed"),

  getProgressSummary: () => api.get("/reading/progress/summary"),
};

// ==================== ADMIN APIs ====================
export const readingAdminAPI = {
  // Lessons
  getAllLessons: (params = {}) => {
    const { page = 0, size = 6, sort = "orderIndex,asc" } = params;
    return api.get("/admin/reading/lessons", { params: { page, size, sort } });
  },

  getLessonDetail: (lessonId) => api.get(`/admin/reading/lessons/${lessonId}`),

  createLesson: (lessonData) => api.post("/admin/reading/lessons", lessonData),

  updateLesson: (id, lessonData) =>
    api.put(`/admin/reading/lessons/${id}`, lessonData),

  deleteLesson: (id) => api.delete(`/admin/reading/lessons/${id}`),

  permanentlyDeleteLesson: (id) =>
    api.delete(`/admin/reading/lessons/${id}/permanent`),

  toggleLessonStatus: (lessonId) =>
    api.post(`/admin/reading/lessons/${lessonId}/toggle-status`),

  getNextLessonOrderIndex: () => api.get("/admin/reading/lessons/next-order"),

  // Reorder
  reorderLesson: (lessonId, newOrderIndex) =>
    api.post(`/admin/reading/lessons/${lessonId}/reorder`, null, {
      params: { newOrderIndex },
    }),

  swapLessons: (lessonId1, lessonId2) =>
    api.post(`/admin/reading/lessons/${lessonId1}/swap/${lessonId2}`),

  // Questions
  getQuestionsByLesson: (lessonId, params = {}) => {
    const { page = 0, size = 10, sort = "orderIndex,asc" } = params;
    return api.get(`/admin/reading/lessons/${lessonId}/questions`, {
      params: { page, size, sort },
    });
  },

  createQuestion: (questionData) =>
    api.post("/admin/reading/questions", questionData),

  updateQuestion: (id, questionData) =>
    api.put(`/admin/reading/questions/${id}`, questionData),

  deleteQuestion: (id) => api.delete(`/admin/reading/questions/${id}`),

  bulkDeleteQuestions: (questionIds) =>
    api.post("/admin/reading/questions/bulk-delete", { questionIds }),

  getNextQuestionOrderIndex: (lessonId) =>
    api.get(`/admin/reading/lessons/${lessonId}/questions/next-order`),

  createQuestionsInBulk: (lessonId, questions) =>
    api.post(`/admin/reading/lessons/${lessonId}/questions/bulk`, questions),

  copyQuestions: (sourceLessonId, targetLessonId) =>
    api.post(
      `/admin/reading/lessons/${sourceLessonId}/copy-to/${targetLessonId}`
    ),

  // AI Features
  parseFile: (file) => {
    const formData = new FormData();
    formData.append("file", file);

    return api.post("/admin/reading/lessons/parse-file", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
      timeout: 600000,
    });
  },

  saveParsedLesson: (parsedLesson) =>
    api.post("/admin/reading/lessons/save-parsed-lesson", parsedLesson),

  // Statistics
  getLessonStatistics: (lessonId) =>
    api.get(`/admin/reading/lessons/${lessonId}/statistics`),

  getModuleStatistics: () => api.get("/admin/reading/statistics"),

  // Validation
  validateAllLessonsOrder: () =>
    api.post("/admin/reading/lessons/validate-all-order"),

  validateQuestionsOrder: (lessonId) =>
    api.post(`/admin/reading/lessons/${lessonId}/questions/validate-order`),

  validateAllQuestionsOrder: () =>
    api.post("/admin/reading/questions/validate-all-order"),

  healthCheck: () => api.post("/admin/reading/health-check"),
};

export default {
  user: readingUserAPI,
  admin: readingAdminAPI,
};
