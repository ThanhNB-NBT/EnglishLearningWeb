import React, { useState } from 'react';
import {
  Card,
  CardBody,
  Typography,
  IconButton,
  Chip,
  Menu,
  MenuHandler,
  MenuList,
  MenuItem,
  Dialog,
  DialogHeader,
  DialogBody,
  DialogFooter,
  Button,
} from '@material-tailwind/react';
import {
  PencilIcon,
  TrashIcon,
  EyeIcon,
  EllipsisVerticalIcon,
  CheckCircleIcon,
  ChatBubbleLeftRightIcon,
  DocumentTextIcon,
  LanguageIcon,
  PencilSquareIcon,
} from '@heroicons/react/24/outline';

/**
 * Component: Table view cho Questions (thay vì Card)
 * Dễ quản lý và xem nhiều thông tin cùng lúc
 */
const QuestionTable = ({ questions, onEdit, onDelete }) => {
  const [previewDialog, setPreviewDialog] = useState({ open: false, question: null });

  const getTypeIcon = (type) => {
    switch (type) {
      case 'MULTIPLE_CHOICE': return ChatBubbleLeftRightIcon;
      case 'FILL_BLANK': return PencilSquareIcon;
      case 'TRANSLATE': return LanguageIcon;
      case 'VERB_FORM': return DocumentTextIcon;
      default: return DocumentTextIcon;
    }
  };

  const getTypeColor = (type) => {
    switch (type) {
      case 'MULTIPLE_CHOICE': return 'blue';
      case 'FILL_BLANK': return 'green';
      case 'TRANSLATE': return 'orange';
      case 'VERB_FORM': return 'purple';
      default: return 'gray';
    }
  };

  const formatQuestionType = (type) => {
    const types = {
      'MULTIPLE_CHOICE': 'Trắc nghiệm',
      'FILL_BLANK': 'Điền từ',
      'TRANSLATE': 'Dịch câu',
      'VERB_FORM': 'Chia động từ',
    };
    return types[type] || type;
  };

  const truncateText = (text, maxLength = 80) => {
    if (!text) return '';
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  };

  const handlePreview = (question) => {
    setPreviewDialog({ open: true, question });
  };

  return (
    <>
      <Card className="border border-blue-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full min-w-max table-auto text-left">
            {/* Table Header */}
            <thead>
              <tr className="bg-blue-gray-50 border-b border-blue-gray-200">
                <th className="p-4 w-16">
                  <Typography variant="small" className="font-bold text-blue-gray-700">
                    #
                  </Typography>
                </th>
                <th className="p-4">
                  <Typography variant="small" className="font-bold text-blue-gray-700">
                    Nội dung câu hỏi
                  </Typography>
                </th>
                <th className="p-4 w-40">
                  <Typography variant="small" className="font-bold text-blue-gray-700">
                    Loại
                  </Typography>
                </th>
                <th className="p-4 w-32 text-center">
                  <Typography variant="small" className="font-bold text-blue-gray-700">
                    Điểm
                  </Typography>
                </th>
                <th className="p-4 w-32 text-center">
                  <Typography variant="small" className="font-bold text-blue-gray-700">
                    Options
                  </Typography>
                </th>
                <th className="p-4 w-32 text-center">
                  <Typography variant="small" className="font-bold text-blue-gray-700">
                    Thao tác
                  </Typography>
                </th>
              </tr>
            </thead>

            {/* Table Body */}
            <tbody>
              {questions.length === 0 ? (
                <tr>
                  <td colSpan="6" className="p-8 text-center">
                    <Typography variant="small" color="blue-gray" className="opacity-60">
                      Chưa có câu hỏi nào
                    </Typography>
                  </td>
                </tr>
              ) : (
                questions.map((question, index) => {
                  const TypeIcon = getTypeIcon(question.questionType);
                  const isLast = index === questions.length - 1;
                  const classes = isLast ? "p-4" : "p-4 border-b border-blue-gray-100";

                  return (
                    <tr key={question.id} className="hover:bg-blue-gray-50 transition-colors">
                      <td className={classes}>
                        <div className="flex items-center justify-center w-8 h-8 bg-purple-100 text-purple-600 rounded-full font-bold text-sm">
                          {index + 1}
                        </div>
                      </td>
                      <td className={classes}>
                        <div>
                          <Typography 
                            variant="small" 
                            color="blue-gray" 
                            className="font-medium cursor-pointer hover:text-purple-600"
                            onClick={() => handlePreview(question)}
                          >
                            {truncateText(question.questionText, 100)}
                          </Typography>
                          {question.explanation && (
                            <Typography variant="small" className="text-gray-500 text-xs mt-1">
                              💡 {truncateText(question.explanation, 60)}
                            </Typography>
                          )}
                        </div>
                      </td>
                      <td className={classes}>
                        <Chip
                          size="sm"
                          value={formatQuestionType(question.questionType)}
                          color={getTypeColor(question.questionType)}
                          icon={<TypeIcon className="h-3 w-3" />}
                          className="text-xs"
                        />
                      </td>
                      <td className={classes}>
                        <Typography variant="small" className="font-bold text-center text-purple-600">
                          {question.points || 5}
                        </Typography>
                      </td>
                      <td className={classes}>
                        <div className="text-center">
                          {question.questionType === 'MULTIPLE_CHOICE' ? (
                            <Chip
                              size="sm"
                              value={`${question.options?.length || 0} lựa chọn`}
                              color="blue"
                              className="text-xs"
                            />
                          ) : (
                            <Typography variant="small" className="text-gray-500">
                              -
                            </Typography>
                          )}
                        </div>
                      </td>
                      <td className={classes}>
                        <div className="flex items-center justify-center space-x-1">
                          <IconButton
                            size="sm"
                            variant="text"
                            color="blue"
                            onClick={() => handlePreview(question)}
                          >
                            <EyeIcon className="h-4 w-4" />
                          </IconButton>
                          <IconButton
                            size="sm"
                            variant="text"
                            color="purple"
                            onClick={() => onEdit(question)}
                          >
                            <PencilIcon className="h-4 w-4" />
                          </IconButton>
                          <IconButton
                            size="sm"
                            variant="text"
                            color="red"
                            onClick={() => onDelete(question)}
                          >
                            <TrashIcon className="h-4 w-4" />
                          </IconButton>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Preview Dialog */}
      <Dialog
        open={previewDialog.open}
        handler={() => setPreviewDialog({ open: false, question: null })}
        size="lg"
      >
        <DialogHeader className="flex items-center space-x-2">
          <EyeIcon className="h-6 w-6 text-purple-500" />
          <span>Chi tiết câu hỏi</span>
        </DialogHeader>
        <DialogBody className="space-y-4 max-h-[60vh] overflow-y-auto">
          {previewDialog.question && (
            <div className="space-y-4">
              {/* Type */}
              <div>
                <Typography variant="h6" color="blue-gray" className="mb-2">
                  Loại câu hỏi:
                </Typography>
                <Chip
                  value={formatQuestionType(previewDialog.question.questionType)}
                  color={getTypeColor(previewDialog.question.questionType)}
                  className="text-sm"
                />
              </div>

              {/* Question Text */}
              <div>
                <Typography variant="h6" color="blue-gray" className="mb-2">
                  Nội dung câu hỏi:
                </Typography>
                <div className="bg-gray-50 p-4 rounded-lg">
                  <Typography variant="paragraph">
                    {previewDialog.question.questionText}
                  </Typography>
                </div>
              </div>

              {/* Options (for MULTIPLE_CHOICE) */}
              {previewDialog.question.questionType === 'MULTIPLE_CHOICE' && 
               previewDialog.question.options && (
                <div>
                  <Typography variant="h6" color="blue-gray" className="mb-2">
                    Các lựa chọn:
                  </Typography>
                  <div className="space-y-2">
                    {previewDialog.question.options.map((option, idx) => (
                      <div
                        key={idx}
                        className={`p-3 rounded-lg border-2 ${
                          option.isCorrect
                            ? 'bg-green-50 border-green-200'
                            : 'bg-gray-50 border-gray-200'
                        }`}
                      >
                        <div className="flex items-center space-x-3">
                          <Typography variant="small" className="font-bold">
                            {String.fromCharCode(65 + idx)}.
                          </Typography>
                          <Typography variant="paragraph" className="flex-1">
                            {option.optionText}
                          </Typography>
                          {option.isCorrect && (
                            <CheckCircleIcon className="h-5 w-5 text-green-600" />
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Correct Answer (for other types) */}
              {previewDialog.question.questionType !== 'MULTIPLE_CHOICE' &&
               previewDialog.question.correctAnswer && (
                <div>
                  <Typography variant="h6" color="blue-gray" className="mb-2">
                    Đáp án đúng:
                  </Typography>
                  <div className="bg-green-50 p-4 rounded-lg border-2 border-green-200">
                    <Typography variant="paragraph" color="green" className="font-medium">
                      {previewDialog.question.correctAnswer}
                    </Typography>
                  </div>
                </div>
              )}

              {/* Explanation */}
              {previewDialog.question.explanation && (
                <div>
                  <Typography variant="h6" color="blue-gray" className="mb-2">
                    Giải thích:
                  </Typography>
                  <div className="bg-blue-50 p-4 rounded-lg border-2 border-blue-200">
                    <Typography variant="paragraph">
                      {previewDialog.question.explanation}
                    </Typography>
                  </div>
                </div>
              )}

              {/* Metadata */}
              <div className="grid grid-cols-2 gap-4 pt-4 border-t border-gray-200">
                <div>
                  <Typography variant="small" color="blue-gray" className="font-medium">
                    Điểm số:
                  </Typography>
                  <Typography variant="paragraph" color="purple">
                    {previewDialog.question.points || 5} điểm
                  </Typography>
                </div>
                <div>
                  <Typography variant="small" color="blue-gray" className="font-medium">
                    Thứ tự:
                  </Typography>
                  <Typography variant="paragraph">
                    #{previewDialog.question.orderIndex}
                  </Typography>
                </div>
              </div>
            </div>
          )}
        </DialogBody>
        <DialogFooter>
          <Button
            variant="outlined"
            onClick={() => setPreviewDialog({ open: false, question: null })}
          >
            Đóng
          </Button>
        </DialogFooter>
      </Dialog>
    </>
  );
};

export default QuestionTable;