import React from "react";
import { Navigate } from "react-router-dom";
import { isAuthenticated, getUserRole } from "../../utils/auth";

const ProtectedRoute = ({ allowedRoles, children }) => {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  const userRole = getUserRole();
  if (allowedRoles && !allowedRoles.includes(userRole)) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

export default ProtectedRoute;