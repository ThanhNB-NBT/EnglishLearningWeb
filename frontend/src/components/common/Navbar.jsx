import React, { useState } from "react";
import { getUserDisplayName, logout, getUserEmail } from "../../utils/auth";
import { MoonIcon, SunIcon, UserCircleIcon } from '@heroicons/react/24/outline';

const Navbar = ({ darkMode, onThemeToggle }) => {
    const [showMenu, setShowMenu] = useState(false);
    const userName = getUserDisplayName();
    const userEmail = getUserEmail();

    return (
        <header className="flex items-center justify-between px-8 py-4 bg-white dark:bg-gray-900 dark:text-gray-100 border-b border-gray-900 dark:border-white">
            <div />
            <div className="flex items-center space-x-4">
                {/* Theme Icons and Toggle */}
                {!darkMode && <MoonIcon className="h-6 w-6 text-gray-800" />}
                {darkMode && <SunIcon className="h-6 w-6 text-white" />}
                <button
                    onClick={onThemeToggle}
                    className="relative w-12 h-6 rounded-full bg-gray-300 dark:bg-blue-600 transition-colors duration-300 focus:outline-none"
                    title="Chuyển theme"
                >
                    <span
                        className={`absolute left-1 top-1 h-4 w-4 bg-white rounded-full transition-transform duration-300 ${darkMode ? 'translate-x-6' : 'translate-x-0'}`}
                    />
                </button>
                
                {/* User Menu Trigger */}
                <div className="relative top-0.5">
                    <button
                        onClick={() => setShowMenu(!showMenu)}
                        className="focus:outline-none focus:ring-0"
                        style={{ background: "none", border: "none", padding: 0 }}
                    >
                        <UserCircleIcon className="h-7 w-7 text-gray-900 dark:text-blue-200 hover:text-blue-600 dark:hover:text-blue-300" />
                    </button>
                    {showMenu && (
                        <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg z-10">
                            <div className="p-3">
                                <div className="px-4 py-2 text-gray-900 dark:text-white font-semibold">{userName}</div>
                                <div className="px-4 py-1 text-gray-600 dark:text-gray-400 text-sm truncate">{userEmail}</div>
                                <hr className="border-t border-gray-100 dark:border-gray-600 my-2" />
                                <button
                                    className="w-full p-2 text-left px-4 py-2 text-gray-900 dark:text-gray-300 hover:bg-blue-50 dark:hover:bg-gray-700 rounded transition-colors duration-200"
                                >
                                    Thông tin cá nhân
                                </button>
                                <button
                                    className="w-full p-2 text-left px-4 py-2 text-gray-900 dark:text-gray-300 hover:bg-blue-50 dark:hover:bg-gray-700 rounded transition-colors duration-200"
                                >
                                    Cài đặt
                                </button>
                                <button
                                    onClick={logout}
                                    className="w-full p-2 text-left px-4 py-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900 rounded transition-colors duration-200"
                                >
                                    Đăng xuất
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
};

export default Navbar;