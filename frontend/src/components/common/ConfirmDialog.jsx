import React from "react";
import {
  Dialog,
  DialogHeader,
  DialogBody,
  DialogFooter,
  Button,
  Typography,
  Card,
  CardBody,
} from "@material-tailwind/react";
import {
  TrashIcon,
  ExclamationTriangleIcon,
  ArrowRightIcon,
  CheckCircleIcon,
} from "@heroicons/react/24/outline";

const ConfirmDialog = ({
  open,
  onClose,
  onConfirm,
  title = "Xác nhận",
  message,
  warningMessage,
  itemName,
  confirmText = "Xác nhận",
  cancelText = "Hủy",
  type = "delete", // "delete" | "warning" | "reorder" | "custom"
  icon: CustomIcon,
  loading = false,
  
  // ✅ NEW: Props for reorder type
  reorderData, // { targetPosition, affectedItems, isEdit }
}) => {
  const getIcon = () => {
    if (CustomIcon) return CustomIcon;

    switch (type) {
      case "delete":
        return TrashIcon;
      case "reorder":
        return ExclamationTriangleIcon;
      case "warning":
        return ExclamationTriangleIcon;
      default:
        return ExclamationTriangleIcon;
    }
  };

  const getColor = () => {
    switch (type) {
      case "delete":
        return "red";
      case "reorder":
        return "amber";
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
      handler={onClose}
      size={type === "reorder" ? "md" : "sm"}
      className="bg-secondary border border-primary"
      dismiss={{ enabled: true, escapeKey: true, outsidePress: !loading }}
    >
      {/* Header */}
      <DialogHeader className="flex items-center gap-3 border-b border-primary pb-4">
        <div
          className={`p-3 rounded-lg ${
            type === 'reorder' 
              ? 'bg-amber-100 dark:bg-amber-900/30' 
              : `bg-${color}-50 dark:bg-${color}-900/20`
          }`}
        >
          <Icon className={`h-6 w-6 ${
            type === 'reorder' ? 'text-amber-600' : `text-${color}-500`
          }`} />
        </div>
        <div className="flex-1">
          <Typography variant="h5" className="text-primary font-bold">
            {title}
          </Typography>
          {type === "reorder" && reorderData && (
            <Typography variant="small" className="text-secondary font-normal">
              {reorderData.isEdit ? 'Thay đổi vị trí' : 'Chèn vào vị trí'} #{reorderData.targetPosition}
            </Typography>
          )}
        </div>
      </DialogHeader>

      {/* Body */}
      <DialogBody className="space-y-4 max-h-[60vh] overflow-y-auto">
        {/* ✅ REORDER TYPE - Show affected items */}
        {type === "reorder" && reorderData?.affectedItems?.length > 0 && (
          <>
            {/* Warning */}
            <Card className="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
              <CardBody className="p-4">
                <Typography variant="small" className="text-amber-900 dark:text-amber-100">
                  ⚠️ Hành động này sẽ tự động điều chỉnh thứ tự của{' '}
                  <strong>{reorderData.affectedItems.length} mục</strong> khác để nhường chỗ.
                </Typography>
              </CardBody>
            </Card>

            {/* Affected items */}
            <div>
              <Typography variant="small" className="text-primary font-semibold mb-3">
                📋 Các mục sẽ bị ảnh hưởng:
              </Typography>
              
              <div className="space-y-2">
                {reorderData.affectedItems.map((item) => (
                  <Card 
                    key={item.id} 
                    className="bg-secondary border border-primary hover:border-blue-300 transition-colors"
                  >
                    <CardBody className="p-3">
                      <div className="flex items-center justify-between gap-3">
                        <div className="flex-1 min-w-0">
                          <Typography variant="small" className="text-primary font-medium truncate">
                            {item.title || item.name}
                          </Typography>
                          {item.subtitle && (
                            <Typography variant="small" className="text-tertiary">
                              {item.subtitle}
                            </Typography>
                          )}
                        </div>
                        
                        <div className="flex items-center gap-2 flex-shrink-0">
                          <div className="flex items-center gap-2 px-3 py-1 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
                            <Typography variant="small" className="text-blue-600 dark:text-blue-400 font-bold">
                              #{item.orderIndex}
                            </Typography>
                            <ArrowRightIcon className="h-3 w-3 text-blue-500" />
                            <Typography variant="small" className="text-green-600 dark:text-green-400 font-bold">
                              #{item.orderIndex + 1}
                            </Typography>
                          </div>
                        </div>
                      </div>
                    </CardBody>
                  </Card>
                ))}
              </div>
            </div>

            {/* Summary */}
            <Card className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800">
              <CardBody className="p-4">
                <div className="flex items-start gap-2">
                  <CheckCircleIcon className="h-5 w-5 text-blue-600 mt-0.5 flex-shrink-0" />
                  <div>
                    <Typography variant="small" className="text-blue-900 dark:text-blue-100 font-semibold">
                      Kết quả sau khi sắp xếp:
                    </Typography>
                    <Typography variant="small" className="text-blue-800 dark:text-blue-200 mt-1">
                      • Mục mới sẽ ở vị trí #{reorderData.targetPosition}<br/>
                      • {reorderData.affectedItems.length} mục khác sẽ dịch xuống 1 vị trí
                    </Typography>
                  </div>
                </div>
              </CardBody>
            </Card>
          </>
        )}

        {/* ✅ NORMAL TYPE - Show message */}
        {type !== "reorder" && (
          <>
            {message && (
              <Typography variant="paragraph" className="text-primary mb-3">
                {message}
                {itemName && (
                  <strong className="text-blue-500"> "{itemName}"</strong>
                )}
                {!message.endsWith("?") && " không?"}
              </Typography>
            )}

            {warningMessage && (
              <Card className={`bg-${color}-50 dark:bg-${color}-900/10 border border-${color}-200 dark:border-${color}-900/30`}>
                <CardBody className="p-4">
                  <Typography
                    variant="small"
                    className={`text-${color}-900 dark:text-${color}-100 flex items-start gap-2`}
                  >
                    <span className="text-lg">⚠️</span>
                    <span>{warningMessage}</span>
                  </Typography>
                </CardBody>
              </Card>
            )}
          </>
        )}
      </DialogBody>

      {/* Footer */}
      <DialogFooter className="gap-3 pt-4">
        <Button
          variant="outlined"
          onClick={onClose}
          disabled={loading}
          className="btn-secondary"
        >
          {cancelText}
        </Button>
        <Button
          color={color === "amber" ? "orange" : color}
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