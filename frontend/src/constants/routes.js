// Admin Routes Constants
export const ADMIN_ROUTES = {
  // Grammar Management
  GRAMMAR_TOPICS: "/admin/grammar/topics",
  GRAMMAR_TOPIC_CREATE: "/admin/grammar/topics/create",
  GRAMMAR_TOPIC_EDIT: (id) => `/admin/grammar/topics/edit/${id}`,

  // GrammarLesson Management
  GRAMMAR_LESSONS: (topicId) => `/admin/grammar/topics/${topicId}/lessons`,
  GRAMMAR_LESSON_CREATE: (topicId) =>
    `/admin/grammar/topics/${topicId}/lessons/create`,
  GRAMMAR_LESSON_EDIT: (topicId, lessonId) =>
    `/admin/grammar/topics/${topicId}/lessons/edit/${lessonId}`,

  // GrammarQuestion Management
  GRAMMAR_QUESTIONS: (lessonId) =>
    `/admin/grammar/lessons/${lessonId}/questions`,
  GRAMMAR_QUESTION_CREATE: (lessonId) =>
    `/admin/grammar/lessons/${lessonId}/questions/create`,
  GRAMMAR_QUESTION_EDIT: (lessonId, questionId) =>
    `/admin/grammar/lessons/${lessonId}/questions/edit/${questionId}`,

  // Reading Management
  READING_LESSONS: "/admin/reading/lessons",
  READING_LESSON_CREATE: "/admin/reading/lessons/create",
  READING_LESSON_EDIT: (id) => `/admin/reading/lessons/edit/${id}`,
  READING_QUESTIONS: (lessonId) =>
    `/admin/reading/lessons/${lessonId}/questions`,
  READING_QUESTION_CREATE: (lessonId) =>
    `/admin/reading/lessons/${lessonId}/questions/create`,
  READING_QUESTION_EDIT: (lessonId, questionId) =>
    `/admin/reading/lessons/${lessonId}/questions/edit/${questionId}`,

  // Dashboard
  DASHBOARD: "/admin/dashboard",

  // Auth
  REGISTER: "/admin/register",
  LOGIN: "/admin/login",
  LOGOUT: "/admin/logout",
};

// User Routes Constants
export const USER_ROUTES = {
  HOME: "/user/home",
  LOGIN: "/user/login",
  REGISTER: "/user/register",
  PROFILE: "/user/profile",

  // Grammar
  GRAMMAR: "/user/grammar",
  GRAMMAR_TOPIC: (topicId) => `/user/grammar/topics/${topicId}`,
  GRAMMAR_LESSON: (lessonId) => `/user/grammar/lessons/${lessonId}`,

  // Reading
  READING: "/user/reading",
  READING_LESSON: (lessonId) => `/user/reading/lessons/${lessonId}`,
};

// Route Patterns (for React Router Route components)
export const ADMIN_ROUTE_PATTERNS = {
  DASHBOARD: "/admin/dashboard",
  GRAMMAR_TOPICS: "/admin/grammar/topics",
  GRAMMAR_TOPIC_CREATE: "/admin/grammar/topics/create",
  GRAMMAR_TOPIC_EDIT: "/admin/grammar/topics/edit/:id",

  GRAMMAR_LESSONS: "/admin/grammar/topics/:topicId/lessons",
  GRAMMAR_LESSON_CREATE: "/admin/grammar/topics/:topicId/lessons/create",
  GRAMMAR_LESSON_EDIT: "/admin/grammar/topics/:topicId/lessons/edit/:lessonId",

  GRAMMAR_QUESTIONS: "/admin/grammar/lessons/:lessonId/questions",
  GRAMMAR_QUESTION_CREATE: "/admin/grammar/lessons/:lessonId/questions/create",
  GRAMMAR_QUESTION_EDIT:
    "/admin/grammar/lessons/:lessonId/questions/edit/:questionId",

  READING_LESSONS: "/admin/reading/lessons",
  READING_LESSON_CREATE: "/admin/reading/lessons/create",
  READING_LESSON_EDIT: "/admin/reading/lessons/edit/:id",
  READING_QUESTIONS: "/admin/reading/lessons/:lessonId/questions",
  READING_QUESTION_CREATE: "/admin/reading/lessons/:lessonId/questions/create",
  READING_QUESTION_EDIT: "/admin/reading/lessons/:lessonId/questions/edit/:questionId",
};
