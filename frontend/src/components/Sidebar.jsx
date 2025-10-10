import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Typography,
  IconButton,
  Menu,
  MenuHandler,
  MenuList,
  MenuItem,
  Button,
} from '@material-tailwind/react';
import {
  HomeIcon,
  BookOpenIcon,
  SpeakerWaveIcon,
  PencilSquareIcon,
  ChatBubbleLeftRightIcon,
  ClipboardDocumentListIcon,
  MicrophoneIcon,
  UserIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  QuestionMarkCircleIcon,
  ChartBarIcon,
} from '@heroicons/react/24/outline';
import { getUserRole } from '../auth/authService';
import { USER_ROUTES, ADMIN_ROUTES } from '../constants/routes'; // Điều chỉnh đường dẫn tới file routes

const Sidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [isCollapsed, setIsCollapsed] = useState(false);
  const userRole = getUserRole();

  const toggleSidebar = () => {
    console.log('toggleSidebar clicked, current state:', isCollapsed);
    setIsCollapsed(!isCollapsed);
  };

  const handleNavigation = (path) => {
    navigate(path);
  };

  const isActive = (path) => {
    return location.pathname === path || location.pathname.startsWith(path);
  };

  const menuUserItems = [
    { label: 'Home', path: USER_ROUTES.HOME, icon: <HomeIcon className="h-5 w-5" /> },
    { label: 'Grammar', path: USER_ROUTES.GRAMMAR, icon: <BookOpenIcon className="h-5 w-5" /> },
    { label: 'Listening', path: '/user/listening', icon: <SpeakerWaveIcon className="h-5 w-5" /> },
    { label: 'Writing', path: '/user/writing', icon: <PencilSquareIcon className="h-5 w-5" /> },
    { label: 'Reading', path: '/user/reading', icon: <ChatBubbleLeftRightIcon className="h-5 w-5" /> },
    { label: 'Vocabulary', path: '/user/vocabulary', icon: <ClipboardDocumentListIcon className="h-5 w-5" /> },
    { label: 'Speaking', path: '/user/speaking', icon: <MicrophoneIcon className="h-5 w-5" /> },
  ];

  const menuAdminItems = [
    { label: 'Dashboard', path: ADMIN_ROUTES.DASHBOARD, icon: <HomeIcon className="h-5 w-5" /> },
    { label: 'Quản lý người dùng', path: '/admin/users', icon: <UserIcon className="h-5 w-5" /> },
    {
      label: 'Quản lý ngữ pháp',
      key: 'grammar',
      icon: <BookOpenIcon className="h-5 w-5" />,
      dropdown: [
        { label: 'Chủ đề', path: ADMIN_ROUTES.GRAMMAR_TOPICS, icon: <BookOpenIcon className="h-5 w-5" /> },
        { label: 'Bài học', path: ADMIN_ROUTES.GRAMMAR_LESSONS(0), icon: <BookOpenIcon className="h-5 w-5" /> }, // Dùng topicId=0 như placeholder
        { label: 'Câu hỏi', path: ADMIN_ROUTES.GRAMMAR_QUESTIONS(0), icon: <QuestionMarkCircleIcon className="h-5 w-5" /> }, // Dùng lessonId=0 như placeholder
      ],
    },
    { label: 'Quản lý bài đọc', path: '/admin/reading', icon: <ChatBubbleLeftRightIcon className="h-5 w-5" /> },
    { label: 'Quản lý từ vựng', path: '/admin/vocabulary', icon: <ClipboardDocumentListIcon className="h-5 w-5" /> },
    { label: 'Quản lý bài nghe', path: '/admin/listening', icon: <SpeakerWaveIcon className="h-5 w-5" /> },
    { label: 'Quản lý bài viết', path: '/admin/writing', icon: <PencilSquareIcon className="h-5 w-5" /> },
    { label: 'Quản lý bài nói', path: '/admin/speaking', icon: <MicrophoneIcon className="h-5 w-5" /> },
    { label: 'Thống kê', path: '/admin/statistics', icon: <ChartBarIcon className="h-5 w-5" /> },
  ];

  const menuItems = userRole === 'ADMIN' ? menuAdminItems : menuUserItems;

  return (
    <div
      className={`h-screen bg-white dark:bg-gray-900 transition-all duration-300 ${
        isCollapsed ? 'w-15' : 'w-64'
      } shadow-lg border-r border-gray-200 dark:border-gray-700 flex flex-col`}
    >
      {/* Header */}
      <div className="p-4 dark:border-gray-100 flex justify-center items-center">
        {!isCollapsed ? (
          <Typography variant="h4" color="blue-gray" className="font-bold text-blue-700 dark:text-light-blue-400">
            {userRole === 'ADMIN' ? 'Admin Panel' : 'English Learning'}
          </Typography>
        ) : (
          <div className="w-9 h-9 bg-blue-500 text-white rounded-full flex items-center justify-center text-sm font-bold">
            {userRole === 'ADMIN' ? 'A' : 'E'}
          </div>
        )}
      </div>

      {/* Menu Items - Take up remaining space */}
      <div className="flex-1 overflow-y-auto py-4">
        {menuItems.map((item, index) => (
          <div key={item.label || index} className="px-2 mb-1">
            {item.dropdown ? (
              // Dropdown Menu Item with proper Menu wrapper
              !isCollapsed ? (
                // Show dropdown menu when expanded
                <Menu placement="right-start" offset={5}>
                  <MenuHandler>
                    <Button
                      variant="text"
                      className={`w-full flex items-center justify-between px-3 py-2 rounded-lg normal-case text-left ${
                        location.pathname.startsWith(`/admin/${item.key}`)
                          ? 'bg-blue-50 dark:bg-blue-900/50 text-blue-600 dark:text-blue-300'
                          : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800'
                      }`}
                    >
                      <div className="flex items-center">
                        {item.icon}
                        <span className="ml-3 text-sm font-medium">{item.label}</span>
                      </div>
                      <ChevronDownIcon className="h-5 w-5" />
                    </Button>
                  </MenuHandler>
                  <MenuList className="bg-white text-gray-900 dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
                    {item.dropdown.map((subItem) => (
                      <MenuItem
                        key={subItem.label}
                        onClick={() => handleNavigation(subItem.path)}
                        className={`flex items-center px-3 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 ${
                          isActive(subItem.path)
                            ? 'bg-blue-50 dark:bg-blue-900/50 text-blue-600 dark:text-blue-300'
                            : ''
                        }`}
                      >
                        <span className="mr-3">{subItem.icon}</span>
                        <span className="text-sm">{subItem.label}</span>
                      </MenuItem>
                    ))}
                  </MenuList>
                </Menu>
              ) : (
                // Show only icon when collapsed - clicking goes to main section
                <Button
                  variant="text"
                  onClick={() => handleNavigation(`/admin/${item.key}`)}
                  className={`w-full flex items-center justify-center px-3 py-2 rounded-lg ${
                    location.pathname.startsWith(`/admin/${item.key}`)
                      ? 'bg-blue-50 dark:bg-blue-900/50 text-blue-600 dark:text-blue-300'
                      : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800'
                  }`}
                  title={item.label}
                >
                  {item.icon}
                </Button>
              )
            ) : (
              // Regular Menu Item
              <Button
                variant="text"
                onClick={() => handleNavigation(item.path)}
                className={`w-full flex items-center px-3 py-2 rounded-lg normal-case text-left ${
                  isActive(item.path)
                    ? 'bg-blue-50 dark:bg-blue-900/50 text-blue-600 dark:text-blue-300'
                    : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800'
                }`}
                title={isCollapsed ? item.label : undefined}
              >
                <div className={`${isCollapsed ? 'flex justify-center w-full' : 'flex items-center'}`}>
                  {item.icon}
                  {!isCollapsed && <span className="ml-3 text-sm font-medium">{item.label}</span>}
                </div>
              </Button>
            )}
          </div>
        ))}
      </div>

      {/* Footer with Toggle Button - Fixed at bottom */}
      <div className="border-t border-gray-400 dark:border-gray-700">
        {/* Toggle Button - Always visible */}
        <div className="p-3 flex justify-end">
          <IconButton
            variant="text"
            size="sm"
            onClick={toggleSidebar}
            className="text-gray-900 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
            title={isCollapsed ? "Mở rộng sidebar" : "Thu gọn sidebar"}
          >
            {isCollapsed ? <ChevronRightIcon className="h-7 w-7" /> : <ChevronLeftIcon className="h-7 w-7" />}
          </IconButton>
        </div>
      </div>
    </div>
  );
};

export default Sidebar;