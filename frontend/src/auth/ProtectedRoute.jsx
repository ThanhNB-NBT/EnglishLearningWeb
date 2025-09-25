import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { isAuthenticated, getUserRole } from "./authService";

const ProtectedRoute = ({ allowedRoles, children }) => {
  const location = useLocation();
  const isAdminRoute = location.pathname.startsWith('/admin');
  console.log('ProtectedRoute: Current path:', location.pathname); // Log đường dẫn hiện tại
  console.log('ProtectedRoute: Is admin route:', isAdminRoute); // Log loại route

  if (!isAuthenticated()) {
    console.log('ProtectedRoute: Not authenticated, redirecting'); // Log khi không xác thực
    return <Navigate to={isAdminRoute ? "/admin/login" : "/user/login"} replace />;
  }

  const userRole = getUserRole();
  console.log('ProtectedRoute: User role:', userRole); // Log vai trò người dùng
  if (allowedRoles && !allowedRoles.includes(userRole)) {
    console.log('ProtectedRoute: Role not allowed, redirecting'); // Log khi vai trò không được phép
    return <Navigate to={isAdminRoute ? "/admin/login" : "/user/login"} replace />;
  }

  return children;
};

export default ProtectedRoute;