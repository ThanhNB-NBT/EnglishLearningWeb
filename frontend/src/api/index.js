import axios from 'axios';
import { authAPI } from './authAPI';
import { userAPI } from './userAPI';
import { grammarAPI } from './grammarAPI';
import { grammarAdminAPI } from './grammarAdminAPI';

const API_BASE_URL = '/api';

// Tạo instance Axios
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor cho request để thêm token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor cho response để xử lý token hết hạn
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const user = localStorage.getItem('user');
      const role = user ? JSON.parse(user).role : null;
      console.log('Interceptor: Vai trò người dùng trước khi chuyển hướng:', role);
      console.log('Interceptor: Dữ liệu user trong localStorage:', user);
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
      window.location.href = role === 'ADMIN' ? '/admin/login' : '/user/login';
      console.log('Interceptor: Chuyển hướng đến:', role === 'ADMIN' ? '/admin/login' : '/user/login');
    }
    return Promise.reject(error);
  }
);

export { api, authAPI, userAPI, grammarAPI, grammarAdminAPI };