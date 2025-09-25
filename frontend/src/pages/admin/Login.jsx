import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authAPI } from '../../api';
import { saveAuthData } from '../../auth/authService';
import toast from 'react-hot-toast';
import { EyeIcon, EyeSlashIcon } from '@heroicons/react/24/outline';

const Login = () => {
  const [formData, setFormData] = useState({ usernameOrEmail: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();

  const handleChange = e => setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleSubmit = async e => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authAPI.login(formData);
      const { token, role } = res.data.data;
      if (role !== 'ADMIN') {
        toast.error('Bạn không có quyền truy cập!');
        setLoading(false);
        return;
      }
      saveAuthData(token, res.data.data);
      toast.success('Đăng nhập thành công!');
      navigate('/admin/dashboard');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Đăng nhập thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex w-full h-full">
      <div className="flex items-center justify-center w-full md:w-1/3 bg-gradient-to-br from-gray-50 to-gray-200 h-screen">
        <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md">
          <div className="text-center mb-8">
            <div className="mx-auto h-16 w-16 bg-gradient-to-r from-gray-500 to-gray-700 rounded-full flex items-center justify-center mb-4">
              <svg className="h-8 w-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 11c0-1.657-1.343-3-3-3s-3 1.343-3 3m6 0c0-1.657 1.343-3 3-3s3 1.343 3 3m-6 0v2m0 4h.01M6 15h.01M18 15h.01" />
              </svg>
            </div>
            <h2 className="text-3xl font-bold text-gray-900">Đăng Nhập Quản Trị</h2>
            <p className="text-gray-600 mt-2">Chỉ dành cho quản trị viên</p>
          </div>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Username hoặc Email
              </label>
              <input
                name="usernameOrEmail"
                value={formData.usernameOrEmail}
                onChange={handleChange}
                placeholder="Username hoặc Email"
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gray-500 focus:border-gray-500 outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Mật khẩu
              </label>
              <div className="relative">
                <input
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Mật khẩu"
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gray-500 focus:border-gray-500 pr-10 outline-none"
                />
                <span
                  className="absolute inset-y-0 right-3 flex items-center cursor-pointer"
                  onClick={() => setShowPassword(!showPassword)}
                  tabIndex={-1}
                >
                  {showPassword ? (
                    <EyeIcon className="h-5 w-5 text-gray-400" />
                  ) : (
                    <EyeSlashIcon className="h-5 w-5 text-gray-400" />
                  )}
                </span>
              </div>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-gradient-to-r from-gray-500 to-gray-700 hover:from-gray-600 hover:to-gray-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 transition duration-200"
            >
              {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
            </button>
          </form>
          <div className="mt-6 text-center">
            <span className="text-gray-500">Chưa có tài khoản?</span>
            <button
              type="button"
              onClick={() => navigate('/admin/register')}
              className="ml-2 text-gray-700 hover:underline font-medium"
            >
              Đăng ký
            </button>
          </div>
        </div>
      </div>
      <div className="hidden md:block w-2/3 h-screen">
        <img
          src="/images/wallhaven-rq2je1_1920x1080.png"
          alt="Decor"
          className="w-full h-full object-cover"
        />
      </div>
    </div>
  );
};

export default Login;