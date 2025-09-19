import React from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { 
  HomeIcon, 
  BookOpenIcon, 
  SpeakerWaveIcon, 
  PencilSquareIcon, 
  DocumentTextIcon, 
  ChatBubbleLeftEllipsisIcon, 
  MicrophoneIcon 
} from '@heroicons/react/24/outline';

const menuItems = [
    { label: 'Dashboard', path: '/dashboard', icon: <HomeIcon className="h-5 w-5" /> },
    { label: 'Grammar', path: '/grammar', icon: <BookOpenIcon className="h-5 w-5" /> },
    { label: 'Listening', path: '/listening', icon: <SpeakerWaveIcon className="h-5 w-5" /> },
    { label: 'Writing', path: '/writing', icon: <PencilSquareIcon className="h-5 w-5" /> },
    { label: 'Reading', path: '/reading', icon: <DocumentTextIcon className="h-5 w-5" /> },
    { label: 'Vocabulary', path: '/vocabulary', icon: <ChatBubbleLeftEllipsisIcon className="h-5 w-5" /> },
    { label: 'Speaking', path: '/speaking', icon: <MicrophoneIcon className="h-5 w-5" /> },
];

const Sidebar = () => {
    const navigate = useNavigate();

    return (
        <aside className="w-56 h-screen flex-shrink-0 dark:bg-gray-900 dark:border-gray-100 dark:text-white border-r border-gray-200 bg-white">
            <div className="p-6 font-bold text-xl text-blue-600 dark:text-blue-300">English Learning</div>
            <nav className="mt-8">
                <ul className="space-y-2 mx-4">
                    {menuItems.map(item => (
                        <li key={item.label} className="mb-2 dark:text-white">
                            <button
                                className="flex items-center w-full px-4 py-2 rounded transition dark:bg-gray-900 text-gray-700 dark:text-gray-100 hover:text-blue-600 dark:hover:text-blue-300 hover:bg-blue-50 dark:hover:bg-[#232b3a]"
                                onClick={() => navigate(item.path)}
                            >
                                <span className="mr-3">{item.icon}</span>
                                {item.label}
                            </button>
                        </li>
                    ))}
                </ul>
            </nav>
        </aside>
    );
};

export default Sidebar;