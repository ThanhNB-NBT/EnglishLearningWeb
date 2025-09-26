import React from "react";

const AdminDashboard = () => {
  return (
    <div className="w-full flex items-center justify-center min-h-[70vh]">
      <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl p-12 text-center">
        <h1 className="text-4xl font-extrabold mb-4 text-blue-700 dark:text-blue-300">
          Chào mừng đến với trang quản trị của Website học tiếng Anh
        </h1>
        <p className="text-lg text-gray-600 dark:text-gray-300 mb-6">
          Học tiếng Anh dễ dàng, hiệu quả và thú vị mỗi ngày!
        </p>
        <button className="px-6 py-3 rounded-lg bg-gradient-to-r from-blue-500 to-indigo-600 text-white font-semibold shadow hover:scale-105 transition">
          Bắt đầu học ngay
        </button>
      </div>
    </div>
  );
};

export default AdminDashboard;