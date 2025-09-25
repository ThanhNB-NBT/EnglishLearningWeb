import React, { useState } from "react";
import Sidebar from "../../components/Sidebar";
import Navbar from "../../components/Navbar";

const Dashboard = () => {
  const [darkMode, setDarkMode] = useState(false);

  const handleThemeToggle = () => {
    setDarkMode(!darkMode);
    document.documentElement.classList.toggle('dark', !darkMode);
  };

  return (
    <div className="flex flex-col h-screen w-full text-white bg-gradient-to-br from-blue-50 to-indigo-100 dark:bg-gray-900 transition-colors duration-300">
      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        <div className="flex-1 flex flex-col w-full">
          <Navbar darkMode={darkMode} onThemeToggle={handleThemeToggle} />
          <main className="flex-1 flex items-center justify-center w-full dark:bg-gray-900 transition-colors duration-300">
            <div className="mx-4 bg-white dark:bg-[#232b3a] rounded-2xl shadow-xl p-12 w-full max-w-2xl text-center">
              <h1 className="text-4xl font-extrabold mb-4 text-blue-700 dark:text-blue-300">
                Chào mừng đến với Website học tiếng Anh
              </h1>
              <p className="text-lg text-gray-600 dark:text-gray-300 mb-6">
                Học tiếng Anh dễ dàng, hiệu quả và thú vị mỗi ngày!
              </p>
              <button className="px-6 py-3 rounded-lg bg-gradient-to-r from-blue-500 to-indigo-600 text-white font-semibold shadow hover:scale-105 transition">
                Bắt đầu học ngay
              </button>
            </div>
          </main>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;