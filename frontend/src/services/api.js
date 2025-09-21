import axios from 'axios';

const API_BASE_URL = '/api';

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token expiry
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const user = localStorage.getItem('user');
      const role = user ? JSON.parse(user).role : null;
      console.log('Interceptor: User role before redirect:', role); // Log vai trò
      console.log('Interceptor: LocalStorage user:', user); // Log dữ liệu user
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
      window.location.href = role === 'ADMIN' ? '/admin/login' : '/user/login';
      console.log('Interceptor: Redirecting to:', role === 'ADMIN' ? '/admin/login' : '/user/login'); // Log đích chuyển hướng
    }
    return Promise.reject(error);
  }
);

// Auth APIs
export const authAPI = {
  getEndpoints: () => api.get('/auth/endpoints'),
  register: (userData) => api.post('/auth/register', userData),
  login: (credentials) => api.post('/auth/login', credentials),
  verifyEmail: (data) => api.post('/auth/verify-email', data),
  resendVerifyEmail: (data) => api.post('/auth/resend-verify-email', data),
  forgotPassword: (data) => api.post('/auth/forgot-password', data),
  verifyResetPassword: (data) => api.post('/auth/verify-reset-password', data),
  logout: () => {
    console.log('Calling authAPI.logout'); // Log khi gọi logout API
    return api.post('/auth/logout');
  },
  logoutAll: () => api.post('/auth/logout-all'),
};

// User APIs
export const userAPI = {
  getAllUsers: () => api.get('/users'),
  getUserById: (id) => api.get(`/users/${id}`),
  getUserByUsername: (username) => api.get(`/users/username/${username}`),
  getUserByEmail: (email) => api.get(`/users/email/${email}`),
  createUser: (userData) => api.post('/users', userData),
  updateUser: (id, userData) => api.put(`/users/${id}`, userData),
  deleteUser: (id) => api.delete(`/users/${id}`),
  blockUser: (id) => api.put(`/users/${id}/block`),
  unblockUser: (id) => api.put(`/users/${id}/unblock`),
  addPoints: (id, points) => api.put(`/users/${id}/points?points=${points}`),
  getActiveUsers: () => api.get('/users/active'),
  getTopUsersByPoints: (minPoints = 0) => api.get(`/users/top-points?minPoints=${minPoints}`),
  getTopUsersByStreak: (minStreakDays = 0) => api.get(`/users/top-streak?minStreakDays=${minStreakDays}`),
  updateLastLogin: (username) => api.put(`/users/${username}/last-login`),
  updateStreakDays: (id, streakDays) => api.put(`/users/${id}/streak?streakDays=${streakDays}`),
};

export default api;