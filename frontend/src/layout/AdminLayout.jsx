import React, { useState } from "react";
import { Outlet } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import Navbar from "../components/Navbar";

const AdminLayout = () => {
  const [darkMode, setDarkMode] = useState(false);

  const handleThemeToggle = () => {
    setDarkMode(!darkMode);
    document.documentElement.classList.toggle("dark", !darkMode);
  };

  return (
    <div className={`flex h-screen w-full ${darkMode ? 'dark' : ''}`}>
      {/* Sidebar */}
      <div className="flex-shrink-0">
        <Sidebar />
      </div>
      
      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0 bg-gray-50 dark:bg-gray-900">
        {/* Navbar */}
        <div className="flex-shrink-0">
          <Navbar darkMode={darkMode} onThemeToggle={handleThemeToggle} />
        </div>
        
        {/* Content Area */}
        <main className="flex-1 overflow-y-auto p-6">
          <div className="max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};

export default AdminLayout;