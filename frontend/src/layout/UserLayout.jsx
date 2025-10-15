import React from "react";
import { Outlet } from "react-router-dom";
import UserNavbar from "../components/common/Navbar";
import Sidebar from "../components/common/Sidebar";

const UserLayout = () => {
  return (
    <div className="flex h-screen w-full">
      <div className="flex-shrink-0">
        <Sidebar />
      </div>
      <div className="flex-1 flex flex-col min-w-0 bg-primary transition-colors duration-300">
        <Navbar />

        {/* Content */}
        <main className="p-4">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default UserLayout;
