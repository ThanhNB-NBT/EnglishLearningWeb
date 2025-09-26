import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Card,
  Input,
  Button,
  Typography,
  Alert,
  Progress,
  Chip,
} from '@material-tailwind/react';
import {
  EnvelopeIcon,
  ClockIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon,
  ArrowPathIcon,
} from '@heroicons/react/24/outline';
import { authAPI } from '../api';
import toast, { Toaster } from 'react-hot-toast';

const VerifyEmail = () => {
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [resendTimer, setResendTimer] = useState(0);
  const [attempts, setAttempts] = useState(0);
  const maxAttempts = 3;
  const navigate = useNavigate();
  const location = useLocation();
  const email = location.state?.email || '';
  const role = location.state?.role || '';

  useEffect(() => {
    let timer;
    if (resendTimer > 0) {
      timer = setInterval(() => {
        setResendTimer(prev => prev - 1);
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [resendTimer]);

  const handleResendOtp = async () => {
    if (resendTimer > 0) return;
    setResendLoading(true);
    try {
      await authAPI.resendVerifyEmail({ email });
      toast.success('Mã OTP đã được gửi lại!', { duration: 2000 });
      setResendTimer(60);
      setAttempts(0);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Gửi lại OTP thất bại', { duration: 2000 });
    } finally {
      setResendLoading(false);
    }
  };

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

  const remainingAttempts = maxAttempts - attempts;
  const progressValue = ((maxAttempts - remainingAttempts) / maxAttempts) * 100;

  return (
    <div className="min-h-screen overflow-auto flex items-center justify-center bg-gradient-to-br from-purple-200 via-blue-100 to-indigo-200 py-12 px-4">
      <div className="w-full max-w-4xl flex flex-col md:flex-row gap-6">
        {/* Main Card (Left) */}
        <div className="flex-1 max-w-lg">
          <Card className="p-8 shadow-2xl border-0">
            <div className="text-center mb-8">
              <div className="mx-auto h-20 w-20 bg-gradient-to-r from-purple-500 to-blue-600 rounded-full flex items-center justify-center mb-6 shadow-lg">
                <EnvelopeIcon className="h-10 w-10 text-white" />
              </div>
              <Typography variant="h3" color="blue-gray" className="mb-3">
                Xác Thực Email
              </Typography>
              <Typography variant="paragraph" color="blue-gray" className="opacity-70 mb-4">
                Chúng tôi đã gửi mã xác thực 6 số đến email của bạn
              </Typography>
              <Card className="bg-blue-50 border border-blue-200 p-3">
                <Typography variant="small" color="blue-gray" className="opacity-60 mb-1">
                  Địa chỉ email
                </Typography>
                <Typography variant="paragraph" color="blue" className="font-semibold">
                  {email}
                </Typography>
              </Card>
            </div>

            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <Typography variant="small" color="blue-gray" className="font-medium">
                  Số lần thử còn lại
                </Typography>
                <Chip
                  value={`${remainingAttempts}/${maxAttempts}`}
                  size="sm"
                  color={remainingAttempts > 1 ? "blue" : remainingAttempts === 1 ? "orange" : "red"}
                  className="font-medium"
                />
              </div>
              <Progress 
                value={progressValue} 
                color={remainingAttempts > 1 ? "blue" : remainingAttempts === 1 ? "orange" : "red"}
                size="sm"
              />
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <Typography variant="small" color="blue-gray" className="mb-3 font-medium">
                  Mã xác thực (OTP) <span className="text-red-500">*</span>
                </Typography>
                <Input
                  type="text"
                  name="otp"
                  value={otp}
                  onChange={e => setOtp(e.target.value.replace(/\D/g, ''))}
                  placeholder="000000"
                  maxLength={6}
                  size="lg"
                  className="!text-center !text-2xl !font-mono !tracking-wider !border-blue-gray-200 focus:!border-purple-500"
                  labelProps={{
                    className: "before:content-none after:content-none",
                  }}
                  containerProps={{
                    className: "min-w-0"
                  }}
                  autoFocus
                />
                <Typography variant="small" color="blue-gray" className="mt-2 opacity-60">
                  Nhập mã OTP gồm 6 chữ số được gửi đến email của bạn
                </Typography>
              </div>

              <Button
                type="submit"
                size="lg"
                disabled={loading || otp.length !== 6 || attempts >= maxAttempts}
                loading={loading}
                className="w-full bg-gradient-to-r from-purple-500 to-blue-600 shadow-lg"
              >
                {loading ? (
                  <div className="flex items-center justify-center">
                    <ArrowPathIcon className="h-5 w-5 mr-2 animate-spin" />
                    Đang xác thực...
                  </div>
                ) : (
                  <div className="flex items-center justify-center">
                    <CheckCircleIcon className="h-5 w-5 mr-2" />
                    Xác thực Email
                  </div>
                )}
              </Button>
            </form>

            <div className="mt-4 pt-2 border-t border-blue-gray-100">
              <div className="text-center">
                <Typography variant="small" color="blue-gray" className="mb-4">
                  Không nhận được mã xác thực?
                </Typography>
                {resendTimer > 0 ? (
                  <div className="flex items-center justify-center space-x-2">
                    <ClockIcon className="h-4 w-4 text-orange-500" />
                    <Typography variant="small" color="orange" className="font-medium">
                      Gửi lại sau {resendTimer} giây
                    </Typography>
                  </div>
                ) : (
                  <Button
                    variant="outlined"
                    size="sm"
                    disabled={resendLoading}
                    loading={resendLoading}
                    onClick={handleResendOtp}
                    className="border-purple-500 text-purple-500 hover:bg-purple-50"
                  >
                    {resendLoading ? 'Đang gửi...' : 'Gửi lại mã OTP'}
                  </Button>
                )}
              </div>
            </div>
          </Card>
        </div>

        {/* Right Column: Warning Alert and Info Card */}
        <div className="flex-1 max-w-sm space-y-6">
          {attempts > 0 && (
            <Alert
              color="orange"
              icon={<ExclamationTriangleIcon className="h-6 w-6" />}
              className="border border-orange-200"
            >
              <Typography variant="small" className="font-medium">
                Cảnh báo: Bạn đã nhập sai {attempts} lần. 
                {remainingAttempts > 0 
                  ? ` Còn ${remainingAttempts} lần thử.` 
                  : ' Vui lòng gửi lại mã OTP mới.'
                }
              </Typography>
            </Alert>
          )}

          <Card className="p-4 bg-blue-50 border border-blue-200">
            <Typography variant="small" color="blue-gray" className="font-medium mb-2">
              Lưu ý quan trọng:
            </Typography>
            <ul className="text-sm text-blue-gray-700 space-y-1">
              <li>• Mã OTP có hiệu lực trong 10 phút</li>
              <li>• Kiểm tra cả hộp thư spam/junk</li>
              <li>• Mỗi mã OTP chỉ sử dụng được 1 lần</li>
              <li>• Tối đa {maxAttempts} lần nhập sai</li>
            </ul>
          </Card>
        </div>
      </div>
      <Toaster />
    </div>
  );
};

export default VerifyEmail;