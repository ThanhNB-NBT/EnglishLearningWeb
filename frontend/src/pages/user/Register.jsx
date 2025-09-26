import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Card,
  Input,
  Button,
  Typography,
  IconButton,
  Stepper,
  Step,
} from '@material-tailwind/react';
import { EyeIcon, EyeSlashIcon, UserIcon, KeyIcon } from '@heroicons/react/24/outline';
import { authAPI } from '../../api';
import toast from 'react-hot-toast';

// Hàm kiểm tra định dạng email
const isValidEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

const AdminRegister = () => {
  const [activeStep, setActiveStep] = useState(0);
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

  const handleNext = () => {
    if (!formData.fullName || !formData.username || !formData.email) {
      toast.error('Vui lòng nhập đầy đủ thông tin!');
      return;
    }
    if (!isValidEmail(formData.email)) {
      toast.error('Định dạng email không hợp lệ!');
      return;
    }
    setActiveStep(1);
  };

  const handlePrev = () => {
    setActiveStep(0);
  };

  const handleSubmit = async e => {
    e.preventDefault();
    if (formData.password !== formData.confirmPassword) {
      toast.error('Mật khẩu và mật khẩu xác nhận không khớp!');
      return;
    }
    if (formData.password.length < 8) {
      toast.error('Mật khẩu phải có ít nhất 8 ký tự!');
      return;
    }
    setLoading(true);
    try {
      await authAPI.register({
        fullName: formData.fullName,
        username: formData.username,
        email: formData.email,
        password: formData.password,
        role: 'USER',
      });
      toast.success('Đăng ký thành công! Kiểm tra email để xác thực.');
      navigate('/verify-email', { state: { email: formData.email, role: 'USER' } });
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Đăng ký thất bại. Vui lòng thử lại!';
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex w-full items-center justify-center bg-gradient-to-br from-blue-600 to-cyan-200 m-0 p-0">
      <Card className="w-full max-w-md p-8 shadow-2xl">
        <div className="text-center mb-8">
          <div className="mx-auto h-16 w-16 bg-gradient-to-r from-red-500 to-pink-600 rounded-full flex items-center justify-center mb-4">
            <svg className="h-8 w-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
            </svg>
          </div>
          <Typography variant="h3" color="blue-gray" className="mb-2">
            Chào mừng bạn đến với English Learning
          </Typography>
        </div>

        {/* Stepper */}
        <div className="mb-8">
          <Stepper activeStep={activeStep}>
            <Step>
              <UserIcon className="h-5 w-5" />
            </Step>
            <Step>
              <KeyIcon className="h-5 w-5" />
            </Step>
          </Stepper>
        </div>

        {activeStep === 0 ? (
          <div className="space-y-6">
            <div>
              <Typography variant="small" color="blue-gray" className="mb-2 font-medium">
                Họ và tên <span className="text-red-500">*</span>
              </Typography>
              <Input
                type="text"
                name="fullName"
                value={formData.fullName}
                onChange={handleChange}
                placeholder="Nguyen Van A"
                size="lg"
                className="w-full placeholder:opacity-100 !border-blue-gray-200 focus:!border-red-500"
                labelProps={{
                  className: "before:content-none after:content-none",
                }}
              />
            </div>

            <div>
              <Typography variant="small" color="blue-gray" className="mb-2 font-medium">
                Tên đăng nhập <span className="text-red-500">*</span>
              </Typography>
              <Input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                placeholder="abcdxyz"
                size="lg"
                className="w-full placeholder:opacity-100 !border-blue-gray-200 focus:!border-red-500"
                labelProps={{
                  className: "before:content-none after:content-none",
                }}
              />
            </div>

            <div>
              <Typography variant="small" color="blue-gray" className="mb-2 font-medium">
                Email <span className="text-red-500">*</span>
              </Typography>
              <Input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="abcdxyz@gmail.com"
                size="lg"
                className="w-full placeholder:opacity-100 !border-blue-gray-200 focus:!border-red-500"
                labelProps={{
                  className: "before:content-none after:content-none",
                }}
              />
            </div>

            <Button
              onClick={handleNext}
              size="lg"
              className="bg-gradient-to-r from-red-500 to-pink-600 w-full"
            >
              Tiếp tục
            </Button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <Typography variant="small" color="blue-gray" className="mb-2 font-medium text-left">
                Mật khẩu <span className="text-red-500">*</span>
              </Typography>
              <div className="relative">
                <Input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="********"
                  minLength="8"
                  size="lg"
                  className="w-full placeholder:opacity-100 !border-blue-gray-200 focus:!border-red-500 !pr-12"
                  labelProps={{
                    className: "hidden",
                  }}
                />
                <IconButton
                  variant="text"
                  size="sm"
                  className="!absolute right-1 top-1"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? (
                    <EyeIcon className="h-5 w-5" />
                  ) : (
                    <EyeSlashIcon className="h-5 w-5" />
                  )}
                </IconButton>
              </div>
              <Typography variant="small" color="red" className="mt-1">
                Mật khẩu tối thiểu 8 ký tự
              </Typography>
            </div>

            <div>
              <Typography variant="small" color="blue-gray" className="mb-2 font-medium text-left">
                Xác nhận mật khẩu <span className="text-red-500">*</span>
              </Typography>
              <div className="relative">
                <Input
                  type={showConfirmPassword ? 'text' : 'password'}
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  placeholder="********"
                  size="lg"
                  className="w-full placeholder:opacity-100 !border-blue-gray-200 focus:!border-red-500"
                  labelProps={{
                    className: "hidden",
                  }}
                />
                <IconButton
                  variant="text"
                  size="sm"
                  className="!absolute right-1 top-1"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                >
                  {showConfirmPassword ? (
                    <EyeIcon className="h-5 w-5" />
                  ) : (
                    <EyeSlashIcon className="h-5 w-5" />
                  )}
                </IconButton>
              </div>
            </div>

            <div className="flex gap-4">
              <Button
                onClick={handlePrev}
                variant="outlined"
                size="lg"
                className="flex-1"
              >
                Quay lại
              </Button>
              <Button
                type="submit"
                size="lg"
                disabled={loading}
                loading={loading}
                className="bg-gradient-to-r from-red-500 to-pink-600 flex-1"
              >
                {loading ? 'Đang đăng ký...' : 'Đăng ký'}
              </Button>
            </div>
          </form>
        )}

        <div className="mt-6 text-center">
          <Typography variant="small" color="blue-gray">
            Đã có tài khoản?{' '}
            <button
              onClick={() => navigate('/user/login')}
              className="text-red-600 hover:underline font-medium"
            >
              Đăng nhập ngay
            </button>
          </Typography>
        </div>
      </Card>
    </div>
  );
};

export default AdminRegister;