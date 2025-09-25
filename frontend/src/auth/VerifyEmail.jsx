import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { authAPI } from '../api';
import toast, { Toaster } from 'react-hot-toast';

const VerifyEmail = () => {
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [resendTimer, setResendTimer] = useState(0);
  const [attempts, setAttempts] = useState(0);
  const maxAttempts = 3; // Đồng bộ với backend
  const navigate = useNavigate();
  const location = useLocation();
  const email = location.state?.email || '';
  const role = location.state?.role || '';

  // Xử lý gửi lại OTP
  const handleResendOtp = async () => {
    if (resendTimer > 0) return;
    setResendLoading(true);
    try {
      await authAPI.resendVerifyEmail({ email });
      toast.success('Mã OTP đã được gửi lại!', { duration: 2000 });
      setResendTimer(60); // Đặt lại thời gian chờ 60 giây
      setAttempts(0); // Reset số lần thử khi gửi lại
    } catch (err) {
      toast.error(err.response?.data?.message || 'Gửi lại OTP thất bại', { duration: 2000 });
    } finally {
      setResendLoading(false);
    }
  };

  // Timer đếm ngược
  useEffect(() => {
    let timer;
    if (resendTimer > 0) {
      timer = setInterval(() => {
        setResendTimer(prev => prev - 1);
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [resendTimer]);

  const handleSubmit = async e => {
    e.preventDefault();
    if (otp.length !== 6) {
      toast.error('Mã OTP phải có 6 ký tự', { duration: 2000 });
      return;
    }
    if (attempts >= maxAttempts) {
      toast.error('Bạn đã nhập sai OTP quá nhiều lần. Vui lòng yêu cầu gửi lại OTP mới', { duration: 2000 });
      return;
    }
    setLoading(true);
    try {
      await authAPI.verifyEmail({ email, otp });
      toast.success('Xác thực email thành công! Đang chuyển hướng...', { duration: 2000 });
      setTimeout(() => {
        navigate(role === 'ADMIN' ? '/admin/login' : '/user/login');
      }, 2000);
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Xác thực thất bại. Vui lòng kiểm tra lại mã OTP';
      setAttempts(prev => prev + 1);
      console.error('API Error Details:', err.response?.data || err.message);
      toast.error(errorMessage, { duration: 2000 });
      if (attempts + 1 >= maxAttempts) {
        toast.error('Bạn đã nhập sai OTP quá nhiều lần. Vui lòng yêu cầu gửi lại OTP mới', { duration: 2000 });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-purple-50 to-pink-100 py-12 px-4">
      <Toaster position="top-right" />
      <div className="max-w-md w-full space-y-8">
        <div className="bg-white rounded-2xl shadow-xl p-8">
          <div className="text-center mb-8">
            <div className="mx-auto h-16 w-16 bg-gradient-to-r from-purple-500 to-pink-600 rounded-full flex items-center justify-center mb-4">
              <svg className="h-8 w-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 8l7.89 3.26a2 2 0 001.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
              </svg>
            </div>
            <h2 className="text-3xl font-bold text-gray-900">Xác Thực Email</h2>
            <p className="text-gray-600 mt-2">
              Chúng tôi đã gửi mã xác thực đến email:
            </p>
            <p className="text-blue-600 font-semibold">{email}</p>
          </div>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Mã xác thực (OTP)
              </label>
              <input
                name="otp"
                value={otp}
                onChange={e => setOtp(e.target.value)}
                placeholder="Nhập mã OTP"
                required
                maxLength={6}
                className="w-full px-4 py-4 text-center text-xl font-mono border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-purple-500 outline-none tracking-widest"
                autoFocus
              />
              <p className="text-xs text-gray-500 mt-2">
                Số lần thử còn lại: {maxAttempts - attempts}. Vui lòng nhập mã OTP gồm 6 số.
              </p>
            </div>
            <button
              type="submit"
              disabled={loading || otp.length !== 6}
              className="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-gradient-to-r from-purple-500 to-pink-600 hover:from-purple-600 hover:to-pink-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 disabled:opacity-50 disabled:cursor-not-allowed transition duration-200"
            >
              {loading ? 'Đang xác thực...' : 'Xác thực'}
            </button>
          </form>
          <div className="mt-6 text-center">
            <span className="text-gray-500">Không nhận được mã?</span>
            <button
              type="button"
              disabled={resendLoading || resendTimer > 0}
              onClick={handleResendOtp}
              className="ml-2 text-purple-600 hover:underline font-medium disabled:text-gray-400 disabled:cursor-not-allowed"
            >
              {resendLoading ? 'Đang gửi...' : resendTimer > 0 ? `Gửi lại (${resendTimer}s)` : 'Gửi lại mã'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VerifyEmail;