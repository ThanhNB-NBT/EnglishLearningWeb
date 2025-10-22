import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  Card,
  Typography,
  List,
  ListItem,
  ListItemPrefix,
  Accordion,
  AccordionHeader,
  AccordionBody,
  IconButton,
} from "@material-tailwind/react";
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
  ChevronLeftIcon,
  ChevronRightIcon,
  QuestionMarkCircleIcon,
  ChartBarIcon,
  Bars3Icon,
} from "@heroicons/react/24/outline";
import { getUserRole } from "../../services/authService";
import { USER_ROUTES, ADMIN_ROUTES } from "../../constants/routes";

const Sidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  // const [openAccordion, setOpenAccordion] = useState(0);
  const userRole = getUserRole();

  useEffect(() => {
    const checkScreenSize = () => {
      const mobile = window.innerWidth < 768;
      setIsMobile(mobile);
      if (!mobile) {
        setIsOpen(false);
      }
    };

    checkScreenSize();
    window.addEventListener("resize", checkScreenSize);
    return () => window.removeEventListener("resize", checkScreenSize);
  }, []);

  const toggleSidebar = () => {
    if (isMobile) {
      setIsOpen(!isOpen);
    } else {
      setIsCollapsed(!isCollapsed);
    }
  };

  // const handleAccordion = (value) => {
  //   setOpenAccordion(openAccordion === value ? 0 : value);
  // };

  const handleNavigation = (path) => {
    navigate(path);
    if (isMobile) {
      setIsOpen(false);
    }
  };

  const isActive = (path) => {
    return location.pathname === path || location.pathname.startsWith(path);
  };

  const menuUserItems = [
    { label: "Home", path: USER_ROUTES.HOME, icon: HomeIcon },
    { label: "Grammar", path: USER_ROUTES.GRAMMAR, icon: BookOpenIcon },
    { label: "Listening", path: "/user/listening", icon: SpeakerWaveIcon },
    { label: "Writing", path: "/user/writing", icon: PencilSquareIcon },
    { label: "Reading", path: "/user/reading", icon: ChatBubbleLeftRightIcon },
    {
      label: "Vocabulary",
      path: "/user/vocabulary",
      icon: ClipboardDocumentListIcon,
    },
    { label: "Speaking", path: "/user/speaking", icon: MicrophoneIcon },
  ];

  const menuAdminItems = [
    { label: "Dashboard", path: ADMIN_ROUTES.DASHBOARD, icon: HomeIcon },
    { label: "Quản lý người dùng", path: "/admin/users", icon: UserIcon },
    {
      label: "Quản lý ngữ pháp",
      path: ADMIN_ROUTES.GRAMMAR_TOPICS,
      icon: BookOpenIcon,
    },
    {
      label: "Quản lý bài đọc",
      path: "/admin/reading",
      icon: ChatBubbleLeftRightIcon,
    },
    {
      label: "Quản lý từ vựng",
      path: "/admin/vocabulary",
      icon: ClipboardDocumentListIcon,
    },
    {
      label: "Quản lý bài nghe",
      path: "/admin/listening",
      icon: SpeakerWaveIcon,
    },
    {
      label: "Quản lý bài viết",
      path: "/admin/writing",
      icon: PencilSquareIcon,
    },
    { label: "Quản lý bài nói", path: "/admin/speaking", icon: MicrophoneIcon },
  ];

  const menuItems = userRole === "ADMIN" ? menuAdminItems : menuUserItems;

  const HamburgerButton = () => (
    <button
      onClick={toggleSidebar}
      className="fixed top-4 left-4 z-50 md:hidden p-2 rounded-lg bg-primary shadow-lg text-primary border border-primary"
    >
      <Bars3Icon className="h-6 w-6" />
    </button>
  );

  return (
    <>
      {isMobile && !isOpen && <HamburgerButton />}

      {isMobile && isOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-40 md:hidden"
          onClick={() => setIsOpen(false)}
        />
      )}

      <Card
        className={`h-screen rounded-none shadow-xl border-r card-base
          ${
            isMobile
              ? `fixed top-0 left-0 z-50 w-72 transform ${
                  isOpen ? "translate-x-0" : "-translate-x-full"
                }`
              : `relative ${isCollapsed ? "w-18" : "w-60"}`
          }`}
      >
        {/* Header */}
        <div className="p-6 border-primary">
          {!isCollapsed || isMobile ? (
            <Typography variant="h4" className="font-bold text-primary">
              {userRole === "ADMIN" ? "Admin" : "English Learning"}
            </Typography>
          ) : (
            <div className="flex justify-center">
              <div className="w-10 h-10 bg-gray-900 dark:bg-white text-white dark:text-gray-900 rounded-full flex items-center justify-center text-lg font-bold">
                {userRole === "ADMIN" ? "A" : "E"}
              </div>
            </div>
          )}
        </div>

        {/* Menu List */}
        <div className="flex-1 overflow-y-auto">
          <List className="min-w-0 p-2">
            {menuItems.map((item, index) => {
              const Icon = item.icon;

              // if (item.dropdown && (!isCollapsed || isMobile)) {
              //   return (
              //     <Accordion
              //       key={item.key || index}
              //       open={openAccordion === item.accordionId}
              //       icon={
              //         <ChevronDownIcon
              //           strokeWidth={2.5}
              //           className={`h-4 w-4 transition-transform ${
              //             openAccordion === item.accordionId ? "rotate-180" : ""
              //           }`}
              //         />
              //       }
              //     >
              //       <ListItem
              //         className="p-0 text-primary relative group"
              //         selected={openAccordion === item.accordionId}
              //       >
              //         {/* Vạch xanh bên trái khi hover */}
              //         <div className="absolute left-0 top-0 bottom-0 w-1 bg-blue-500 opacity-0 scale-y-0 origin-center group-hover:opacity-100 group-hover:scale-y-100 transition-all duration-300 ease-out" />

              //         <AccordionHeader
              //           onClick={() => handleAccordion(item.accordionId)}
              //           className="border-b-0 p-3"
              //         >
              //           <ListItemPrefix>
              //             <Icon className="h-5 w-5 text-primary" />
              //           </ListItemPrefix>
              //           <Typography className="mr-auto font-normal text-primary">
              //             {item.label}
              //           </Typography>
              //         </AccordionHeader>
              //       </ListItem>
              //       <AccordionBody className="py-1">
              //         <List className="p-0 pl-4">
              //           {item.dropdown.map((subItem) => {
              //             const SubIcon = subItem.icon;
              //             return (
              //               <ListItem
              //                 key={subItem.label}
              //                 onClick={() => handleNavigation(subItem.path)}
              //                 className={`text-primary relative group ${
              //                   isActive(subItem.path)
              //                     ? "bg-primary text-primary"
              //                     : ""
              //                 }`}
              //               >
              //                 {/* Vạch xanh bên trái cho submenu khi hover */}
              //                 <div className="absolute left-0 top-0 bottom-0 w-1 bg-blue-500 opacity-0 scale-y-0 origin-center group-hover:opacity-100 group-hover:scale-y-100 transition-all duration-300 ease-out" />

              //                 <ListItemPrefix>
              //                   <SubIcon className="h-4 w-4" />
              //                 </ListItemPrefix>
              //                 <Typography className="text-sm">
              //                   {subItem.label}
              //                 </Typography>
              //               </ListItem>
              //             );
              //           })}
              //         </List>
              //       </AccordionBody>
              //     </Accordion>
              //   );
              // }

              // Regular menu item hoặc collapsed dropdown
              return (
                <ListItem
                  key={item.label || index}
                  onClick={() => {
                    if (item.dropdown && isCollapsed && !item.path) {
                      handleNavigation(item.dropdown[0].path);
                    } else if (item.path) {
                      handleNavigation(item.path);
                    }
                  }}
                  className={`text-primary hover:bg-gray-100 dark:hover:bg-slate-900 dark:hover:text-slate-100 relative group ${
                    item.path && isActive(item.path) ? "bg-primary" : ""
                  } ${isCollapsed && !isMobile ? "justify-center" : ""}`}
                  title={isCollapsed && !isMobile ? item.label : undefined}
                >
                  {/* Vạch xanh bên trái khi hover */}
                  <div className="absolute left-0 top-2 bottom-2 w-1 bg-blue-500 opacity-0 scale-y-0 origin-center group-hover:opacity-100 group-hover:scale-y-100 transition-all duration-300 ease-out" />
                  {/* <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-0 bg-blue-500 group-hover:h-full group-hover:top-0 group-hover:translate-y-0 transition-all duration-300 ease-out" /> */}
                  <ListItemPrefix
                    className={isCollapsed && !isMobile ? "m-0" : ""}
                  >
                    <Icon className="h-5 w-5" />
                  </ListItemPrefix>
                  {(!isCollapsed || isMobile) && (
                    <Typography className="font-normal hover:text-light-blue-600">
                      {item.label}
                    </Typography>
                  )}
                </ListItem>
              );
            })}
          </List>
        </div>

        {/* Footer - Toggle button (desktop only) */}
        {!isMobile && (
          <div className="border-t border-primary p-3 flex justify-end">
            <IconButton
              variant="text"
              size="sm"
              onClick={toggleSidebar}
              className="text-primary hover:bg-gray-100 dark:hover:bg-gray-800"
              title={isCollapsed ? "Mở rộng sidebar" : "Thu gọn sidebar"}
            >
              {isCollapsed ? (
                <ChevronRightIcon className="h-6 w-6" />
              ) : (
                <ChevronLeftIcon className="h-6 w-6" />
              )}
            </IconButton>
          </div>
        )}
      </Card>
    </>
  );
};

export default Sidebar;
