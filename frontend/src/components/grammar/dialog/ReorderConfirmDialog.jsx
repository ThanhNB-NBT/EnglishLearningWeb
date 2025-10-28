// components/grammar/dialog/ReorderConfirmDialog.jsx
import React from 'react';
import {
  Dialog,
  DialogHeader,
  DialogBody,
  DialogFooter,
  Button,
  Typography,
  Card,
  CardBody,
} from '@material-tailwind/react';
import {
  ExclamationTriangleIcon,
  ArrowRightIcon,
  CheckCircleIcon,
} from '@heroicons/react/24/outline';

const ReorderConfirmDialog = ({ 
  open, 
  onClose, 
  onConfirm,
  targetPosition,
  affectedLessons,
  isEdit,
  loading 
}) => {
  if (!affectedLessons || affectedLessons.length === 0) return null;

  return (
    <Dialog 
      open={open} 
      handler={onClose}
      size="md"
      className="bg-primary"
    >
      <DialogHeader className="flex items-center gap-3">
        <div className="p-3 bg-amber-100 dark:bg-amber-900/30 rounded-xl">
          <ExclamationTriangleIcon className="h-6 w-6 text-amber-600" />
        </div>
        <div>
          <Typography variant="h5" className="text-primary">
            Xác nhận sắp xếp lại
          </Typography>
          <Typography variant="small" className="text-secondary font-normal">
            {isEdit ? 'Thay đổi vị trí' : 'Chèn vào vị trí'} #{targetPosition}
          </Typography>
        </div>
      </DialogHeader>

      <DialogBody className="space-y-4 max-h-[60vh] overflow-y-auto">
        {/* Warning message */}
        <Card className="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
          <CardBody className="p-4">
            <Typography variant="small" className="text-amber-900 dark:text-amber-100">
              ⚠️ Hành động này sẽ tự động điều chỉnh thứ tự của <strong>{affectedLessons.length} bài học</strong> khác để nhường chỗ cho bài học này.
            </Typography>
          </CardBody>
        </Card>

        {/* Affected lessons preview */}
        <div>
          <Typography variant="small" className="text-primary font-semibold mb-3">
            📋 Các bài học sẽ bị ảnh hưởng:
          </Typography>
          
          <div className="space-y-2">
            {affectedLessons.map((lesson) => (
              <Card 
                key={lesson.id} 
                className="bg-secondary border border-primary hover:border-blue-300 transition-colors"
              >
                <CardBody className="p-3">
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <Typography variant="small" className="text-primary font-medium truncate">
                        {lesson.title}
                      </Typography>
                      <Typography variant="small" className="text-tertiary">
                        {lesson.lessonType === 'THEORY' ? '📖 Lý thuyết' : '✏️ Thực hành'}
                      </Typography>
                    </div>
                    
                    <div className="flex items-center gap-2 flex-shrink-0">
                      <div className="flex items-center gap-2 px-3 py-1 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
                        <Typography variant="small" className="text-blue-600 dark:text-blue-400 font-bold">
                          #{lesson.orderIndex}
                        </Typography>
                        <ArrowRightIcon className="h-3 w-3 text-blue-500" />
                        <Typography variant="small" className="text-green-600 dark:text-green-400 font-bold">
                          #{lesson.orderIndex + 1}
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
                  • Bài học mới sẽ ở vị trí #{targetPosition}<br/>
                  • {affectedLessons.length} bài học khác sẽ dịch xuống 1 vị trí<br/>
                  • Tổng số bài: {affectedLessons[0]?.orderIndex + affectedLessons.length} bài
                </Typography>
              </div>
            </div>
          </CardBody>
        </Card>
      </DialogBody>

      <DialogFooter className="gap-3">
        <Button
          variant="outlined"
          onClick={onClose}
          disabled={loading}
          className="btn-secondary"
        >
          Hủy
        </Button>
        <Button
          color="blue"
          onClick={onConfirm}
          disabled={loading}
          loading={loading}
          className="flex items-center gap-2"
        >
          <CheckCircleIcon className="h-5 w-5" />
          Xác nhận sắp xếp lại
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default ReorderConfirmDialog;