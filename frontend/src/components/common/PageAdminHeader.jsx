import React from "react";
import { useNavigate } from "react-router-dom";
import {
  Card,
  CardBody,
  Typography,
  Button,
  IconButton,
} from "@material-tailwind/react";
import { ArrowLeftIcon } from "@heroicons/react/24/solid";

const PageAdminHeader = ({
  title, //Tiêu đề
  description, //Mô tả
  icon: Icon, //Icon hiển thị
  iconBgColor = "blue-500", //Màu nền icon
  iconColor = "blue-400", //Màu icon
  showBackButton = false, //Hiển thị nút quay lại
  onBack, //Hàm xử lý khi nhấn nút quay lại
  actions, //Các hành động bổ sung
  gradientFrom = "slate-800", //Màu gradient bắt đầu
  gradientTo = "slate-900", //Màu gradient kết thúc
}) => {
  const navigate = useNavigate();

  const handleBack = () => {
    if (onBack) {
      onBack();
    } else {
      navigate(-1);
    }
  };

  return (
    <Card className={`bg-gradient-to-r from-${gradientFrom} to-${gradientTo} dark:from-${gradientTo} dark:to-black border border-slate-700 shadow-xl`}>
        <CardBody className="p-4 md:p-6">
            <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center lg:space-y-0 gap-4">
                <div className="flex items-center gap-3 flex-1">
                    {/* Back Button */}
                    {showBackButton && (
                        <IconButton
                            variant="outlined"
                            size="lg"
                            onClick={handleBack}
                            className="text-slate-300 border-slate-500 hover:bg-slate-700 hidden md:flex shrink-0">
                            <ArrowLeftIcon className="h-5 w-5" />
                        </IconButton>
                    )}
                    {/* Icon */}
                    {Icon && (
                        <div className={`p-3 rounded-xl bg-${iconBgColor}/20 border border-${iconBgColor}/30 shrink-0`}>
                            <Icon className={`h-6 w-6 text-${iconColor}`} />
                        </div>
                    )}
                    {/* Title and Description */}
                    <div>
                        <Typography variant="h4" className="text-slate-100 font-bold mb-1">
                            {title}
                        </Typography>
                        <Typography variant="small" className="text-slate-400">
                            {description}
                        </Typography>
                    </div>
                </div>
                {/* Actions */}
                {actions && (
                    <div className="shrink-0 w-full lg:w-auto">
                        {actions}
                    </div>
                )}
            </div>
        </CardBody>
    </Card>
  )
};

export default PageAdminHeader;
