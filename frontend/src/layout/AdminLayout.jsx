import React from "react";
import { Outlet } from "react-router-dom";
import Navbar from "../components/common/Navbar";
import Sidebar from "../components/common/Sidebar";

const AdminLayout = () => {
  return (
    <div className="flex h-screen w-full">
      <div className="flex-shrink-0">
        <Sidebar />
      </div>
      <div className="flex-1 flex flex-col min-w-0 bg-primary transition-colors duration-300">
        <Navbar />

        {/* Content */}
        <main className="flex-1 p-4 overflow-y-auto">
          <div className="max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};

export default AdminLayout;
