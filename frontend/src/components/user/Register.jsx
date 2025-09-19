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

const Register = () => {
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
        role: 'USER',
      });
      console.log('API Response:', response);
      toast.success('Đăng ký thành công! Kiểm tra email để xác thực.', { duration: 2000 });
      navigate('/verify-email', { state: { email: formData.email, role: 'USER' } });
    } catch (err) {
      console.error('API Error:', err);
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
      <div className="flex items-center justify-center w-full md:w-1/3 h-screen bg-gradient-to-br from-green-50 to-emerald-100">
        <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-md">
          <div className="text-center mb-8">
            <div className="mx-auto h-16 w-16 bg-gradient-to-r from-green-500 to-emerald-600 rounded-full flex items-center justify-center mb-4">
              <svg className="h-8 w-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
              </svg>
            </div>
            <h2 className="text-3xl font-bold text-gray-900">Đăng Ký Người Dùng</h2>
            <p className="text-gray-600 mt-2">Tạo tài khoản người dùng để bắt đầu học</p>
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
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 outline-none"
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
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 outline-none"
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
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 outline-none"
                  placeholder="Nhập địa chỉ email"
                />
              </div>
              <button
                type="submit"
                className="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 transition duration-200"
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
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 pr-12 outline-none"
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
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 pr-12 outline-none"
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
                  className="w-1/2 flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 transition duration-200"
                >
                  {loading ? 'Đang đăng ký...' : 'Đăng Ký'}
                </button>
              </div>
            </form>
          )}
          <div className="mt-6 text-center">
            <span className="text-gray-500">Đã có tài khoản?</span>
            <button
              onClick={() => navigate('/user/login')}
              className="ml-2 text-green-600 hover:underline font-medium"
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

export default Register;