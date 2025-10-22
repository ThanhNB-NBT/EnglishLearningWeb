import React, { useState } from 'react';
import {
  Card,
  Typography,
  IconButton,
  Chip,
  Checkbox,
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
  CheckCircleIcon,
  ChatBubbleLeftRightIcon,
  LanguageIcon,
  PencilSquareIcon,
} from '@heroicons/react/24/outline';

const QuestionTable = ({ 
  questions, 
  selectedQuestions = [],
  onSelectAll,
  onSelectOne,
  isAllSelected = false,
  isSomeSelected = false,
  onEdit, 
  onDelete 
}) => {
  const [previewDialog, setPreviewDialog] = useState({ open: false, question: null });

  const getTypeIcon = (type) => {
    switch (type) {
      case 'MULTIPLE_CHOICE': return ChatBubbleLeftRightIcon;
      case 'FILL_BLANK': return PencilSquareIcon;
      case 'TRANSLATE': return LanguageIcon;
      default: return ChatBubbleLeftRightIcon;
    }
  };

  const getTypeColor = (type) => {
    switch (type) {
      case 'MULTIPLE_CHOICE': return 'blue';
      case 'FILL_BLANK': return 'green';
      case 'TRANSLATE': return 'orange';
      default: return 'gray';
    }
  };

  const formatQuestionType = (type) => {
    const types = {
      'MULTIPLE_CHOICE': 'Trắc nghiệm',
      'FILL_BLANK': 'Điền từ',
      'TRANSLATE': 'Dịch câu',
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

  const isSelected = (questionId) => selectedQuestions.includes(questionId);

  return (
    <>
      <Card className="card-base border-primary overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full min-w-max table-auto text-left">
            {/* Table Header */}
            <thead>
              <tr className="bg-tertiary border-b border-primary">
                <th className="p-4 w-12">
                  <Checkbox
                    checked={isAllSelected}
                    indeterminate={isSomeSelected}
                    onChange={(e) => onSelectAll(e.target.checked)}
                    color="purple"
                  />
                </th>
                <th className="p-4 w-16">
                  <Typography variant="small" className="font-bold text-primary">
                    #
                  </Typography>
                </th>
                <th className="p-4">
                  <Typography variant="small" className="font-bold text-primary">
                    Nội dung câu hỏi
                  </Typography>
                </th>
                <th className="p-4 w-40">
                  <Typography variant="small" className="font-bold text-primary">
                    Loại
                  </Typography>
                </th>
                <th className="p-4 w-32 text-center">
                  <Typography variant="small" className="font-bold text-primary">
                    Điểm
                  </Typography>
                </th>
                <th className="p-4 w-32 text-center">
                  <Typography variant="small" className="font-bold text-primary">
                    Options
                  </Typography>
                </th>
                <th className="p-4 w-32 text-center">
                  <Typography variant="small" className="font-bold text-primary">
                    Thao tác
                  </Typography>
                </th>
              </tr>
            </thead>

            {/* Table Body */}
            <tbody>
              {questions.length === 0 ? (
                <tr>
                  <td colSpan="7" className="p-8 text-center">
                    <Typography variant="small" className="text-tertiary">
                      Chưa có câu hỏi nào
                    </Typography>
                  </td>
                </tr>
              ) : (
                questions.map((question, index) => {
                  const TypeIcon = getTypeIcon(question.questionType);
                  const selected = isSelected(question.id);
                  const isLast = index === questions.length - 1;
                  const classes = isLast ? "p-4" : "p-4 border-b border-primary";

                  return (
                    <tr 
                      key={question.id} 
                      className={`hover:bg-tertiary transition-colors ${selected ? 'bg-purple-50 dark:bg-purple-900/10' : ''}`}
                    >
                      <td className={classes}>
                        <Checkbox
                          checked={selected}
                          onChange={(e) => onSelectOne(question.id, e.target.checked)}
                          color="purple"
                        />
                      </td>
                      <td className={classes}>
                        <div className="flex items-center justify-center w-8 h-8 bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 rounded-full font-bold text-sm">
                          {index + 1}
                        </div>
                      </td>
                      <td className={classes}>
                        <div>
                          <Typography 
                            variant="small" 
                            className="font-medium cursor-pointer hover:text-purple-600 text-primary"
                            onClick={() => handlePreview(question)}
                          >
                            {truncateText(question.questionText, 100)}
                          </Typography>
                          {question.explanation && (
                            <Typography variant="small" className="text-tertiary text-xs mt-1">
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
                            <Typography variant="small" className="text-tertiary">
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
        className="bg-secondary border border-primary"
      >
        <DialogHeader className="flex items-center gap-3 border-b border-primary">
          <EyeIcon className="h-6 w-6 text-purple-500" />
          <span className="text-primary font-bold">Chi tiết câu hỏi</span>
        </DialogHeader>
        <DialogBody className="space-y-4 max-h-[60vh] overflow-y-auto border-b border-primary">
          {previewDialog.question && (
            <div className="space-y-4">
              {/* Type */}
              <div>
                <Typography variant="h6" className="mb-2 text-primary">
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
                <Typography variant="h6" className="mb-2 text-primary">
                  Nội dung câu hỏi:
                </Typography>
                <div className="bg-tertiary p-4 rounded-lg border border-primary">
                  <Typography variant="paragraph" className="text-primary">
                    {previewDialog.question.questionText}
                  </Typography>
                </div>
              </div>

              {/* Options (for MULTIPLE_CHOICE) */}
              {previewDialog.question.questionType === 'MULTIPLE_CHOICE' && 
               previewDialog.question.options && (
                <div>
                  <Typography variant="h6" className="mb-2 text-primary">
                    Các lựa chọn:
                  </Typography>
                  <div className="space-y-2">
                    {previewDialog.question.options.map((option, idx) => (
                      <div
                        key={idx}
                        className={`p-3 rounded-lg border-2 ${
                          option.isCorrect
                            ? 'bg-green-50 dark:bg-green-900/20 border-green-500'
                            : 'bg-tertiary border-primary'
                        }`}
                      >
                        <div className="flex items-center space-x-3">
                          <Typography variant="small" className="font-bold text-primary">
                            {String.fromCharCode(65 + idx)}.
                          </Typography>
                          <Typography variant="paragraph" className="flex-1 text-primary">
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
                  <Typography variant="h6" className="mb-2 text-primary">
                    Đáp án đúng:
                  </Typography>
                  <div className="bg-green-50 dark:bg-green-900/20 p-4 rounded-lg border-2 border-green-500">
                    <Typography variant="paragraph" className="text-green-700 dark:text-green-400 font-medium">
                      {previewDialog.question.correctAnswer}
                    </Typography>
                  </div>
                </div>
              )}

              {/* Explanation */}
              {previewDialog.question.explanation && (
                <div>
                  <Typography variant="h6" className="mb-2 text-primary">
                    Giải thích:
                  </Typography>
                  <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg border-2 border-blue-500">
                    <Typography variant="paragraph" className="text-primary">
                      {previewDialog.question.explanation}
                    </Typography>
                  </div>
                </div>
              )}

              {/* Metadata */}
              <div className="grid grid-cols-2 gap-4 pt-4 border-t border-primary">
                <div>
                  <Typography variant="small" className="font-medium text-primary">
                    Điểm số:
                  </Typography>
                  <Typography variant="paragraph" className="text-purple-600 dark:text-purple-400">
                    {previewDialog.question.points || 5} điểm
                  </Typography>
                </div>
                <div>
                  <Typography variant="small" className="font-medium text-primary">
                    Thứ tự:
                  </Typography>
                  <Typography variant="paragraph" className="text-primary">
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
            className="btn-secondary"
          >
            Đóng
          </Button>
        </DialogFooter>
      </Dialog>
    </>
  );
};

export default QuestionTable;