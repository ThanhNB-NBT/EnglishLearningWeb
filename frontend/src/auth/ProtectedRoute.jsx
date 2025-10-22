import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { isAuthenticated, getUserRole, isTokenExpired } from "../services/authService";

const ProtectedRoute = ({ allowedRoles, children }) => {
  const location = useLocation();
  const isAdminRoute = location.pathname.startsWith('/admin');

  // 1. Check xem có authenticated không
  if (!isAuthenticated()) {
    console.log('ProtectedRoute: Not authenticated, redirecting to login');
    return <Navigate to={isAdminRoute ? "/admin/login" : "/user/login"} replace />;
  }

  // 2. Check token có hết hạn không (double check)
  if (isTokenExpired()) {
    console.log('ProtectedRoute: Token expired, redirecting to login');
    return <Navigate to={isAdminRoute ? "/admin/login?expired=true" : "/user/login?expired=true"} replace />;
  }

  // 3. Check role có match không
  const userRole = getUserRole();
  if (allowedRoles && !allowedRoles.includes(userRole)) {
    console.log('ProtectedRoute: Role mismatch', { userRole, allowedRoles });
    
    // Nếu user vào admin route → redirect về user
    // Nếu admin vào user route → redirect về admin
    if (userRole === 'ADMIN') {
      return <Navigate to="/admin/dashboard" replace />;
    } else {
      return <Navigate to="/user/home" replace />;
    }
  }

  // 4. All checks passed → render children
  return children;
};

export default ProtectedRoute;