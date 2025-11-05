import React from "react";
import { Outlet, useLocation } from "react-router-dom";
import Navbar from "../components/common/Navbar";
import Sidebar from "../components/common/Sidebar";

const UserLayout = () => {
  const location = useLocation();
  const isGrammarPage = location.pathname.includes('/grammar');
  const isReadingPage = location.pathname.includes('/reading'); // ✅ ADD

  return (
    <div className="flex h-screen w-full overflow-hidden">
      {/* Left Sidebar */}
      <div className="flex-shrink-0">
        <Sidebar />
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0 bg-primary">
        <Navbar />

        {/* Content - Full width for grammar and reading pages */}
        <main className={`flex-1 overflow-hidden ${
          isGrammarPage || isReadingPage ? '' : 'p-4 overflow-y-auto' // ✅ CHANGE
        }`}>
          {isGrammarPage || isReadingPage ? ( // ✅ CHANGE
            <Outlet />
          ) : (
            <div className="max-w-7xl mx-auto">
              <Outlet />
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default UserLayout;