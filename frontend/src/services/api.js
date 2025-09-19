import axios from 'axios';

const API_BASE_URL = '/api'; // Sử dụng proxy

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
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth APIs - Dựa theo AuthController
export const authAPI = {
  // GET /api/auth/endpoints - Xem danh sách endpoints
  getEndpoints: () => api.get('/auth/endpoints'),

  // POST /api/auth/register - Đăng ký tài khoản mới
  register: (userData) => api.post('/auth/register', userData),

  // POST /api/auth/login - Đăng nhập
  login: (credentials) => api.post('/auth/login', credentials),

  // POST /api/auth/verify-email - Xác thực email
  verifyEmail: (data) => api.post('/auth/verify-email', data),

  // POST /api/auth/resend-verify-email - Gửi lại OTP xác thực email
  resendVerifyEmail: (data) => api.post('/auth/resend-verify-email', data),

  // POST /api/auth/forgot-password - Quên mật khẩu
  forgotPassword: (data) => api.post('/auth/forgot-password', data),

  // POST /api/auth/verify-reset-password - Xác thực OTP reset password
  verifyResetPassword: (data) => api.post('/auth/verify-reset-password', data),

  // POST /api/auth/logout - Đăng xuất
  logout: () => api.post('/auth/logout'),

  // POST /api/auth/logout-all - Đăng xuất tất cả thiết bị
  logoutAll: () => api.post('/auth/logout-all'),
};

// User APIs - Dựa theo UserController (giữ nguyên)
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