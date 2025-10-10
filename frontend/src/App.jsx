import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import ProtectedRoute from './auth/ProtectedRoute';

// Import route constants
import { USER_ROUTES, ADMIN_ROUTE_PATTERNS } from './constants/routes';

import UserLogin from './pages/user/Login';
import UserRegister from './pages/user/Register';
import UserHome from './pages/user/Home';
import UserLayout from './layout/UserLayout';

import AdminLogin from './pages/admin/Login';
import AdminRegister from './pages/admin/Register';
import AdminDashboard from './pages/admin/Dashboard';

import VerifyEmail from './auth/VerifyEmail';

// Admin Grammar
import AdminGrammarTopicList from './pages/admin/grammar/GrammarTopicList';
import AdminGrammarTopicCreate from './pages/admin/grammar/GrammarTopicCreate';
import AdminGrammarTopicEdit from './pages/admin/grammar/GrammarTopicEdit';
import AdminGrammarLessonList from './pages/admin/grammar/GrammarLessonList';
import AdminGrammarLessonForm from './pages/admin/grammar/GrammarLessonForm';
import AdminGrammarQuestionList from './pages/admin/grammar/GrammarQuestionList';
import AdminGrammarQuestionForm from './pages/admin/grammar/GrammarQuestionForm'; 
import AdminLayout from './layout/AdminLayout';

const App = () => {
  return (
    <BrowserRouter>
      <Toaster position="top-right" toastOptions={{ duration: 2000 }} />
      <Routes>
        {/* USER */}
        <Route path={USER_ROUTES.LOGIN} element={<UserLogin />} />
        <Route path={USER_ROUTES.REGISTER} element={<UserRegister />} />
        <Route
          path={USER_ROUTES.HOME}
          element={<ProtectedRoute allowedRoles={['USER']}><UserLayout /></ProtectedRoute>}
        >
          <Route path={USER_ROUTES.HOME} element={<UserHome />} />
          {/* Add more user routes here */}
        </Route>

        {/* ADMIN */}
        <Route path={USER_ROUTES.LOGIN.replace('user', 'admin')} element={<AdminLogin />} />
        <Route path={USER_ROUTES.REGISTER.replace('user', 'admin')} element={<AdminRegister />} />
        <Route
          element={<ProtectedRoute allowedRoles={['ADMIN']}><AdminLayout /></ProtectedRoute>}
        >
          <Route path={ADMIN_ROUTE_PATTERNS.DASHBOARD} element={<AdminDashboard />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_TOPICS} element={<AdminGrammarTopicList />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_TOPIC_CREATE} element={<AdminGrammarTopicCreate />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_TOPIC_EDIT} element={<AdminGrammarTopicEdit />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_LESSONS} element={<AdminGrammarLessonList />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_LESSON_CREATE} element={<AdminGrammarLessonForm />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_LESSON_EDIT} element={<AdminGrammarLessonForm />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_QUESTIONS} element={<AdminGrammarQuestionList />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_QUESTION_CREATE} element={<AdminGrammarQuestionForm />} />
          <Route path={ADMIN_ROUTE_PATTERNS.GRAMMAR_QUESTION_EDIT} element={<AdminGrammarQuestionForm />} />
        </Route>

        {/* COMMON */}
        <Route path="/verify-email" element={<VerifyEmail />} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;