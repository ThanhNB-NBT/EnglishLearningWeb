import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ADMIN_ROUTES } from '../../../constants/routes';
import {
  Card,
  Input,
  Button,
  Typography,
  IconButton,
} from '@material-tailwind/react';
import { EyeIcon, EyeSlashIcon } from '@heroicons/react/24/outline';
import { authAPI } from '../../../api/modules/auth.api';
import { saveAuthData } from '../../../services/authService';
import toast from 'react-hot-toast';

const AdminLogin = () => {
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
      navigate(ADMIN_ROUTES.DASHBOARD);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Đăng nhập thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center 
                    bg-gradient-to-r from-[#dfe2fe] via-[#b1cbfa] to-[#8e98f5] m-0 p-0">
      <Card className="max-w-md w-full mx-4 p-8 shadow-2xl">
        <div className="text-center mb-8">
          <div className="mx-auto h-16 w-16 bg-gradient-to-r from-light-blue-500 to-light-blue-700 rounded-full flex items-center justify-center mb-4">
            <svg className="h-8 w-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.746 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <Typography variant="h3" color="blue-gray" className="mb-2">
            Đăng Nhập Admin
          </Typography>
          <Typography variant="paragraph" color="blue-gray" className="opacity-70">
            Website học tiếng Anh trực tuyến
          </Typography>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <Typography variant="small" color="blue-gray" className="mb-2 font-medium text-left">
              Username hoặc Email
            </Typography>
            <Input
              type="text"
              name="usernameOrEmail"
              value={formData.usernameOrEmail}
              onChange={handleChange}
              placeholder="name@example.com"
              size="lg"
              className="w-full placeholder:opacity-100 !border-blue-gray-200 focus:!border-blue-gray-500"
              labelProps={{
                className: "before:content-none after:content-none",
              }}
            />
          </div>

          <div>
            <Typography variant="small" color="blue-gray" className="mb-2 font-medium text-left">
              Mật khẩu
            </Typography>
            <Input
              type={showPassword ? 'text' : 'password'}
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="********"
              size="lg"
              className="w-full placeholder:opacity-100 !border-blue-gray-200 focus:!border-blue-gray-500"
              labelProps={{
                className: "hidden",
              }}
              icon={
                <IconButton
                  variant="text"
                  size="sm"
                  className="!absolute"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? (
                    <EyeIcon className="h-5 w-5" />
                  ) : (
                    <EyeSlashIcon className="h-5 w-5" />
                  )}
                </IconButton>
              }
            />
          </div>

          <Button
            type="submit"
            size="lg"
            disabled={loading}
            className="bg-gradient-to-r from-light-blue-500 to-light-blue-700 w-full"
            loading={loading}
          >
            {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </Button>
        </form>

        <div className="mt-6 text-center">
          <Typography variant="small" color="blue-gray">
            Chưa có tài khoản?{' '}
            <button
              type="button"
              onClick={() => navigate(ADMIN_ROUTES.REGISTER)}
              className="text-blue-gray-700 hover:underline font-medium"
            >
              Đăng ký
            </button>
          </Typography>
        </div>
      </Card>
    </div>
  );
};

export default AdminLogin;