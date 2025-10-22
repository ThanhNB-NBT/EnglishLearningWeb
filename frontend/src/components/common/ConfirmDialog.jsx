import React from "react";
import {
  Dialog,
  DialogHeader,
  DialogBody,
  DialogFooter,
  Button,
  Typography,
} from "@material-tailwind/react";
import {
  TrashIcon,
  ExclamationTriangleIcon,
} from "@heroicons/react/24/outline";

const ConfirmDialog = ({
  open, // Trạng thái hiển thị dialog
  onClose, // Hàm đóng dialog
  onConfirm, // Hàm xác nhận hành động
  title = "Xác nhận", // Tiêu đề dialog
  message, // Nội dung thông báo
  warningMessage, // Nội dung cảnh báo (red)
  itemName, // Tên mục cần xác nhận
  confirmText = "Xác nhận", // Văn bản nút xác nhận
  cancelText = "Hủy", // Văn bản nút hủy
  type = "delete", // Loại hành động: "delete" hoặc "custom"
  icon: CustomIcon, // Icon tùy chỉnh cho loại "custom"
  loading = false, // Trạng thái tải
}) => {
  //Icon dựa trên loại hành động
  const getIcon = () => {
    if (CustomIcon) return CustomIcon;

    switch (type) {
      case "delete":
        return TrashIcon;
      case "warning":
        return ExclamationTriangleIcon;
      default:
        return ExclamationTriangleIcon;
    }
  };

  // Màu dựa trên hành động
  const getColor = () => {
    switch (type) {
      case "delete":
        return "red";
      case "warning":
        return "orange";
      default:
        return "blue";
    }
  };

  const Icon = getIcon();
  const color = getColor();

  return (
    <Dialog
      open={open}
      handle={onClose}
      size="sm"
      className="bg-secondary border border-primary"
    >
      {/* Header */}
      <DialogHeader className="flex items-center gap-3 border-b border-primary pb-4">
        <div
          className={`p-2 bg-${color}-50 dark:bg-${color}-900/20 rounded-lg`}
        >
          <Icon className={`h-6 w-6 text-${color}-500`} />
        </div>
        <span className="text-primary font-bold">{title}</span>
      </DialogHeader>

      {/* Body */}
      <DialogBody>
        {/* Nội dung thông báo */}
        {message && (
          <Typography variant="paragraph" className="text-primary mb-3">
            {message}
            {itemName && (
              <strong className="text-blue-500"> "{itemName}"</strong>
            )}
            {message.endsWith("?") ? "" : "không?"}
          </Typography>
        )}

        {/* Nội dung cảnh báo */}
        {warningMessage && (
          <div
            className={`p-4 bg-${color}-50 dark:bg-${color}-900/10 rounded-lg border border-${color}-200 dark:border-${color}-900/30`}
          >
            <Typography
              variant="small"
              color={color}
              className="flex items-start gap-2"
            >
              <span className="text-lg">⚠️</span>
              <span>{warningMessage}</span>
            </Typography>
          </div>
        )}
      </DialogBody>

      {/* Footer */}
      <DialogFooter className="gap-2 pt-4">
        <Button
          variant="outlined"
          onClick={onClose}
          disabled={loading}
          className="btn-secondary"
        >
          {cancelText}
        </Button>
        <Button
          color={color}
          onClick={onConfirm}
          disabled={loading}
          loading={loading}
          className="hover:shadow-lg transition-all flex items-center gap-2"
        >
          {!loading && <Icon className="h-4 w-4" />}
          {confirmText}
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default ConfirmDialog;
