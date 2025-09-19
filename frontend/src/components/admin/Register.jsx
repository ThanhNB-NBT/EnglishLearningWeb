import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authAPI } from '../../services/api';
import { EyeIcon, EyeSlashIcon } from '@heroicons/react/24/outline';
import toast, { Toaster } from 'react-hot-toast';

// Hàm kiểm tra định dạng email
const isValidEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

const AdminRegister = () => {
  const [step, setStep] = useState(1);
  const [formData, setFormData] = useState({
    fullName: '',
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const navigate = useNavigate();

  const handleChange = e => setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleNext = e => {
    e.preventDefault();
    console.log('Checking step 1:', formData); // Debug
    if (!formData.fullName || !formData.username || !formData.email) {
      toast.error('Vui lòng nhập đầy đủ thông tin!', { duration: 2000 });
      return;
    }
    if (!isValidEmail(formData.email)) {
      toast.error('Định dạng email không hợp lệ! Vui lòng nhập lại (ví dụ: user@example.com)', { duration: 2000 });
      return;
    }
    setStep(2);
  };

  const handleSubmit = async e => {
    e.preventDefault();
    console.log('Submitting form:', formData); // Debug
    if (formData.password !== formData.confirmPassword) {
      toast.error('Mật khẩu và mật khẩu xác nhận không khớp!', { duration: 2000 });
      return;
    }
    if (formData.password.length < 8) {
      toast.error('Mật khẩu phải có ít nhất 8 ký tự!', { duration: 2000 });
      return;
    }
    setLoading(true);
    try {
      const response = await authAPI.register({
        fullName: formData.fullName,
        username: formData.username,
        email: formData.email,
        password: formData.password,
        role: 'ADMIN',
      });
      console.log('API Response:', response);
      toast.success('Đăng ký thành công! Kiểm tra email để xác thực.', { duration: 2000 });
      navigate('/verify-email', { state: { email: formData.email, role: 'ADMIN' } });
    } catch (err) {
      console.error('API Error:', err);
      // Ưu tiên thông báo tiếng Việt, nếu backend trả lỗi khác thì ghi đè
      const errorMessage = err.response?.data?.message || 'Đăng ký thất bại. Vui lòng thử lại!';
      toast.error(errorMessage, { duration: 2000 });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen w-full">
      <Toaster position="top-right" />
      {/* Form bên trái */}
      <div className="flex items-center justify-center w-full md:w-1/3 h-screen bg-gradient-to-br from-red-50 to-rose-100">
        <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md">
          <div className="text-center mb-8">
            <div className="mx-auto h-16 w-16 bg-gradient-to-r from-red-500 to-rose-600 rounded-full flex items-center justify-center mb-4">
              <svg className="h-8 w-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 11c0 3.517-1.009 6.799-2.753 9.571m-3.44-2.04l.054-.09A13.916 13.916 0 008 11a4 4 0 118 0c0 1.017-.07 2.019-.203 3m-2.118 6.844A21.88 21.88 0 0015.171 17m3.839 1.132c.645-2.266.99-4.659.99-7.132A8 8 0 008 4.07M3 15.364c.64-1.319 1-2.8 1-4.364 0-1.457.39-2.823.922-4m-1.148 4.364a6 6 0 00-.081-13.059m13.208 13.059A6 6 0 0018 8h-2.414" />
              </svg>
            </div>
            <h2 className="text-3xl font-bold text-gray-900">Đăng Ký Quản Trị Viên</h2>
            <p className="text-gray-600 mt-2">Tạo tài khoản quản trị để quản lý hệ thống</p>
          </div>
          {step === 1 ? (
            <form onSubmit={handleNext} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Họ và tên <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none"
                  placeholder="Nhập họ và tên"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Tên đăng nhập <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="username"
                  value={formData.username}
                  onChange={handleChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none"
                  placeholder="Nhập tên đăng nhập"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email <span className="text-red-500">*</span>
                </label>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500 outline-none"
                  placeholder="Nhập địa chỉ email"
                />
              </div>
              <button
                type="submit"
                className="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-gradient-to-r from-red-500 to-rose-600 hover:from-red-600 hover:to-rose-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 transition duration-200"
              >
                Tiếp tục
              </button>
            </form>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Mật khẩu <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    required
                    minLength="8"
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500 pr-12 outline-none"
                    placeholder="Nhập mật khẩu..."
                  />
                  <span
                    className="absolute inset-y-0 right-0 pr-3 flex items-center"
                    onClick={() => setShowPassword(!showPassword)}
                    tabIndex={-1}
                  >
                    {showPassword ? (
                      <EyeIcon className="h-4 w-4" />
                    ) : (
                      <EyeSlashIcon className="h-4 w-4" />
                    )}
                  </span>
                </div>
                <p className="text-xs text-red-500 mt-1">Mật khẩu tối thiểu 8 ký tự</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Xác nhận mật khẩu <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    required
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-red-500 pr-12 outline-none"
                    placeholder="Nhập lại mật khẩu"
                  />
                  <span
                    className="absolute inset-y-0 right-0 pr-3 flex items-center"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    tabIndex={-1}
                  >
                    {showConfirmPassword ? (
                      <EyeIcon className="h-4 w-4" />
                    ) : (
                      <EyeSlashIcon className="h-4 w-4" />
                    )}
                  </span>
                </div>
              </div>
              <div className="flex justify-between space-x-4">
                <button
                  type="button"
                  onClick={() => setStep(1)}
                  className="w-1/2 flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-gradient-to-r from-gray-400 to-gray-500 hover:from-gray-500 hover:to-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-400 transition duration-200"
                >
                  Quay lại
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="w-1/2 flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-gradient-to-r from-red-500 to-rose-600 hover:from-red-600 hover:to-rose-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 transition duration-200"
                >
                  {loading ? 'Đang đăng ký...' : 'Đăng Ký'}
                </button>
              </div>
            </form>
          )}
          <div className="mt-6 text-center">
            <span className="text-gray-500">Đã có tài khoản?</span>
            <button
              onClick={() => navigate('/admin/login')}
              className="ml-2 text-red-600 hover:underline font-medium"
            >
              Đăng nhập ngay
            </button>
          </div>
        </div>
      </div>
      {/* Ảnh bên phải */}
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

export default AdminRegister;