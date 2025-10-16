import React, { useState } from "react";
import { getUserDisplayName, logout, getUserEmail } from "../../services/authService";
import { MoonIcon, SunIcon, UserCircleIcon } from '@heroicons/react/24/outline';
import { toast } from 'react-hot-toast';
import { useTheme } from '../../contexts/ThemeContext'; 

const Navbar = () => { 
    const [showMenu, setShowMenu] = useState(false);
    const { toggleTheme, isDark } = useTheme(); 
    const userName = getUserDisplayName();
    const userEmail = getUserEmail();

    const handleLogout = () => {
        console.log('Navbar: Initiating logout');
        toast.success('Đăng xuất thành công!', {
            duration: 1000,
        });
        setTimeout(logout, 1100);
    };

    return (
        <header className="navbar-base flex items-center justify-between px-8 py-4">
            <div />
            <div className="flex items-center space-x-4">
                {/* Theme Icons and Toggle */}
                {!isDark && <MoonIcon className="h-6 w-6 text-primary" />}
                {isDark && <SunIcon className="h-6 w-6 text-primary" />}
                <button
                    onClick={toggleTheme} 
                    className="relative w-12 h-6 rounded-full bg-gray-300 dark:bg-blue-600 transition-colors duration-300 focus:outline-none"
                    title="Chuyển theme"
                >
                    <span
                        className={`absolute left-1 top-1 h-4 w-4 bg-white rounded-full transition-transform duration-300 ${isDark ? 'translate-x-6' : 'translate-x-0'}`}
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
                                <div className="px-4 py-2 text-primary font-semibold">{userName}</div>
                                <div className="px-4 py-1 text-secondary text-sm truncate">{userEmail}</div>
                                <hr className="border-t border-primary my-2" />
                                <button
                                    className="w-full p-2 text-left px-4 py-2 btn-gray"
                                >
                                    Thông tin cá nhân
                                </button>
                                <button
                                    className="w-full p-2 text-left px-4 py-2 btn-gray"
                                >
                                    Cài đặt
                                </button>
                                <button
                                    onClick={handleLogout}
                                    className="w-full p-2 text-left px-4 py-2 btn-danger"
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