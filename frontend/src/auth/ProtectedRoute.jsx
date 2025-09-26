import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { isAuthenticated, getUserRole } from "./authService";

const ProtectedRoute = ({ allowedRoles, children }) => {
  const location = useLocation();
  const isAdminRoute = location.pathname.startsWith('/admin');

  if (!isAuthenticated()) {
    return <Navigate to={isAdminRoute ? "/admin/login" : "/user/login"} replace />;
  }

  const userRole = getUserRole();
  if (allowedRoles && !allowedRoles.includes(userRole)) {
    return <Navigate to={isAdminRoute ? "/admin/login" : "/user/login"} replace />;
  }

  return children;
};

export default ProtectedRoute;