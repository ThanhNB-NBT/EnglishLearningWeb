// frontend/src/components/reading/dialog/ReadingContentViewDialog.jsx
import React from "react";
import {
  Dialog,
  DialogHeader,
  DialogBody,
  DialogFooter,
  Button,
  Card,
  CardBody,
  Typography,
  Chip,
} from "@material-tailwind/react";
import {
  XMarkIcon,
  BookOpenIcon,
} from "@heroicons/react/24/outline";
import '../../../styles/lessonPreview.css';

const ReadingContentViewDialog = ({
  open,
  lesson,
  onClose,
}) => {
  if (!lesson) return null;

  return (
    <Dialog
      open={open}
      handler={onClose}
      size="xl"
      className="max-h-[90vh] overflow-hidden"
    >
      <DialogHeader className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <BookOpenIcon className="h-6 w-6 text-blue-500" />
          <span>Chi tiết bài đọc</span>
        </div>
        <Chip
          value={lesson.isActive ? "Hoạt động" : "Tạm dừng"}
          color={lesson.isActive ? "green" : "gray"}
          size="sm"
        />
      </DialogHeader>

      <DialogBody className="max-h-[calc(90vh-200px)] overflow-y-auto">
        <Card className="bg-blue-50 dark:bg-blue-900/10 border border-blue-200 dark:border-blue-800">
          <CardBody className="p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2 bg-blue-500 rounded-lg">
                <BookOpenIcon className="h-5 w-5 text-white" />
              </div>
              <div>
                <Typography variant="h6" className="text-primary font-bold">
                  {lesson.title || "Chưa có tiêu đề"}
                </Typography>
                <Typography variant="small" className="text-secondary">
                  Bài {lesson.orderIndex} • {lesson.pointsReward} điểm
                </Typography>
              </div>
            </div>

            {/* English Content */}
            {lesson.content && (
              <div className="mb-4">
                <Typography variant="small" className="mb-2 font-semibold text-blue-600">
                  🇬🇧 English
                </Typography>
                <div className="lesson-preview-wrapper">
                  <div 
                    className="lesson-preview-content"
                    dangerouslySetInnerHTML={{ __html: lesson.content }}
                  />
                </div>
              </div>
            )}

            {/* Vietnamese Translation */}
            {lesson.contentTranslation && (
              <div>
                <Typography variant="small" className="mb-2 font-semibold text-green-600">
                  🇻🇳 Tiếng Việt
                </Typography>
                <div className="lesson-preview-wrapper">
                  <div 
                    className="lesson-preview-content"
                    dangerouslySetInnerHTML={{ __html: lesson.contentTranslation }}
                  />
                </div>
              </div>
            )}

            {/* Metadata */}
            <div className="flex items-center flex-wrap gap-3 text-xs text-gray-600 mt-4 pt-4 border-t">
              <span>📍 Thứ tự: {lesson.orderIndex}</span>
              <span>🎯 {lesson.pointsReward} điểm</span>
              {lesson.questionCount > 0 && (
                <span className="text-green-600 font-medium">
                  ✅ {lesson.questionCount} câu hỏi
                </span>
              )}
            </div>
          </CardBody>
        </Card>
      </DialogBody>

      <DialogFooter>
        <Button
          variant="outlined"
          onClick={onClose}
          className="flex items-center"
        >
          <XMarkIcon className="h-4 w-4 mr-2" />
          Đóng
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default ReadingContentViewDialog;