import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import ProtectedRoute from './auth/ProtectedRoute';

import Login from './pages/user/Login';
import Register from './pages/user/Register';
import Dashboard from './pages/user/Dashboard';

import AdminLogin from './pages/admin/Login';
import AdminRegister from './pages/admin/Register';
import AdminDashboard from './pages/admin/Dashboard';

import VerifyEmail from './auth/VerifyEmail';

// Admin Grammar
import AdminGrammarTopicList from './pages/admin/grammar/GrammarTopicList';
import AdminGrammarTopicCreate from './pages/admin/grammar/GrammarTopicCreate';
import AdminGrammarTopicEdit from './pages/admin/grammar/GrammarTopicEdit';
import AdminLayout from './layout/AdminLayout';

const App = () => {
  return (
    <BrowserRouter>
      <Toaster position="top-right" toastOptions={{ duration: 2000 }} />
      <Routes>
        {/* USER */}
        <Route path="/user/login" element={<Login />} />
        <Route path="/user/register" element={<Register />} />
        <Route
          path="/user/dashboard"
          element={<ProtectedRoute allowedRoles={['USER']}><Dashboard /></ProtectedRoute>}
        />

        {/* ADMIN */}
        <Route path="/admin/login" element={<AdminLogin />} />
        <Route path="/admin/register" element={<AdminRegister />} />
        <Route
          element={<ProtectedRoute allowedRoles={['ADMIN']}><AdminLayout /></ProtectedRoute>}
        >
          <Route path="/admin/dashboard" element={<AdminDashboard />} />
          <Route path="/admin/grammar" element={<AdminGrammarTopicList />} />
          <Route path="/admin/grammar/create" element={<AdminGrammarTopicCreate />} />
          <Route path="/admin/grammar/edit/:id" element={<AdminGrammarTopicEdit />} />
        </Route>

        {/* COMMON */}
        <Route path="/verify-email" element={<VerifyEmail />} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;