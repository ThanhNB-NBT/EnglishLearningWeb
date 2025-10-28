import axios from 'axios';
import { authAPI } from './modules/auth.api';
import { userAPI } from './modules/user.api';
import { grammarUserAPI, grammarAdminAPI } from './modules/grammar.api';

const API_BASE_URL = '/api';

// Tạo instance Axios
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 600000, // 10 minutes timeout
});

// ===== HELPER: Detect role từ URL =====
const detectRoleFromUrl = (url) => {
  // Nếu URL có /admin → lấy admin token
  if (url.includes('/admin')) return 'ADMIN';
  
  // Nếu URL có /user hoặc path hiện tại là admin → lấy admin token
  if (window.location.pathname.startsWith('/admin')) return 'ADMIN';
  
  return 'USER';
};

// ===== REQUEST INTERCEPTOR: Thêm token =====
api.interceptors.request.use(
  (config) => {
    // Detect role dựa vào URL của request
    const role = detectRoleFromUrl(config.url);
    const tokenKey = role === 'ADMIN' ? 'admin_authToken' : 'user_authToken';
    const token = localStorage.getItem(tokenKey);
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => Promise.reject(error)
);

// ===== RESPONSE INTERCEPTOR: Xử lý 401 =====
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Detect role từ path hiện tại thay vì từ storage
      const isAdminPath = window.location.pathname.startsWith('/admin');
      const role = isAdminPath ? 'ADMIN' : 'USER';
      
      console.log('Interceptor: 401 Unauthorized - Role:', role);
      
      // Xóa storage của role hiện tại
      const tokenKey = role === 'ADMIN' ? 'admin_authToken' : 'user_authToken';
      const userKey = role === 'ADMIN' ? 'admin_user' : 'user_user';
      
      localStorage.removeItem(tokenKey);
      localStorage.removeItem(userKey);
      
      // Redirect đến trang login tương ứng
      const loginUrl = role === 'ADMIN' ? '/admin/login' : '/user/login';
      window.location.href = `${loginUrl}?expired=true`;
      
      console.log('Interceptor: Redirecting to:', loginUrl);
    }
    
    return Promise.reject(error);
  }
);

export { api, authAPI, userAPI, grammarUserAPI, grammarAdminAPI };