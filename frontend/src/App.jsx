import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import ProtectedRoute from './auth/ProtectedRoute';

// Import route constants
import { USER_ROUTES, ADMIN_ROUTE_PATTERNS } from './constants/routes';

// ===== USER PAGES =====
import UserLogin from './pages/user/auth/Login';
import UserRegister from './pages/user/auth/Register';
import UserHome from './pages/user/Home';
import UserLayout from './layout/UserLayout';
import GrammarLearning from './pages/user/grammar/GrammarLearning';
import ReadingLearning from './pages/user/reading/ReadingLearning';

// ===== ADMIN PAGES =====
import AdminLogin from './pages/admin/auth/Login';
import AdminRegister from './pages/admin/auth/Register';
import AdminDashboard from './pages/admin/Dashboard';
import AdminLayout from './layout/AdminLayout';

// ===== ADMIN GRAMMAR PAGES =====
import AdminGrammarTopicList from './pages/admin/grammar/topics/GrammarTopicList';
import AdminGrammarTopicForm from './pages/admin/grammar/topics/GrammarTopicForm';
import AdminGrammarLessonList from './pages/admin/grammar/lessons/GrammarLessonList';
import AdminGrammarLessonForm from './pages/admin/grammar/lessons/GrammarLessonForm';
import AdminGrammarQuestionList from './pages/admin/grammar/questions/GrammarQuestionList';
import AdminGrammarQuestionForm from './pages/admin/grammar/questions/GrammarQuestionForm';

// ===== ADMIN READING PAGES =====
import AdminReadingLessonList from './pages/admin/reading/lessons/ReadingLessonList';
import AdminReadingLessonForm from './pages/admin/reading/lessons/ReadingLessonForm';
import AdminReadingQuestionList from './pages/admin/reading/questions/ReadingQuestionList';
import AdminReadingQuestionForm from './pages/admin/reading/questions/ReadingQuestionForm';

// ===== COMMON PAGES =====
import VerifyEmail from './auth/VerifyEmail';

/**
 * Main App Component
 * - User routes: /user/*
 * - Admin routes: /admin/*
 * - Common routes: /verify-email, etc.
 */
const App = () => {
  return (
    <BrowserRouter>
      {/* Toast Notifications */}
      <Toaster 
        position="top-right" 
        toastOptions={{ 
          duration: 3000,
          style: {
            background: '#363636',
            color: '#fff',
          },
          success: {
            duration: 3000,
            iconTheme: {
              primary: '#4ade80',
              secondary: '#fff',
            },
          },
          error: {
            duration: 4000,
            iconTheme: {
              primary: '#ef4444',
              secondary: '#fff',
            },
          },
        }} 
      />

      <Routes>
        {/* ===== ROOT REDIRECT ===== */}
        <Route path="/" element={<Navigate to={USER_ROUTES.LOGIN} replace />} />

        {/* ===== USER ROUTES ===== */}
        <Route path={USER_ROUTES.LOGIN} element={<UserLogin />} />
        <Route path={USER_ROUTES.REGISTER} element={<UserRegister />} />
        
        {/* Protected User Routes */}
        <Route
          element={
            <ProtectedRoute allowedRoles={['USER']}>
              <UserLayout />
            </ProtectedRoute>
          }
        >
          <Route path={USER_ROUTES.HOME} element={<UserHome />} />
          {/* Add more user routes here */}
          <Route path={USER_ROUTES.GRAMMAR} element={<GrammarLearning />} />
          <Route path={USER_ROUTES.READING} element={<ReadingLearning />} />
        </Route>

        {/* ===== ADMIN ROUTES ===== */}
        <Route path="/admin/login" element={<AdminLogin />} />
        <Route path="/admin/register" element={<AdminRegister />} />

        {/* Protected Admin Routes */}
        <Route
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminLayout />
            </ProtectedRoute>
          }
        >
          {/* Dashboard */}
          <Route path={ADMIN_ROUTE_PATTERNS.DASHBOARD} element={<AdminDashboard />} />

          {/* Grammar Topics */}
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_TOPICS} element={<AdminGrammarTopicList />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_TOPIC_CREATE} element={<AdminGrammarTopicForm />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_TOPIC_EDIT} element={<AdminGrammarTopicForm />} />

          {/* Grammar Lessons */}
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_LESSONS} element={<AdminGrammarLessonList />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_LESSON_CREATE} element={<AdminGrammarLessonForm />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_LESSON_EDIT} element={<AdminGrammarLessonForm />} />

          {/* Grammar Questions */}
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_QUESTIONS} element={<AdminGrammarQuestionList />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_QUESTION_CREATE} element={<AdminGrammarQuestionForm />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_QUESTION_EDIT} element={<AdminGrammarQuestionForm />} />

          {/* Reading Lessons */}
          <Route path={ADMIN_ROUTE_PATTERNS.READING_LESSONS} element={<AdminReadingLessonList />} />
          <Route path={ADMIN_ROUTE_PATTERNS.READING_LESSON_CREATE} element={<AdminReadingLessonForm />} />
          <Route path={ADMIN_ROUTE_PATTERNS.READING_LESSON_EDIT} element={<AdminReadingLessonForm/>} />
          
          {/* READING QUESTIONS */}
          <Route path={ADMIN_ROUTE_PATTERNS.READING_QUESTIONS} element={<AdminReadingQuestionList />} />
          <Route path={ADMIN_ROUTE_PATTERNS.READING_QUESTION_CREATE} element={<AdminReadingQuestionForm />} />
          <Route path={ADMIN_ROUTE_PATTERNS.READING_QUESTION_EDIT} element={<AdminReadingQuestionForm />} />
        </Route>

        {/* ===== COMMON ROUTES ===== */}
        <Route path="/verify-email" element={<VerifyEmail />} />

        {/* ===== 404 NOT FOUND ===== */}
        <Route path="*" element={<Navigate to={USER_ROUTES.LOGIN} replace />} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;