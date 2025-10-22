import React from "react";
import {
  Card,
  CardBody,
  CardHeader,
  Typography,
  Chip,
  IconButton,
  Button,
  Menu,
  MenuHandler,
  MenuList,
  MenuItem,
} from "@material-tailwind/react";
import {
  PencilIcon,
  TrashIcon,
  EyeIcon,
  EllipsisVerticalIcon,
} from "@heroicons/react/24/outline";

/**
 * Reusable Resource Card Component for Grid Display
 * 
 * @param {object} item - Data object (topic, lesson, question, etc.)
 * @param {string} title - Card title (item.name or item.title)
 * @param {string} description - Card description
 * @param {string} orderLabel - Label for order (e.g., "#1", "Bài 1")
 * @param {ReactNode} icon - Icon component
 * @param {string} iconBgColor - Icon background color
 * @param {string} iconColor - Icon color
 * @param {array} chips - Array of chip objects: [{ label, color, icon }]
 * @param {array} stats - Array of stat objects: [{ icon, label, value }]
 * @param {array} actions - Array of action objects: [{ label, icon, color, onClick }]
 * @param {object} menuActions - Menu actions: { view, edit, delete }
 * @param {function} onTitleClick - Click handler for title
 * @param {string} gradientFrom - Card header gradient start
 * @param {string} gradientTo - Card header gradient end
 */
const ResourceCard = ({
  item,
  title,
  description,
  orderLabel,
  icon: Icon,
  iconBgColor = "blue-500",
  iconColor = "blue-400",
  chips = [],
  stats = [],
  actions = [],
  menuActions = {},
  onTitleClick,
  gradientFrom = "slate-800",
  gradientTo = "slate-900",
}) => {

  const handleTitleClick = () => {
    if (onTitleClick) {
      onTitleClick(item);
    }
  };

  return (
    <Card className="card-base border-primary hover:shadow-2xl hover:border-blue-500/50 transition-all duration-300">
      {/* Header */}
      <CardHeader
        floated={false}
        shadow={false}
        className={`m-0 rounded-t-xl rounded-b-none bg-gradient-to-br from-${gradientFrom} to-${gradientTo} dark:from-${gradientTo} dark:to-slate-950`}
      >
        <div className="p-5">
          <div className="flex justify-between items-start mb-4">
            {/* Order & Icon */}
            <div className="flex items-center gap-2">
              {orderLabel && (
                <div className="px-3 py-1 bg-slate-700/50 rounded-lg border border-slate-600">
                  <Typography variant="small" className="text-slate-300 font-bold">
                    {orderLabel}
                  </Typography>
                </div>
              )}
              {Icon && (
                <div className={`p-1.5 bg-${iconBgColor}/20 rounded-lg border border-${iconBgColor}/30`}>
                  <Icon className={`h-4 w-4 text-${iconColor}`} />
                </div>
              )}
            </div>

            {/* Menu Actions */}
            {Object.keys(menuActions).length > 0 && (
              <Menu>
                <MenuHandler>
                  <IconButton variant="text" size="sm" className="text-slate-300 hover:bg-slate-700">
                    <EllipsisVerticalIcon className="h-5 w-5" />
                  </IconButton>
                </MenuHandler>
                <MenuList className="bg-secondary border-primary min-w-[180px]">
                  {menuActions.view && (
                    <MenuItem
                      onClick={menuActions.view.onClick}
                      className="hover:bg-tertiary flex items-center gap-2"
                    >
                      <EyeIcon className="h-4 w-4 text-green-500" />
                      <Typography variant="small" className="text-primary">
                        {menuActions.view.label || "Xem"}
                      </Typography>
                    </MenuItem>
                  )}
                  {menuActions.edit && (
                    <MenuItem
                      onClick={menuActions.edit.onClick}
                      className="hover:bg-tertiary flex items-center gap-2"
                    >
                      <PencilIcon className="h-4 w-4 text-blue-500" />
                      <Typography variant="small" className="text-primary">
                        {menuActions.edit.label || "Chỉnh sửa"}
                      </Typography>
                    </MenuItem>
                  )}
                  {menuActions.delete && (
                    <MenuItem
                      onClick={menuActions.delete.onClick}
                      className="hover:bg-red-50 dark:hover:bg-red-900/20 flex items-center gap-2"
                    >
                      <TrashIcon className="h-4 w-4 text-red-500" />
                      <Typography variant="small" className="text-red-500">
                        {menuActions.delete.label || "Xóa"}
                      </Typography>
                    </MenuItem>
                  )}
                  {/* Custom menu actions */}
                  {menuActions.custom && menuActions.custom.map((action, idx) => (
                    <MenuItem
                      key={idx}
                      onClick={action.onClick}
                      className={action.className || "hover:bg-tertiary flex items-center gap-2"}
                    >
                      {action.icon && <action.icon className={`h-4 w-4 ${action.iconColor}`} />}
                      <Typography variant="small" className={action.textColor || "text-primary"}>
                        {action.label}
                      </Typography>
                    </MenuItem>
                  ))}
                </MenuList>
              </Menu>
            )}
          </div>

          {/* Title */}
          <Typography
            variant="h6"
            className={`text-slate-100 font-bold mb-3 line-clamp-2 leading-tight transition-colors min-h-[3rem] ${
              onTitleClick ? "cursor-pointer hover:text-blue-400" : ""
            }`}
            onClick={handleTitleClick}
          >
            {title}
          </Typography>

          {/* Chips */}
          {chips.length > 0 && (
            <div className="flex items-center gap-2 flex-wrap">
              {chips.map((chip, idx) => (
                <Chip
                  key={idx}
                  size="sm"
                  value={chip.label}
                  color={chip.color}
                  className="text-xs font-semibold"
                />
              ))}
            </div>
          )}
        </div>
      </CardHeader>

      {/* Body */}
      <CardBody className="p-5">
        {/* Description */}
        {description && (
          <Typography variant="small" className="text-secondary mb-4 line-clamp-3 leading-relaxed min-h-[4.5rem]">
            {description}
          </Typography>
        )}

        {/* Stats */}
        {stats.length > 0 && (
          <div className="flex items-center gap-4 mb-5 pb-5 border-b border-primary flex-wrap">
            {stats.map((stat, idx) => (
              <div key={idx} className="flex items-center gap-2">
                {stat.icon && (
                  <div className={`p-1.5 ${stat.bgColor} rounded-lg`}>
                    <stat.icon className={`h-4 w-4 ${stat.iconColor}`} />
                  </div>
                )}
                <Typography variant="small" className="text-secondary font-medium">
                  {stat.label} {stat.value}
                </Typography>
              </div>
            ))}
          </div>
        )}

        {/* Action Buttons */}
        {actions.length > 0 && (
          <div className="flex gap-2">
            {actions.map((action, idx) => {
              if (action.type === "icon") {
                return (
                  <IconButton
                    key={idx}
                    size="sm"
                    variant="outlined"
                    color={action.color}
                    className={action.className}
                    onClick={action.onClick}
                  >
                    {action.icon && <action.icon className="h-4 w-4" />}
                  </IconButton>
                );
              }
              return (
                <IconButton
                  key={idx}
                  size="sm"
                  variant="outlined"
                  className={`${action.className}`}
                  onClick={action.onClick}
                >
                  {action.icon && <action.icon className="h-4 w-4" />}
                </IconButton>
              );
            })}
          </div>
        )}
      </CardBody>
    </Card>
  );
};

export default ResourceCard;