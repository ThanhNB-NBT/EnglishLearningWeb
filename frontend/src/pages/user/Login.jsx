import React, { useState}  from "react";
import { useNavigate } from "react-router-dom";
import { authAPI } from "../../api";
import { saveAuthData } from "../../auth/authService";
import { EyeIcon, EyeSlashIcon } from '@heroicons/react/24/outline';
import { toast } from 'react-hot-toast'; // Sửa import

const Login = () => {
  const [formData, setFormData] = useState({ usernameOrEmail: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authAPI.login(formData);
      const { token, role } = res.data.data;
      if (role !== 'USER') {
        toast.error('Bạn không phải người dùng!');
        setLoading(false);
        return;
      }
      saveAuthData(token, res.data.data);
      toast.success('Đăng nhập thành công!');
      navigate('/user/dashboard');
    } catch (err) {
      console.error('Login error:', err); // Debug lỗi
      toast.error(err.response?.data?.message || 'Đăng nhập thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen w-full h-full">
      {/* Form bên trái */}
      <div className="items-center justify-center w-full md:w-1/3 bg-transparent px-4 py-12 min-h-screen flex bg-gradient-to-br from-blue-50 to-indigo-100">
        <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md">
          <div className="text-center mb-8">
            <div className="mx-auto h-16 w-16 bg-gradient-to-r from-blue-500 to-indigo-600 rounded-full flex items-center justify-center mb-4">
              <svg className="h-8 w-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.746 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
            <h2 className="text-3xl font-bold text-gray-900">Đăng Nhập</h2>
            <p className="text-gray-600 mt-2">Chào mừng bạn quay trở lại!</p>
          </div>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Tên tài khoản hoặc Email
              </label>
              <input
                type="text"
                name="usernameOrEmail"
                value={formData.usernameOrEmail}
                onChange={handleChange}
                required
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition duration-200 outline-none"
                placeholder="Nhập thông tin...."
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Mật khẩu
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  required
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 pr-12 outline-none"
                  placeholder="Nhập thông tin...."
                />
                <span
                  className="absolute inset-y-0 right-2 flex items-center cursor-pointer pr-3 text-gray-500"
                  onClick={() => setShowPassword(!showPassword)}
                  tabIndex={-1}
                >
                  {showPassword ? (
                    <span role="img" aria-label="Hide">
                      <EyeIcon className="h-4 w-4 text-dark-500" />
                    </span>
                  ) : (
                    <span role="img" aria-label="Show">
                      <EyeSlashIcon className="h-4 w-4 text-dark-500" />
                    </span>
                  )}
                </span>
              </div>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition duration-200"
            >
              {loading ? (
                <div className="flex justify-center items-center">
                  <div className="spinner"></div>
                </div>
              ) : (
                'Đăng Nhập'
              )}
            </button>
          </form>
          <div className="mt-6 text-center">
            <span className="text-gray-500">Chưa có tài khoản?</span>
            <button
              onClick={() => navigate('/user/register')}
              className="ml-2 text-blue-600 hover:underline font-medium"
            >
              Đăng ký ngay
            </button>
          </div>
        </div>
      </div>
      {/* Hình ảnh bên phải (chỉ hiện trên màn hình lớn) */}
      <div className="hidden md:block w-2/3 h-screen">
        <img
          src="/images/wallhaven-rq2je1_1920x1080.png"
          alt="Decor"
          className="h-full w-full object-cover"
        />
      </div>
    </div>
  );
};

export default Login;