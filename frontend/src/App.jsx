import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import UserLogin from './components/user/Login';
import UserRegister from './components/user/Register';
import UserDashboard from './components/user/Dashboard';
import VerifyEmail from './components/auth/VerifyEmail';
import AdminLogin from './components/admin/Login';
import AdminRegister from './components/admin/Register';
import AdminDashboard from './components/admin/Dashboard';
import { Toaster} from 'react-hot-toast';

function App() {
  return (
    <Router>
      <Toaster
        position="top-right" 
        toastOptions={{
          duration: 2000, // Đặt thời gian mặc định là 2 giây
        }}
      />
      <Routes>
        <Route path="/user/login" element={<UserLogin />} />
        <Route path="/user/register" element={<UserRegister />} />
        <Route path="/user/dashboard" element={<UserDashboard />} />
        <Route path="/verify-email" element={<VerifyEmail />} />
        <Route path="/admin/login" element={<AdminLogin />} />
        <Route path="/admin/register" element={<AdminRegister />} />
        <Route path="/admin/dashboard" element={<AdminDashboard />} />
        {/* Thêm các route khác ở đây */}
        <Route
          path="/"
          element={
            <div className="p-4">
              <h1 className="text-3xl font-bold text-blue-600">English Learning</h1>
              <button className="bg-blue-500 text-white px-4 py-2 rounded">
                Start Learning
              </button>
            </div>
          }
        />
      </Routes>
    </Router>
  );
}

export default App;