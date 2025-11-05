// frontend/src/components/reading/dialog/ReadingParsedResultDialog.jsx
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
  Alert,
  Chip,
} from "@material-tailwind/react";
import {
  SparklesIcon,
  CheckCircleIcon,
  XMarkIcon,
  BookOpenIcon,
} from "@heroicons/react/24/outline";
import '../../../styles/lessonPreview.css';

const ReadingParsedResultDialog = ({
  open,
  parsedData,
  summary,
  onClose,
  onConfirm,
}) => {
  if (!parsedData) return null;

  const handleConfirm = () => {
    onConfirm(parsedData);
  };

  return (
    <Dialog
      open={open}
      handler={onClose}
      size="xl"
      className="max-h-[90vh] overflow-hidden"
    >
      <DialogHeader className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <SparklesIcon className="h-6 w-6 text-purple-500" />
          <span>Kết quả phân tích từ AI</span>
        </div>
        <Chip
          value="1 bài đọc"
          color="green"
          size="sm"
        />
      </DialogHeader>

      <DialogBody className="max-h-[calc(90vh-200px)] overflow-y-auto">
        {/* Summary */}
        {summary && (
          <Alert
            color="blue"
            icon={<SparklesIcon className="h-5 w-5" />}
            className="mb-4"
          >
            <Typography variant="small" className="font-medium mb-2">
              📊 Thông tin file
            </Typography>
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <strong>Tên file:</strong> {summary.fileName}
              </div>
              <div>
                <strong>Kích thước:</strong> {summary.fileSize}
              </div>
              <div>
                <strong>Nội dung:</strong> {summary.contentLength} ký tự
              </div>
              <div>
                <strong>Bản dịch:</strong> {summary.translationLength > 0 ? summary.translationLength + ' ký tự' : 'Không có'}
              </div>
            </div>
            {summary.questionCount > 0 && (
              <div className="mt-2 pt-2 border-t border-blue-300">
                <strong>📝 Câu hỏi:</strong> {summary.questionCount} câu
                ({summary.multipleChoice} trắc nghiệm, {summary.fillBlank} điền từ)
              </div>
            )}
          </Alert>
        )}

        {/* Parsed Content */}
        <Card className="mb-4 bg-blue-50 dark:bg-blue-900/10 border border-blue-200 dark:border-blue-800">
          <CardBody className="p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2 bg-blue-500 rounded-lg">
                <BookOpenIcon className="h-5 w-5 text-white" />
              </div>
              <div>
                <Typography variant="h6" className="text-primary font-bold">
                  {parsedData.title || "Bài đọc chưa có tiêu đề"}
                </Typography>
              </div>
            </div>

            {/* English Content */}
            {parsedData.content && (
              <div className="mb-4">
                <Typography variant="small" className="mb-2 font-semibold text-blue-600">
                  🇬🇧 English
                </Typography>
                <div className="lesson-preview-wrapper">
                  <div 
                    className="lesson-preview-content"
                    dangerouslySetInnerHTML={{ __html: parsedData.content }}
                  />
                </div>
              </div>
            )}

            {/* Vietnamese Translation */}
            {parsedData.contentTranslation && (
              <div>
                <Typography variant="small" className="mb-2 font-semibold text-green-600">
                  🇻🇳 Tiếng Việt
                </Typography>
                <div className="lesson-preview-wrapper">
                  <div 
                    className="lesson-preview-content"
                    dangerouslySetInnerHTML={{ __html: parsedData.contentTranslation }}
                  />
                </div>
              </div>
            )}

            {/* Metadata */}
            {(parsedData.orderIndex || parsedData.pointsReward) && (
              <div className="flex items-center space-x-4 text-xs text-gray-600 mt-3 pt-3 border-t">
                {parsedData.orderIndex && <span>📍 Thứ tự: {parsedData.orderIndex}</span>}
                {parsedData.pointsReward && <span>🎯 {parsedData.pointsReward} điểm</span>}
              </div>
            )}
          </CardBody>
        </Card>

        {/* Warning */}
        <Alert color="amber">
          <Typography variant="small">
            ⚠️ <strong>Lưu ý:</strong> Hãy kiểm tra kỹ nội dung trước khi lưu.
            Bạn có thể chỉnh sửa sau khi import.
          </Typography>
        </Alert>
      </DialogBody>

      <DialogFooter className="space-x-2">
        <Button
          variant="outlined"
          onClick={onClose}
          className="flex items-center"
        >
          <XMarkIcon className="h-4 w-4 mr-2" />
          Hủy
        </Button>
        <Button
          color="purple"
          onClick={handleConfirm}
          className="flex items-center shadow-lg"
        >
          <CheckCircleIcon className="h-4 w-4 mr-2" />
          Lưu bài đọc
        </Button>
      </DialogFooter>
    </Dialog>
  );
};

export default ReadingParsedResultDialog;