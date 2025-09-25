import { api } from './index';

export const authAPI = {
  getEndpoints: () => api.get('/auth/endpoints'),
  register: (userData) => api.post('/auth/register', userData),
  login: (credentials) => api.post('/auth/login', credentials),
  verifyEmail: (data) => api.post('/auth/verify-email', data),
  resendVerifyEmail: (data) => api.post('/auth/resend-verify-email', data),
  forgotPassword: (data) => api.post('/auth/forgot-password', data),
  verifyResetPassword: (data) => api.post('/auth/verify-reset-password', data),
  logout: () => {
    console.log('Gọi authAPI.logout');
    return api.post('/auth/logout');
  },
  logoutAll: () => api.post('/auth/logout-all'),
};