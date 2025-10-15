import React from "react";
import { IconButton, Tooltip } from "@material-tailwind/react";
import { SunIcon, MoonIcon } from "@heroicons/react/24/solid";
import { useTheme } from "../../contexts/ThemeContext";

const ThemeToggle = ({ className = "" }) => {
  const { theme, toggleTheme } = useTheme();

  return (
    <Tooltip
      content={theme === "light" ? "Chế độ tối" : "Chế độ sáng"}
      placement="bottom"
    >
      <IconButton
        variant="text"
        onClick={toggleTheme}
        className={`transition-all duration-300 hover:bg-tertiary ${className}`}
      >
        {theme === "light" ? (
          <MoonIcon className="h-5 w-5 text-primary" />
        ) : (
          <SunIcon className="h-5 w-5 text-primary" />
        )}
      </IconButton>
    </Tooltip>
  );
};

export default ThemeToggle;
