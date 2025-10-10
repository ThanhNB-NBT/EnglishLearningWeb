import React from "react";

const Home = () => {

  return (
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
  );
};

export default Home;