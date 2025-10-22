import React from 'react';
import {
  Typography,
  Input,
  Textarea,
  Card,
  CardBody,
  Switch,
  Alert,
} from '@material-tailwind/react';
import {
  CheckCircleIcon,
} from '@heroicons/react/24/outline';

/**
 * Component: Phần nhập đáp án và options
 * - MULTIPLE_CHOICE: 4 options cố định (2x2 grid)
 * - TRANSLATE: Textarea
 * - FILL_BLANK: Input
 */
const QuestionAnswerSection = ({ 
  questionType, 
  correctAnswer, 
  options, 
  errors,
  onCorrectAnswerChange,
  onOptionsChange 
}) => {
  const handleOptionChange = (index, field, value) => {
    const newOptions = [...options];
    newOptions[index][field] = value;
    
    // If marking as correct, unmark others
    if (field === 'isCorrect' && value === true) {
      newOptions.forEach((opt, i) => {
        if (i !== index) opt.isCorrect = false;
      });
    }
    
    onOptionsChange(newOptions);
  };

  // ✅ FOR MULTIPLE_CHOICE - 4 FIXED OPTIONS (2x2 GRID)
  if (questionType === 'MULTIPLE_CHOICE') {
    return (
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <Typography variant="small" className="font-semibold text-primary">
            Các lựa chọn <span className="text-red-500">*</span>
          </Typography>
          <Typography variant="small" className="text-tertiary">
            4 lựa chọn cố định
          </Typography>
        </div>

        {errors.options && (
          <Alert color="red" className="py-2 bg-red-50 dark:bg-red-900/20">
            <Typography variant="small" className="text-red-600">{errors.options}</Typography>
          </Alert>
        )}

        {/* ✨ 2x2 GRID LAYOUT */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {options.map((option, index) => (
            <Card 
              key={index} 
              className={`border-2 transition-all ${
                option.isCorrect 
                  ? 'border-green-500 bg-green-50 dark:bg-green-900/20' 
                  : 'border-primary bg-secondary'
              }`}
            >
              <CardBody className="p-4">
                <div className="space-y-3">
                  {/* Option Label */}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <div className="flex items-center justify-center w-8 h-8 bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 rounded-full font-bold text-sm">
                        {String.fromCharCode(65 + index)}
                      </div>
                      <Typography variant="small" className="font-semibold text-primary">
                        Lựa chọn {String.fromCharCode(65 + index)}
                      </Typography>
                    </div>
                    {option.isCorrect && (
                      <CheckCircleIcon className="h-5 w-5 text-green-500" />
                    )}
                  </div>
                  
                  {/* Option Input */}
                  <Textarea
                    value={option.optionText}
                    onChange={(e) => handleOptionChange(index, 'optionText', e.target.value)}
                    placeholder={`Nhập nội dung lựa chọn ${String.fromCharCode(65 + index)}`}
                    className="bg-white dark:bg-gray-900"
                    color="purple"
                    rows={2}
                  />
                  
                  {/* Correct Answer Toggle */}
                  <div className="flex items-center gap-2 pt-2 border-t border-primary">
                    <Switch
                      checked={option.isCorrect}
                      onChange={(e) => handleOptionChange(index, 'isCorrect', e.target.checked)}
                      color="green"
                      className="checked:bg-green-500"
                    />
                    <Typography 
                      variant="small" 
                      className={option.isCorrect ? "text-green-600 dark:text-green-400 font-semibold" : "text-tertiary"}
                    >
                      {option.isCorrect ? "✓ Đáp án đúng" : "Đánh dấu là đáp án đúng"}
                    </Typography>
                  </div>
                </div>
              </CardBody>
            </Card>
          ))}
        </div>

        {/* Summary */}
        <div className="bg-tertiary p-4 rounded-lg border border-primary">
          <div className="flex items-center justify-between">
            <Typography variant="small" className="text-primary font-medium">
              Đáp án đã chọn:
            </Typography>
            {options.some(opt => opt.isCorrect) ? (
              <div className="flex items-center gap-2">
                <div className="w-6 h-6 bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 rounded-full font-bold text-sm flex items-center justify-center">
                  {String.fromCharCode(65 + options.findIndex(opt => opt.isCorrect))}
                </div>
                <Typography variant="small" className="text-green-600 dark:text-green-400 font-semibold">
                  {options.find(opt => opt.isCorrect)?.optionText || 'Chưa nhập'}
                </Typography>
              </div>
            ) : (
              <Typography variant="small" className="text-red-500">
                ⚠️ Chưa chọn đáp án đúng
              </Typography>
            )}
          </div>
        </div>
      </div>
    );
  }

  // ✅ FOR TRANSLATE - TEXTAREA
  if (questionType === 'TRANSLATE') {
    return (
      <div>
        <Typography
          variant="small"
          className="mb-2 font-semibold text-primary"
        >
          Câu dịch đúng <span className="text-red-500">*</span>
        </Typography>
        <Textarea
          value={correctAnswer}
          onChange={(e) => onCorrectAnswerChange(e.target.value)}
          placeholder="Nhập câu dịch hoàn chỉnh. Có thể nhập nhiều đáp án đúng, cách nhau bởi dấu |"
          error={!!errors.correctAnswer}
          className="bg-secondary"
          color="purple"
          rows={4}
        />
        {errors.correctAnswer && (
          <Typography variant="small" color="red" className="mt-1 flex items-center gap-1">
            <span>⚠️</span> {errors.correctAnswer}
          </Typography>
        )}
        <Typography variant="small" className="text-tertiary mt-2">
          💡 Ví dụ: "Tôi đi học mỗi ngày|Tôi đến trường mỗi ngày" (chấp nhận cả 2 đáp án)
        </Typography>
      </div>
    );
  }

  // ✅ FOR FILL_BLANK - INPUT
  return (
    <div>
      <Typography
        variant="small"
        className="mb-2 font-semibold text-primary"
      >
        Đáp án đúng <span className="text-red-500">*</span>
      </Typography>
      <Input
        value={correctAnswer}
        onChange={(e) => onCorrectAnswerChange(e.target.value)}
        placeholder="Nhập đáp án đúng. Nhiều đáp án cách nhau bởi dấu |"
        error={!!errors.correctAnswer}
        className="bg-secondary"
        color="purple"
        size="lg"
      />
      {errors.correctAnswer && (
        <Typography variant="small" color="red" className="mt-1 flex items-center gap-1">
          <span>⚠️</span> {errors.correctAnswer}
        </Typography>
      )}
      <Typography variant="small" className="text-tertiary mt-2">
        💡 Ví dụ: "goes|is going|does go" (chấp nhận nhiều đáp án đúng)
      </Typography>
    </div>
  );
};

export default QuestionAnswerSection;